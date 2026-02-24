const fs = require('fs')
const path = require('path')
const { shell } = require('electron')
const log = require('electron-log')

/**
 * Screenshot Manager for Nexaria Launcher
 */
class ScreenshotManager {
    constructor(gameDir) {
        this.screenshotsDir = path.join(gameDir, 'screenshots')
        if (!fs.existsSync(this.screenshotsDir)) {
            try {
                fs.mkdirSync(this.screenshotsDir, { recursive: true })
            } catch (err) {
                log.error('Failed to create screenshots directory:', err)
            }
        }
    }

    /**
     * List all screenshots in the directory
     * @returns {Array} List of screenshot objects { name, path, date, size }
     */
    list() {
        if (!fs.existsSync(this.screenshotsDir)) return []

        try {
            const files = fs.readdirSync(this.screenshotsDir)
            return files
                .filter(file => /\.(png|jpg|jpeg)$/i.test(file))
                .map(file => {
                    const filePath = path.join(this.screenshotsDir, file)
                    const stats = fs.statSync(filePath)
                    return {
                        name: file,
                        path: filePath,
                        url: `asset://${filePath}`,
                        date: stats.mtime,
                        size: stats.size
                    }
                })
                .sort((a, b) => b.date - a.date)
        } catch (err) {
            log.error('Error listing screenshots:', err)
            return []
        }
    }

    /**
     * Open a screenshot with the system viewer
     * @param {string} fileName 
     */
    open(fileName) {
        const filePath = path.join(this.screenshotsDir, fileName)
        if (fs.existsSync(filePath)) {
            shell.openPath(filePath)
            return true
        }
        return false
    }

    /**
     * Delete a screenshot
     * @param {string} fileName 
     */
    delete(fileName) {
        const filePath = path.join(this.screenshotsDir, fileName)
        if (fs.existsSync(filePath)) {
            try {
                fs.unlinkSync(filePath)
                return true
            } catch (err) {
                log.error(`Failed to delete screenshot ${fileName}:`, err)
                return false
            }
        }
        return false
    }

    /**
     * Open the screenshots folder in file explorer
     */
    openFolder() {
        if (fs.existsSync(this.screenshotsDir)) {
            shell.openPath(this.screenshotsDir)
            return true
        }
        return false
    }
}

module.exports = ScreenshotManager
