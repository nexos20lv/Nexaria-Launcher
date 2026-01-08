package com.nexaria.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.downloader.GitHubModManager;
import com.nexaria.launcher.ui.LauncherWindow;
import com.nexaria.launcher.updater.UpdateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;

public class NexariaLauncher {
    private static final Logger logger = LoggerFactory.getLogger(NexariaLauncher.class);
    private static final String LAUNCHER_VERSION = "1.0.5";

    public static void main(String[] args) {
        // Initialiser FlatLaf L&F pour un style moderne
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
        } catch (Exception e) {
            logger.warn("Impossible d'initialiser FlatLaf, utilisation du L&F par défaut", e);
        }

        // Nom d'application OS (macOS menu/dock)
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac")) {
                System.setProperty("apple.awt.application.name", "Nexaria Launcher");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Nexaria Launcher");
            }
        } catch (Exception ignore) { }

        logger.info("Démarrage du Nexaria Launcher v{}", LAUNCHER_VERSION);

        try {
            // Charger la configuration
            LauncherConfig.loadConfig();
            logger.info("Configuration chargée");
            LauncherConfig config = LauncherConfig.getInstance();

            // Afficher le splash screen de mise à jour si activé
            if (config.isAutoUpdate() && config.githubRepo != null && !config.githubRepo.isEmpty()) {
                try {
                    UpdateManager updateManager = new UpdateManager(config.githubRepo, LAUNCHER_VERSION);
                    
                    // Vérifier si une mise à jour est disponible sans afficher le splash à chaque fois
                    // Le cache empêche les vérifications trop fréquentes (24h)
                    updateManager.autoUpdateOnStartup();
                    
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
}
