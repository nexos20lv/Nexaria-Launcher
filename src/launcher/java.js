const fs = require('fs')
const path = require('path')
const { app } = require('electron')
const { downloadFile } = require('./downloader')
const log = require('electron-log')
const AdmZip = require('adm-zip')
const tar = require('tar-fs')
const gunzip = require('gunzip-maybe')

const RUNTIME_DIR = path.join(app.getPath('userData'), 'runtime')
const JAVA_VERSION = '21'

// URLs pour Adoptium (Temurin) JRE 21
const JRE_URLS = {
    win32: 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jre_x64_windows_hotspot_21.0.4_7.zip',
    darwin: 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jre_x64_mac_hotspot_21.0.4_7.tar.gz',
    linux: 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jre_x64_linux_hotspot_21.0.4_7.tar.gz'
}

async function ensureJava(onProgress) {
    const platform = process.platform
    const jreDir = path.join(RUNTIME_DIR, `jre-${JAVA_VERSION}`)
    const javaExec = platform === 'win32' ? 'bin/java.exe' : (platform === 'darwin' ? 'Contents/Home/bin/java' : 'bin/java')
    const javaPath = path.join(jreDir, javaExec)

    if (fs.existsSync(javaPath)) {
        log.info('Local Java 21 found:', javaPath)
        return javaPath
    }

    log.info('Java 21 not found locally, starting download...')
    if (!fs.existsSync(RUNTIME_DIR)) fs.mkdirSync(RUNTIME_DIR, { recursive: true })

    const url = JRE_URLS[platform]
    if (!url) throw new Error(`Platform ${platform} not supported for auto-java`)

    const archiveName = path.basename(url)
    const archivePath = path.join(RUNTIME_DIR, archiveName)

    onProgress({ type: 'info', message: 'Téléchargement de Java 21...', percent: 0 })

    await downloadFile(url, archivePath, (pct) => {
        onProgress({ type: 'file', message: 'Téléchargement de Java 21...', percent: Math.round(pct * 100) })
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

    // Rename extraction folder if needed to match jre-21
    const extractedDir = fs.readdirSync(RUNTIME_DIR).find(d => d.startsWith('jdk-21') && d.includes('jre'))
    if (extractedDir && extractedDir !== `jre-${JAVA_VERSION}`) {
        const oldPath = path.join(RUNTIME_DIR, extractedDir)
        if (fs.existsSync(oldPath)) {
            fs.renameSync(oldPath, jreDir)
        }
    }

    try { fs.unlinkSync(archivePath) } catch (e) { }
    log.info('Java 21 installed successfully at:', javaPath)
    return javaPath
}

module.exports = { ensureJava }
