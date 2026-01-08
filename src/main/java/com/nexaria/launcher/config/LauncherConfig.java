package com.nexaria.launcher.config;

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
    private static final String WORKSPACE_DATA_DIR = "data";

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
    public boolean minimizeOnLaunch;
    public int downloadTimeout;
    public int downloadRateLimitKBps;
    public String downloadMirrorBase;
    public boolean rememberMeDefault;
    public String launcherVersion;
    public String serverHost;
    public int serverPort;
    public String serverName;

    // Répertoires
    // Dossier de données runtime (AppData selon l'OS)
    public String dataFolder;
    public String modsDir;
    public String configsDir;
    public String launcherDir;
    public String cacheDir;
    public String versionsDir;
    public String gameDir;

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
                logger.warn("config.yml non trouvé, utilisation des valeurs par défaut");
            }

            // Charger les propriétés
            instance.azuriomUrl = instance.configLoader.getString("azuriomUrl", "https://your-azuriom.com");
            instance.githubRepo = instance.configLoader.getString("githubRepo", "username/nexaria");
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
            instance.minimizeOnLaunch = instance.configLoader.getBoolean("minimizeOnLaunch", true);
                instance.downloadTimeout = instance.configLoader.getInt("downloadTimeout", 30);
                instance.downloadRateLimitKBps = instance.configLoader.getInt("downloadRateLimitKBps", 0);
                instance.downloadMirrorBase = instance.configLoader.getString("downloadMirrorBase", "");
                instance.rememberMeDefault = instance.configLoader.getBoolean("rememberMeDefault", true);
            instance.launcherVersion = instance.configLoader.getString("launcherVersion", "1.0.0");
            instance.serverHost = instance.configLoader.getString("serverHost", "127.0.0.1");
            instance.serverPort = instance.configLoader.getInt("serverPort", 25565);
            instance.serverName = instance.configLoader.getString("serverName", "Serveur");

                // Initialiser le dossier de données runtime (AppData selon OS)
                instance.dataFolder = computeAppDataDir();
                // Mods et configs doivent être sous le dossier du jeu pour que Forge les charge
                // gameDir est défini plus bas; recalculer après définition
            instance.launcherDir = instance.dataFolder + "/"
                    + instance.configLoader.getString("launcherSubfolder", "launcher");
            instance.cacheDir = instance.dataFolder + "/" + instance.configLoader.getString("cacheSubfolder", "cache");
                instance.versionsDir = instance.dataFolder + "/" + instance.configLoader.getString("versionsSubfolder", "versions");
                instance.gameDir = instance.configLoader.getString("gameDir", instance.dataFolder + "/" + instance.configLoader.getString("gameSubfolder", "game"));
                instance.modsDir = instance.gameDir + "/" + instance.configLoader.getString("modsSubfolder", "mods");
                instance.configsDir = instance.gameDir + "/" + instance.configLoader.getString("configsSubfolder", "configs");

            // Créer les répertoires
            new File(instance.modsDir).mkdirs();
            new File(instance.configsDir).mkdirs();
            new File(instance.launcherDir).mkdirs();
            new File(instance.cacheDir).mkdirs();
            // Le dossier versions n'est plus utilisé par le launcher (FlowUpdater gère tout)
            // new File(instance.versionsDir).mkdirs();
            new File(instance.gameDir).mkdirs();

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

    public String getServerHost() { return serverHost; }
    public int getServerPort() { return serverPort; }
    public String getServerName() { return serverName; }

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

    public static String getGameDir() {
        return getInstance().gameDir;
    }

    public static String getDataFolder() {
        return getInstance().dataFolder;
    }

    /**
     * Calcule le dossier AppData spécifique à l'OS pour stocker les données runtime
     */
    private static String computeAppDataDir() {
        String appName = "NexariaLauncher";
        String os = System.getProperty("os.name").toLowerCase();
        String base;
        try {
            if (os.contains("win")) {
                String appdata = System.getenv("APPDATA");
                if (appdata == null || appdata.isBlank()) {
                    appdata = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming";
                }
                base = appdata + File.separator + appName;
            } else if (os.contains("mac")) {
                base = System.getProperty("user.home") + "/Library/Application Support/" + appName;
            } else {
                base = System.getProperty("user.home") + "/.local/share/" + appName;
            }
        } catch (Exception e) {
            base = WORKSPACE_DATA_DIR; // Repli si impossible de déterminer
        }
        return base;
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

    public void setDebugMode(boolean value) { this.debugMode = value; }

    public void setGameDir(String path) {
        if (path == null || path.isBlank()) return;
        this.gameDir = path;
        this.modsDir = this.gameDir + "/mods";
        this.configsDir = this.gameDir + "/configs";
        new File(this.gameDir).mkdirs();
        new File(this.modsDir).mkdirs();
        new File(this.configsDir).mkdirs();
        logger.info("Game directory set to: {}", this.gameDir);
    }

    public void setDownloadRateLimitKBps(int kbps) { this.downloadRateLimitKBps = Math.max(0, kbps); }
    public void setDownloadMirrorBase(String base) { this.downloadMirrorBase = base != null ? base : ""; }
    public void setRememberMeDefault(boolean v) { this.rememberMeDefault = v; }

    public void saveConfig() {
        // TODO: implémenter la sauvegarde YAML réelle; pour l'instant, log des valeurs
        logger.info("Saving config: Memory={}, AutoUpdate={}, DebugMode={}, GameDir={}, RateLimitKBps={}, MirrorBase={}, RememberMeDefault={}",
                maxMemory, autoUpdate, debugMode, gameDir, downloadRateLimitKBps, downloadMirrorBase, rememberMeDefault);
    }
}
