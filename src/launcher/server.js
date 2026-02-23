// ============================================================
// Nexaria Launcher - Minecraft Server Ping (SLP Protocol 1.7+)
// ============================================================
const net = require('net')

// CONFIGURE: Adresse et port de ton serveur Minecraft
const MC_HOST = 'mc.nemesius.com'
const MC_PORT = 25568

// ── Encodage VarInt (format Minecraft) ───────────────────
function writeVarInt(value) {
    const bytes = []
    do {
        let byte = value & 0x7F
        value >>>= 7
        if (value !== 0) byte |= 0x80
        bytes.push(byte)
    } while (value !== 0)
    return Buffer.from(bytes)
}

// ── Décodage VarInt depuis un Buffer ─────────────────────
function readVarInt(buf, offset = 0) {
    let value = 0
    let shift = 0
    let pos = offset

    while (pos < buf.length) {
        const byte = buf[pos++]
        value |= (byte & 0x7F) << shift
        shift += 7
        if ((byte & 0x80) === 0) break
        if (shift >= 35) throw new Error('VarInt trop long')
    }

    return { value, bytesRead: pos - offset }
}

// ── Encoder une String Minecraft (VarInt length + UTF-8) ─
function writeString(str) {
    const strBuf = Buffer.from(str, 'utf8')
    return Buffer.concat([writeVarInt(strBuf.length), strBuf])
}

// ── Construire un paquet Minecraft ───────────────────────
function buildPacket(packetId, ...payloads) {
    const id = writeVarInt(packetId)
    const payload = Buffer.concat(payloads)
    const body = Buffer.concat([id, payload])
    const length = writeVarInt(body.length)
    return Buffer.concat([length, body])
}

/**
 * Ping a Minecraft server using the modern SLP protocol (1.7+)
 * Returns: { online, players, max, motd, version }
 */
function getServerStatus(host = MC_HOST, port = MC_PORT, timeoutMs = 5000) {
    return new Promise((resolve) => {
        const fallback = { online: false, players: 0, max: 0, motd: '', version: '' }
        let resolved = false
        let buffer = Buffer.alloc(0)

        const done = (result) => {
            if (resolved) return
            resolved = true
            clearTimeout(timer)
            socket.destroy()
            resolve(result)
        }

        const timer = setTimeout(() => done(fallback), timeoutMs)

        const socket = net.createConnection({ host, port })

        socket.on('error', () => done(fallback))
        socket.on('timeout', () => done(fallback))

        socket.on('connect', () => {
            // ── 1. Handshake packet (0x00) ──────────────────────
            // Fields: protocolVersion (VarInt) | host (String) | port (UShort) | nextState (VarInt=1)
            const portBuf = Buffer.alloc(2)
            portBuf.writeUInt16BE(port, 0)

            const handshake = buildPacket(
                0x00,
                writeVarInt(767),      // Protocol version 1.21.1 = 767
                writeString(host),     // Hostname
                portBuf,               // Port
                writeVarInt(1),        // Next state: Status
            )

            // ── 2. Status Request packet (0x00, empty) ──────────
            const statusRequest = buildPacket(0x00)

            socket.write(Buffer.concat([handshake, statusRequest]))
        })

        socket.on('data', (data) => {
            buffer = Buffer.concat([buffer, data])

            try {
                // Lire la longueur totale du paquet
                const lenResult = readVarInt(buffer, 0)
                const totalLength = lenResult.value + lenResult.bytesRead

                // Attendre que tout le paquet soit reçu
                if (buffer.length < totalLength) return

                let offset = lenResult.bytesRead

                // Lire le packet ID
                const idResult = readVarInt(buffer, offset)
                offset += idResult.bytesRead

                if (idResult.value !== 0x00) return // Pas un paquet Status Response

                // Lire la longueur de la String JSON
                const jsonLenResult = readVarInt(buffer, offset)
                offset += jsonLenResult.bytesRead

                // Lire la String JSON
                const jsonStr = buffer.slice(offset, offset + jsonLenResult.value).toString('utf8')
                const json = JSON.parse(jsonStr)

                done({
                    online: true,
                    players: json.players?.online ?? 0,
                    max: json.players?.max ?? 0,
                    motd: extractMotd(json.description),
                    version: json.version?.name ?? '',
                })
            } catch {
                // Paquet incomplet, on attend plus de données
            }
        })
    })
}

// ── Extraire le texte du MOTD (peut être String ou Object) 
function extractMotd(desc) {
    if (!desc) return ''
    if (typeof desc === 'string') return desc
    if (typeof desc === 'object') {
        if (desc.text) return desc.text
        if (Array.isArray(desc.extra)) {
            return desc.extra.map(e => (typeof e === 'string' ? e : e.text || '')).join('')
        }
    }
    return ''
}

module.exports = { getServerStatus }
