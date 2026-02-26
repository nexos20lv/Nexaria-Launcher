// ============================================================
// Nexaria Launcher - Intelligent Crash Diagnostics
// ============================================================
const fs = require('fs')
const path = require('path')

/**
 * Analyzes logs/crash reports to find a probable cause and solution.
 */
function analyze(gameDir, fullLog = '') {
    const findings = []

    // 1. Check for OutOfMemory
    if (fullLog.includes('java.lang.OutOfMemoryError')) {
        findings.push({
            cause: 'Mémoire insuffisante (RAM)',
            solution: 'Augmentez la RAM allouée dans les Paramètres du launcher (ex: 4096 Mo).'
        })
    }

    // 2. Check for Java Version issues
    if (fullLog.includes('Has been compiled by a more recent version of the Java Runtime') ||
        fullLog.includes('UnsupportedClassVersionError')) {
        findings.push({
            cause: 'Version de Java incompatible',
            solution: 'Installez une version plus récente de Java (Java 17 ou 21 est recommandé pour Minecraft 1.21.11).'
        })
    }

    // 3. Check for Incompatible Mods (Fabric/Forge)
    if (fullLog.includes('Incompatible mods found') || fullLog.includes('ModResolutionException')) {
        findings.push({
            cause: 'Conflit entre certains mods',
            solution: 'Vérifiez vos mods optionnels ou essayez de les désactiver un par un pour identifier le coupable.'
        })
    }

    // 4. Graphics issues
    if (fullLog.includes('org.lwjgl.LWJGLException: Pixel format not accelerated') ||
        fullLog.includes('glfwCreateWindow: GLFW_API_UNAVAILABLE')) {
        findings.push({
            cause: 'Problème d\'accélération graphique',
            solution: 'Mettez à jour vos pilotes de carte graphique (NVIDIA/AMD/Intel).'
        })
    }

    // 5. Corrupted world (subtle)
    if (fullLog.includes('java.util.zip.ZipException: invalid distance too far back')) {
        findings.push({
            cause: 'Fichier de monde ou archive corrompu',
            solution: 'Tentez de supprimer le dossier \'versions\' ou \'libraries\' pour forcer une réinstallation propre.'
        })
    }

    return findings.length > 0 ? findings[0] : null
}

module.exports = { analyze }
