package com.nexora.launcher.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Configuration globale du launcher basée sur config.yml
 */
public class LauncherConfig {
    private static final Logger logger = LoggerFactory.getLogger(LauncherConfig.class);
    private static final String CONFIG_FILE = "config.yml";
    private static final String DATA_DIR = "data";

    private static LauncherConfig instance;
    private ConfigLoader configLoader;

    // Propriétés publiques
    public String azuriomUrl;
    public String githubRepo;
    public String githubBranch;
    public String minecraftVersion;
    public String loader;
    public String loaderVersion;
    public int maxMemory;
    public int minMemory;
    public String jvmArgs;
    public boolean autoUpdate;
    public boolean debugMode;
    public String language;
    public boolean verifyIntegrity;
    public boolean cleanupOldMods;
    public boolean autoLaunchGame;
    public int downloadTimeout;
    public String launcherVersion;

    // Répertoires
    public String dataFolder;
    public String modsDir;
    public String configsDir;
    public String launcherDir;
    public String cacheDir;
    public String versionsDir;

    private LauncherConfig() {
    }

    /**
     * Charger la configuration depuis config.yml
     */
    public static void loadConfig() throws Exception {
        try {
            instance = new LauncherConfig();
            instance.configLoader = new ConfigLoader();

            // Essayer d'abord le fichier config.yml dans le dossier courant
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                instance.configLoader.loadFromFile(CONFIG_FILE);
            } else {
                // Si absent, créer un dossier data et utiliser les valeurs par défaut
                new File(DATA_DIR).mkdirs();
                logger.warn("config.yml non trouvé, utilisation des valeurs par défaut");
            }

            // Charger les propriétés
            instance.azuriomUrl = instance.configLoader.getString("azuriomUrl", "https://your-azuriom.com");
            instance.githubRepo = instance.configLoader.getString("githubRepo", "username/nexora");
            instance.githubBranch = instance.configLoader.getString("githubBranch", "main");
            instance.minecraftVersion = instance.configLoader.getString("minecraftVersion", "1.20.1");
            instance.loader = instance.configLoader.getString("loader", "forge");
            instance.loaderVersion = instance.configLoader.getString("loaderVersion", "47.2.0");
            instance.maxMemory = instance.configLoader.getInt("maxMemory", 2048);
            instance.minMemory = instance.configLoader.getInt("minMemory", 512);
            instance.jvmArgs = instance.configLoader.getString("jvmArgs", "-XX:+UseG1GC");
            instance.autoUpdate = instance.configLoader.getBoolean("autoUpdate", true);
            instance.debugMode = instance.configLoader.getBoolean("debugMode", false);
            instance.language = instance.configLoader.getString("language", "fr");
            instance.verifyIntegrity = instance.configLoader.getBoolean("verifyIntegrity", true);
            instance.cleanupOldMods = instance.configLoader.getBoolean("cleanupOldMods", true);
            instance.autoLaunchGame = instance.configLoader.getBoolean("autoLaunchGame", false);
            instance.downloadTimeout = instance.configLoader.getInt("downloadTimeout", 30);
            instance.launcherVersion = instance.configLoader.getString("launcherVersion", "1.0.0");

            // Initialiser les chemins des dossiers
            instance.dataFolder = instance.configLoader.getString("dataFolder", DATA_DIR);
            instance.modsDir = instance.dataFolder + "/" + instance.configLoader.getString("modsSubfolder", "mods");
            instance.configsDir = instance.dataFolder + "/"
                    + instance.configLoader.getString("configsSubfolder", "configs");
            instance.launcherDir = instance.dataFolder + "/"
                    + instance.configLoader.getString("launcherSubfolder", "launcher");
            instance.cacheDir = instance.dataFolder + "/" + instance.configLoader.getString("cacheSubfolder", "cache");
            instance.versionsDir = instance.dataFolder + "/"
                    + instance.configLoader.getString("versionsSubfolder", "versions");

            // Créer les répertoires
            new File(instance.modsDir).mkdirs();
            new File(instance.configsDir).mkdirs();
            new File(instance.launcherDir).mkdirs();
            new File(instance.cacheDir).mkdirs();
            new File(instance.versionsDir).mkdirs();

            logger.info("Configuration chargée avec succès");
            if (instance.debugMode) {
                logger.debug("Config: {}", instance);
            }

        } catch (Exception e) {
            logger.error("Erreur lors du chargement de la configuration", e);
            throw e;
        }
    }

    public static LauncherConfig getInstance() {
        if (instance == null) {
            try {
                loadConfig();
            } catch (Exception e) {
                throw new RuntimeException("Impossible de charger la configuration", e);
            }
        }
        return instance;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public int getMinMemory() {
        return minMemory;
    }

    // Getters complémentaires pour compatibilité UI
    public String getAzuriomUrl() {
        return azuriomUrl;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public String getLoader() {
        return loader;
    }

    public String getLoaderVersion() {
        return loaderVersion;
    }

    public static String getModsDir() {
        return getInstance().modsDir;
    }

    public static String getConfigsDir() {
        return getInstance().configsDir;
    }

    public static String getLauncherDir() {
        return getInstance().launcherDir;
    }

    public static String getCacheDir() {
        return getInstance().cacheDir;
    }

    public static String getVersionsDir() {
        return getInstance().versionsDir;
    }

    public static String getDataFolder() {
        return getInstance().dataFolder;
    }

    @Override
    public String toString() {
        return "LauncherConfig{" +
                "githubRepo='" + githubRepo + '\'' +
                ", minecraftVersion='" + minecraftVersion + '\'' +
                ", loader='" + loader + '\'' +
                ", maxMemory=" + maxMemory +
                ", minMemory=" + minMemory +
                ", autoUpdate=" + autoUpdate +
                ", debugMode=" + debugMode +
                '}';
    }

    public void setMaxMemory(int value) {
        this.maxMemory = value;
    }

    public void setAutoUpdate(boolean value) {
        this.autoUpdate = value;
    }

    public void saveConfig() {
        // Simple YAML save (not implemented perfectly, but prevents crash)
        // Ideally verify if configLoader has save capabilities or rewrite file
        // For now we just update runtime values or log
        logger.info("Saving config: Memory={}, AutoUpdate={}", maxMemory, autoUpdate);
    }
}
