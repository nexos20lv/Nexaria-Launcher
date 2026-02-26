// ============================================================
// Nexaria Launcher - Azuriom Auth Wrapper
// ============================================================
const FormData = require('form-data')
const fs = require('fs')
const { fetchWithRetry } = require('./net')

// CONFIGURE: Set your Azuriom site URL here
const AZURIOM_URL = 'https://nexaria.site'

/**
 * Authenticate a user via Azuriom API
 * POST /api/auth/authenticate
 */
async function authenticate(email, password, twoFactorCode = null) {
    const body = { email, password }
    if (twoFactorCode) body.code = twoFactorCode

    const res = await fetchWithRetry(`${AZURIOM_URL}/api/auth/authenticate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(body),
    }, { retries: 2, timeoutMs: 8000 })

    const data = await res.json()

    if (!res.ok) {
        if (data.reason === '2fa') {
            return { status: 'pending', requires2fa: true }
        }
        return { status: 'error', reason: data.reason, message: data.message || 'Erreur inconnue' }
    }

    if (!data.email_verified) {
        return {
            status: 'error',
            reason: 'email_not_verified',
            message: 'Votre adresse email n\'est pas vérifiée. Veuillez vérifier vos emails (et spams) pour valider votre compte avant de vous connecter.'
        }
    }

    return {
        status: 'success',
        user: {
            id: data.id,
            username: data.username,
            uuid: data.uuid,
            email_verified: data.email_verified,
            money: data.money,
            role: data.role,
            banned: data.banned,
            created_at: data.created_at,
        },
        accessToken: data.access_token,
    }
}

/**
 * Verify an access token
 * POST /api/auth/verify
 */
async function verify(accessToken) {
    const res = await fetchWithRetry(`${AZURIOM_URL}/api/auth/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ access_token: accessToken }),
    }, { retries: 2, timeoutMs: 8000 })

    const data = await res.json()

    if (!res.ok) {
        return { status: 'error', reason: data.reason, message: data.message }
    }

    return {
        status: 'success',
        user: {
            id: data.id,
            username: data.username,
            uuid: data.uuid,
            email_verified: data.email_verified,
            money: data.money,
            role: data.role,
        },
        accessToken: data.access_token,
    }
}

/**
 * Logout and invalidate access token
 * POST /api/auth/logout
 */
async function logout(accessToken) {
    await fetchWithRetry(`${AZURIOM_URL}/api/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ access_token: accessToken }),
    }, { retries: 1, timeoutMs: 7000 })
}

/**
 * Upload a skin to Azuriom Skin API
 * POST /api/skin-api/skins
 */
async function uploadSkin(accessToken, filePath) {
    const form = new FormData()
    form.append('access_token', accessToken)
    form.append('skin', fs.createReadStream(filePath))

    const res = await fetchWithRetry(`${AZURIOM_URL}/api/skin-api/skins`, {
        method: 'POST',
        headers: form.getHeaders(),
        body: form,
    }, { retries: 1, timeoutMs: 15000 })

    const data = await res.json().catch(() => ({}))

    if (!res.ok) {
        return { status: 'error', message: data.message || 'Erreur lors de l\'envoi du skin' }
    }

    return { status: 'success', message: data.message || 'Skin mis à jour avec succès' }
}

/**
 * Upload a cape to Azuriom Skin API
 * POST /api/skin-api/capes
 */
async function uploadCape(accessToken, filePath) {
    const form = new FormData()
    form.append('access_token', accessToken)
    form.append('cape', fs.createReadStream(filePath))

    const res = await fetchWithRetry(`${AZURIOM_URL}/api/skin-api/capes`, {
        method: 'POST',
        headers: form.getHeaders(),
        body: form,
    }, { retries: 1, timeoutMs: 15000 })

    const data = await res.json().catch(() => ({}))

    if (!res.ok) {
        return { status: 'error', message: data.message || 'Erreur lors de l\'envoi de la cape' }
    }

    return { status: 'success', message: data.message || 'Cape mise à jour avec succès' }
}

module.exports = { authenticate, verify, logout, uploadSkin, uploadCape, AZURIOM_URL }
