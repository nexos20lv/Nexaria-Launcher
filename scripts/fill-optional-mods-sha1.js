const fs = require('fs')
const path = require('path')
const crypto = require('crypto')
const http = require('http')
const https = require('https')

const manifestPath = path.join(__dirname, '..', 'php-server', 'optional_mods.json')

function fetchSha1(url) {
    return new Promise((resolve, reject) => {
        const client = url.startsWith('https') ? https : http
        const hash = crypto.createHash('sha1')

        const req = client.get(url, (res) => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                fetchSha1(res.headers.location).then(resolve).catch(reject)
                return
            }

            if (res.statusCode !== 200) {
                reject(new Error(`HTTP ${res.statusCode} for ${url}`))
                return
            }

            res.on('data', chunk => hash.update(chunk))
            res.on('end', () => resolve(hash.digest('hex')))
            res.on('error', reject)
        })

        req.setTimeout(30000, () => req.destroy(new Error(`Timeout for ${url}`)))
        req.on('error', reject)
    })
}

async function run() {
    const raw = fs.readFileSync(manifestPath, 'utf8')
    const mods = JSON.parse(raw)

    for (const mod of mods) {
        if (!mod.url) continue
        process.stdout.write(`Hashing ${mod.id}... `)
        try {
            const sha1 = await fetchSha1(mod.url)
            mod.sha1 = sha1
            process.stdout.write('ok\n')
        } catch (err) {
            process.stdout.write(`failed (${err.message})\n`)
        }
    }

    fs.writeFileSync(manifestPath, `${JSON.stringify(mods, null, 2)}\n`, 'utf8')
    console.log('Done. optional_mods.json updated.')
}

run().catch((err) => {
    console.error(err)
    process.exit(1)
})
