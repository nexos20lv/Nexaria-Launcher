const fs = require('fs')
const path = require('path')
const crypto = require('crypto')
const http = require('http')
const https = require('https')

const manifestPath = path.join(__dirname, '..', 'php-server', 'optional_mods.json')

function fetchSha1(fileName) {
    return new Promise((resolve, reject) => {
        const filePath = path.join(__dirname, '..', 'php-server', 'files', 'mods', fileName);
        if (!fs.existsSync(filePath)) {
            reject(new Error(`File not found: ${filePath}`));
            return;
        }

        const hash = crypto.createHash('sha1');
        const stream = fs.createReadStream(filePath);

        stream.on('data', chunk => hash.update(chunk));
        stream.on('end', () => resolve(hash.digest('hex')));
        stream.on('error', reject);
    });
}

async function run() {
    const raw = fs.readFileSync(manifestPath, 'utf8')
    const mods = JSON.parse(raw)

    for (const mod of mods) {
        if (!mod.fileName) {
            console.log(`Skipping ${mod.id} (no fileName)`)
            continue
        }
        process.stdout.write(`Hashing ${mod.id} (${mod.fileName})... `)
        try {
            const sha1 = await fetchSha1(mod.fileName)
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
