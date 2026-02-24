// ============================================================
// Nexaria Launcher - Main Process
// ============================================================
const { app, BrowserWindow, ipcMain, shell } = require('electron')
const { autoUpdater } = require('electron-updater')
const log = require('electron-log')
log.transports.file.level = 'info'
autoUpdater.logger = log
const path = require('path')
const Store = require('electron-store')
const { authenticate, verify, logout } = require('./launcher/auth')
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

// ── Main Window ───────────────────────────────────────────
let mainWindow

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
        show: false,
        icon: path.join(__dirname, '../assets/icon.png'),
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
        },
    })

    mainWindow.loadFile(path.join(__dirname, 'renderer/index.html'))

    mainWindow.once('ready-to-show', () => {
        mainWindow.show()
    })

    if (process.argv.includes('--dev')) {
        mainWindow.webContents.openDevTools({ mode: 'detach' })
    }
}

app.whenReady().then(() => {
    createWindow()
    autoUpdater.checkForUpdatesAndNotify()
    initRPC()

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
            // Save account
            const accounts = store.get('accounts', [])
            const existing = accounts.findIndex(a => a.uuid === result.user.uuid)
            const account = {
                uuid: result.user.uuid,
                username: result.user.username,
                email,
                accessToken: result.accessToken,
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
    return store.get('accounts', [])
})

ipcMain.handle('auth:getLastAccount', () => {
    const uuid = store.get('lastAccount')
    if (!uuid) return null
    const accounts = store.get('accounts', [])
    return accounts.find(a => a.uuid === uuid) || null
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

// ── Auto Update Events ────────────────────────────────────
autoUpdater.on('update-available', () => {
    log.info('Update available.')
    if (mainWindow) {
        mainWindow.webContents.send('update:available')
    }
})

autoUpdater.on('update-not-available', () => {
    log.info('Update not available.')
})

autoUpdater.on('update-downloaded', () => {
    log.info('Update downloaded. Restarting to install...')
    if (mainWindow) {
        mainWindow.webContents.send('update:downloaded')
    }
    // Auto-install and restart
    setTimeout(() => {
        autoUpdater.quitAndInstall(false, true)
    }, 3000) // Give 3 seconds for the UI to show a toast
})

autoUpdater.on('error', (err) => {
    log.error('Error in auto-updater: ' + err)
})

autoUpdater.on('checking-for-update', () => {
    log.info('Checking for update...')
})

autoUpdater.on('download-progress', (progressObj) => {
    let log_message = "Download speed: " + progressObj.bytesPerSecond
    log_message = log_message + ' - Downloaded ' + progressObj.percent + '%'
    log_message = log_message + ' (' + progressObj.transferred + "/" + progressObj.total + ')'
    log.info(log_message)
})

ipcMain.on('update:quitAndInstall', () => {
    autoUpdater.quitAndInstall()
})
