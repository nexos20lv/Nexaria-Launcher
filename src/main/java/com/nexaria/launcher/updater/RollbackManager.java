package com.nexaria.launcher.updater;

import com.nexaria.launcher.logging.LoggingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;

import java.util.List;
import java.util.stream.Stream;

/**
 * Gestionnaire de rollback automatique en cas de crash après mise à jour.
 * Sauvegarde l'ancienne version avant update et peut restaurer si nécessaire.
 */
public class RollbackManager {
    private static final LoggingService logger = LoggingService.getLogger(RollbackManager.class);

    private static final String BACKUP_DIR_NAME = "backups";
    private static final String CRASH_MARKER_FILE = ".crash_detected";
    private static final int MAX_BACKUPS = 5;

    private final Path launcherDir;
    private final Path backupDir;

    public RollbackManager(Path launcherDir) {
        this.launcherDir = launcherDir;
        this.backupDir = launcherDir.resolve(BACKUP_DIR_NAME);

        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            logger.error("Failed to create backup directory", e);
        }
    }

    /**
     * Sauvegarde le JAR actuel avant une mise à jour.
     * 
     * @param currentJar Chemin du JAR actuel
     * @param version    Version actuelle
     * @return true si la sauvegarde a réussi
     */
    public boolean backupCurrentVersion(Path currentJar, String version) {
        logger.info("Backing up current version: {}", version);

        try {
            String timestamp = Instant.now().toString().replace(":", "-");
            String backupName = String.format("nexaria-launcher-%s-%s.jar", version, timestamp);
            Path backupFile = backupDir.resolve(backupName);

            Files.copy(currentJar, backupFile, StandardCopyOption.REPLACE_EXISTING);

            logger.eventBuilder()
                    .level(LoggingService.LogLevel.INFO)
                    .message("Version backup created")
                    .addContext("version", version)
                    .addContext("backupFile", backupFile.toString())
                    .log();

            // Nettoyer les anciennes sauvegardes
            cleanupOldBackups();

            return true;

        } catch (Exception e) {
            logger.error("Failed to backup current version", e);
            return false;
        }
    }

    /**
     * Vérifie si un crash a été détecté au démarrage précédent.
     * Si oui, restaure automatiquement l'ancienne version.
     * 
     * @return true si un rollback a été effectué
     */
    public boolean checkAndRollbackIfNeeded() {
        Path crashMarker = launcherDir.resolve(CRASH_MARKER_FILE);

        if (Files.exists(crashMarker)) {
            logger.warn("Crash marker detected, initiating automatic rollback");

            try {
                // Lire les infos du crash marker
                String crashInfo = Files.readString(crashMarker);
                logger.info("Crash info: {}", crashInfo);

                // Effectuer le rollback
                boolean success = rollbackToLatestBackup();

                if (success) {
                    // Supprimer le marker après rollback réussi
                    Files.delete(crashMarker);
                    logger.info("Rollback completed successfully, crash marker removed");
                    return true;
                } else {
                    logger.error("Rollback failed");
                    return false;
                }

            } catch (Exception e) {
                logger.error("Error during rollback check", e);
                return false;
            }
        }

        return false;
    }

    /**
     * Crée un marqueur de crash pour déclencher un rollback au prochain démarrage.
     */
    public void markCrash(String reason, String version) {
        logger.warn("Marking crash for version: {} - Reason: {}", version, reason);

        Path crashMarker = launcherDir.resolve(CRASH_MARKER_FILE);

        try {
            String crashInfo = String.format("Crash detected at: %s%nVersion: %s%nReason: %s%n",
                    Instant.now(), version, reason);
            Files.writeString(crashMarker, crashInfo);

            logger.info("Crash marker created, rollback will be triggered on next start");

        } catch (IOException e) {
            logger.error("Failed to create crash marker", e);
        }
    }

    /**
     * Supprime le marqueur de crash (appeler après un démarrage réussi).
     */
    public void clearCrashMarker() {
        Path crashMarker = launcherDir.resolve(CRASH_MARKER_FILE);

        try {
            if (Files.exists(crashMarker)) {
                Files.delete(crashMarker);
                logger.debug("Crash marker cleared - startup successful");
            }
        } catch (IOException e) {
            logger.error("Failed to clear crash marker", e);
        }
    }

    /**
     * Restaure la sauvegarde la plus récente.
     */
    private boolean rollbackToLatestBackup() {
        logger.info("Rolling back to latest backup");

        try {
            // Trouver la backup la plus récente
            List<Path> backups = findAllBackups();

            if (backups.isEmpty()) {
                logger.error("No backups found for rollback");
                return false;
            }

            Path latestBackup = backups.get(0); // Déjà trié par date
            logger.info("Rolling back to: {}", latestBackup.getFileName());

            // Déterminer le chemin du JAR actuel
            Path currentJar = getCurrentJarPath();

            // Remplacer par la backup
            Files.copy(latestBackup, currentJar, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Rollback completed: {} restored", latestBackup.getFileName());
            return true;

        } catch (Exception e) {
            logger.error("Failed to rollback", e);
            return false;
        }
    }

    /**
     * Liste toutes les sauvegardes disponibles, triées par date (plus récent en
     * premier).
     */
    private List<Path> findAllBackups() throws IOException {
        List<Path> backups = new ArrayList<>();

        if (!Files.exists(backupDir)) {
            return backups;
        }

        try (Stream<Path> files = Files.list(backupDir)) {
            backups.addAll(files
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList());
        }

        return backups;
    }

    /**
     * Nettoie les anciennes sauvegardes (garde seulement les N plus récentes).
     */
    private void cleanupOldBackups() {
        logger.debug("Cleaning up old backups (keeping {} most recent)", MAX_BACKUPS);

        try {
            List<Path> backups = findAllBackups();

            if (backups.size() <= MAX_BACKUPS) {
                return; // Rien à nettoyer
            }

            // Supprimer les backups au-delà de MAX_BACKUPS
            for (int i = MAX_BACKUPS; i < backups.size(); i++) {
                Path backup = backups.get(i);
                Files.delete(backup);
                logger.debug("Deleted old backup: {}", backup.getFileName());
            }

            logger.info("Cleaned up {} old backups", backups.size() - MAX_BACKUPS);

        } catch (Exception e) {
            logger.error("Failed to cleanup old backups", e);
        }
    }

    /**
     * Détermine le chemin du JAR actuel.
     */
    private Path getCurrentJarPath() {
        try {
            String jarPath = RollbackManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            return Paths.get(jarPath);
        } catch (Exception e) {
            logger.error("Failed to determine current JAR path", e);
            // Fallback
            return launcherDir.resolve("nexaria-launcher.jar");
        }
    }

    /**
     * Crée un gestionnaire avec le répertoire par défaut.
     */
    public static RollbackManager createDefault() {
        String userHome = System.getProperty("user.home");
        Path defaultPath = Paths.get(userHome, ".nexaria-launcher");
        return new RollbackManager(defaultPath);
    }
}
