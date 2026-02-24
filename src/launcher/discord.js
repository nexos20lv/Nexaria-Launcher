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

function setActivity(details, state = 'Prêt à jouer') {
    if (!client) return

    client.setActivity({
        details: details,
        state: state,
        startTimestamp: new Date(),
        largeImageKey: 'logo',
        largeImageText: 'Nexaria Launcher',
        instance: false,
    }).catch(err => {
        log.warn('Error updating Discord presence:', err.message)
    })
}

function destroyRPC() {
    if (client) {
        client.destroy().catch(err => {
            log.warn('Error destroying Discord RPC:', err.message)
        })
        client = null
    }
}

module.exports = { initRPC, setActivity, destroyRPC }
