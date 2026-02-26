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
    skinViewer: null,
    currentNewsSlide: 0,
    newsInterval: null,
    serverStatusInterval: null,
    accountRefreshInterval: null,
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

    if (viewId === 'mods') {
        renderMods()
    } else if (viewId === 'screenshots') {
        renderScreenshots()
    }
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

    // Refresh server status every 15s
    refreshServerStatus()
    if (state.serverStatusInterval) clearInterval(state.serverStatusInterval)
    state.serverStatusInterval = setInterval(refreshServerStatus, 15000) // Plus fréquent (15s) pour plus de réactivité

    // Refresh stats (Money, Role) every 60s
    if (state.accountRefreshInterval) clearInterval(state.accountRefreshInterval)
    state.accountRefreshInterval = setInterval(async () => {
        if (state.currentAccount) {
            const result = await window.nexaria.verify({ accessToken: state.currentAccount.accessToken })
            if (result.status === 'success') {
                state.currentAccount = { ...state.currentAccount, ...result.user }
                updatePlayerCard(state.currentAccount)
            }
        }
    }, 60000)

    // Run async tasks in background without blocking UI
    Promise.all([
        loadAccounts().catch(e => console.error('loadAccounts fail:', e)),
        loadNews().catch(e => console.error('loadNews fail:', e)),
    ]).catch(console.error)
}

function getAvatarUrl(uuid, username, size = 64, type = 'face') {
    const azuriomUrl = window._azuriomUrl || state.settings?.azuriomUrl || 'https://nexaria.site'
    if (username) {
        return `${azuriomUrl}/api/skin-api/avatars/${type}/${encodeURIComponent(username)}?size=${size}`
    }
    // Fallback simple
    return `https://minotar.net/avatar/steve/${size}`
}

function parseMinecraftColors(text) {
    if (!text) return ''

    // First escape HTML to prevent XSS
    let html = escapeHtml(text)

    // Hex colors &#RRGGBB or <#RRGGBB>
    html = html.replace(/(&#|&lt;#)([0-9a-fA-F]{6})(&gt;)?/g, '<span style="color: #$2">')

    const colors = {
        '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
        '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
        '8': '#555555', '9': '#5555FF', 'a': '#55FF55', 'b': '#55FFFF',
        'c': '#FF5555', 'd': '#FF55FF', 'e': '#FFFF55', 'f': '#FFFFFF'
    }

    const formats = {
        'l': 'font-weight: bold;',
        'm': 'text-decoration: line-through;',
        'n': 'text-decoration: underline;',
        'o': 'font-style: italic;'
    }

    let result = ''
    let spanCount = 0

    // Match §code or &code (excluding &amp; etc)
    const regex = /(?:§|&)(?![a-z]+;)([0-9a-fk-orx])/gi
    let lastIndex = 0
    let match

    while ((match = regex.exec(html)) !== null) {
        result += html.substring(lastIndex, match.index)
        lastIndex = regex.lastIndex

        const code = match[1].toLowerCase()

        if (code === 'r') {
            result += '</span>'.repeat(spanCount)
            spanCount = 0
        } else if (colors[code]) {
            result += '</span>'.repeat(spanCount)
            spanCount = 0
            result += `<span style="color: ${colors[code]};">`
            spanCount++
        } else if (formats[code]) {
            result += `<span style="${formats[code]}">`
            spanCount++
        }
    }

    result += html.substring(lastIndex)
    result += '</span>'.repeat(spanCount)

    return result
}

function updatePlayerCard(account) {
    const nameEl = $('#player-name')
    const roleEl = $('#player-role')
    const avatarEl = $('#player-avatar')

    if (nameEl) nameEl.textContent = account.username
    if (roleEl) {
        roleEl.textContent = account.role?.name ? `Compte ${account.role.name}` : 'Compte Joueur'
        // Reset classes
        roleEl.classList.remove('role-mvp', 'role-vip')

        // Apply specialized role styling
        const roleName = account.role?.name?.toUpperCase() || ''
        if (roleName.includes('MVP')) roleEl.classList.add('role-mvp')
        else if (roleName.includes('VIP')) roleEl.classList.add('role-vip')
    }

    if (avatarEl) {
        // Ajouter un cache buster basé sur le timestamp global pour forcer le rafraîchissement
        const v = window._avatarVersion || Date.now()
        let url = getAvatarUrl(account.uuid, account.username, 64, 'face')
        avatarEl.src = url + (url.includes('?') ? '&' : '?') + `v=${v}`
    }

    // Mettre à jour la grande preview du skin dans l'onglet Personnalisation
    const skinContainer = $('#skin-container')
    if (skinContainer && account.username) {
        try {
            const azuriomUrl = window._azuriomUrl || state.settings?.azuriomUrl || 'https://nexaria.site'
            const skinUrl = `${azuriomUrl}/api/skin-api/skins/${encodeURIComponent(account.username)}`

            // Check if skinview3d is available (might be on window or global)
            const sv = window.skinview3d || (typeof skinview3d !== 'undefined' ? skinview3d : null)

            if (sv) {
                if (!state.skinViewer) {
                    state.skinViewer = new sv.SkinViewer({
                        canvas: skinContainer,
                        width: 300,
                        height: 300,
                        skin: skinUrl
                    })
                    state.skinViewer.animations.add(sv.WalkingAnimation)
                    state.skinViewer.autoRotate = true
                    state.skinViewer.autoRotateSpeed = 0.5
                } else {
                    state.skinViewer.loadSkin(skinUrl)
                }
            } else {
                console.warn('skinview3d library not found')
            }
        } catch (err) {
            console.error('Failed to initialize or update skin viewer:', err)
        }
    }
}

// ── Server status ────────────────────────────────────────
async function refreshServerStatus() {
    const dot = $('#status-dot')
    const text = $('#status-text')
    const playersEl = $('#players-count')
    const playersListEl = $('#players-list')

    try {
        const status = await window.nexaria.getServerStatus()

        if (status.online) {
            dot?.classList.add('online')
            dot?.classList.remove('offline')
            if (text) text.textContent = 'Serveur en ligne'
            if (playersEl) playersEl.textContent = `${status.players} Joueur${status.players !== 1 ? 's' : ''}`

            if (playersListEl) {
                if (status.sample && status.sample.length > 0) {
                    playersListEl.innerHTML = status.sample.map(p => `
                        <div style="display: flex; align-items: center; gap: 8px; padding: 6px; background: rgba(255,255,255,0.03); border-radius: 6px;">
                            <img src="${getAvatarUrl(p.id, p.name, 24, 'face')}" style="width: 24px; height: 24px; border-radius: 4px;" onerror="this.src='https://minotar.net/avatar/${encodeURIComponent(p.name)}/24';"/>
                            <span style="font-size: 12px; font-weight: 600;">${parseMinecraftColors(p.name)}</span>
                        </div>
                    `).join('')
                } else if (status.players > 0) {
                    playersListEl.innerHTML = `<p style="color:var(--text-muted);font-size:11px;text-align:center;">Liste privée ou indisponible</p>`
                } else {
                    playersListEl.innerHTML = `<p style="color:var(--text-muted);font-size:11px;text-align:center;">Aucun joueur</p>`
                }
            }
        } else {
            dot?.classList.add('offline')
            dot?.classList.remove('online')
            if (text) text.textContent = 'Serveur hors ligne'
            if (playersEl) playersEl.textContent = '0 Joueur'
            if (playersListEl) playersListEl.innerHTML = `<p style="color:var(--text-muted);font-size:11px;text-align:center;">Hors ligne</p>`
        }
    } catch {
        if (text) text.textContent = 'Statut inconnu'
        if (playersListEl) playersListEl.innerHTML = `<p style="color:var(--text-muted);font-size:11px;text-align:center;">Erreur</p>`
    }

    // Refresh the modal if open
    if ($('#modal-players')?.style.display === 'flex') {
        updatePlayersModal()
    }
}

async function updatePlayersModal() {
    const listEl = $('#players-full-list')
    if (!listEl) return

    try {
        const status = await window.nexaria.getServerStatus()
        if (!status.online || !status.sample || status.sample.length === 0) {
            listEl.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 40px;">Aucun joueur en ligne</p>`
            return
        }

        listEl.innerHTML = status.sample.map(p => `
            <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px; background: rgba(255,255,255,0.03); border: 1px solid var(--border); border-radius: var(--radius-md);">
                <div style="display: flex; align-items: center; gap: 12px;">
                    <img src="${getAvatarUrl(p.id, p.name, 32, 'face')}" style="width: 32px; height: 32px; border-radius: 4px;" onerror="this.src='https://minotar.net/avatar/${encodeURIComponent(p.name)}/32';"/>
                    <span style="font-weight: 700; font-size: 14px;">${parseMinecraftColors(p.name)}</span>
                </div>
            </div>
        `).join('')
    } catch (err) {
        listEl.innerHTML = `<p style="text-align: center; color: var(--red); padding: 40px;">Erreur de chargement</p>`
    }
}

// ── News ─────────────────────────────────────────────────
async function loadNews() {
    const track = $('#news-track')
    const dots = $('#news-dots')
    if (!track) return

    try {
        const news = await window.nexaria.fetchNews()

        if (!news || news.length === 0) {
            track.innerHTML = '<p style="color:var(--text-muted);font-size:12px;padding:40px;text-align:center;width:100%">Aucune actualité</p>'
            return
        }

        // Render slides
        track.innerHTML = news.map(post => `
            <div class="news-slide" data-url="${post.url || ''}">
                <img class="news-slide-img" src="${post.image || '../../assets/news-placeholder.jpg'}" onerror="this.src='../../assets/news-placeholder.jpg'">
                <div class="news-slide-content">
                    <div class="news-slide-date">${post.date}</div>
                    <div class="news-slide-title">${escapeHtml(post.title)}</div>
                </div>
            </div>
        `).join('')

        // Render dots
        if (dots) {
            dots.innerHTML = news.map((_, i) => `<div class="dot ${i === 0 ? 'active' : ''}" data-index="${i}"></div>`).join('')
        }

        // Setup events
        $$('.news-slide').forEach(slide => {
            slide.addEventListener('click', () => {
                const url = slide.dataset.url
                if (url) window.nexaria.openUrl(url)
            })
        })

        $$('.dot').forEach(dot => {
            dot.addEventListener('click', () => {
                goToSlide(parseInt(dot.dataset.index))
                resetNewsTimer()
            })
        })

        $('#news-prev')?.addEventListener('click', () => {
            goToSlide(state.currentNewsSlide - 1)
            resetNewsTimer()
        })

        $('#news-next')?.addEventListener('click', () => {
            goToSlide(state.currentNewsSlide + 1)
            resetNewsTimer()
        })

        // Auto-rotate
        resetNewsTimer()

    } catch (err) {
        console.error('Failed to load news:', err)
        track.innerHTML = '<p style="color:var(--text-muted);font-size:12px;padding:40px;text-align:center;width:100%">Erreur de chargement des actualités</p>'
    }
}

function goToSlide(index) {
    const track = $('#news-track')
    const slides = $$('.news-slide')
    const dots = $$('.dot')
    if (!track || slides.length === 0) return

    state.currentNewsSlide = (index + slides.length) % slides.length

    track.style.transform = `translateX(-${state.currentNewsSlide * 100}%)`

    dots.forEach((dot, i) => {
        dot.classList.toggle('active', i === state.currentNewsSlide)
    })
}

function resetNewsTimer() {
    clearInterval(state.newsInterval)
    state.newsInterval = setInterval(() => {
        goToSlide(state.currentNewsSlide + 1)
    }, 5000)
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
        src="${getAvatarUrl(acc.uuid, acc.username, 36, 'face')}"
        onerror="this.src='https://minotar.net/avatar/${encodeURIComponent(acc.username || 'steve')}/36';this.onerror=null;"
        alt="${escapeHtml(acc.username)}" />
      <div class="account-info">
        <div class="account-name">${escapeHtml(acc.username)}</div>
        <div class="account-type">${acc.role?.name ? `Compte ${acc.role.name}` : 'Compte Premium'}</div>
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

    // UI Console
    appendToConsole('Lancement du jeu...', 'info')

    // Show progress
    const progressContainer = $('#progress-container')
    if (progressContainer) progressContainer.style.display = 'block'

    const settings = state.settings || {}
    const version = settings.serverVersion || '1.21.11'

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

// ── Console ──────────────────────────────────────────────
function appendToConsole(data, type = 'log') {
    const out = $('#console-output')
    if (!out) return

    // Allow object data for typed logs
    let message = data
    let logType = type
    if (typeof data === 'object' && data.data) {
        message = data.data
        logType = data.type || type
    }

    const line = document.createElement('div')
    line.className = `console-line ${logType}`
    line.textContent = message.trim()
    out.appendChild(line)

    // Limit to 500 lines for performance
    if (out.childNodes.length > 500) out.removeChild(out.firstChild)

    // Auto-scroll to bottom
    out.scrollTop = out.scrollHeight
}

function clearConsole() {
    const out = $('#console-output')
    if (out) out.innerHTML = '<div class="console-line info">Console effacée.</div>'
}

async function loadSettings() {
    state.settings = await window.nexaria.getSettings()

    // Configuration par défaut de la RAM si non définie
    if (!state.settings || !state.settings.ram) {
        state.settings = state.settings || {}
        // navigator.deviceMemory donne la RAM en Go (ex: 8, 16, 32). S'il n'est pas dispo, on fallback sur 4 Go.
        const sysRamGb = navigator.deviceMemory || 8
        const sysRamMb = sysRamGb * 1024

        // Profil normal : la moitié de la RAM sysérale, max 6 Go, min 2 Go.
        let defaultRam = Math.floor(sysRamMb / 2)
        if (defaultRam > 6144) defaultRam = 6144
        if (defaultRam < 2048) defaultRam = 2048

        state.settings.ram = defaultRam
        // Sauvegarde immédiate
        window.nexaria.saveSettings(state.settings)
    }

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

    if ($('#setting-jvm-args')) $('#setting-jvm-args').value = s.jvmArgs || ''

    // Highlight profile
    $$('.btn-profile').forEach(b => b.classList.remove('active'))
    if (s.ram <= 2048) $('#profile-potate')?.classList.add('active')
    else if (s.ram >= 8192) $('#profile-gamer')?.classList.add('active')
    else $('#profile-easy')?.classList.add('active')

    const mcVersionEl = $('#about-mc-version')
    if (mcVersionEl) mcVersionEl.textContent = s.serverVersion || '1.21.11'

    const serverVersionEl = $('#server-version')
    if (serverVersionEl) serverVersionEl.textContent = `Survie Communautaire ${s.serverVersion || '1.21.11'}`

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
        jvmArgs: $('#setting-jvm-args')?.value || '',
    }
    await window.nexaria.saveSettings(settings)
    state.settings = { ...state.settings, ...settings }
    showToast('Paramètres sauvegardés', 'success')
}

function selectProfile(profile) {
    const ramInput = $('#setting-ram')
    const ramDisplay = $('#ram-display')

    // Remove active class from all profile buttons
    $$('.btn-profile').forEach(b => b.classList.remove('active'))

    let ram = 4096
    if (profile === 'potate') {
        ram = 2048
    } else if (profile === 'gamer') {
        ram = 8192
    } else if (profile === 'easy') {
        const sysRamGb = navigator.deviceMemory || 8
        const sysRamMb = sysRamGb * 1024
        ram = Math.floor(sysRamMb / 2)
        if (ram > 6144) ram = 6144
        if (ram < 2048) ram = 2048
    }

    // Update local state and UI
    if (state.settings) state.settings.ram = ram
    if (ramInput) ramInput.value = ram
    if (ramDisplay) ramDisplay.textContent = `${ram} Mo`

    $(`#profile-${profile}`)?.classList.add('active')
    showToast(`Profil ${profile === 'easy' ? 'Normal' : profile} activé (${ram} Mo)`, 'info')
}

// ── Utility ───────────────────────────────────────────────
function escapeHtml(str = '') {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
}

// ── Particles Background ──────────────────────────────────
function initParticles() {
    const container = $('#particles-bg')
    if (!container) return

    const particleCount = 30
    for (let i = 0; i < particleCount; i++) {
        createParticle(container)
    }
}

function createParticle(container) {
    const p = document.createElement('div')
    p.classList.add('particle')

    // Taille aléatoire (2px à 5px)
    const size = Math.random() * 3 + 2
    p.style.width = `${size}px`
    p.style.height = `${size}px`

    // Position initiale aléatoire
    p.style.left = `${Math.random() * 100}%`
    p.style.top = `${Math.random() * 100}%`

    // Durée d'animation aléatoire
    const duration = Math.random() * 20 + 10
    p.style.animationDuration = `${duration}s`

    // Délai aléatoire
    p.style.animationDelay = `-${Math.random() * 20}s`

    container.appendChild(p)
}

// ── Init ──────────────────────────────────────────────────
async function init() {
    // Start background particles
    initParticles()

    // Load settings
    await loadSettings()

    // Listen for updates
    window.nexaria.onUpdateAvailable(() => {
        showToast('🚀 Une mise à jour est disponible !', 'info')
    })

    window.nexaria.onUpdateDownloaded(() => {
        showToast('✅ Mise à jour prête ! Redémarrage automatique dans 3s...', 'success')
    })

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

            // Map integration lazy load with 502 handling
            if (view === 'map') {
                checkMapAvailability()
            }

            showView(view)
        })
    })

    // Social links
    $('#btn-website')?.addEventListener('click', () => window.nexaria.openUrl('https://nexaria.site'))
    $('#btn-discord')?.addEventListener('click', () => window.nexaria.openUrl('https://discord.gg/rwRAj5SbRH'))
    $('#btn-youtube')?.addEventListener('click', () => window.nexaria.openUrl('https://www.youtube.com/@nexos20'))

    // Azuriom Vote link
    $('#btn-vote')?.addEventListener('click', () => window.nexaria.openUrl('https://nexaria.site/vote'))

    // Customization (Skins/Capes)
    $('#btn-change-skin')?.addEventListener('click', async () => {
        if (!state.currentAccount) return
        const file = await window.nexaria.selectSkinFile()
        if (!file) return

        const btn = $('#btn-change-skin')
        btn.disabled = true
        showToast('Envoi du skin en cours...', 'info')

        try {
            const res = await window.nexaria.uploadSkin({ accessToken: state.currentAccount.accessToken, filePath: file })
            if (res.status === 'success') {
                showToast('Skin mis à jour !', 'success')
                // Forcer le rafraîchissement de l'avatar
                window._avatarVersion = Date.now()
                updatePlayerCard(state.currentAccount)
                renderAccounts()
            } else {
                showToast(res.message, 'error')
            }
        } catch (e) {
            showToast('Erreur lors de l\'envoi', 'error')
        } finally {
            btn.disabled = false
        }
    })

    $('#btn-change-cape')?.addEventListener('click', async () => {
        if (!state.currentAccount) return
        const file = await window.nexaria.selectSkinFile()
        if (!file) return

        const btn = $('#btn-change-cape')
        btn.disabled = true
        showToast('Envoi de la cape en cours...', 'info')

        try {
            const res = await window.nexaria.uploadCape({ accessToken: state.currentAccount.accessToken, filePath: file })
            if (res.status === 'success') {
                showToast('Cape mise à jour !', 'success')
            } else {
                showToast(res.message, 'error')
            }
        } catch (e) {
            showToast('Erreur lors de l\'envoi', 'error')
        } finally {
            btn.disabled = false
        }
    })

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

    // ── Auto-Save Settings ──────────────────────────────────────
    let saveTimeout
    const triggerAutoSave = () => {
        clearTimeout(saveTimeout)
        saveTimeout = setTimeout(() => {
            saveSettings()
        }, 800) // 800ms debounce
    }

    // RAM slider live update + auto-save
    const ramInput = $('#setting-ram')
    const ramDisplay = $('#ram-display')
    ramInput?.addEventListener('input', () => {
        if (ramDisplay) ramDisplay.textContent = `${ramInput.value} Mo`
        // Deselect profile buttons if manual change
        $$('.btn-profile').forEach(b => b.classList.remove('active'))
        triggerAutoSave()
    })

    $('#setting-java')?.addEventListener('input', triggerAutoSave)
    $('#setting-gamedir')?.addEventListener('input', triggerAutoSave)
    $('#setting-jvm-args')?.addEventListener('input', triggerAutoSave)
    $('#setting-fullscreen')?.addEventListener('change', triggerAutoSave)
    $('#setting-keep-open')?.addEventListener('change', triggerAutoSave)

    // Clear Cache
    $('#btn-clear-cache')?.addEventListener('click', async () => {
        if (!confirm('Voulez-vous vraiment vider le cache du jeu ? Cela supprimera les assets et bibliothèques (ils seront re-téléchargés au prochain lancement).')) return
        const res = await window.nexaria.clearCache()
        if (res.status === 'success') {
            showToast('Cache vidé avec succès !', 'success')
        }
    })

    // Reset settings
    $('#btn-reset-settings')?.addEventListener('click', async () => {
        if (!confirm('ATTENTION : Voulez-vous vraiment réinitialiser le launcher ? Tous vos comptes et paramètres seront effacés.')) return
        await window.nexaria.resetSettings()
        location.reload()
    })

    // Screenshot management
    $('#btn-open-screenshots-folder')?.addEventListener('click', () => {
        window.nexaria.openScreenshotsFolder()
    })

    // Override original saveSettings to prevent duplicate toasts if called too fast
    const originalSaveSettings = saveSettings
    saveSettings = async () => {
        await originalSaveSettings()
    }

    // Profile buttons (direct save on click)
    $('#profile-potate')?.addEventListener('click', () => { selectProfile('potate'); triggerAutoSave(); })
    $('#profile-easy')?.addEventListener('click', () => { selectProfile('easy'); triggerAutoSave(); })
    $('#profile-gamer')?.addEventListener('click', () => { selectProfile('gamer'); triggerAutoSave(); })

    // Start at login view by default to render UI immediately
    showView('login')

    // Try to auto-login with last account in the background
    const lastAccount = await window.nexaria.getLastAccount()
    if (lastAccount) {
        setLoginLoading(true)
        try {
            const verify = await window.nexaria.verify({ accessToken: lastAccount.accessToken })
            if (verify.status === 'success') {
                state.currentAccount = { ...verify.user, accessToken: verify.accessToken }
                await enterMainView()
            }
        } catch (e) {
            console.error('Auto-login failed', e)
        } finally {
            setLoginLoading(false)
        }
    }

    // console log events from game
    window.nexaria.onGameLog((data) => {
        appendToConsole(data)
    })

    // clear console button
    $('#btn-clear-console')?.addEventListener('click', clearConsole)

    // Repair game
    $('#btn-repair')?.addEventListener('click', async () => {
        const btn = $('#btn-repair')
        if (!btn || btn.disabled) return

        if (!confirm('Voulez-vous vraiment forcer la réparation du jeu ? Cela supprimera les caches de version, mais cela conservera vos mondes, ressources et paramètres de mods.')) {
            return
        }

        btn.disabled = true
        btn.innerHTML = 'En cours...'
        showToast('Réparation en cours...', 'info')

        try {
            const version = state.settings?.serverVersion || '1.21.11'
            const result = await window.nexaria.repairGame({ version })
            if (result.status === 'success') {
                showToast('Jeu réparé avec succès ! Lancez le jeu pour re-télécharger.', 'success')
            } else {
                showToast(`Erreur: ${result.message}`, 'error')
            }
        } catch (e) {
            showToast(`Erreur: ${e.message}`, 'error')
        } finally {
            btn.disabled = false
            btn.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 14px; height: 14px; margin-right: 4px; vertical-align: middle;"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" /></svg>Réparer`
        }
    })

    // Crash Log handler
    if (window.nexaria.onGameCrashed) {
        window.nexaria.onGameCrashed(async (data) => {
            const modal = $('#modal-crash')
            const textArea = $('#crash-log-text')
            const diagnosisBox = $('#crash-diagnosis-box')
            const causeEl = $('#crash-diagnosis-cause')
            const solutionEl = $('#crash-diagnosis-solution')

            if (modal && textArea) {
                // data might be a string (old format) or object { log, analysis }
                const crashLog = typeof data === 'string' ? data : data.log
                const analysis = typeof data === 'object' ? data.analysis : null

                textArea.value = crashLog

                if (diagnosisBox && analysis) {
                    causeEl.innerText = analysis.cause
                    solutionEl.innerText = analysis.solution
                    diagnosisBox.style.display = 'block'
                } else if (diagnosisBox) {
                    diagnosisBox.style.display = 'none'
                }

                modal.style.display = 'flex'
                showView('console')
            }
        })
    }

    const btnCopyCrash = $('#btn-copy-crash')
    if (btnCopyCrash) {
        btnCopyCrash.addEventListener('click', () => {
            const textArea = $('#crash-log-text')
            if (textArea) {
                navigator.clipboard.writeText(textArea.value).then(() => {
                    showToast("Rapport copié dans le presse-papiers !", "success")
                }).catch(err => {
                    showToast("Erreur lors de la copie", "error")
                })
            }
        })
    }

    // Players Modal
    $('#btn-show-players')?.addEventListener('click', () => {
        $('#modal-players').style.display = 'flex'
        updatePlayersModal()
    })
    $('#btn-close-players')?.addEventListener('click', () => {
        $('#modal-players').style.display = 'none'
    })
}

// ── Mods Optionnels ──────────────────────────────────────
async function renderMods() {
    const list = $('#mods-list')
    const scrollContainer = $('#view-mods')
    if (!list || !scrollContainer) return

    const scrollPos = scrollContainer.scrollTop
    list.innerHTML = '<div style="color: var(--text-muted); text-align: center; padding: 24px;">Chargement des mods...</div>'

    try {
        const mods = await window.nexaria.getOptionalMods()
        list.innerHTML = ''

        if (!mods || mods.length === 0) {
            list.innerHTML = '<div style="color: var(--text-muted); text-align: center; padding: 24px;">Aucun mod optionnel disponible.</div>'
            return
        }

        mods.forEach(mod => {
            const card = document.createElement('div')
            card.style.cssText = `
                background: var(--bg-panel);
                border: 1px solid var(--border);
                border-radius: var(--radius-md);
                padding: 20px 24px;
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 24px;
                transition: transform 0.2s;
            `

            const info = document.createElement('div')
            info.innerHTML = `
                <div style="font-size: 16px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px;">${mod.name}</div>
                <div style="font-size: 13px; color: var(--text-muted); line-height: 1.5;">${mod.description}</div>
            `

            const actionContainer = document.createElement('div')
            const btn = document.createElement('button')
            btn.className = mod.installed ? 'btn-cancel' : 'btn-play'
            btn.style.width = '140px'
            btn.style.padding = '10px 0'
            btn.style.fontSize = '12px'
            btn.innerHTML = mod.installed ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 14px; height: 14px; margin-right: 4px; vertical-align: middle;"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path><path d="M10 11v6M14 11v6"></path></svg>Désinstaller' : 'Installer'

            btn.addEventListener('click', async () => {
                if (state.isLaunching) {
                    showToast("Impossible de modifier les mods avec le jeu en cours de lancement.", "error")
                    return
                }

                btn.disabled = true
                btn.innerHTML = 'En cours...'
                try {
                    const res = await window.nexaria.toggleOptionalMod({ modId: mod.id })
                    if (res.status === 'success') {
                        showToast(res.installed ? `${mod.name} a été installé !` : `${mod.name} a été supprimé.`, 'success')
                        renderMods()
                    } else {
                        showToast(`Erreur : ${res.message}`, 'error')
                        btn.disabled = false
                        btn.innerHTML = mod.installed ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 14px; height: 14px; margin-right: 4px; vertical-align: middle;"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path><path d="M10 11v6M14 11v6"></path></svg>Désinstaller' : 'Installer'
                    }
                } catch (e) {
                    showToast(`Erreur de connexion`, 'error')
                    btn.disabled = false
                    btn.innerHTML = mod.installed ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 14px; height: 14px; margin-right: 4px; vertical-align: middle;"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path><path d="M10 11v6M14 11v6"></path></svg>Désinstaller' : 'Installer'
                }
            })

            actionContainer.appendChild(btn)
            card.appendChild(info)
            card.appendChild(actionContainer)
            list.appendChild(card)
        })

        // Restore scroll position after DOM update
        scrollContainer.scrollTop = scrollPos
    } catch (e) {
        list.innerHTML = '<div style="color: #ef4444; text-align: center; padding: 24px;">Erreur lors du chargement des mods.</div>'
    }
}


// ── Screenshots ──────────────────────────────────────────
async function renderScreenshots() {
    const grid = $('#screenshots-grid')
    if (!grid) return

    grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 40px;">Chargement...</div>'

    try {
        const list = await window.nexaria.getScreenshots()
        grid.innerHTML = ''

        if (!list || list.length === 0) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 40px;">Aucune capture d\'écran trouvée.</div>'
            return
        }

        list.forEach(scr => {
            const card = document.createElement('div')
            card.className = 'screenshot-card'
            card.style.cssText = `
                background: var(--bg-panel);
                border: 1px solid var(--border);
                border-radius: var(--radius-md);
                overflow: hidden;
                display: flex;
                flex-direction: column;
                transition: transform 0.2s;
            `

            // Relative date formatting
            const dateStr = new Date(scr.date).toLocaleDateString()

            card.innerHTML = `
                <div class="screenshot-img-box" style="aspect-ratio: 16/9; background: #000; cursor: pointer; position: relative; overflow: hidden;">
                    <img src="${scr.url}" style="width: 100%; height: 100%; object-fit: cover; opacity: 0.8; transition: opacity 0.3s;" />
                </div>
                <div style="padding: 12px; display: flex; flex-direction: column; gap: 8px;">
                    <div style="font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: var(--text-primary);">${scr.name}</div>
                    <div style="font-size: 10px; color: var(--text-muted);">${dateStr} • ${(scr.size / 1024 / 1024).toFixed(2)} Mo</div>
                    <div style="display: flex; gap: 8px; margin-top: 4px;">
                        <button class="settings-btn btn-open-scr" style="flex: 1; padding: 4px; font-size: 10px;">Voir</button>
                        <button class="settings-btn btn-delete-scr" style="flex: 1; padding: 4px; font-size: 10px; border-color: var(--red); color: var(--red);">Supprimer</button>
                    </div>
                </div>
            `

            const imgBox = card.querySelector('.screenshot-img-box')
            imgBox.addEventListener('mouseenter', () => imgBox.querySelector('img').style.opacity = '1')
            imgBox.addEventListener('mouseleave', () => imgBox.querySelector('img').style.opacity = '0.8')
            imgBox.addEventListener('click', () => window.nexaria.openScreenshot(scr.name))

            card.querySelector('.btn-open-scr').addEventListener('click', () => window.nexaria.openScreenshot(scr.name))
            card.querySelector('.btn-delete-scr').addEventListener('click', async (e) => {
                e.stopPropagation()
                if (confirm(`Supprimer ${scr.name} ?`)) {
                    const res = await window.nexaria.deleteScreenshot(scr.name)
                    if (res) {
                        showToast('Capture supprimée', 'info')
                        renderScreenshots()
                    }
                }
            })

            grid.appendChild(card)
        })
    } catch (e) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--red); padding: 40px;">Erreur lors du chargement.</div>'
    }
}


// ── Map Availability ─────────────────────────────────────
async function checkMapAvailability() {
    const mapFrame = $('#map-frame')
    const offlinePlaceholder = $('#map-offline')
    const url = 'https://map.nexaria.netlib.re'

    if (!mapFrame || !offlinePlaceholder) return

    // Show loading or just try
    try {
        // Use fetch with a short timeout to check if the server responds
        const controller = new AbortController()
        const timeoutId = setTimeout(() => controller.abort(), 5000)

        const response = await fetch(url, {
            method: 'HEAD',
            mode: 'no-cors', // Map might not have CORS, so we use no-cors to at least see if it's "alive"
            signal: controller.signal
        }).catch(() => ({ ok: false })) // Catch abort/network errors

        clearTimeout(timeoutId)

        // With no-cors, we can't see the status code, so we try a "cors" request to specifically catch 502
        // IF the server is Azuriom/Cloudflare, we might be able to get status or it will just fail.
        const checkStatus = await fetch(url).catch(() => ({ status: 502 }))

        if (checkStatus.status === 502 || checkStatus.status === 504 || !checkStatus.ok) {
            mapFrame.style.display = 'none'
            offlinePlaceholder.style.display = 'flex'
        } else {
            mapFrame.style.display = 'block'
            offlinePlaceholder.style.display = 'none'
            if (mapFrame.src === 'about:blank' || mapFrame.src !== url) {
                mapFrame.src = url
            }
        }
    } catch (err) {
        mapFrame.style.display = 'none'
        offlinePlaceholder.style.display = 'flex'
    }
}

// Map Retry button
$('#btn-retry-map')?.addEventListener('click', checkMapAvailability)

// Live Listeners
window.nexaria.onScreenshotsUpdated(() => {
    if (state.currentView === 'screenshots') {
        renderScreenshots()
    }
})

// Start when DOM is ready
document.addEventListener('DOMContentLoaded', init)
