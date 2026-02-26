// ============================================================
// Nexaria Launcher - News Fetcher (Azuriom API)
// ============================================================
const { AZURIOM_URL } = require('./auth')
const { fetchWithRetry } = require('./net')

/**
 * Fetch latest posts from the Azuriom site API
 * GET /api/posts
 */
async function fetchNews() {
    const res = await fetchWithRetry(`${AZURIOM_URL}/api/posts?limit=5`, {
        headers: { Accept: 'application/json' },
    }, { retries: 2, timeoutMs: 7000 })

    if (!res.ok) return getFallbackNews()

    const data = await res.json()
    const posts = data.data || data || []

    return posts.slice(0, 5).map(post => ({
        id: post.id,
        title: post.title,
        excerpt: post.excerpt || stripHtml(post.content || '').substring(0, 120) + '...',
        date: formatDate(post.published_at || post.created_at),
        url: post.url,
        image: post.image,
        author: post.author?.name || 'Nexaria',
    }))
}

function stripHtml(html) {
    return html.replace(/<[^>]*>/g, '').trim()
}

function formatDate(dateStr) {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' })
}

function getFallbackNews() {
    return [
        {
            id: 1,
            title: 'Bienvenue sur Nexaria !',
            excerpt: 'Le launcher Nexaria est maintenant disponible. Connectez-vous et profitez du serveur !',
            date: '23 Février 2024',
            author: 'Nexaria',
        },
    ]
}

module.exports = { fetchNews }
