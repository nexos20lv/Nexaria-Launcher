// repair function module
const fs = require('fs')
const path = require('path')

async function repairGame({ gameDir, version }) {
    if (!gameDir || !version) throw new Error('gameDir and version are required for repair.')

    console.log(`[Repair] Starting repair for version ${version} in ${gameDir}`)

    // 1. Delete version folder
    const versionDir = path.join(gameDir, 'versions', version)
    if (fs.existsSync(versionDir)) {
        fs.rmSync(versionDir, { recursive: true, force: true })
        console.log(`[Repair] Deleted ${versionDir}`)
    }

    // Attempt to delete fabric/forge versions if it exists too (wildcard match or specific)
    const versionsBaseDir = path.join(gameDir, 'versions')
    if (fs.existsSync(versionsBaseDir)) {
        const dirs = fs.readdirSync(versionsBaseDir)
        for (const dir of dirs) {
            if (dir.includes(version)) {
                const fullPath = path.join(versionsBaseDir, dir)
                fs.rmSync(fullPath, { recursive: true, force: true })
                console.log(`[Repair] Deleted associated version dir ${fullPath}`)
            }
        }
    }

    // 2. Delete assets/indexes
    const assetsIndexesDir = path.join(gameDir, 'assets', 'indexes')
    if (fs.existsSync(assetsIndexesDir)) {
        fs.rmSync(assetsIndexesDir, { recursive: true, force: true })
        console.log(`[Repair] Deleted ${assetsIndexesDir}`)
    }

    console.log('[Repair] Repair cleanup completed.')
}

module.exports = { repairGame }
