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
     * Copie toutes les configs présentes dans data/configs vers le dossier de configs cible
     */
    public void syncLocalConfigs() throws Exception {
        // Source des configs dans le workspace (pas AppData)
        String localConfigsPath = "data/configs";
        File localConfigsDir = new File(localConfigsPath);

        if (!localConfigsDir.exists() || !localConfigsDir.isDirectory()) {
            logger.info("Aucun dossier local de configs trouvé: {}", localConfigsPath);
            return;
        }

        copyDirectoryRecursively(localConfigsDir.toPath(), Paths.get(LauncherConfig.getConfigsDir()));
        logger.info("Synchronisation des configs locales complétée");
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
