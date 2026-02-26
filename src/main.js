// ============================================================
// Nexaria Launcher - Main Process
// ============================================================
const { app, BrowserWindow, ipcMain, shell, safeStorage, dialog, protocol } = require('electron')
const { autoUpdater } = require('electron-updater')
const log = require('electron-log')
log.transports.file.level = 'info'
autoUpdater.logger = log
const path = require('path')
const { getStore } = require('./store')
const { authenticate, verify, logout, uploadSkin, uploadCape } = require('./launcher/auth')
const { launchGame, downloadGame, getGameDir: getDefaultGameDir } = require('./launcher/game')
const { getServerStatus } = require('./launcher/server')
const { fetchNews } = require('./launcher/news')
const { initRPC, setActivity, resetTimestamp, destroyRPC } = require('./launcher/discord')
log.info('Discord RPC module loaded:', typeof initRPC)
autoUpdater.autoDownload = true

// ── Config & persistent store ─────────────────────────────
const store = getStore()

// Register custom protocol for local assets
protocol.registerSchemesAsPrivileged([
    { scheme: 'asset', privileges: { secure: true, standard: true, supportFetchAPI: true } }
])

function isPathInside(baseDir, targetPath) {
    const base = path.resolve(baseDir)
    const target = path.resolve(targetPath)
    const relative = path.relative(base, target)
    return relative === '' || (!relative.startsWith('..') && !path.isAbsolute(relative))
}

// ── Windows ───────────────────────────────────────────────
let mainWindow
let splashWindow

function createSplashWindow() {
    splashWindow = new BrowserWindow({
        width: 400,
        height: 500,
        transparent: true,
        frame: false,
        alwaysOnTop: true,
        resizable: false,
        show: false,
        icon: path.join(__dirname, '../assets/icon.png'),
        webPreferences: {
            preload: path.join(__dirname, 'splash-preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: true,
        }
    })

    splashWindow.loadFile(path.join(__dirname, 'renderer/splash.html'))

    splashWindow.once('ready-to-show', () => {
        splashWindow.show()
    })
}

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1200,
        height: 750,
        minWidth: 1200,
        minHeight: 750,
        frame: false,
        transparent: true,
        backgroundColor: '#00000000',
        resizable: false,
        show: false, // Caché initialement le temps du splash
        icon: path.join(__dirname, '../assets/icon.png'),
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
        },
    })

    mainWindow.loadFile(path.join(__dirname, 'renderer/index.html'))

    // On ne montre pas tout de suite, on délègue ça au chargement terminé
    if (process.argv.includes('--dev')) {
        mainWindow.webContents.openDevTools({ mode: 'detach' })
    }

    // Setup screenshot watcher
    setupScreenshotWatcher()
}

let screenshotWatcher = null
function setupScreenshotWatcher() {
    const fs = require('fs')
    const gameDir = store.get('settings.gameDir') || getDefaultGameDir()
    const screenshotsDir = path.join(gameDir, 'screenshots')

    if (!fs.existsSync(screenshotsDir)) {
        try { fs.mkdirSync(screenshotsDir, { recursive: true }) } catch (e) { }
    }

    if (screenshotWatcher) screenshotWatcher.close()

    screenshotWatcher = fs.watch(screenshotsDir, (eventType, filename) => {
        if (mainWindow && !mainWindow.isDestroyed()) {
            mainWindow.webContents.send('screenshots:updated')
        }
    })
}

app.whenReady().then(() => {
    // Register the handler for our custom protocol
    protocol.registerFileProtocol('asset', (request, callback) => {
        try {
            // Security: Only allow files within the app directory or the game directory
            const parsed = new URL(request.url)
            let assetPath = decodeURIComponent(parsed.pathname || '')
            if (process.platform === 'win32' && assetPath.startsWith('/')) {
                assetPath = assetPath.slice(1)
            }

            const absolutePath = path.resolve(assetPath)
            const gameDir = store.get('settings.gameDir') || getDefaultGameDir()
            const allowedDirs = [__dirname, path.join(__dirname, '..'), gameDir].map(dir => path.resolve(dir))

            const isAllowed = allowedDirs.some(dir => isPathInside(dir, absolutePath))

            if (!isAllowed) {
                log.warn('[Security] Blocked asset protocol access to:', absolutePath)
                return callback({ error: -10 /* net::ERR_ACCESS_DENIED */ })
            }

            return callback(absolutePath)
        } catch (error) {
            log.error('Failed to handle asset protocol:', error)
            return callback({ error: -2 /* net::ERR_FAILED */ })
        }
    })

    createSplashWindow()
    // createWindow() // Déplacé dans finishSplash pour éviter le clignotement pendant l'update

    initRPC()

    let updateInProgress = false

    autoUpdater.on('checking-for-update', () => {
        if (splashWindow && !splashWindow.isDestroyed()) {
            splashWindow.webContents.send('splash:status', 'RECHERCHE DE MISES À JOUR...')
        }
    })

    autoUpdater.on('update-available', () => {
        updateInProgress = true
        if (splashWindow && !splashWindow.isDestroyed()) {
            splashWindow.webContents.send('splash:status', 'MISE À JOUR TROUVÉE...')
        }
    })

    autoUpdater.on('update-not-available', () => {
        if (splashWindow && !splashWindow.isDestroyed()) {
            splashWindow.webContents.send('splash:status', 'VÉRIFICATION SYSTÈME...')
        }
        finishSplash()
    })

    autoUpdater.on('error', (err) => {
        log.error('Erreur de mise à jour:', err)
        updateInProgress = false
        finishSplash()
    })

    autoUpdater.on('download-progress', (progressObj) => {
        if (splashWindow && !splashWindow.isDestroyed()) {
            splashWindow.webContents.send('splash:status', `TÉLÉCHARGEMENT... ${Math.round(progressObj.percent)}%`)
        }
    })

    autoUpdater.on('update-downloaded', () => {
        if (splashWindow && !splashWindow.isDestroyed()) {
            splashWindow.webContents.send('splash:status', 'REDÉMARRAGE...')
        }
        setTimeout(() => {
            setImmediate(() => {
                destroyRPC()
                app.removeAllListeners('window-all-closed')
                if (splashWindow && !splashWindow.isDestroyed()) splashWindow.destroy()
                if (mainWindow && !mainWindow.isDestroyed()) mainWindow.destroy()
                autoUpdater.quitAndInstall(true, true)
            })
        }, 1500)
    })

    // Lancement de la vérification
    if (app.isPackaged) {
        autoUpdater.checkForUpdatesAndNotify().catch(err => {
            log.error('Erreur lancement update check:', err)
            finishSplash()
        })
    } else {
        // En mode développement, on skip l'auto-updater car il ne se lance pas
        log.info("Mode développement détecté : saut de la recherche de mise à jour.")
        if (splashWindow && !splashWindow.isDestroyed()) {
            splashWindow.webContents.send('splash:status', 'VÉRIFICATION SYSTÈME (DEV)...')
        }
        setTimeout(finishSplash, 1000)
    }

    function finishSplash() {
        if (updateInProgress) return

        // Afficher la page principale après un léger délai pour la lisibilité
        setTimeout(() => {
            if (splashWindow && !splashWindow.isDestroyed()) {
                // On crée le main window AVANT de fermer le splash pour un effet fluide
                if (!mainWindow || mainWindow.isDestroyed()) {
                    createWindow()

                    // Une fois que le main window est prêt (dom-ready), on cache le splash
                    mainWindow.once('ready-to-show', () => {
                        if (splashWindow && !splashWindow.isDestroyed()) {
                            splashWindow.close()
                        }
                        mainWindow.show()
                        mainWindow.focus()
                    })
                } else {
                    splashWindow.close()
                }
            } else if (!mainWindow || mainWindow.isDestroyed()) {
                createWindow()
                mainWindow.show()
            }
        }, 500)
    }

    // Periodic check every 60 minutes
    setInterval(() => {
        autoUpdater.checkForUpdatesAndNotify()
    }, 60 * 60 * 1000)
})

app.on('before-quit', () => {
    destroyRPC()
})

app.on('window-all-closed', () => app.quit())

// ── Window Controls ───────────────────────────────────────
ipcMain.on('window:minimize', () => mainWindow.minimize())
ipcMain.on('window:close', () => {
    destroyRPC()
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.close()
})

// ── Auth IPC ──────────────────────────────────────────────
ipcMain.handle('auth:login', async (_, { email, password, twoFactorCode }) => {
    try {
        const result = await authenticate(email, password, twoFactorCode)
        if (result.status === 'success') {
            // Chiffrement du token avant sauvegarde
            const isAvail = safeStorage.isEncryptionAvailable()
            const encryptedToken = isAvail
                ? safeStorage.encryptString(result.accessToken).toString('base64')
                : result.accessToken // fallback non sécurisé (rare)

            // Save account
            const accounts = store.get('accounts', [])
            const existing = accounts.findIndex(a => a.uuid === result.user.uuid)
            const account = {
                uuid: result.user.uuid,
                username: result.user.username,
                email,
                accessTokenEncrypted: encryptedToken,
                role: result.user.role,
                money: result.user.money,
                savedAt: new Date().toISOString(),
            }
            if (existing >= 0) {
                accounts[existing] = account
            } else {
                accounts.push(account)
            }
            store.set('accounts', accounts)
            store.set('lastAccount', result.user.uuid)
        }
        return result
    } catch (err) {
        return { status: 'error', message: err.message }
    }
})

ipcMain.handle('auth:verify', async (_, { accessToken }) => {
    try {
        return await verify(accessToken)
    } catch (err) {
        return { status: 'error', message: err.message }
    }
})

ipcMain.handle('auth:logout', async (_, { accessToken, uuid }) => {
    try {
        await logout(accessToken)
    } catch (_) { }
    const accounts = store.get('accounts', []).filter(a => a.uuid !== uuid)
    store.set('accounts', accounts)
    if (store.get('lastAccount') === uuid) {
        store.delete('lastAccount')
    }
    return { status: 'success' }
})

ipcMain.handle('auth:getAccounts', () => {
    const isAvail = safeStorage.isEncryptionAvailable()
    const accounts = store.get('accounts', [])
    return accounts.map(a => {
        let act = { ...a }
        if (a.accessTokenEncrypted) {
            try {
                act.accessToken = isAvail
                    ? safeStorage.decryptString(Buffer.from(a.accessTokenEncrypted, 'base64'))
                    : a.accessTokenEncrypted
            } catch (e) {
                log.warn('Could not decrypt token for user', a.username, e)
                act.accessToken = a.accessTokenEncrypted
            }
        }
        return act
    })
})

ipcMain.handle('auth:getLastAccount', () => {
    const uuid = store.get('lastAccount')
    if (!uuid) return null

    // Réutilise la logique de décryptage
    const isAvail = safeStorage.isEncryptionAvailable()
    const accounts = store.get('accounts', [])
    const a = accounts.find(acc => acc.uuid === uuid)
    if (!a) return null

    let act = { ...a }
    if (a.accessTokenEncrypted) {
        try {
            act.accessToken = isAvail
                ? safeStorage.decryptString(Buffer.from(a.accessTokenEncrypted, 'base64'))
                : a.accessTokenEncrypted
        } catch (e) {
            log.warn('Could not decrypt token for last user', a.username, e)
            act.accessToken = a.accessTokenEncrypted
        }
    }
    return act
})

// ── Game IPC ──────────────────────────────────────────────
ipcMain.handle('game:launch', async (_, { account, version, settings }) => {
    try {
        await launchGame({ account, version, settings, mainWindow })
        resetTimestamp()
        setActivity('En jeu', `Survie Nexaria`)
        return { status: 'success' }
    } catch (err) {
        return { status: 'error', message: err.message }
    }
})

ipcMain.handle('game:download', async (_, { version }) => {
    try {
        setActivity('Mise à jour en cours...', 'Téléchargement des fichiers')
        await downloadGame(version, (progress) => {
            mainWindow.webContents.send('game:progress', progress)
        })
        setActivity('Dans le menu')
        return { status: 'success' }
    } catch (err) {
        setActivity('Dans le menu')
        return { status: 'error', message: err.message }
    }
})

ipcMain.handle('game:repair', async (_, { version }) => {
    try {
        const { repairGame } = require('./launcher/repair')
        const storeSettings = store.get('settings', {})
        const gameDir = storeSettings.gameDir || require('./launcher/downloader').getGameDir()

        await repairGame({ gameDir, version: version || storeSettings.serverVersion || '1.21.11' })
        return { status: 'success' }
    } catch (err) {
        log.error('Repair error:', err)
        return { status: 'error', message: err.message }
    }
})

ipcMain.handle('settings:get', () => {
    const savedSettings = store.get('settings', {
        ram: 2048,
        javaPath: '',
        gameDir: '',
        autoUpdate: true,
        keepLauncherOpen: true,
        fullscreen: false,
        serverVersion: '1.21.11',
        azuriomUrl: 'https://nexaria.site',
        jvmArgs: '', // Valeur par défaut vide
    })

    // Ajout des versions système pour la section "À propos"
    const versions = {
        app: app.getVersion(),
        electron: process.versions.electron,
        chrome: process.versions.chrome,
        node: process.versions.node,
        os: process.platform
    }

    return { ...savedSettings, versions }
})

ipcMain.handle('settings:set', (_, settings) => {
    store.set('settings', { ...store.get('settings', {}), ...settings })
    return { status: 'success' }
})

// ── Skins & Capes IPC ─────────────────────────────────────
ipcMain.handle('skin:selectFile', async () => {
    const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
        title: 'Sélectionner une image (.png)',
        filters: [{ name: 'Images', extensions: ['png'] }],
        properties: ['openFile']
    })
    if (canceled || filePaths.length === 0) return null
    return filePaths[0]
})

ipcMain.handle('skin:uploadSkin', async (_, { accessToken, filePath }) => {
    try {
        return await uploadSkin(accessToken, filePath)
    } catch (err) {
        log.error('Erreur upload skin:', err)
        return { status: 'error', message: err.message }
    }
})

ipcMain.handle('skin:uploadCape', async (_, { accessToken, filePath }) => {
    try {
        return await uploadCape(accessToken, filePath)
    } catch (err) {
        log.error('Erreur upload cape:', err)
        return { status: 'error', message: err.message }
    }
})

// ── Server Status IPC ─────────────────────────────────────
ipcMain.handle('server:status', async () => {
    try {
        return await getServerStatus()
    } catch (err) {
        return { online: false, players: 0, max: 0 }
    }
})

// ── News IPC ──────────────────────────────────────────────
ipcMain.handle('news:fetch', async () => {
    try {
        return await fetchNews()
    } catch (err) {
        return []
    }
})

// ── Mods Optionnels IPC ───────────────────────────────────
ipcMain.handle('mods:getOptional', async () => {
    const { getModsStatus } = require('./launcher/mods')
    return getModsStatus()
})

ipcMain.handle('mods:toggle', async (_, { modId }) => {
    const { toggleMod } = require('./launcher/mods')
    try {
        return await toggleMod(modId)
    } catch (err) {
        return { status: 'error', message: err.message }
    }
})

// ── screenshots IPC ──────────────────────────────────────
ipcMain.handle('screenshots:list', async () => {
    const ScreenshotManager = require('./launcher/screenshots')
    const manager = new ScreenshotManager(store.get('settings.gameDir') || require('./launcher/downloader').getGameDir())
    return manager.list()
})

ipcMain.handle('screenshots:open', async (_, { fileName }) => {
    const ScreenshotManager = require('./launcher/screenshots')
    const manager = new ScreenshotManager(store.get('settings.gameDir') || require('./launcher/downloader').getGameDir())
    return manager.open(fileName)
})

ipcMain.handle('screenshots:delete', async (_, { fileName }) => {
    const ScreenshotManager = require('./launcher/screenshots')
    const manager = new ScreenshotManager(store.get('settings.gameDir') || require('./launcher/downloader').getGameDir())
    return manager.delete(fileName)
})

ipcMain.handle('screenshots:openFolder', async () => {
    const ScreenshotManager = require('./launcher/screenshots')
    const manager = new ScreenshotManager(store.get('settings.gameDir') || require('./launcher/downloader').getGameDir())
    return manager.openFolder()
})

// ── Troubleshoot IPC ──────────────────────────────────────
ipcMain.handle('troubleshoot:clearCache', async () => {
    const gameDir = store.get('settings.gameDir') || require('./launcher/downloader').getGameDir()
    const cacheDirs = ['assets', 'libraries', 'versions', 'runtime']
    let deletedCount = 0

    for (const dir of cacheDirs) {
        const fullPath = path.join(gameDir, dir)
        if (fs.existsSync(fullPath)) {
            try {
                fs.rmSync(fullPath, { recursive: true, force: true })
                deletedCount++
            } catch (err) {
                log.error(`Failed to delete cache dir ${dir}:`, err)
            }
        }
    }
    return { status: 'success', deletedCount }
})

ipcMain.handle('troubleshoot:resetSettings', async () => {
    store.clear()
    return { status: 'success' }
})

// ── External links ────────────────────────────────────────
ipcMain.on('open:url', (_, url) => {
    try {
        const parsedUrl = new URL(url)
        if (['http:', 'https:'].includes(parsedUrl.protocol)) {
            shell.openExternal(url)
        } else {
            log.warn('[Security] Blocked attempt to open non-http(s) URL:', url)
        }
    } catch (e) {
        log.error('[Security] Invalid URL provided to open:url:', url)
    }
})

ipcMain.on('update:quitAndInstall', () => {
    setImmediate(() => {
        destroyRPC()
        app.removeAllListeners('window-all-closed')
        if (splashWindow && !splashWindow.isDestroyed()) splashWindow.destroy()
        if (mainWindow && !mainWindow.isDestroyed()) mainWindow.destroy()
        autoUpdater.quitAndInstall(true, true)
    })
})
