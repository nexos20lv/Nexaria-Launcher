// ============================================================
// Nexaria Launcher - Preload Script (Context Bridge)
// ============================================================
const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('nexaria', {
    // Window controls
    minimize: () => ipcRenderer.send('window:minimize'),
    close: () => ipcRenderer.send('window:close'),
    openUrl: (url) => ipcRenderer.send('open:url', url),

    // Auth
    login: (data) => ipcRenderer.invoke('auth:login', data),
    verify: (data) => ipcRenderer.invoke('auth:verify', data),
    logout: (data) => ipcRenderer.invoke('auth:logout', data),
    getAccounts: () => ipcRenderer.invoke('auth:getAccounts'),
    getLastAccount: () => ipcRenderer.invoke('auth:getLastAccount'),

    // Game
    launchGame: (data) => ipcRenderer.invoke('game:launch', data),
    downloadGame: (data) => ipcRenderer.invoke('game:download', data),
    repairGame: (data) => ipcRenderer.invoke('game:repair', data),
    onGameProgress: (cb) => ipcRenderer.on('game:progress', (_, data) => cb(data)),
    onGameLaunched: (cb) => ipcRenderer.on('game:launched', (_, data) => cb(data)),
    onGameCrashed: (cb) => ipcRenderer.on('game:crashed', (_, data) => cb(data)),
    removeGameListeners: () => {
        ipcRenderer.removeAllListeners('game:progress')
        ipcRenderer.removeAllListeners('game:launched')
        ipcRenderer.removeAllListeners('game:crashed')
    },

    // Settings
    getSettings: () => ipcRenderer.invoke('settings:get'),
    saveSettings: (data) => ipcRenderer.invoke('settings:set', data),

    // Server & news
    getServerStatus: () => ipcRenderer.invoke('server:status'),
    fetchNews: () => ipcRenderer.invoke('news:fetch'),

    // Updates
    onUpdateAvailable: (cb) => ipcRenderer.on('update:available', () => cb()),
    onUpdateDownloaded: (cb) => ipcRenderer.on('update:downloaded', () => cb()),
    quitAndInstall: () => ipcRenderer.send('update:quitAndInstall'),

    // Logic Console
    onGameLog: (cb) => ipcRenderer.on('game:log', (event, data) => cb(data)),

    // Skins & Capes
    selectSkinFile: () => ipcRenderer.invoke('skin:selectFile'),
    uploadSkin: (data) => ipcRenderer.invoke('skin:uploadSkin', data),
    uploadCape: (data) => ipcRenderer.invoke('skin:uploadCape', data),

    // Mods Optionnels
    getOptionalMods: () => ipcRenderer.invoke('mods:getOptional'),
    toggleOptionalMod: (data) => ipcRenderer.invoke('mods:toggle', data),

    // Screenshots
    getScreenshots: () => ipcRenderer.invoke('screenshots:list'),
    openScreenshot: (fileName) => ipcRenderer.invoke('screenshots:open', { fileName }),
    deleteScreenshot: (fileName) => ipcRenderer.invoke('screenshots:delete', { fileName }),
    openScreenshotsFolder: () => ipcRenderer.invoke('screenshots:openFolder'),
    onScreenshotsUpdated: (cb) => ipcRenderer.on('screenshots:updated', () => cb()),

    // Troubleshooting
    clearCache: () => ipcRenderer.invoke('troubleshoot:clearCache'),
    resetSettings: () => ipcRenderer.invoke('troubleshoot:resetSettings'),
})
