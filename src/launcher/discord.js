const RPC = require('discord-rpc')
const log = require('electron-log')

const clientId = '1459940548009001304' // ID de l'application Nexaria
let client = null

function initRPC() {
    client = new RPC.Client({ transport: 'ipc' })

    client.on('ready', () => {
        log.info('Discord RPC ready')
        setActivity('Dans le menu')
    })

    client.login({ clientId }).catch(err => {
        log.warn('Could not connect to Discord RPC:', err.message)
    })
}

let rpcStart = new Date()

function setActivity(details, stateText = 'Prêt à jouer', showTimestamp = true) {
    if (!client) return

    const activity = {
        details: details,
        state: stateText,
        largeImageKey: 'logo',
        largeImageText: 'Nexaria Launcher',
        instance: false,
        buttons: [
            { label: 'Rejoindre Nexaria', url: 'https://nexaria.site' },
            { label: 'Discord', url: 'https://discord.gg/nexaria' }
        ]
    }

    if (showTimestamp) {
        activity.startTimestamp = rpcStart
    }

    client.setActivity(activity).catch(err => {
        log.warn('Error updating Discord presence:', err.message)
    })
}

function resetTimestamp() {
    rpcStart = new Date()
}

function destroyRPC() {
    if (client) {
        client.destroy().catch(err => {
            log.warn('Error destroying Discord RPC:', err.message)
        })
        client = null
    }
}

module.exports = { initRPC, setActivity, resetTimestamp, destroyRPC }
