const path = require('path')
const fs = require('fs')
const { getGameDir, downloadFile, fetchOptionalMods } = require('./downloader')

// Cache the mods list so we don't have to fetch it every time we toggle
let cachedMods = []

async function loadOptionalMods() {
    const mods = await fetchOptionalMods()
    if (mods && Array.isArray(mods)) {
        cachedMods = mods
    }
    return cachedMods
}

function getOptionalModFileNames() {
    return cachedMods.map(m => m.fileName)
}

async function getModsStatus() {
    const mods = await loadOptionalMods()
    const gameDir = getGameDir()
    const modsDir = path.join(gameDir, 'mods')

    return mods.map(mod => {
        const filePath = path.join(modsDir, mod.fileName)
        return {
            ...mod,
            installed: fs.existsSync(filePath)
        }
    })
}

async function toggleMod(modId) {
    let mods = cachedMods
    if (mods.length === 0) mods = await loadOptionalMods()

    const mod = mods.find(m => m.id === modId)
    if (!mod) throw new Error("Mod introuvable sur le serveur")

    const gameDir = getGameDir()
    const modsDir = path.join(gameDir, 'mods')
    const filePath = path.join(modsDir, mod.fileName)

    if (fs.existsSync(filePath)) {
        // Désinstaller
        fs.unlinkSync(filePath)
        return { status: 'success', installed: false }
    } else {
        // Installer
        if (!fs.existsSync(modsDir)) {
            fs.mkdirSync(modsDir, { recursive: true })
        }
        await downloadFile(mod.url, filePath, null)
        return { status: 'success', installed: true }
    }
}

module.exports = { getModsStatus, toggleMod, getOptionalModFileNames }
