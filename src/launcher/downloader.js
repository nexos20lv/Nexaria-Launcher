// ============================================================
// Nexaria Launcher - Game Downloader (PHP Server)
// ============================================================
const fs = require('fs')
const path = require('path')
const https = require('https')
const http = require('http')
const crypto = require('crypto')
const { app } = require('electron')

// CONFIGURE: URL de ton serveur PHP (peut aussi être défini dans les Paramètres de l'app)
// Laisser vide pour sauter le téléchargement et lancer le jeu directement.
const DEFAULT_FILE_SERVER_URL = 'https://launcher.nexaria.netlib.re'

let _storeRef = null
function getFileServerUrl() {
    try {
        if (!_storeRef) _storeRef = new (require('electron-store'))({ name: 'nexaria-launcher', encryptionKey: 'nexaria-secure-key-2024' })
        return (_storeRef.get('settings.fileServerUrl') || DEFAULT_FILE_SERVER_URL).trim()
    } catch {
        return DEFAULT_FILE_SERVER_URL
    }
}

function getGameDir() {
    return path.join(app.getPath('appData'), '.nexaria')
}

/**
 * Récupère les infos du serveur (version MC, loader, etc.)
 * GET /info.json
 */
async function fetchServerInfo() {
    const fetch = require('node-fetch')
    const url = getFileServerUrl()
    if (!url) return null
    try {
        const res = await fetch(`${url}/?action=info`, { timeout: 5000 })
        if (!res.ok) return null
        return res.json()
    } catch {
        return null
    }
}

/**
 * Récupère le manifest généré par PHP
 * GET /manifest.json
 */
async function fetchManifest() {
    const fetch = require('node-fetch')
    const url = getFileServerUrl()
    if (!url) return []

    let res
    try {
        res = await fetch(`${url}/?action=manifest`, { timeout: 15000 })
    } catch (err) {
        throw new Error(`Impossible de joindre ${url} — vérifie ta connexion internet.`)
    }

    if (res.status === 404) {
        throw new Error(
            `Le serveur PHP n'est pas encore déployé sur ${url}.\n` +
            `Upload le dossier php-server/ sur ton hébergement, puis relance.`
        )
    }

    if (!res.ok) {
        throw new Error(`Erreur serveur HTTP ${res.status} sur ${url}/manifest.json`)
    }

    return res.json()
}

/**
 * Calcul SHA1 d'un fichier local
 */
function sha1File(filePath) {
    return new Promise((resolve, reject) => {
        if (!fs.existsSync(filePath)) return resolve(null)
        const hash = crypto.createHash('sha1')
        const stream = fs.createReadStream(filePath)
        stream.on('data', d => hash.update(d))
        stream.on('end', () => resolve(hash.digest('hex')))
        stream.on('error', reject)
    })
}

/**
 * Télécharge un fichier unique
 */
function downloadFile(url, destPath, onProgress) {
    return new Promise((resolve, reject) => {
        const dir = path.dirname(destPath)
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })

        const proto = url.startsWith('https') ? https : http
        const file = fs.createWriteStream(destPath + '.tmp')

        const request = proto.get(url, (res) => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                file.close()
                fs.unlinkSync(destPath + '.tmp')
                return downloadFile(res.headers.location, destPath, onProgress)
                    .then(resolve).catch(reject)
            }
            if (res.statusCode !== 200) {
                file.close()
                return reject(new Error(`HTTP ${res.statusCode} pour ${url}`))
            }

            const total = parseInt(res.headers['content-length'] || '0', 10)
            let downloaded = 0

            res.on('data', (chunk) => {
                downloaded += chunk.length
                file.write(chunk)
                if (total > 0 && onProgress) onProgress(downloaded / total)
            })

            res.on('end', () => {
                file.end()
                fs.renameSync(destPath + '.tmp', destPath)
                resolve()
            })

            res.on('error', (err) => { file.destroy(); reject(err) })
        })

        request.on('error', (err) => { file.destroy(); reject(err) })
        request.setTimeout(30000, () => { request.destroy(); reject(new Error('Timeout')) })
    })
}

/**
 * Télécharge tous les fichiers manquants ou modifiés.
 * Bloque le lancement si le serveur est injoignable.
 */
async function downloadGame(version, onProgress) {
    const gameDir = getGameDir()
    const serverUrl = getFileServerUrl()

    // URL non configurée → erreur claire
    if (!serverUrl) {
        throw new Error('URL du serveur de fichiers non configurée.\nRendez-vous dans Paramètres pour la saisir.')
    }

    onProgress({ type: 'info', message: 'Connexion au serveur...', percent: 0 })

    let manifest
    try {
        manifest = await fetchManifest()
    } catch (err) {
        throw new Error(`Serveur de fichiers injoignable : ${err.message}`)
    }

    if (!manifest || manifest.length === 0) {
        onProgress({ type: 'complete', message: 'Aucun fichier à télécharger', percent: 100 })
        return
    }

    // Vérifier quels fichiers sont manquants ou modifiés
    onProgress({ type: 'info', message: 'Vérification des fichiers...', percent: 0 })

    const toDownload = []
    for (const file of manifest) {
        const dest = path.join(gameDir, file.path)
        const hash = await sha1File(dest)
        if (hash !== file.sha1) {
            toDownload.push(file)
        }
    }

    if (toDownload.length === 0) {
        onProgress({ type: 'complete', message: 'Tout est à jour ✓', percent: 100 })
        return
    }

    onProgress({
        type: 'start',
        total: toDownload.length,
        message: `Téléchargement de ${toDownload.length} fichier(s)...`,
        percent: 0,
    })

    for (let i = 0; i < toDownload.length; i++) {
        const file = toDownload[i]
        const dest = path.join(gameDir, file.path)

        onProgress({
            type: 'file',
            file: path.basename(file.path),
            current: i + 1,
            total: toDownload.length,
            percent: Math.round((i / toDownload.length) * 100),
            message: `${path.basename(file.path)}`,
        })

        await downloadFile(file.url, dest, (pct) => {
            const overall = ((i + pct) / toDownload.length) * 100
            onProgress({
                type: 'file',
                file: path.basename(file.path),
                current: i + 1,
                total: toDownload.length,
                percent: Math.round(overall),
                message: `${path.basename(file.path)} — ${Math.round(pct * 100)}%`,
            })
        })
    }

    // ── NOUVEAU : Nettoyage anti-triche ──
    onProgress({ type: 'info', message: 'Vérification de la sécurité...', percent: 99 })
    await cleanupGameFiles(gameDir, manifest)

    onProgress({ type: 'complete', message: 'Téléchargement terminé !', percent: 100 })
}

/**
 * Supprime les fichiers non autorisés (Anti-Triche)
 */
async function cleanupGameFiles(gameDir, manifest) {
    const allowed = new Set(manifest.map(f => path.normalize(f.path)))

    // Dossiers critiques à surveiller
    const criticalDirs = ['mods', 'config', 'resourcepacks', 'loader']

    for (const subDir of criticalDirs) {
        const dirPath = path.join(gameDir, subDir)
        if (!fs.existsSync(dirPath)) continue

        const files = getAllFiles(dirPath)
        for (const file of files) {
            const relative = path.relative(gameDir, file)
            if (!allowed.has(path.normalize(relative))) {
                console.log(`[Security] Suppression du fichier non autorisé : ${relative}`)
                try {
                    fs.unlinkSync(file)
                } catch (e) {
                    console.error(`[Security] Erreur lors de la suppression de ${relative}:`, e)
                }
            }
        }
    }
}

/**
 * Récupère récursivement tous les fichiers d'un dossier
 */
function getAllFiles(dirPath, arrayOfFiles) {
    const files = fs.readdirSync(dirPath)
    arrayOfFiles = arrayOfFiles || []

    files.forEach(function (file) {
        if (fs.statSync(dirPath + "/" + file).isDirectory()) {
            arrayOfFiles = getAllFiles(dirPath + "/" + file, arrayOfFiles)
        } else {
            arrayOfFiles.push(path.join(dirPath, "/", file))
        }
    })

    return arrayOfFiles
}

module.exports = { downloadGame, fetchServerInfo, getGameDir, getFileServerUrl, downloadFile }
