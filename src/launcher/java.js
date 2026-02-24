const fs = require('fs')
const path = require('path')
const { app } = require('electron')
const { downloadFile } = require('./downloader')
const log = require('electron-log')
const AdmZip = require('adm-zip')
const tar = require('tar-fs')
const gunzip = require('gunzip-maybe')

const RUNTIME_DIR = path.join(app.getPath('userData'), 'runtime')

// URLs pour Adoptium (Temurin)
const JRE_URLS = {
    '8': {
        win32: 'https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u422-b05/OpenJDK8U-jre_x64_windows_hotspot_8u422b05.zip',
        darwin: 'https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u422-b05/OpenJDK8U-jre_x64_mac_hotspot_8u422b05.tar.gz',
        linux: 'https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u422-b05/OpenJDK8U-jre_x64_linux_hotspot_8u422b05.tar.gz'
    },
    '17': {
        win32: 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jre_x64_windows_hotspot_17.0.12_7.zip',
        darwin: 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jre_x64_mac_hotspot_17.0.12_7.tar.gz',
        linux: 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jre_x64_linux_hotspot_17.0.12_7.tar.gz'
    },
    '21': {
        win32: 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jre_x64_windows_hotspot_21.0.4_7.zip',
        darwin: 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jre_x64_mac_hotspot_21.0.4_7.tar.gz',
        linux: 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jre_x64_linux_hotspot_21.0.4_7.tar.gz'
    }
}

function getRequiredJavaVersion(mcVersion) {
    if (!mcVersion) return '21'
    const parts = mcVersion.split('.')
    if (parts.length < 2) return '21'

    const minor = parseInt(parts[1], 10)

    if (minor < 17) return '8' // 1.8 -> 1.16 utilise Java 8
    if (minor >= 17 && minor < 20) return '17' // 1.17 -> 1.19.4 utilise Java 17
    return '21' // 1.20+ utilise Java 21
}

async function ensureJava(mcVersion, onProgress) {
    const javaVersion = getRequiredJavaVersion(mcVersion)
    const platform = process.platform
    const jreDir = path.join(RUNTIME_DIR, `jre-${javaVersion}`)
    const javaExec = platform === 'win32' ? 'bin/java.exe' : (platform === 'darwin' ? 'Contents/Home/bin/java' : 'bin/java')
    const javaPath = path.join(jreDir, javaExec)

    if (fs.existsSync(javaPath)) {
        log.info(`Local Java ${javaVersion} found:`, javaPath)
        return javaPath
    }

    log.info(`Java ${javaVersion} not found locally, starting download...`)
    if (!fs.existsSync(RUNTIME_DIR)) fs.mkdirSync(RUNTIME_DIR, { recursive: true })

    const url = JRE_URLS[javaVersion][platform]
    if (!url) throw new Error(`Platform ${platform} not supported for auto-java ${javaVersion}`)

    const archiveName = path.basename(url)
    const archivePath = path.join(RUNTIME_DIR, archiveName)

    onProgress({ type: 'info', message: `Téléchargement de Java ${javaVersion}...`, percent: 0 })

    await downloadFile(url, archivePath, (pct) => {
        onProgress({ type: 'file', message: `Téléchargement de Java ${javaVersion}...`, percent: Math.round(pct * 100) })
    })

    onProgress({ type: 'info', message: 'Extraction de Java...', percent: 90 })

    if (archivePath.endsWith('.zip')) {
        const zip = new AdmZip(archivePath)
        zip.extractAllTo(RUNTIME_DIR, true)
    } else {
        await new Promise((resolve, reject) => {
            fs.createReadStream(archivePath)
                .pipe(gunzip())
                .pipe(tar.extract(RUNTIME_DIR))
                .on('finish', resolve)
                .on('error', reject)
        })
    }

    // Rename extraction folder if needed to match jre-X
    const dirPrefix = javaVersion === '8' ? 'jdk8' : `jdk-${javaVersion}`
    const files = fs.readdirSync(RUNTIME_DIR)
    const extractedDir = files.find(d => d.startsWith(dirPrefix) && d.includes('jre')) || files.find(d => d.startsWith(dirPrefix))

    if (extractedDir && extractedDir !== `jre-${javaVersion}`) {
        const oldPath = path.join(RUNTIME_DIR, extractedDir)
        if (fs.existsSync(oldPath)) {
            // Sur mac l'extraction tar crée parfois un path très profond, on s'assure juste du renommage racine
            try {
                fs.renameSync(oldPath, jreDir)
            } catch (e) {
                log.error("Erreur renommage java:", e)
            }
        }
    }

    try { fs.unlinkSync(archivePath) } catch (e) { }
    log.info(`Java ${javaVersion} installed successfully at:`, javaPath)
    return javaPath
}

module.exports = { ensureJava }
