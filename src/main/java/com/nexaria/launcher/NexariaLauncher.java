package com.nexaria.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.downloader.GitHubModManager;
import com.nexaria.launcher.ui.LauncherWindow;
import com.nexaria.launcher.updater.UpdateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NexariaLauncher {
    private static final Logger logger = LoggerFactory.getLogger(NexariaLauncher.class);
    private static final String LAUNCHER_VERSION_FALLBACK = "0.0.0";

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
        } catch (Exception e) {
            logger.warn("Erreur configuration OS", e);
        }

        String currentVersion = LAUNCHER_VERSION_FALLBACK;
        try {
            currentVersion = LauncherConfig.getInstance().launcherVersion != null
                    ? LauncherConfig.getInstance().launcherVersion
                    : LAUNCHER_VERSION_FALLBACK;
        } catch (Exception e) {
            logger.warn("Erreur lecture version", e);
        }

        logger.info("Démarrage du Nexaria Launcher v{}", currentVersion);

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
                    String updateVersion = config.launcherVersion != null ? config.launcherVersion
                            : LAUNCHER_VERSION_FALLBACK;
                    final UpdateManager updateManager = new UpdateManager(config.githubRepo, updateVersion);
                    CountDownLatch latch = new CountDownLatch(1);

                    SwingUtilities.invokeLater(() -> {
                        try {
                            com.nexaria.launcher.ui.UpdateSplashScreen splash = new com.nexaria.launcher.ui.UpdateSplashScreen();
                            splash.startUpdateCheck(updateManager, latch::countDown);
                        } catch (Exception ex) {
                            logger.warn("Affichage du splash update impossible", ex);
                            latch.countDown();
                        }
                    });

                    // Attendre que la vérif/erreur se termine ou qu'une installation redémarre
                    // (120s max)
                    latch.await(120, TimeUnit.SECONDS);
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
        try {
            return LauncherConfig.getInstance().launcherVersion != null ? LauncherConfig.getInstance().launcherVersion
                    : LAUNCHER_VERSION_FALLBACK;
        } catch (Exception e) {
            return LAUNCHER_VERSION_FALLBACK;
        }
    }

    /**
     * Détecte si l'application tourne en mode développement.
     * En mode dev, l'app tourne depuis des fichiers .class dans target/classes,
     * alors qu'en production elle tourne depuis un JAR packagé.
     */
    private static boolean isDevelopmentMode() {
        try {
            String path = NexariaLauncher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            // En mode dev, le path contient "target/classes" et n'est pas un .jar
            return path.contains("/target/classes") || path.contains("\\target\\classes") || !path.endsWith(".jar");
        } catch (Exception e) {
            logger.debug("Impossible de détecter le mode dev, assume production", e);
            return false;
        }
    }
}
