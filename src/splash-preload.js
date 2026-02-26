const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('nexariaSplash', {
    onStatus: (cb) => ipcRenderer.on('splash:status', (_, message) => cb(message)),
})
