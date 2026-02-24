// ============================================================
// Nexaria Launcher - Main Process
// ============================================================
const { app, BrowserWindow, ipcMain, shell, safeStorage, dialog } = require('electron')
const { autoUpdater } = require('electron-updater')
const log = require('electron-log')
log.transports.file.level = 'info'
autoUpdater.logger = log
const path = require('path')
const Store = require('electron-store')
const { authenticate, verify, logout, uploadSkin, uploadCape } = require('./launcher/auth')
const { launchGame, downloadGame } = require('./launcher/game')
const { getServerStatus } = require('./launcher/server')
const { fetchNews } = require('./launcher/news')
const { initRPC, setActivity } = require('./launcher/discord')
log.info('Discord RPC module loaded:', typeof initRPC)
autoUpdater.autoDownload = true

// ── Config & persistent store ─────────────────────────────
const store = new Store({
    name: 'nexaria-launcher',
    encryptionKey: 'nexaria-secure-key-2024',
})

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
            nodeIntegration: true,
            contextIsolation: false // Requis simple script ipc pour splash
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
}

app.whenReady().then(() => {
    createSplashWindow()
    createWindow()

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
                app.removeAllListeners('window-all-closed')
                if (splashWindow && !splashWindow.isDestroyed()) splashWindow.destroy()
                if (mainWindow && !mainWindow.isDestroyed()) mainWindow.destroy()
                autoUpdater.quitAndInstall(false, true)
            })
        }, 1500)
    })

    // Lancement de la vérification
    if (app.isPackaged) {
        autoUpdater.checkForUpdatesAndNotify()
    } else {
        // En mode développement, on skip l'auto-updater car il ne se lance pas
        log.info("Mode développement détecté : saut de la recherche de mise à jour.")
        if (splashWindow && !splashWindow.isDestroyed()) {
            splashWindow.webContents.send('splash:status', 'VÉRIFICATION SYSTÈME (DEV)...')
        }
        finishSplash()
    }

    function finishSplash() {
        // Afficher la page principale après un léger délai pour la lisibilité
        setTimeout(() => {
            if (!updateInProgress) {
                if (splashWindow && !splashWindow.isDestroyed()) splashWindow.close()
                if (mainWindow && !mainWindow.isDestroyed()) mainWindow.show()
            }
        }, 1500)
    }

    // Periodic check every 60 minutes
    setInterval(() => {
        autoUpdater.checkForUpdatesAndNotify()
    }, 60 * 60 * 1000)
})
app.on('window-all-closed', () => app.quit())

// ── Window Controls ───────────────────────────────────────
ipcMain.on('window:minimize', () => mainWindow.minimize())
ipcMain.on('window:close', () => mainWindow.close())

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
                act.accessToken = ''
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
            act.accessToken = ''
        }
    }
    return act
})

// ── Game IPC ──────────────────────────────────────────────
ipcMain.handle('game:launch', async (_, { account, version, settings }) => {
    try {
        await launchGame({ account, version, settings, mainWindow })
        setActivity('En jeu', `Survie Nexaria`)
        return { status: 'success' }
    } catch (err) {
        return { status: 'error', message: err.message }
    }
})

ipcMain.handle('game:download', async (_, { version }) => {
    try {
        await downloadGame(version, (progress) => {
            mainWindow.webContents.send('game:progress', progress)
        })
        return { status: 'success' }
    } catch (err) {
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
        azuriomUrl: 'https://nexaria.netlib.re',
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

// ── External links ────────────────────────────────────────
ipcMain.on('open:url', (_, url) => {
    shell.openExternal(url)
})

ipcMain.on('update:quitAndInstall', () => {
    setImmediate(() => {
        app.removeAllListeners('window-all-closed')
        if (splashWindow && !splashWindow.isDestroyed()) splashWindow.destroy()
        if (mainWindow && !mainWindow.isDestroyed()) mainWindow.destroy()
        autoUpdater.quitAndInstall(false, true)
    })
})
