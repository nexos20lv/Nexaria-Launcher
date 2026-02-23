// ============================================================
// Nexaria Launcher - Azuriom Auth Wrapper
// ============================================================
const fetch = require('node-fetch')

// CONFIGURE: Set your Azuriom site URL here
const AZURIOM_URL = 'https://nexaria.netlib.re'

/**
 * Authenticate a user via Azuriom API
 * POST /api/auth/authenticate
 */
async function authenticate(email, password, twoFactorCode = null) {
    const body = { email, password }
    if (twoFactorCode) body.code = twoFactorCode

    const res = await fetch(`${AZURIOM_URL}/api/auth/authenticate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(body),
    })

    const data = await res.json()

    if (!res.ok) {
        if (data.reason === '2fa') {
            return { status: 'pending', requires2fa: true }
        }
        return { status: 'error', reason: data.reason, message: data.message || 'Erreur inconnue' }
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
    const res = await fetch(`${AZURIOM_URL}/api/auth/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ access_token: accessToken }),
    })

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
    await fetch(`${AZURIOM_URL}/api/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ access_token: accessToken }),
    })
}

module.exports = { authenticate, verify, logout, AZURIOM_URL }
