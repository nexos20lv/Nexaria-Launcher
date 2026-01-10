package com.nexaria.launcher;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.downloader.GitHubModManager;
import com.nexaria.launcher.ui.LauncherWindow;
import com.nexaria.launcher.updater.UpdateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LauncherApp {
    private static final Logger logger = LoggerFactory.getLogger(LauncherApp.class);
    private static final String LAUNCHER_VERSION = "1.0.5";

    public static void main(String[] args) {
        logger.info("Démarrage du Nexaria Launcher v{}", LAUNCHER_VERSION);
        
        try {
            // Charger la configuration
            LauncherConfig.loadConfig();
            logger.info("Configuration chargée");
            LauncherConfig config = LauncherConfig.getInstance();
            
            // Détecter le mode développement
            boolean isDevMode = isDevelopmentMode();
            if (isDevMode) {
                logger.info("Mode développement détecté - auto-update désactivé");
            }
            
            // Afficher le splash screen de mise à jour si activé (sauf en mode dev)
            if (!isDevMode && config.isAutoUpdate() && config.githubRepo != null && !config.githubRepo.isEmpty()) {
                try {
                    UpdateManager updateManager = new UpdateManager(config.githubRepo, LAUNCHER_VERSION);
                    
                    // Vérifier si une mise à jour est disponible sans afficher le splash à chaque fois
                    // Le cache empêche les vérifications trop fréquentes (24h)
                    updateManager.autoUpdateOnStartup();
                    
                } catch (Exception e) {
                    logger.warn("Erreur lors de la vérification des mises à jour", e);
                }
            }
            
            // Synchroniser data/ complet (mods + config)
            try {
                GitHubModManager modManager = new GitHubModManager(null);
                modManager.syncAllData();
            } catch (Exception e) {
                logger.warn("Erreur lors de la synchronisation de data/", e);
            }

            // Lancer l'interface graphique
            LauncherWindow gui = new LauncherWindow();
            gui.setVisible(true);
        } catch (Exception e) {
            logger.error("Erreur lors du démarrage du launcher", e);
            e.printStackTrace();
        }
    }

    public static String getVersion() {
        return LAUNCHER_VERSION;
    }

    /**
     * Détecte si l'application tourne en mode développement.
     * En mode dev, l'app tourne depuis des fichiers .class dans target/classes,
     * alors qu'en production elle tourne depuis un JAR packagé.
     */
    private static boolean isDevelopmentMode() {
        try {
            String path = LauncherApp.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            // En mode dev, le path contient "target/classes" et n'est pas un .jar
            return path.contains("/target/classes") || path.contains("\\target\\classes") || !path.endsWith(".jar");
        } catch (Exception e) {
            logger.debug("Impossible de détecter le mode dev, assume production", e);
            return false;
        }
    }
}
