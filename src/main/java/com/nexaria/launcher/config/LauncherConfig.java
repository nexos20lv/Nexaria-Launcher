package com.nexaria.launcher.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Configuration globale du launcher basée sur config.yml
 */
public class LauncherConfig {
    private static final Logger logger = LoggerFactory.getLogger(LauncherConfig.class);

    private static LauncherConfig instance;

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
    public boolean enforceModPolicy;
    public String modManifestUrl;
    public String bannedProcessesCsv;
    public String configManifestUrl;
    public String dataManifestUrl;
    public boolean blockSymlinks;
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
    public boolean forceRepair;

    // Répertoires
    // Dossier de données runtime (AppData selon l'OS)
    public String dataFolder;
    public String modsDir;
    public String configDir;
    public String launcherDir;
    public String cacheDir;
    public String versionsDir;
    public String gameDir;

    private LauncherConfig() {
    }

    /**
     * Charger la configuration (valeurs en dur)
     */
    public static void loadConfig() throws Exception {
        try {
            instance = new LauncherConfig();

            // Configuration en dur - Nexaria Launcher
            instance.azuriomUrl = "https://eclozionmc.ovh";
            instance.githubRepo = "nexos20lv/Nexaria-Launcher";
            instance.githubBranch = "main";
            instance.minecraftVersion = "1.21.5";
            instance.loader = "forge";
            instance.loaderVersion = "55.1.4";
            instance.maxMemory = 4096;
            instance.minMemory = 1024;
            instance.jvmArgs = "-XX:+UseG1GC -XX:MaxGCPauseMillis=200";
            instance.autoUpdate = true;
            instance.debugMode = false;
            instance.language = "fr";
            instance.verifyIntegrity = true;
            instance.cleanupOldMods = true;
            instance.enforceModPolicy = true; // Par défaut, blocage et quarantaine activés
            instance.modManifestUrl = ""; // Optionnel: URL JSON d'un manifest {"mods":[{"name":"...","sha256":"..."}]}
            instance.bannedProcessesCsv = String.join(", ",
                    "cheatengine.exe",
                    "processhacker.exe",
                    "x64dbg.exe",
                    "x32dbg.exe",
                    "ida64.exe",
                    "ida.exe",
                    "ollydbg.exe",
                    "wireshark.exe",
                    "fiddler.exe");
            instance.configManifestUrl = ""; // Optionnel: URL JSON {"configs":[{"name":"...","sha256":"..."}]}
            instance.dataManifestUrl = ""; // Optionnel: URL JSON {"files":[{"path":"...","sha256":"..."}]} pour tout
                                           // data/
            instance.blockSymlinks = true; // Bloquer mods/configs symlinkés hors gameDir
            instance.autoLaunchGame = false;
            instance.minimizeOnLaunch = true;
            instance.downloadTimeout = 30;
            instance.downloadRateLimitKBps = 0;
            instance.downloadMirrorBase = "";
            instance.rememberMeDefault = true;
            instance.launcherVersion = "1.0.23";
            instance.serverHost = "151.240.30.3";
            instance.serverPort = 25545;
            instance.serverName = "Nexaria";
            instance.forceRepair = false;

            // Initialiser le dossier de données runtime (AppData selon OS)
            instance.dataFolder = computeAppDataDir();
            instance.launcherDir = instance.dataFolder + File.separator + "launcher";
            instance.cacheDir = instance.dataFolder + File.separator + "cache";
            instance.versionsDir = instance.dataFolder + File.separator + "versions";
            instance.gameDir = instance.dataFolder + File.separator + "game";
            instance.modsDir = instance.gameDir + File.separator + "mods";
            instance.configDir = instance.gameDir + File.separator + "config";

            // Créer les répertoires
            new File(instance.modsDir).mkdirs();
            new File(instance.configDir).mkdirs();
            new File(instance.launcherDir).mkdirs();
            new File(instance.cacheDir).mkdirs();
            // Le dossier versions n'est plus utilisé par le launcher (FlowUpdater gère
            // tout)
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

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServerName() {
        return serverName;
    }

    public static String getModsDir() {
        return getInstance().modsDir;
    }

    public static String getConfigDir() {
        return getInstance().configDir;
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
            base = "data"; // Repli si impossible de déterminer
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

    public void setDebugMode(boolean value) {
        this.debugMode = value;
    }

    public void setGameDir(String path) {
        if (path == null || path.isBlank())
            return;
        this.gameDir = path;
        this.modsDir = this.gameDir + File.separator + "mods";
        this.configDir = this.gameDir + File.separator + "config";
        new File(this.gameDir).mkdirs();
        new File(this.modsDir).mkdirs();
        new File(this.configDir).mkdirs();
        logger.info("Game directory set to: {}", this.gameDir);
    }

    public void setDownloadRateLimitKBps(int kbps) {
        this.downloadRateLimitKBps = Math.max(0, kbps);
    }

    public void setDownloadMirrorBase(String base) {
        this.downloadMirrorBase = base != null ? base : "";
    }

    public void setRememberMeDefault(boolean v) {
        this.rememberMeDefault = v;
    }

    public void setForceRepair(boolean v) {
        this.forceRepair = v;
    }

    public void saveConfig() {
        // TODO: implémenter la sauvegarde YAML réelle; pour l'instant, log des valeurs
        logger.info(
                "Saving config: Memory={}, AutoUpdate={}, DebugMode={}, GameDir={}, RateLimitKBps={}, MirrorBase={}, RememberMeDefault={}",
                maxMemory, autoUpdate, debugMode, gameDir, downloadRateLimitKBps, downloadMirrorBase,
                rememberMeDefault);
    }
}
