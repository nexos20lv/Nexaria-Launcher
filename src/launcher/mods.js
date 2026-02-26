const path = require('path')
const fs = require('fs')
const crypto = require('crypto')
// Removed top-level downloader require to avoid circular dependency

// Cache the mods list so we don't have to fetch it every time we toggle
let cachedMods = []

async function loadOptionalMods() {
    const { fetchOptionalMods } = require('./downloader')
    const mods = await fetchOptionalMods()
    if (mods && Array.isArray(mods)) {
        cachedMods = mods
    }
    return cachedMods
}

async function getOptionalModFileNames() {
    if (cachedMods.length === 0) await loadOptionalMods()
    return cachedMods.map(m => m.fileName)
}

async function getModsStatus() {
    const mods = await loadOptionalMods()
    const { getGameDir } = require('./downloader')
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

function isPathSafe(baseDir, targetPath) {
    const base = path.resolve(baseDir)
    const target = path.resolve(targetPath)
    const relative = path.relative(base, target)
    return relative === '' || (!relative.startsWith('..') && !path.isAbsolute(relative))
}

function sha1File(filePath) {
    return new Promise((resolve, reject) => {
        const hash = crypto.createHash('sha1')
        const stream = fs.createReadStream(filePath)
        stream.on('data', chunk => hash.update(chunk))
        stream.on('end', () => resolve(hash.digest('hex')))
        stream.on('error', reject)
    })
}

async function toggleMod(modId) {
    let mods = cachedMods
    if (mods.length === 0) mods = await loadOptionalMods()

    const mod = mods.find(m => m.id === modId)
    if (!mod) throw new Error("Mod introuvable sur le serveur")

    const { getGameDir, downloadFile } = require('./downloader')
    const gameDir = getGameDir()
    const modsDir = path.join(gameDir, 'mods')
    const filePath = path.join(modsDir, mod.fileName)

    if (!isPathSafe(modsDir, filePath)) {
        throw new Error('Nom de fichier de mod invalide (chemin non autorisé).')
    }

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

        if (mod.sha1) {
            const downloadedSha1 = await sha1File(filePath)
            if (downloadedSha1.toLowerCase() !== String(mod.sha1).toLowerCase()) {
                try { fs.unlinkSync(filePath) } catch { }
                throw new Error(`Échec de vérification d'intégrité pour ${mod.name} (SHA-1 invalide).`)
            }
        }

        return { status: 'success', installed: true }
    }
}

module.exports = { getModsStatus, toggleMod, getOptionalModFileNames }
