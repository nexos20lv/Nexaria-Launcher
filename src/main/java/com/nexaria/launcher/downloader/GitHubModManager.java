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

        // Destination: dossiers du jeu (racine) sans duplicata dans game/data
        Path gameRootDir = Paths.get(LauncherConfig.getGameDir());
        Files.createDirectories(gameRootDir);

        // Copier récursivement data/config -> game/config
        File localConfigDir = new File(localDataDir, "config");
        if (localConfigDir.exists()) {
            Path liveConfigDir = gameRootDir.resolve("config");
            copyDirectoryReplacing(localConfigDir.toPath(), liveConfigDir);
            logger.info("Configs copiés: data/config -> game/config");
        }

        // Copier data/mods -> game/mods
        File localModsDir = new File(localDataDir, "mods");
        if (localModsDir.exists()) {
            Path liveModsDir = gameRootDir.resolve("mods");
            Files.createDirectories(liveModsDir);
            
            File[] mods = localModsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (mods != null) {
                for (File mod : mods) {
                    Path destLive = liveModsDir.resolve(mod.getName());
                    Files.copy(mod.toPath(), destLive, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    logger.debug("Mod copié: {}", mod.getName());
                }
            }
            logger.info("Mods copiés: data/mods -> game/mods");
        }

        // Restaurer automatiquement les mods éventuels placés en quarantaine
        restoreQuarantineMods(gameRootDir);

        // Copier le manifest (racine du jeu)
        File manifestFile = new File(localDataDir, "data-manifest.json");
        if (manifestFile.exists()) {
            Files.copy(manifestFile.toPath(), gameRootDir.resolve("data-manifest.json"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.info("Manifest copié: data-manifest.json -> game/");
        }
        
        logger.info("Synchronisation complète de data/ terminée");
    }

    /**
     * Si des mods .jar sont restés en quarantaine, on les remet automatiquement dans mods/
     */
    private void restoreQuarantineMods(Path gameRootDir) throws IOException {
        Path quarantineDir = gameRootDir.resolve("quarantine");
        Path modsLiveDir = gameRootDir.resolve("mods");
        if (!Files.isDirectory(quarantineDir)) return;

        Files.createDirectories(modsLiveDir);
        try (var stream = Files.list(quarantineDir)) {
            stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar"))
                  .forEach(p -> {
                      try {
                          Path destLive = modsLiveDir.resolve(p.getFileName().toString());
                          Files.move(p, destLive, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                          logger.info("Mod restauré depuis quarantaine: {}", p.getFileName());
                      } catch (IOException e) {
                          logger.warn("Impossible de restaurer {} depuis quarantaine", p.getFileName(), e);
                      }
                  });
        }
    }

    private void copyDirectoryReplacing(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        Files.walk(source).forEach(path -> {
            try {
                Path relative = source.relativize(path);
                Path dest = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(path, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
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
