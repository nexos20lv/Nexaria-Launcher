// ============================================================
// Nexaria Launcher - Minecraft Game Launcher
// ============================================================
const path = require('path')
const fs = require('fs')
const { Client } = require('minecraft-launcher-core')
const { downloadGame, fetchServerInfo, getGameDir } = require('./downloader')
const { ensureJava } = require('./java')
const diagnostics = require('./diagnostics')
const { fetchWithRetry } = require('./net')

const launcher = new Client()

const logToUI = (mainWindow, data, type = 'log') => {
    if (mainWindow && !mainWindow.isDestroyed()) {
        mainWindow.webContents.send('game:log', { data, type })
    }
}

/**
 * Download game files then launch Minecraft
 */
async function launchGame({ account, settings, mainWindow }) {
    const gameDir = settings.gameDir || getGameDir()

    // 1 — Récupérer les infos du serveur (version, loader)
    mainWindow.webContents.send('game:progress', {
        type: 'info', message: 'Récupération des informations...', percent: 0,
    })

    const info = await fetchServerInfo()
    const version = info?.mc_version || settings.serverVersion || '1.21.11'
    const loader = info?.loader || 'vanilla'
    const loaderVersion = info?.loader_version || ''

    // 2 — Télécharger / vérifier les fichiers (mods, config, etc.)
    await downloadGame(version, (progress) => {
        // On ne transmet pas 'complete' ici pour garder la barre affichée
        // jusqu'au lancement réel de Minecraft
        if (progress.type !== 'complete') {
            mainWindow.webContents.send('game:progress', progress)
        }
    })

    // 2.5 — Installation automatique du mod loader si nécessaire
    if (loader === 'fabric') {
        mainWindow.webContents.send('game:progress', {
            type: 'info', message: 'Installation de Fabric...', percent: 0,
        })
        await ensureFabricJson(gameDir, version, loaderVersion)
    }

    if (loader === 'forge') {
        mainWindow.webContents.send('game:progress', {
            type: 'info', message: 'Installation de Forge...', percent: 0,
        })
        await ensureForgeJson(gameDir, version, loaderVersion)
    }

    // 3 — Préparer les options de lancement
    const versionData = buildVersionData(version, loader, loaderVersion)

    const opts = {
        authorization: {
            access_token: account.accessToken,
            client_token: '',
            uuid: account.uuid,
            name: account.username,
            user_properties: '{}',
        },
        root: gameDir,
        version: versionData,
        memory: {
            max: `${settings.ram || 2048}M`,
            min: '512M',
        },
        javaPath: settings.javaPath || await ensureJava(version, progress => {
            mainWindow.webContents.send('game:progress', progress)
            if (progress.message) {
                logToUI(mainWindow, progress.message, progress.type === 'error' ? 'error' : 'info')
            }
        }),
        window: {
            width: 1280,
            height: 720,
            fullscreen: settings.fullscreen || false,
        },
        jvmArgs: (() => {
            const os = require('os');
            const userArgs = settings.jvmArgs ? settings.jvmArgs.split(' ').map(arg => arg.trim()).filter(arg => arg !== '' && !/[|&;$><`\\]/.test(arg)) : [];

            // Nexaria Smart Optimization (Aikar's Flags adaptation)
            // Memory to allocate in MB
            const memAllocated = settings.ram || 2048;
            const gcFlags = [];

            // Only apply advanced GC flags if memory is large enough and user didn't override GC
            const hasCustomGC = userArgs.some(arg => arg.includes('UseG1GC') || arg.includes('UseZGC') || arg.includes('UseParallelGC'));

            if (!hasCustomGC && memAllocated >= 2000) {
                gcFlags.push('-XX:+UseG1GC');
                gcFlags.push('-XX:+ParallelRefProcEnabled');
                gcFlags.push('-XX:MaxGCPauseMillis=200');
                gcFlags.push('-XX:+UnlockExperimentalVMOptions');
                gcFlags.push('-XX:+DisableExplicitGC');
                gcFlags.push('-XX:G1NewSizePercent=30');
                gcFlags.push('-XX:G1MaxNewSizePercent=40');
                gcFlags.push('-XX:G1HeapRegionSize=8M');
                gcFlags.push('-XX:G1ReservePercent=20');
                gcFlags.push('-XX:G1HeapWastePercent=5');
                gcFlags.push('-XX:G1MixedGCCountTarget=4');
                gcFlags.push('-XX:InitiatingHeapOccupancyPercent=15');
                gcFlags.push('-XX:G1MixedGCLiveThresholdPercent=90');
                gcFlags.push('-XX:G1RSetUpdatingPauseTimePercent=5');
                gcFlags.push('-XX:SurvivorRatio=32');
                gcFlags.push('-XX:+PerfDisableSharedMem');
                gcFlags.push('-XX:MaxTenuringThreshold=1');

                // Adaptive GC threads based on OS cores
                const cpus = os.cpus().length;
                if (cpus > 1) {
                    const parallelCpus = Math.max(2, Math.min(cpus, 8)); // Prevent excessive GC threads
                    gcFlags.push(`-XX:ParallelGCThreads=${parallelCpus}`);
                }
            } else if (!hasCustomGC && memAllocated < 2000) {
                // Low memory optimized flags
                gcFlags.push('-XX:+UseZGC');
                gcFlags.push('-ZCompiler');
            }

            return [...gcFlags, ...userArgs];
        })(),
        overrides: {
            gameDirectory: gameDir,
        },
    }

    // Security check for javaPath
    if (opts.javaPath && !fs.existsSync(opts.javaPath)) {
        throw new Error(`Le chemin Java spécifié est invalide ou introuvable : ${opts.javaPath}`)
    }

    // 4 — Lancer Minecraft
    // Nettoyage défensif des listeners entre deux lancements
    ;['progress', 'download-status', 'download-progress', 'debug', 'data', 'command-line', 'close']
        .forEach((eventName) => launcher.removeAllListeners(eventName))

    let launchedSent = false
    const signalLaunch = () => {
        if (launchedSent) return
        launchedSent = true
        mainWindow.webContents.send('game:launched', { status: 'launched' })
        mainWindow.webContents.send('game:progress', { type: 'complete', message: 'Minecraft lancé !', percent: 100 })
        console.log('[Launcher] Minecraft a démarré avec succès.')

        if (!settings.keepLauncherOpen) {
            setTimeout(() => mainWindow.minimize(), 3000)
        }
    }

    // On écoute les événements de progression de MCLC (assets, libraries, etc.)
    launcher.on('progress', (e) => {
        const percent = (e.total > 0) ? Math.round((e.task / e.total) * 100) : 0
        mainWindow.webContents.send('game:progress', {
            type: 'file',
            message: `Préparation : ${e.type} (${percent}%)`,
            percent: percent,
        })
    })

    launcher.on('download-status', (e) => {
        mainWindow.webContents.send('game:progress', {
            type: 'info',
            message: `Téléchargement : ${e.type}...`,
            percent: 0,
        })
    })

    launcher.on('download-progress', (e) => {
        const percent = Math.round(e.percent)
        mainWindow.webContents.send('game:progress', {
            type: 'file',
            message: `Téléchargement : ${e.type} (${percent}%)`,
            percent: percent,
        })
    })

    launcher.on('debug', (data) => {
        console.log('[MC Debug]', data)
        logToUI(mainWindow, data, 'debug')
    })
    launcher.on('data', (data) => {
        console.log('[MC]', data)
        logToUI(mainWindow, data, 'log')
        signalLaunch()
    })
    launcher.on('command-line', (data) => {
        logToUI(mainWindow, `Commande: ${data}`, 'debug')
    })

    launcher.on('close', (code) => {
        console.log(`[Launcher] Le process Minecraft s'est terminé avec le code : ${code}`)
        mainWindow.webContents.send('game:launched', { status: 'closed', code })

        const { setActivity, resetTimestamp } = require('./discord')
        resetTimestamp()
        setActivity('Dans le menu')

        if (code !== 0) {
            const crashDir = opts.overrides?.gameDirectory || getGameDir()
            const rawLog = getLatestCrashLog(crashDir)
            const analysis = diagnostics.analyze(crashDir, rawLog)

            mainWindow.webContents.send('game:crashed', {
                log: rawLog,
                analysis: analysis
            })
        }
    })

    // Lancement effectif
    try {
        console.log('[Launcher] Vérification de sécurité avec le serveur (NexariaAuth)...')
        mainWindow.webContents.send('game:progress', { type: 'info', message: 'Autorisation sur le serveur Minecraft...' })

        const authRes = await fetchWithRetry('http://mc.nemesius.com:25566/auth', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ access_token: account.accessToken })
        }, { retries: 2, timeoutMs: 5000 });

        if (!authRes.ok) {
            throw new Error(`Le serveur a refusé l'accès. (Erreur HTTP ${authRes.status})`);
        }

        console.log('[Launcher] Appel de launcher.launch()...')
        await launcher.launch(opts)

        // On n'envoie pas tout de suite "launched" car MCLC peut encore travailler
        console.log('[Launcher] Le process est créé, on attend les premières données du jeu...')

        // Sécurité : si après 15 secondes on n'a pas de data mais que le child tourne encore
        setTimeout(() => {
            if (!launchedSent) signalLaunch()
        }, 15000)

    } catch (err) {
        console.error('[Launch Error]', err)
        mainWindow.webContents.send('game:launched', { status: 'closed', code: 1 })
        mainWindow.webContents.send('game:progress', { type: 'error', message: `Erreur au lancement: ${err.message}` })
    }
}

/**
 * Construire la config version selon le loader
 */
function buildVersionData(mcVersion, loader, loaderVersion) {
    if (loader === 'fabric') {
        // Si on utilise Fabric, MCLC cherche un dossier 'fabric-loader-...'
        // On utilise souvent cet identifiant pour que MCLC sache quoi charger
        return {
            number: mcVersion,
            type: 'release',
            custom: loaderVersion
                ? `fabric-loader-${loaderVersion}-${mcVersion}`
                : `fabric-loader-${mcVersion}`
        }
    }

    if (loader === 'forge') {
        return {
            number: mcVersion,
            type: 'release',
            custom: loaderVersion
                ? `${mcVersion}-forge-${loaderVersion}`
                : mcVersion,
        }
    }

    if (loader === 'neoforge') {
        return {
            number: mcVersion,
            type: 'release',
            custom: loaderVersion ? `neoforge-${loaderVersion}` : mcVersion,
        }
    }

    // Vanilla
    return { number: mcVersion, type: 'release' }
}

/**
 * Récupère automatiquement le JSON de Forge et installe le JAR universel s'ils sont manquants.
 * MCLC attend le fichier : <gameDir>/versions/<mcVersion>-forge-<loaderVersion>/<mcVersion>-forge-<loaderVersion>.json
 * Le JAR universel Forge (library) est extrait de l'installeur et placé dans le dossier libraries.
 */
async function ensureForgeJson(gameDir, mcVersion, loaderVersion) {
    if (!loaderVersion) return // Vanilla fallback, rien à faire

    const versionId = `${mcVersion}-forge-${loaderVersion}`
    const versionDir = path.join(gameDir, 'versions', versionId)
    const jsonPath = path.join(versionDir, `${versionId}.json`)

    // On vérifie aussi le JAR universel : MCLC peut laisser un fichier corrompu
    // (très petit) en cas d'échec de téléchargement. On reconsidère comme absent
    // si le JAR pèse moins de 1 Mo.
    const MIN_FORGE_JAR_SIZE = 1 * 1024 * 1024 // 1 Mo
    // On calcule à l'avance les deux variantes possibles du chemin library
    const legacyLibPath = path.join(
        gameDir, 'libraries', 'net', 'minecraftforge', 'forge',
        `${mcVersion}-${loaderVersion}-${mcVersion}`,
        `forge-${mcVersion}-${loaderVersion}-${mcVersion}.jar`
    )
    const modernLibPath = path.join(
        gameDir, 'libraries', 'net', 'minecraftforge', 'forge',
        `${mcVersion}-${loaderVersion}`,
        `forge-${mcVersion}-${loaderVersion}.jar`
    )
    const forgeLibOk = [legacyLibPath, modernLibPath].some(
        p => fs.existsSync(p) && fs.statSync(p).size >= MIN_FORGE_JAR_SIZE
    )

    if (fs.existsSync(jsonPath) && forgeLibOk) return

    if (!fs.existsSync(versionDir)) fs.mkdirSync(versionDir, { recursive: true })

    // Forge Maven utilise deux formats d'URL selon la version :
    //   - Moderne (>=1.13) : forge-{mc}-{forge}-installer.jar
    //   - Ancienne (<1.13)  : forge-{mc}-{forge}-{mc}-installer.jar  (suffixe mc en plus)
    const mavenRoot = `https://maven.minecraftforge.net/net/minecraftforge/forge`
    const candidateUrls = [
        // Format moderne
        `${mavenRoot}/${mcVersion}-${loaderVersion}/forge-${mcVersion}-${loaderVersion}-installer.jar`,
        // Format legacy (ex: 1.8.9 / 1.7.10)
        `${mavenRoot}/${mcVersion}-${loaderVersion}-${mcVersion}/forge-${mcVersion}-${loaderVersion}-${mcVersion}-installer.jar`,
    ]

    let installerBuf = null
    for (const url of candidateUrls) {
        try {
            const res = await fetchWithRetry(url, {}, { retries: 1, timeoutMs: 20000 })
            if (!res.ok) continue
            installerBuf = Buffer.from(await res.arrayBuffer())
            console.log(`[Forge] Installeur téléchargé depuis : ${url}`)
            break
        } catch (_) { /* essayer l'URL suivante */ }
    }

    if (!installerBuf) {
        throw new Error(`Impossible de télécharger l'installeur Forge pour ${mcVersion}-${loaderVersion} (toutes les URLs ont échoué)`)
    }

    const AdmZip = require('adm-zip')
    const zip = new AdmZip(installerBuf)

    // 1. Extraire install_profile.json → version JSON pour MCLC
    const profileEntry = zip.getEntry('install_profile.json')
    if (!profileEntry) throw new Error('install_profile.json introuvable dans l\'installeur Forge')

    const profile = JSON.parse(profileEntry.getData().toString('utf8'))
    // Anciens Forge : versionInfo contient le launcher JSON
    // Nouveaux Forge (>=1.13) : le profil lui-même est le launcher JSON
    const versionJson = profile.versionInfo || profile
    fs.writeFileSync(jsonPath, JSON.stringify(versionJson, null, 2))
    console.log(`[Forge] JSON installé pour ${versionId}`)

    // 2. Extraire le JAR universel Forge embarqué dans l'installeur
    //    et le placer dans le dossier libraries au bon chemin Maven.
    //    Sans cela, MCLC tente de le télécharger depuis Maven où il n'existe pas (404).
    const installInfo = profile.install || {}
    const mavenPath = installInfo.path // ex: "net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9"
    const embeddedJarName = installInfo.filePath // ex: "forge-1.8.9-11.15.1.2318-1.8.9-universal.jar"

    if (mavenPath && embeddedJarName) {
        // Convertir le chemin Maven en chemin filesystem
        // "group:artifact:version" → "group/artifact/version/artifact-version.jar"
        const [group, artifact, ver] = mavenPath.split(':')
        const libRelPath = path.join(
            group.replace(/\./g, '/'),
            artifact,
            ver,
            `${artifact}-${ver}.jar`
        )
        const libAbsPath = path.join(gameDir, 'libraries', libRelPath)

        if (!fs.existsSync(libAbsPath) || fs.statSync(libAbsPath).size < MIN_FORGE_JAR_SIZE) {
            const universalEntry = zip.getEntry(embeddedJarName)
            if (universalEntry) {
                fs.mkdirSync(path.dirname(libAbsPath), { recursive: true })
                fs.writeFileSync(libAbsPath, universalEntry.getData())
                console.log(`[Forge] JAR universel extrait vers : ${libAbsPath}`)
            } else {
                console.warn(`[Forge] JAR universel "${embeddedJarName}" non trouvé dans l'installeur.`)
            }
        }
    }

    // 3. Télécharger toutes les bibliothèques Forge manquantes AVANT le lancement.
    //    MCLC ne gère pas correctement certaines libs (Mojang CDN pour "url: default",
    //    Forge Maven ancienne convention, etc.) et lance le jeu même si elles manquent.
    const MOJANG_LIB_CDN = 'https://libraries.minecraft.net'
    const libs = versionJson.libraries || []

    await Promise.all(libs.map(async (lib) => {
        try {
            const name = lib.name
            const parts = name.split(':')
            if (parts.length < 3) return
            const [grp, art, libVer, classifier] = parts
            const jarFile = classifier ? `${art}-${libVer}-${classifier}.jar` : `${art}-${libVer}.jar`
            const relPath = path.join(grp.replace(/\./g, '/'), art, libVer, jarFile)
            const destPath = path.join(gameDir, 'libraries', relPath)

            if (fs.existsSync(destPath) && fs.statSync(destPath).size > 0) return

            let baseUrl
            if (!lib.url || lib.url === 'default') {
                baseUrl = MOJANG_LIB_CDN
            } else {
                baseUrl = lib.url.replace(/\/$/, '')
            }
            const downloadUrl = `${baseUrl}/${relPath.replace(/\\/g, '/')}`

            const res = await fetchWithRetry(downloadUrl, {}, { retries: 2, timeoutMs: 15000 })
            if (!res.ok) {
                console.warn(`[Forge] Lib ${name} : HTTP ${res.status} — ${downloadUrl}`)
                return
            }
            const buf = Buffer.from(await res.arrayBuffer())
            fs.mkdirSync(path.dirname(destPath), { recursive: true })
            fs.writeFileSync(destPath, buf)
            console.log(`[Forge] Lib téléchargée : ${name}`)
        } catch (e) {
            console.warn(`[Forge] Échec lib ${lib.name} : ${e.message}`)
        }
    }))

    console.log(`[Forge] Installation terminée pour ${versionId}`)
}

/**
 * Récupère automatiquement le JSON de Fabric s'il est manquant
 */
async function ensureFabricJson(gameDir, mcVersion, loaderVersion) {
    const versionId = `fabric-loader-${loaderVersion}-${mcVersion}`
    const versionDir = path.join(gameDir, 'versions', versionId)
    const jsonPath = path.join(versionDir, `${versionId}.json`)

    if (fs.existsSync(jsonPath)) return

    if (!fs.existsSync(versionDir)) fs.mkdirSync(versionDir, { recursive: true })

    const url = `https://meta.fabricmc.net/v2/versions/loader/${mcVersion}/${loaderVersion}/profile/json`
    try {
        const res = await fetchWithRetry(url, {}, { retries: 2, timeoutMs: 10000 })
        if (!res.ok) throw new Error(`Fabric Meta API a répondu HTTP ${res.status}`)
        const json = await res.json()
        fs.writeFileSync(jsonPath, JSON.stringify(json, null, 2))
        console.log(`[Fabric] JSON généré pour ${versionId}`)
    } catch (err) {
        throw new Error(`Impossible d'installer Fabric automatiquement : ${err.message}`)
    }
}

/**
 * Retrieves the latest crash report or log
 */
function getLatestCrashLog(gameDir) {
    try {
        const crashDir = path.join(gameDir, 'crash-reports')
        if (fs.existsSync(crashDir)) {
            const files = fs.readdirSync(crashDir)
                .filter(f => f.endsWith('.txt'))
                .map(f => ({ name: f, time: fs.statSync(path.join(crashDir, f)).mtime.getTime() }))
                .sort((a, b) => b.time - a.time)

            if (files.length > 0) {
                const latest = path.join(crashDir, files[0].name)
                return fs.readFileSync(latest, 'utf8')
            }
        }
        // Fallback to latest.log
        const latestLog = path.join(gameDir, 'logs', 'latest.log')
        if (fs.existsSync(latestLog)) {
            const content = fs.readFileSync(latestLog, 'utf8')
            // Return last 150 lines just to give context without freezing the UI
            return "--- LATEST LOG ---\n" + content.toString().split('\n').slice(-150).join('\n')
        }
    } catch (err) {
        console.error('Failed to read crash log:', err)
        return `Impossible de lire le rapport de plantage : ${err.message}`
    }
    return 'Le jeu s\'est fermé anormalement, mais aucun rapport de plantage n\'a été trouvé.'
}

module.exports = { launchGame, downloadGame, getGameDir }
