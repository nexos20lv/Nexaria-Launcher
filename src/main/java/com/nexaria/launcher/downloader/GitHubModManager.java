package com.nexaria.launcher.downloader;

import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gestion des mods/configs en local depuis le dossier data/ du launcher
 */
public class GitHubModManager {
    private static final Logger logger = LoggerFactory.getLogger(GitHubModManager.class);

    public GitHubModManager(String ignored) {
        // Conservé pour compatibilité, mais non utilisé en mode local
    }

    /**
     * Copie tous les mods présents dans data/mods vers le dossier de mods cible
     */
    public void syncLocalMods() throws Exception {
        // Source des mods dans le workspace (pas AppData)
        String localModsPath = "data/mods";
        File localModsDir = new File(localModsPath);

        if (!localModsDir.exists() || !localModsDir.isDirectory()) {
            logger.info("Aucun dossier local de mods trouvé: {}", localModsPath);
            return;
        }

        File[] localMods = localModsDir.listFiles();
        if (localMods == null) {
            return;
        }

        for (File modFile : localMods) {
            if (modFile.isFile() && modFile.getName().endsWith(".jar")) {
                Path destination = Paths.get(LauncherConfig.getModsDir(), modFile.getName());
                if (!Files.exists(destination)) {
                    logger.info("Copie du mod local: {}", modFile.getName());
                    Files.copy(modFile.toPath(), destination);
                } else {
                    logger.debug("Mod déjà présent: {}", modFile.getName());
                }
            }
        }

        if (LauncherConfig.getInstance().cleanupOldMods) {
            cleanupModsNotInLocal();
        }
    }

    /**
     * Synchronise tout le dossier data/ (mods + config) vers game/data/
     * Ceci permet de vérifier l'intégrité complète avec le manifest data-manifest.json
     */
    public void syncAllData() throws Exception {
        // Source: dossier data/ du launcher
        File localDataDir = new File("data");
        if (!localDataDir.exists() || !localDataDir.isDirectory()) {
            logger.info("Aucun dossier data/ trouvé");
            return;
        }

        // Destination: game/data/
        Path gameDataDir = Paths.get(LauncherConfig.getGameDir(), "data");
        Files.createDirectories(gameDataDir);

        // Copier récursivement data/config -> game/data/config
        File localConfigDir = new File(localDataDir, "config");
        if (localConfigDir.exists()) {
            Path targetConfigDir = gameDataDir.resolve("config");
            copyDirectoryRecursively(localConfigDir.toPath(), targetConfigDir);
            logger.info("Configs copiés: data/config -> game/data/config");
        }

        // Copier data/mods -> game/data/mods
        File localModsDir = new File(localDataDir, "mods");
        if (localModsDir.exists()) {
            Path targetModsDir = gameDataDir.resolve("mods");
            Files.createDirectories(targetModsDir);
            
            File[] mods = localModsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (mods != null) {
                for (File mod : mods) {
                    Path dest = targetModsDir.resolve(mod.getName());
                    if (!Files.exists(dest)) {
                        Files.copy(mod.toPath(), dest);
                        logger.debug("Mod copié: {}", mod.getName());
                    }
                }
            }
            logger.info("Mods copiés: data/mods -> game/data/mods");
        }

        // Copier le manifest
        File manifestFile = new File(localDataDir, "data-manifest.json");
        if (manifestFile.exists()) {
            Files.copy(manifestFile.toPath(), gameDataDir.resolve("data-manifest.json"), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.info("Manifest copié: data-manifest.json");
        }
        
        logger.info("Synchronisation complète de data/ terminée");
    }

    private void copyDirectoryRecursively(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        Files.walk(source).forEach(path -> {
            try {
                Path relative = source.relativize(path);
                Path dest = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    if (!Files.exists(dest)) {
                        Files.copy(path, dest);
                        logger.debug("Copie config: {}", dest);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Nettoie les mods présents dans le dossier cible mais absents dans data/mods
     */
    private void cleanupModsNotInLocal() throws IOException {
        File targetModsDir = new File(LauncherConfig.getModsDir());
        File localModsDir = new File("data/mods");
        if (!targetModsDir.exists()) return;

        String[] targetMods = targetModsDir.list((dir, name) -> name.endsWith(".jar"));
        if (targetMods == null) return;

        for (String modName : targetMods) {
            File localFile = new File(localModsDir, modName);
            if (!localFile.exists()) {
                Files.delete(Paths.get(targetModsDir.getAbsolutePath(), modName));
                logger.info("Suppression du mod non présent en local: {}", modName);
            }
        }
    }
}
