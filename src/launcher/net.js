const fetch = require('node-fetch')

const DEFAULT_RETRY_STATUSES = new Set([408, 425, 429, 500, 502, 503, 504])

function wait(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}

async function fetchWithRetry(url, options = {}, retryOptions = {}) {
    const {
        retries = 2,
        retryDelayMs = 700,
        timeoutMs = 8000,
        retryStatuses = DEFAULT_RETRY_STATUSES,
    } = retryOptions

    const statuses = retryStatuses instanceof Set ? retryStatuses : new Set(retryStatuses)
    let lastError = null

    for (let attempt = 0; attempt <= retries; attempt++) {
        try {
            const response = await fetch(url, {
                ...options,
                timeout: options.timeout ?? timeoutMs,
            })

            if (attempt < retries && statuses.has(response.status)) {
                await wait(retryDelayMs * (attempt + 1))
                continue
            }

            return response
        } catch (error) {
            lastError = error
            if (attempt >= retries) break
            await wait(retryDelayMs * (attempt + 1))
        }
    }

    throw lastError || new Error('Network request failed')
}

module.exports = { fetchWithRetry }
