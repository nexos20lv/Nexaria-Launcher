package com.nexaria.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.nexaria.launcher.cache.CacheFactory;
import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.core.ServiceContainer;
import com.nexaria.launcher.downloader.GitHubModManager;
import com.nexaria.launcher.health.HealthCheckService;
import com.nexaria.launcher.logging.LoggingService;
import com.nexaria.launcher.resilience.CircuitBreaker;
import com.nexaria.launcher.ui.LauncherWindow;
import com.nexaria.launcher.ui.notification.ToastNotificationManager;
import com.nexaria.launcher.updater.UpdateManager;

import javax.swing.*;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Point d'entrée unifié du Nexaria Launcher avec injection de dépendances.
 * Cette classe remplace LauncherApp et NexariaLauncher pour une architecture unifiée.
 * 
 * @since 1.1.0
 */
public class NexariaLauncherMain {
    private static final LoggingService logger = LoggingService.getLogger(NexariaLauncherMain.class);
    private static final String LAUNCHER_VERSION_FALLBACK = "0.0.0";

    public static void main(String[] args) {
        // Générer un session ID unique pour cette session
        String sessionId = UUID.randomUUID().toString();
        logger.setSessionId(sessionId);
        logger.startOperation("launcher_startup");

        try {
            // 1. Charger la configuration
            LauncherConfig.loadConfig();
            LauncherConfig config = LauncherConfig.getInstance();
            String version = getVersion(config);
            
            logger.eventBuilder()
                    .level(LoggingService.LogLevel.INFO)
                    .message("Starting Nexaria Launcher")
                    .addContext("version", version)
                    .addContext("sessionId", sessionId)
                    .log();

            // 2. Initialiser l'UI Look & Feel
            initializeLookAndFeel();

            // 3. Détecter le mode développement
            boolean isDevMode = isDevelopmentMode();
            if (isDevMode) {
                logger.info("Development mode detected - auto-update disabled");
            }

            // 4. Health Check au démarrage
            performHealthCheck();

            // 5. Initialiser le conteneur de services
            initializeServices(config, version, isDevMode);

            // 6. Vérifier les mises à jour (si activé)
            if (!isDevMode && config.isAutoUpdate()) {
                checkForUpdates(config, version);
            }

            // 7. Synchroniser les mods et configs
            synchronizeGameData();

            // 8. Lancer l'interface graphique
            launchUI();

            logger.info("Launcher startup completed successfully");

        } catch (Exception e) {
            logger.error("Fatal error during launcher startup", e);
            showFatalErrorDialog(e);
            System.exit(1);
        } finally {
            logger.endOperation("launcher_startup");
        }
    }

    /**
     * Initialise FlatLaf Look & Feel.
     */
    private static void initializeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            logger.debug("FlatLaf Look & Feel initialized");
        } catch (Exception e) {
            logger.warn("Failed to initialize FlatLaf, using default L&F", e);
        }

        // Configuration spécifique macOS
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac")) {
                System.setProperty("apple.awt.application.name", "Nexaria Launcher");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Nexaria Launcher");
                logger.debug("macOS application name configured");
            }
        } catch (Exception e) {
            logger.warn("Failed to configure OS-specific settings", e);
        }
    }

    /**
     * Effectue les vérifications de santé au démarrage.
     */
    private static void performHealthCheck() {
        logger.info("Performing health checks");
        
        HealthCheckService healthCheck = new HealthCheckService();
        HealthCheckService.HealthCheckResult result = healthCheck.performHealthCheck();

        if (!result.isAllPassed()) {
            logger.warn("Some health checks failed");
            for (HealthCheckService.HealthCheck check : result.getFailedChecks()) {
                logger.eventBuilder()
                        .level(LoggingService.LogLevel.WARN)
                        .message("Health check failed")
                        .addContext("check", check.getName())
                        .addContext("message", check.getMessage())
                        .addContext("severity", check.getSeverity().name())
                        .log();
            }

            if (result.hasCriticalIssues()) {
                logger.error("Critical health check issues detected");
                showHealthCheckWarning(result);
            }
        } else {
            logger.info("All health checks passed");
        }
    }

    /**
     * Initialise les services avec injection de dépendances.
     */
    private static void initializeServices(LauncherConfig config, String version, boolean isDevMode) {
        logger.debug("Initializing services");
        
        ServiceContainer container = ServiceContainer.getInstance();
        
        // Enregistrer les services
        container.registerSingleton(LauncherConfig.class, config);
        container.registerSingleton(ToastNotificationManager.class, ToastNotificationManager.getInstance());
        
        // Circuit breakers pour les appels réseau
        CircuitBreaker githubBreaker = CircuitBreaker.builder("github")
                .failureThreshold(5)
                .timeout(Duration.ofSeconds(10))
                .resetTimeout(Duration.ofMinutes(5))
                .build();
        container.registerSingleton(CircuitBreaker.class, githubBreaker);
        
        logger.debug("Services initialized");
    }

    /**
     * Vérifie les mises à jour du launcher.
     */
    private static void checkForUpdates(LauncherConfig config, String version) {
        if (config.githubRepo == null || config.githubRepo.isEmpty()) {
            logger.debug("No GitHub repo configured, skipping update check");
            return;
        }

        logger.info("Checking for updates");
        
        try {
            UpdateManager updateManager = new UpdateManager(config.githubRepo, version);
            CountDownLatch latch = new CountDownLatch(1);

            SwingUtilities.invokeLater(() -> {
                try {
                    com.nexaria.launcher.ui.UpdateSplashScreen splash = 
                            new com.nexaria.launcher.ui.UpdateSplashScreen();
                    splash.startUpdateCheck(updateManager, latch::countDown);
                } catch (Exception ex) {
                    logger.warn("Failed to show update splash screen", ex);
                    latch.countDown();
                }
            });

            // Attendre max 120 secondes
            boolean completed = latch.await(120, TimeUnit.SECONDS);
            if (!completed) {
                logger.warn("Update check timed out");
            }
        } catch (Exception e) {
            logger.warn("Error during update check", e);
        }
    }

    /**
     * Synchronise les mods et configs du jeu.
     */
    private static void synchronizeGameData() {
        logger.info("Synchronizing game data (mods + config)");
        
        try {
            GitHubModManager modManager = new GitHubModManager(null);
            modManager.syncAllData();
            logger.info("Game data synchronized successfully");
        } catch (Exception e) {
            logger.warn("Error during game data synchronization", e);
            // Non-fatal, continue
        }
    }

    /**
     * Lance l'interface graphique principale.
     */
    private static void launchUI() {
        logger.info("Launching main UI");
        
        SwingUtilities.invokeLater(() -> {
            try {
                LauncherWindow window = new LauncherWindow();
                window.setVisible(true);
                logger.info("Main window displayed");
                
                // Afficher une notification de bienvenue
                ToastNotificationManager.getInstance().showSuccess(
                        "Nexaria Launcher",
                        "Launcher démarré avec succès!"
                );
            } catch (Exception e) {
                logger.error("Fatal error launching UI", e);
                showFatalErrorDialog(e);
                System.exit(1);
            }
        });
    }

    /**
     * Affiche un avertissement pour les health checks échoués.
     */
    private static void showHealthCheckWarning(HealthCheckService.HealthCheckResult result) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder message = new StringBuilder("Certaines vérifications ont échoué:\n\n");
            for (HealthCheckService.HealthCheck check : result.getFailedChecks()) {
                message.append("• ").append(check.getName()).append(": ")
                       .append(check.getMessage()).append("\n");
            }
            message.append("\nLe launcher peut ne pas fonctionner correctement.");

            JOptionPane.showMessageDialog(
                    null,
                    message.toString(),
                    "Avertissement - Health Check",
                    JOptionPane.WARNING_MESSAGE
            );
        });
    }

    /**
     * Affiche une boîte de dialogue pour les erreurs fatales.
     */
    private static void showFatalErrorDialog(Exception e) {
        SwingUtilities.invokeLater(() -> {
            String message = String.format(
                    "Une erreur fatale s'est produite:\n\n%s\n\n" +
                    "Consultez les logs pour plus de détails.",
                    e.getMessage()
            );

            JOptionPane.showMessageDialog(
                    null,
                    message,
                    "Erreur Fatale",
                    JOptionPane.ERROR_MESSAGE
            );
        });
    }

    /**
     * Récupère la version du launcher.
     */
    private static String getVersion(LauncherConfig config) {
        return config.launcherVersion != null ? config.launcherVersion : LAUNCHER_VERSION_FALLBACK;
    }

    public static String getVersion() {
        try {
            LauncherConfig config = LauncherConfig.getInstance();
            return getVersion(config);
        } catch (Exception e) {
            return LAUNCHER_VERSION_FALLBACK;
        }
    }

    /**
     * Détecte si l'application tourne en mode développement.
     */
    private static boolean isDevelopmentMode() {
        try {
            String path = NexariaLauncherMain.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            boolean isDevMode = path.contains("/target/classes") || 
                               path.contains("\\target\\classes") || 
                               !path.endsWith(".jar");
            return isDevMode;
        } catch (Exception e) {
            logger.debug("Failed to detect dev mode, assuming production", e);
            return false;
        }
    }

    /**
     * Hook de shutdown pour nettoyer les ressources.
     */
    public static void shutdown() {
        logger.info("Shutting down launcher");
        
        try {
            // Arrêter les caches
            CacheFactory.shutdownAll();
            
            // Nettoyer le contexte de logging
            logger.clearContext();
            
            logger.info("Shutdown completed");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    // Ajouter un shutdown hook
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(NexariaLauncherMain::shutdown));
    }
}
