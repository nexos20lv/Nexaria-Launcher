// ============================================================
// Nexaria Launcher - Minecraft Game Launcher
// ============================================================
const path = require('path')
const fs = require('fs')
const { Client } = require('minecraft-launcher-core')
const { downloadGame, fetchServerInfo, getGameDir } = require('./downloader')
const { ensureJava } = require('./java')

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

    // 2.5 — Installation automatique de Fabric si nécessaire
    if (loader === 'fabric') {
        mainWindow.webContents.send('game:progress', {
            type: 'info', message: 'Installation de Fabric...', percent: 0,
        })
        await ensureFabricJson(gameDir, version, loaderVersion)
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
        overrides: {
            gameDirectory: gameDir,
            // detached: true, // Désactivé car peut causer des soucis de détection sur Mac
        },
    }

    // 4 — Lancer Minecraft
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

    launcher.on('debug', (e) => console.log('[MC Debug]', e))
    launcher.on('data', (e) => console.log('[MC]', e))

    launcher.on('close', (code) => {
        mainWindow.webContents.send('game:launched', { status: 'closed', code })
        if (code !== 0) {
            const crashLog = getLatestCrashLog(gameDir)
            if (crashLog) {
                mainWindow.webContents.send('game:crashed', crashLog)
            }
        }
    })

    // Lancement effectif
    try {
        console.log('[Launcher] Appel de launcher.launch()...')
        const child = await launcher.launch(opts)

        // On n'envoie pas tout de suite "launched" car MCLC peut encore travailler
        console.log('[Launcher] Le process est créé, on attend les premières données du jeu...')

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

        launcher.on('debug', (data) => logToUI(mainWindow, data, 'debug'))
        launcher.on('download-status', (data) => logToUI(mainWindow, `Téléchargement: ${data.type} (${data.current}/${data.total})`, 'info'))
        launcher.on('command-line', (data) => logToUI(mainWindow, `Commande: ${data}`, 'debug'))

        launcher.on('data', (data) => {
            logToUI(mainWindow, data, 'log')
            signalLaunch()
        })

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
 * Récupère automatiquement le JSON de Fabric s'il est manquant
 */
async function ensureFabricJson(gameDir, mcVersion, loaderVersion) {
    const fetch = require('node-fetch')
    const versionId = `fabric-loader-${loaderVersion}-${mcVersion}`
    const versionDir = path.join(gameDir, 'versions', versionId)
    const jsonPath = path.join(versionDir, `${versionId}.json`)

    if (fs.existsSync(jsonPath)) return

    if (!fs.existsSync(versionDir)) fs.mkdirSync(versionDir, { recursive: true })

    const url = `https://meta.fabricmc.net/v2/versions/loader/${mcVersion}/${loaderVersion}/profile/json`
    try {
        const res = await fetch(url)
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

module.exports = { launchGame, downloadGame }
