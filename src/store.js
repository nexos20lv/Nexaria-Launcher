const fs = require('fs')
const path = require('path')
const crypto = require('crypto')
const os = require('os')
const { app, safeStorage } = require('electron')
const Store = require('electron-store')

const STORE_NAME = 'nexaria-launcher-v2'
const LEGACY_STORE_NAME = 'nexaria-launcher'
const LEGACY_STORE_KEY = 'nexaria-secure-key-2024'
const KEY_FILE_NAME = '.storekey'

let storeInstance = null

function getKeyFilePath() {
    return path.join(app.getPath('userData'), KEY_FILE_NAME)
}

function deriveFallbackKey() {
    const fingerprint = `${os.hostname()}::${os.userInfo().username}::${app.getName()}::v2`
    return crypto.createHash('sha256').update(fingerprint).digest('hex')
}

function loadOrCreateStoreKey() {
    const keyPath = getKeyFilePath()

    if (fs.existsSync(keyPath)) {
        const saved = fs.readFileSync(keyPath, 'utf8')
        if (saved && saved.trim()) {
            if (saved.startsWith('enc:') && safeStorage.isEncryptionAvailable()) {
                const payload = Buffer.from(saved.slice(4), 'base64')
                return safeStorage.decryptString(payload)
            }
            return saved.trim()
        }
    }

    const generated = crypto.randomBytes(32).toString('hex')

    try {
        let content = generated
        if (safeStorage.isEncryptionAvailable()) {
            const encrypted = safeStorage.encryptString(generated).toString('base64')
            content = `enc:${encrypted}`
        }
        fs.writeFileSync(keyPath, content, { mode: 0o600 })
        return generated
    } catch {
        return deriveFallbackKey()
    }
}

function migrateFromLegacyIfNeeded(store) {
    if (store.get('__migratedFromLegacy')) return

    try {
        const legacyStore = new Store({
            name: LEGACY_STORE_NAME,
            encryptionKey: LEGACY_STORE_KEY,
            clearInvalidConfig: false,
        })

        const legacyData = legacyStore.store || {}
        const hasLegacyData = Object.keys(legacyData).length > 0

        if (hasLegacyData) {
            store.store = { ...legacyData, __migratedFromLegacy: new Date().toISOString() }
        } else {
            store.set('__migratedFromLegacy', 'no-legacy-data')
        }
    } catch {
        store.set('__migratedFromLegacy', 'legacy-read-failed')
    }
}

function getStore() {
    if (storeInstance) return storeInstance

    const encryptionKey = loadOrCreateStoreKey()
    storeInstance = new Store({
        name: STORE_NAME,
        encryptionKey,
        clearInvalidConfig: true,
    })

    migrateFromLegacyIfNeeded(storeInstance)
    return storeInstance
}

module.exports = { getStore }
