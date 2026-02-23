/* ============================================================
   Nexaria Launcher — Renderer (UI Logic)
   ============================================================ */

'use strict'

// ── State ────────────────────────────────────────────────
const state = {
    currentView: 'login',
    currentAccount: null,
    accounts: [],
    settings: null,
    isLaunching: false,
    requires2fa: false,
}

// ── DOM helpers ──────────────────────────────────────────
const $ = (sel) => document.querySelector(sel)
const $$ = (sel) => document.querySelectorAll(sel)

// ── View management ──────────────────────────────────────
function showView(viewId) {
    $$('.view').forEach(v => v.classList.remove('active'))
    $$('.sidebar-btn[data-view]').forEach(b => b.classList.remove('active'))

    const view = $(`#view-${viewId}`)
    if (view) view.classList.add('active')

    const navBtn = $(`.sidebar-btn[data-view="${viewId}"]`)
    if (navBtn) navBtn.classList.add('active')

    state.currentView = viewId
}

// ── Toast notifications ──────────────────────────────────
let toastTimer = null
function showToast(msg, type = 'info') {
    let toast = $('#toast')
    if (!toast) {
        toast = document.createElement('div')
        toast.id = 'toast'
        toast.className = 'toast'
        document.body.appendChild(toast)
    }
    toast.textContent = msg
    toast.className = `toast toast--${type} show`
    clearTimeout(toastTimer)
    toastTimer = setTimeout(() => toast.classList.remove('show'), 3500)
}

// ── Auth ─────────────────────────────────────────────────
async function handleLogin() {
    const email = $('#input-email').value.trim()
    const password = $('#input-password').value
    const twoFa = $('#input-2fa')?.value.trim()

    if (!email || !password) {
        setLoginError('Veuillez remplir tous les champs.')
        return
    }

    setLoginError('')
    setLoginLoading(true)

    const result = await window.nexaria.login({ email, password, twoFactorCode: twoFa || null })

    if (result.status === 'pending' && result.requires2fa) {
        state.requires2fa = true
        const twoFaGroup = $('#twofa-group')
        if (twoFaGroup) twoFaGroup.style.display = 'flex'
        setLoginError('Veuillez entrer votre code 2FA.')
        setLoginLoading(false)
        return
    }

    if (result.status !== 'success') {
        setLoginError(result.message || 'Identifiants incorrects.')
        setLoginLoading(false)
        return
    }

    // Save remember-me
    if ($('#remember-me')?.checked) {
        localStorage.setItem('nexaria-remember', email)
    } else {
        localStorage.removeItem('nexaria-remember')
    }

    state.currentAccount = { ...result.user, accessToken: result.accessToken }
    setLoginLoading(false)
    await enterMainView()
}

function setLoginError(msg) {
    const el = $('#login-error')
    if (el) el.textContent = msg
}

function setLoginLoading(loading) {
    const btn = $('#btn-login')
    if (!btn) return
    btn.disabled = loading
    btn.querySelector('.btn-play-text').textContent = loading ? 'Connexion...' : 'SE CONNECTER'
}

// ── Main view setup ──────────────────────────────────────
async function enterMainView() {
    showView('main')

    if (state.currentAccount) {
        updatePlayerCard(state.currentAccount)
    }

    await Promise.all([
        refreshServerStatus(),
        loadAccounts(),
        loadNews(),
    ])
}

function getAvatarUrl(uuid, username, size = 64) {
    // Use Azuriom skin API (works with offline/custom UUIDs)
    const azuriomUrl = window._azuriomUrl || ''
    if (azuriomUrl && uuid) {
        return `${azuriomUrl}/api/auth/skin/${encodeURIComponent(uuid)}/face?size=${size}`
    }
    // Fallback: minotar (uses username, works without UUID)
    if (username) return `https://minotar.net/avatar/${encodeURIComponent(username)}/${size}`
    return `https://minotar.net/avatar/steve/${size}`
}

function updatePlayerCard(account) {
    const nameEl = $('#player-name')
    const roleEl = $('#player-role')
    const avatarEl = $('#player-avatar')

    if (nameEl) nameEl.textContent = account.username
    if (roleEl) roleEl.textContent = account.role?.name ? `Compte ${account.role.name}` : 'Compte Premium'
    if (avatarEl) {
        avatarEl.src = getAvatarUrl(account.uuid, account.username, 64)
        avatarEl.onerror = () => {
            // Final fallback: minotar with username
            if (account.username) {
                avatarEl.src = `https://minotar.net/avatar/${encodeURIComponent(account.username)}/64`
            }
            avatarEl.onerror = null
        }
    }
}

// ── Server status ────────────────────────────────────────
async function refreshServerStatus() {
    const dot = $('#status-dot')
    const text = $('#status-text')
    const playersEl = $('#players-count')

    try {
        const status = await window.nexaria.getServerStatus()

        if (status.online) {
            dot?.classList.add('online')
            dot?.classList.remove('offline')
            if (text) text.textContent = 'Serveur en ligne'
            if (playersEl) playersEl.textContent = `${status.players} Joueur${status.players !== 1 ? 's' : ''}`
        } else {
            dot?.classList.add('offline')
            dot?.classList.remove('online')
            if (text) text.textContent = 'Serveur hors ligne'
            if (playersEl) playersEl.textContent = '0 Joueur'
        }
    } catch {
        if (text) text.textContent = 'Statut inconnu'
    }
}

// ── News ─────────────────────────────────────────────────
async function loadNews() {
    const newsList = $('#news-list')
    if (!newsList) return

    try {
        const news = await window.nexaria.fetchNews()

        if (!news || news.length === 0) {
            newsList.innerHTML = '<p style="color:var(--text-muted);font-size:12px;padding:12px 0;text-align:center">Aucune actualité</p>'
            return
        }

        newsList.innerHTML = news.map(post => `
      <div class="news-item" data-url="${post.url || ''}">
        <div class="news-date">${post.date}</div>
        <div class="news-title">${escapeHtml(post.title)}</div>
        <div class="news-excerpt">${escapeHtml(post.excerpt || '')}</div>
      </div>
    `).join('')

        $$('.news-item').forEach(item => {
            item.addEventListener('click', () => {
                const url = item.dataset.url
                if (url) window.nexaria.openUrl(url)
            })
        })
    } catch {
        newsList.innerHTML = '<p style="color:var(--text-muted);font-size:12px;padding:12px 0;text-align:center">Impossible de charger les actualités</p>'
    }
}

// ── Accounts ─────────────────────────────────────────────
async function loadAccounts() {
    state.accounts = await window.nexaria.getAccounts()
    renderAccounts()
}

function renderAccounts() {
    const list = $('#accounts-list')
    if (!list) return

    if (!state.accounts.length) {
        list.innerHTML = '<p style="color:var(--text-muted);font-size:12px;padding:12px;text-align:center">Aucun compte enregistré</p>'
        return
    }

    list.innerHTML = state.accounts.map(acc => `
    <div class="account-item ${state.currentAccount?.uuid === acc.uuid ? 'active' : ''}" data-uuid="${acc.uuid}">
      <img class="account-avatar"
        src="${getAvatarUrl(acc.uuid, acc.username, 36)}"
        onerror="this.src='https://minotar.net/avatar/${encodeURIComponent(acc.username || 'steve')}/36';this.onerror=null;"
        alt="${escapeHtml(acc.username)}" />
      <div class="account-info">
        <div class="account-name">${escapeHtml(acc.username)}</div>
        <div class="account-type">Compte Premium</div>
      </div>
      <button class="account-delete" data-uuid="${acc.uuid}" title="Supprimer ce compte">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
          <path d="M10 11v6M14 11v6"/>
        </svg>
      </button>
    </div>
  `).join('')

    // Switch account on click
    $$('.account-item').forEach(item => {
        item.addEventListener('click', async (e) => {
            if (e.target.closest('.account-delete')) return
            const uuid = item.dataset.uuid
            const acc = state.accounts.find(a => a.uuid === uuid)
            if (!acc || acc.uuid === state.currentAccount?.uuid) return

            // Verify token
            const result = await window.nexaria.verify({ accessToken: acc.accessToken })
            if (result.status === 'success') {
                state.currentAccount = { ...result.user, accessToken: result.accessToken }
                updatePlayerCard(state.currentAccount)
                renderAccounts()
                showToast(`Compte changé : ${acc.username}`, 'success')
            } else {
                showToast('Session expirée. Veuillez vous reconnecter.', 'error')
                await removeAccount(acc.uuid, acc.accessToken)
            }
        })
    })

    // Delete account buttons
    $$('.account-delete').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.stopPropagation()
            const uuid = btn.dataset.uuid
            const acc = state.accounts.find(a => a.uuid === uuid)
            if (acc) await removeAccount(uuid, acc.accessToken)
        })
    })
}

async function removeAccount(uuid, accessToken) {
    await window.nexaria.logout({ accessToken, uuid })
    state.accounts = state.accounts.filter(a => a.uuid !== uuid)
    if (state.currentAccount?.uuid === uuid) {
        state.currentAccount = state.accounts[0] || null
        if (state.currentAccount) {
            updatePlayerCard(state.currentAccount)
        } else {
            showView('login')
            return
        }
    }
    renderAccounts()
    if (state.currentAccount) updatePlayerCard(state.currentAccount)
    showToast('Compte supprimé', 'info')
}

// ── Game Launch ───────────────────────────────────────────
async function handlePlay() {
    if (state.isLaunching) return
    if (!state.currentAccount) { showView('login'); return }

    state.isLaunching = true
    const btn = $('#btn-play')
    const playText = $('#play-text')

    btn.disabled = true
    if (playText) playText.textContent = 'LANCEMENT...'

    // Show progress
    const progressContainer = $('#progress-container')
    if (progressContainer) progressContainer.style.display = 'block'

    const settings = state.settings || {}
    const version = settings.serverVersion || '1.21.1'

    // Listen for progress
    window.nexaria.onGameProgress((progress) => {
        updateProgress(progress)
    })

    window.nexaria.onGameLaunched((info) => {
        if (info.status === 'launched') {
            showToast('Minecraft lancé !', 'success')
            if (playText) playText.textContent = 'EN JEU...'
        } else if (info.status === 'closed') {
            resetPlayButton()
        }
    })

    try {
        const result = await window.nexaria.launchGame({
            account: state.currentAccount,
            version,
            settings,
        })

        if (result.status === 'error') {
            showToast(result.message || 'Erreur lors du lancement', 'error')
            resetPlayButton()
        }
    } catch (err) {
        showToast('Erreur lors du lancement', 'error')
        resetPlayButton()
    }
}

function updateProgress(progress) {
    const fill = $('#progress-fill')
    const msg = $('#progress-message')
    const pct = $('#progress-percent')

    if (msg) msg.textContent = progress.message || 'Téléchargement...'
    if (pct) pct.textContent = `${progress.percent || 0}%`
    if (fill) fill.style.width = `${progress.percent || 0}%`

    if (progress.type === 'complete') {
        setTimeout(() => {
            const progressContainer = $('#progress-container')
            if (progressContainer) progressContainer.style.display = 'none'
        }, 2000)
    }
}

function resetPlayButton() {
    state.isLaunching = false
    const btn = $('#btn-play')
    const playText = $('#play-text')
    if (btn) btn.disabled = false
    if (playText) playText.textContent = 'JOUER'
    const progressContainer = $('#progress-container')
    if (progressContainer) progressContainer.style.display = 'none'
    window.nexaria.removeGameListeners()
}

// ── Settings ──────────────────────────────────────────────
async function loadSettings() {
    state.settings = await window.nexaria.getSettings()
    applySettings(state.settings)
}

function applySettings(s) {
    const ramInput = $('#setting-ram')
    const javaInput = $('#setting-java')
    const gameDirInput = $('#setting-gamedir')
    const fullscreenToggle = $('#setting-fullscreen')
    const keepOpenToggle = $('#setting-keep-open')
    const ramDisplay = $('#ram-display')

    if (ramInput) {
        ramInput.value = s.ram || 2048
        if (ramDisplay) ramDisplay.textContent = `${s.ram || 2048} Mo`
    }
    if (javaInput) javaInput.value = s.javaPath || ''
    if (gameDirInput) gameDirInput.value = s.gameDir || ''
    if (fullscreenToggle) fullscreenToggle.checked = !!s.fullscreen
    if (keepOpenToggle) keepOpenToggle.checked = s.keepLauncherOpen !== false

    const mcVersionEl = $('#about-mc-version')
    if (mcVersionEl) mcVersionEl.textContent = s.serverVersion || '1.21.1'

    const serverVersionEl = $('#server-version')
    if (serverVersionEl) serverVersionEl.textContent = `Survie Communautaire ${s.serverVersion || '1.21.1'}`

    // ── NOUVEAU : Versions dynamiques ──
    if (s.versions) {
        const appVersionEl = $('#about-app-version')
        const electronVersionEl = $('#about-electron-version')
        const osVersionEl = $('#about-os-version')

        if (appVersionEl) appVersionEl.textContent = s.versions.app
        if (electronVersionEl) electronVersionEl.textContent = s.versions.electron
        if (osVersionEl) osVersionEl.textContent = s.versions.os === 'darwin' ? 'macOS' : s.versions.os
    }
}

async function saveSettings() {
    const settings = {
        ram: parseInt($('#setting-ram')?.value || '2048'),
        javaPath: $('#setting-java')?.value || '',
        gameDir: $('#setting-gamedir')?.value || '',
        fullscreen: $('#setting-fullscreen')?.checked || false,
        keepLauncherOpen: $('#setting-keep-open')?.checked !== false,
    }
    await window.nexaria.saveSettings(settings)
    state.settings = { ...state.settings, ...settings }
    showToast('Paramètres sauvegardés', 'success')
}

// ── Utility ───────────────────────────────────────────────
function escapeHtml(str = '') {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
}

// ── Init ──────────────────────────────────────────────────
async function init() {
    // Load settings
    await loadSettings()

    // Expose Azuriom URL for avatar helper (fetched from main via IPC)
    window.nexaria.getSettings().then(s => {
        window._azuriomUrl = s.azuriomUrl || ''
    })

    // Window controls
    $('#btn-minimize')?.addEventListener('click', () => window.nexaria.minimize())
    $('#btn-close')?.addEventListener('click', () => window.nexaria.close())

    // Sidebar nav
    $$('.sidebar-btn[data-view]').forEach(btn => {
        btn.addEventListener('click', () => {
            const view = btn.dataset.view
            if (view === 'main' && !state.currentAccount) {
                showView('login')
                return
            }
            showView(view)
        })
    })

    // Social links
    $('#btn-website')?.addEventListener('click', () => window.nexaria.openUrl('https://nexaria.netlib.re'))
    $('#btn-discord')?.addEventListener('click', () => window.nexaria.openUrl('https://discord.gg/rwRAj5SbRH'))
    $('#btn-youtube')?.addEventListener('click', () => window.nexaria.openUrl('https://youtube.com/@nexaria'))

    // Login
    $('#btn-login')?.addEventListener('click', handleLogin)
    $('#input-password')?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') handleLogin()
    })
    $('#input-email')?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') $('#input-password')?.focus()
    })

    // Remember me
    const remembered = localStorage.getItem('nexaria-remember')
    if (remembered) {
        const emailInput = $('#input-email')
        const rememberCheck = $('#remember-me')
        if (emailInput) emailInput.value = remembered
        if (rememberCheck) rememberCheck.checked = true
    }

    // Play button
    $('#btn-play')?.addEventListener('click', handlePlay)

    // Logout
    $('#btn-logout')?.addEventListener('click', async () => {
        if (!state.currentAccount) return
        await window.nexaria.logout({
            accessToken: state.currentAccount.accessToken,
            uuid: state.currentAccount.uuid,
        })
        state.currentAccount = null
        state.isLaunching = false
        showView('login')
        showToast('Déconnecté', 'info')
    })

    // Add account
    $('#btn-add-account')?.addEventListener('click', () => showView('login'))

    // Settings save
    $('#btn-save-settings')?.addEventListener('click', saveSettings)

    // RAM slider live update
    const ramInput = $('#setting-ram')
    const ramDisplay = $('#ram-display')
    ramInput?.addEventListener('input', () => {
        if (ramDisplay) ramDisplay.textContent = `${ramInput.value} Mo`
    })

    // Try to auto-login with last account
    const lastAccount = await window.nexaria.getLastAccount()
    if (lastAccount) {
        const verify = await window.nexaria.verify({ accessToken: lastAccount.accessToken })
        if (verify.status === 'success') {
            state.currentAccount = { ...verify.user, accessToken: verify.accessToken }
            await enterMainView()
            return
        }
    }

    // Start at login view
    showView('login')

    // Refresh server status every 30s
    setInterval(refreshServerStatus, 30000)
}

// Start when DOM is ready
document.addEventListener('DOMContentLoaded', init)
