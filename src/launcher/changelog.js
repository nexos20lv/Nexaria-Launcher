// ============================================================
// Nexaria Launcher - Changelog Fetcher
// Scrapes https://nexaria.site/changelog (Azuriom custom page)
// ============================================================
const { fetchWithRetry } = require('./net')

const CHANGELOG_URL = 'https://nexaria.site/changelog'

// Category badge → icon emoji mapping
const CATEGORY_ICONS = {
    launcher: '🚀',
    'site-web': '🌐',
    'site web': '🌐',
    serveur: '⚙️',
    server: '⚙️',
}

/**
 * Fetch and parse the Nexaria changelog page.
 * Returns an array of { title, category, categoryIcon, date, body, bodyHtml }
 */
async function fetchChangelog() {
    try {
        const res = await fetchWithRetry(CHANGELOG_URL, {
            headers: { Accept: 'text/html,application/xhtml+xml', 'User-Agent': 'NexariaLauncher/1.0' },
        }, { retries: 2, timeoutMs: 10000 })

        if (!res.ok) return getFallbackChangelog()

        const html = await res.text()
        return parseChangelog(html)
    } catch (err) {
        console.error('[changelog] fetch error:', err.message)
        return getFallbackChangelog()
    }
}

/**
 * Parse the changelog HTML page into structured entries.
 */
function parseChangelog(html) {
    const entries = []

    // Split on card openings — each section starts with <div class="card">
    const parts = html.split('<div class="card">')
    // parts[0] is before the first card — skip it
    for (let i = 1; i < parts.length; i++) {
        const cardHtml = parts[i]

        // Extract title from <h2 class="card-title ..."> or newly styled headers like <h6> inside <div class="card-header">
        // nexaria.site uses <h6 class="fw-bold mb-0">Changelog</h6> inside card-header, but the actual entry titles are <h2 class="fw-bold my-3">
        // Since we split by card, let's find the first h6 or h2, but actually looking at the DOM:
        // The azuriom standard changelog structure has a single large <div class="card"> wrapping the whole changelog body usually.
        // Let's adapt the scraper. If it fails, fallback gracefully.
        const titleMatch = cardHtml.match(/<h[2-6][^>]*>(.*?)<\/h[2-6]>/i)
        const title = titleMatch ? stripTags(titleMatch[1]).trim() : 'Mise à jour'

        // Extract badges (category + date)
        const badges = []
        const badgeRegex = /<span[^>]*class="[^"]*badge[^"]*"[^>]*>([\s\S]*?)<\/span>/gi
        let bMatch
        while ((bMatch = badgeRegex.exec(cardHtml)) !== null) {
            badges.push(stripTags(bMatch[1]).trim())
        }

        const category = badges[0] || ''
        const date = badges[1] || ''

        const catKey = category.toLowerCase().replace(/\s+/g, '-')
        const categoryIcon = CATEGORY_ICONS[catKey] || CATEGORY_ICONS[category.toLowerCase()] || '📄'

        // Body text: strip all HTML, collapse whitespace
        const bodyText = stripTags(cardHtml).replace(/\s+/g, ' ').trim()
        // Remove the title and badge text from the beginning for cleaner excerpt
        let excerpt = bodyText
        if (title && excerpt.startsWith(title)) excerpt = excerpt.slice(title.length).trim()
        if (category && excerpt.startsWith(category)) excerpt = excerpt.slice(category.length).trim()
        if (date && excerpt.startsWith(date)) excerpt = excerpt.slice(date.length).trim()
        excerpt = excerpt.substring(0, 220) + (excerpt.length > 220 ? '...' : '')

        // Ignore the main wrapper card or "no updates" messages
        if (title.toLowerCase() === 'changelog') continue
        if (excerpt.toLowerCase().includes('aucune mise à jour')) continue

        entries.push({
            title,
            category,
            categoryIcon,
            date,
            excerpt,
            url: CHANGELOG_URL,
        })
    }

    return entries.length > 0 ? entries : getFallbackChangelog()
}


function stripTags(html) {
    return (html || '').replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&nbsp;/g, ' ').trim()
}

function getFallbackChangelog() {
    return [
        {
            title: 'Changelog indisponible',
            category: 'Serveur',
            categoryIcon: '⚙️',
            date: '',
            excerpt: 'Impossible de récupérer le changelog. Consultez nexaria.site/changelog pour les dernières mises à jour.',
            url: CHANGELOG_URL,
        },
    ]
}

module.exports = { fetchChangelog }
