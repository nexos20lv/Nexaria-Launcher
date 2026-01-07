package com.nexora.launcher;

import com.nexora.launcher.config.LauncherConfig;
import com.nexora.launcher.downloader.GitHubModManager;
import com.nexora.launcher.ui.FuturisticLauncherGUI;
import com.nexora.launcher.updater.GitHubUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LauncherApp {
    private static final Logger logger = LoggerFactory.getLogger(LauncherApp.class);
    private static final String LAUNCHER_VERSION = "1.0.0";

    public static void main(String[] args) {
        logger.info("Démarrage du Nexora Launcher v{}", LAUNCHER_VERSION);
        
        try {
            // Charger la configuration
            LauncherConfig.loadConfig();
            logger.info("Configuration chargée");
            LauncherConfig config = LauncherConfig.getInstance();
            
            // Vérifier les mises à jour du launcher via GitHub
            if (config.isAutoUpdate()) {
                try {
                    GitHubUpdater updater = new GitHubUpdater(config.githubRepo, LAUNCHER_VERSION);
                    if (updater.hasUpdate()) {
                        logger.info("Mise à jour du launcher disponible");
                        GitHubUpdater.GitHubRelease release = updater.getLatestRelease();
                        String updatePath = LauncherConfig.getCacheDir() + "/nexora-launcher-update.jar";
                        updater.downloadUpdate(release, updatePath);
                        GitHubUpdater.installUpdate(updatePath);
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Erreur lors de la vérification des mises à jour", e);
                }
            }
            
            // Synchroniser uniquement les mods/configs locaux depuis le dossier data/
            try {
                GitHubModManager modManager = new GitHubModManager(null);
                modManager.syncLocalMods();
                modManager.syncLocalConfigs();
            } catch (Exception e) {
                logger.warn("Erreur lors de la synchronisation des fichiers locaux", e);
            }

            // Lancer l'interface graphique futuriste
            FuturisticLauncherGUI gui = new FuturisticLauncherGUI();
            gui.setVisible(true);
        } catch (Exception e) {
            logger.error("Erreur lors du démarrage du launcher", e);
            e.printStackTrace();
        }
    }

    public static String getVersion() {
        return LAUNCHER_VERSION;
    }
}
