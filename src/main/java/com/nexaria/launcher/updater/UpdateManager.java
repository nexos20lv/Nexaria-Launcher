package com.nexaria.launcher.updater;

import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Gestionnaire centralisé des mises à jour du launcher
 * Gère le workflow complet: vérification -> téléchargement -> installation
 * Inclut un système de cache pour éviter les vérifications trop fréquentes
 */
public class UpdateManager {
    private static final Logger logger = LoggerFactory.getLogger(UpdateManager.class);
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000; // 24 heures en ms
    private static final String CACHE_FILE = "update-check.cache";
    
    private final GitHubUpdater updater;
    private UpdateCallback callback;
    
    public interface UpdateCallback {
        void onCheckStart();
        void onCheckComplete(boolean hasUpdate, GitHubUpdater.GitHubRelease release, String error);
        void onDownloadStart(String version);
        void onDownloadProgress(long bytesDownloaded, long totalBytes);
        void onDownloadComplete();
        void onError(String message);
    }

    public UpdateManager(String githubRepo, String currentVersion) {
        this.updater = new GitHubUpdater(githubRepo, currentVersion);
    }

    public void setCallback(UpdateCallback callback) {
        this.callback = callback;
    }

    /**
     * Vérifie les mises à jour de manière asynchrone
     */
    public void checkForUpdatesAsync() {
        new Thread(() -> {
            try {
                if (callback != null) callback.onCheckStart();
                
                GitHubUpdater.UpdateCheckResult result = updater.checkForUpdates();
                
                if (callback != null) {
                    callback.onCheckComplete(result.hasUpdate, result.release, result.error);
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la vérification des mises à jour", e);
                if (callback != null) {
                    callback.onError("Erreur lors de la vérification des mises à jour: " + e.getMessage());
                }
            }
        }, "UpdateChecker").start();
    }

    /**
     * Vérifie et installe les mises à jour automatiquement
     * Utilise un cache pour ne vérifier qu'une fois par 24h
     * Idéal pour être appelé au démarrage du launcher
     */
    public void autoUpdateOnStartup() {
        new Thread(() -> {
            try {
                // Vérifier le cache d'abord
                if (isCacheValid()) {
                    logger.info("Vérification des mises à jour en cache (dernière vérification < 24h)");
                    return;
                }
                
                logger.info("Vérification automatique des mises à jour au démarrage");
                
                GitHubUpdater.UpdateCheckResult result = updater.checkForUpdates();
                updateCache(); // Mettre à jour le timestamp du cache
                
                if (result.hasUpdate && result.release != null) {
                    logger.info("Mise à jour disponible: {}", result.release.tagName);
                    
                    try {
                        String cacheDir = LauncherConfig.getCacheDir();
                        String updatePath = cacheDir + "/nexaria-launcher-update.jar";
                        
                        logger.info("Téléchargement de la mise à jour vers: {}", updatePath);
                        updater.downloadUpdate(result.release, updatePath);
                        
                        logger.info("Installation de la mise à jour...");
                        GitHubUpdater.installUpdate(updatePath);
                        
                        // Si on arrive ici, installUpdate aurait appelé System.exit(0)
                    } catch (GitHubUpdater.UpdateException e) {
                        logger.error("Erreur lors du téléchargement/installation: {}", e.getMessage());
                    }
                } else {
                    logger.info("Aucune mise à jour disponible");
                    if (result.error != null) {
                        logger.warn("Erreur lors de la vérification: {}", result.error);
                    }
                }
            } catch (Exception e) {
                logger.error("Erreur critique lors de la vérification automatique", e);
            }
        }, "AutoUpdater").start();
    }

    /**
     * Vérifie si le cache est valide (dernière vérification < 24h)
     */
    private boolean isCacheValid() {
        try {
            String cacheDir = LauncherConfig.getCacheDir();
            String cacheFilePath = cacheDir + "/" + CACHE_FILE;
            
            if (!Files.exists(Paths.get(cacheFilePath))) {
                return false;
            }
            
            long lastModified = Files.getLastModifiedTime(Paths.get(cacheFilePath)).toMillis();
            long currentTime = System.currentTimeMillis();
            
            return (currentTime - lastModified) < CACHE_DURATION;
        } catch (Exception e) {
            logger.debug("Erreur lors de la vérification du cache", e);
            return false;
        }
    }

    /**
     * Met à jour le timestamp du cache
     */
    private void updateCache() {
        try {
            String cacheDir = LauncherConfig.getCacheDir();
            String cacheFilePath = cacheDir + "/" + CACHE_FILE;
            Files.write(Paths.get(cacheFilePath), String.valueOf(System.currentTimeMillis()).getBytes());
            logger.debug("Cache des mises à jour mis à jour");
        } catch (Exception e) {
            logger.warn("Impossible de mettre à jour le cache", e);
        }
    }

    /**
     * Efface le cache pour forcer une vérification à la prochaine démarrage
     */
    public void clearUpdateCache() {
        try {
            String cacheDir = LauncherConfig.getCacheDir();
            String cacheFilePath = cacheDir + "/" + CACHE_FILE;
            Files.deleteIfExists(Paths.get(cacheFilePath));
            logger.info("Cache des mises à jour effacé");
        } catch (Exception e) {
            logger.warn("Impossible d'effacer le cache", e);
        }
    }

    /**
     * Télécharge et installe une mise à jour spécifique
     */
    public void downloadAndInstallUpdate(GitHubUpdater.GitHubRelease release) {
        new Thread(() -> {
            try {
                if (callback != null) callback.onDownloadStart(release.tagName);
                
                String cacheDir = LauncherConfig.getCacheDir();
                String updatePath = cacheDir + "/nexaria-launcher-update.jar";
                
                updater.downloadUpdate(release, updatePath);
                
                if (callback != null) callback.onDownloadComplete();
                
                // Installer la mise à jour
                GitHubUpdater.installUpdate(updatePath);
            } catch (GitHubUpdater.UpdateException e) {
                logger.error("Erreur lors du téléchargement/installation", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        }, "UpdateInstaller").start();
    }

    /**
     * Obtient la dernière release en cache (doit être appelé après checkForUpdates)
     */
    public GitHubUpdater.GitHubRelease getCachedRelease() {
        try {
            return updater.getLatestRelease();
        } catch (GitHubUpdater.UpdateException e) {
            logger.error("Impossible d'obtenir la release en cache", e);
            return null;
        }
    }
}
