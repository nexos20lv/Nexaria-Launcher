package com.nexaria.launcher.minecraft;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.model.User;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.versions.VanillaVersion;
import fr.flowarg.flowupdater.versions.forge.ForgeVersion;
import fr.flowarg.flowupdater.versions.forge.ForgeVersionBuilder;
import fr.flowarg.flowupdater.versions.fabric.FabricVersion;
import fr.flowarg.flowupdater.versions.fabric.FabricVersionBuilder;
import fr.flowarg.openlauncherlib.NoFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nexaria.launcher.security.AntiCheatService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

/**
 * Wrapper pour OpenLauncherLib qui gère le lancement de Minecraft avec
 * Forge/Fabric/Vanilla
 */
public class OpenLauncherLibLauncher {
    private static final Logger logger = LoggerFactory.getLogger(OpenLauncherLibLauncher.class);
    private static String customJavaPath;

    public static void setJavaPath(String path) {
        customJavaPath = path;
    }

    public interface ProgressListener {
        void onStatus(String status);

        void onProgress(int percent);
    }

    /**
     * Lance Minecraft avec le profil configuré
     */
    public static Process launchGame(User user, ProgressListener progressListener) throws Exception {
        LauncherConfig cfg = LauncherConfig.getInstance();
        Path gameDir = Paths.get(cfg.gameDir);

        String minecraftVersion = cfg.minecraftVersion;
        String loader = cfg.loader.toLowerCase();
        String loaderVersion = cfg.loaderVersion;

        logger.info("Lancement de Minecraft {} avec {} {}", minecraftVersion, loader, loaderVersion);

        if (progressListener != null) {
            progressListener.onStatus("Préparation du lancement...");
            progressListener.onProgress(0);
        }

        // Configuration du FlowUpdater
        VanillaVersion vanillaVersion = new VanillaVersion.VanillaVersionBuilder()
                .withName(minecraftVersion)
                .build();

        // Callback de progression
        IProgressCallback callback = new IProgressCallback() {
            @Override
            public void step(Step step) {
                if (progressListener != null) {
                    String status;
                    switch (step) {
                        case READ:
                            status = "Lecture des fichiers...";
                            break;
                        case DL_LIBS:
                            status = "Téléchargement des bibliothèques...";
                            break;
                        case DL_ASSETS:
                            status = "Téléchargement des assets...";
                            break;
                        case EXTRACT_NATIVES:
                            status = "Extraction des natives...";
                            break;
                        case MOD_LOADER:
                            status = "Installation du loader...";
                            break;
                        case INTEGRATION:
                            status = "Intégration...";
                            break;
                        case POST_EXECUTIONS:
                            status = "Finalisation...";
                            break;
                        default:
                            status = "Préparation...";
                            break;
                    }
                    progressListener.onStatus(status);
                }
            }

            @Override
            public void onFileDownloaded(Path path) {
                logger.debug("Téléchargé: {}", path.getFileName());
            }

            @Override
            public void update(DownloadList.DownloadInfo info) {
                if (progressListener != null && info.getTotalToDownloadBytes() > 0) {
                    int percent = (int) ((info.getDownloadedBytes() * 100) / info.getTotalToDownloadBytes());
                    progressListener.onProgress(percent);
                }
            }
        };

        FlowUpdater.FlowUpdaterBuilder updaterBuilder = new FlowUpdater.FlowUpdaterBuilder()
                .withVanillaVersion(vanillaVersion)
                .withProgressCallback(callback);

        // Ajouter le loader si nécessaire
        if (loader.contains("forge")) {
            if (progressListener != null)
                progressListener.onStatus("Configuration de Forge " + loaderVersion + "...");

            // FlowUpdater 1.9.x: ForgeVersion nécessite le format
            // "minecraftVersion-forgeVersion"
            String forgeFullVersion = loaderVersion.contains("-") ? loaderVersion
                    : minecraftVersion + "-" + loaderVersion;
            ForgeVersion forgeVersion = new ForgeVersionBuilder()
                    .withForgeVersion(forgeFullVersion)
                    .build();
            updaterBuilder.withModLoaderVersion(forgeVersion);

            logger.info("Configuration Forge: version {}", forgeFullVersion);

        } else if (loader.contains("fabric")) {
            if (progressListener != null)
                progressListener.onStatus("Configuration de Fabric " + loaderVersion + "...");

            // FlowUpdater 1.9.x: FabricVersion avec le nouveau FabricVersionBuilder
            FabricVersion fabricVersion = new FabricVersionBuilder()
                    .withFabricVersion(loaderVersion)
                    .build();
            updaterBuilder.withModLoaderVersion(fabricVersion);

        } else if (loader.contains("neo")) {
            if (progressListener != null)
                progressListener.onStatus("Configuration de NeoForge " + loaderVersion + "...");
            // NeoForge n'est pas supporté par la version actuelle de FlowUpdater
            throw new UnsupportedOperationException(
                    "NeoForge n'est pas encore supporté par OpenLauncherLib - utilisez Forge à la place");
        }

        // Lancer l'updater
        if (progressListener != null)
            progressListener.onStatus("Vérification des fichiers...");

        FlowUpdater updater = updaterBuilder.build();

        // IMPORTANT: update() installe Minecraft, le loader et toutes les bibliothèques
        // Cette étape est CRITIQUE pour Forge - elle télécharge bootstraplauncher et
        // securejarhandler
        logger.info("Démarrage de FlowUpdater.update() - Installation complète...");
        updater.update(gameDir);
        logger.info("FlowUpdater.update() terminé - Toutes les bibliothèques sont installées");

        // Synchroniser les assets requis par OpenLauncherLib dans le dossier Minecraft
        // global
        try {
            Path mcAssets = Paths.get(MinecraftLocator.getMinecraftDir()).resolve("assets");
            Path localAssets = gameDir.resolve("assets");
            syncAllAssets(localAssets, mcAssets);
        } catch (Exception e) {
            logger.warn("Synchronisation des assets vers le dossier Minecraft échouée", e);
        }

        if (progressListener != null) {
            progressListener.onStatus("Démarrage de Minecraft...");
            progressListener.onProgress(100);
        }

        // Configuration du launcher - GameFolder pointe vers les bons répertoires
        // Utiliser les dossiers du profil de jeu (FlowUpdater) plutôt que le dossier
        // global Minecraft

        fr.theshark34.openlauncherlib.minecraft.AuthInfos authInfos = new fr.theshark34.openlauncherlib.minecraft.AuthInfos(
                user != null ? user.getUsername() : "Joueur",
                user != null && user.getAccessToken() != null ? user.getAccessToken() : "0",
                user != null && user.getId() != null ? user.getId() : java.util.UUID.randomUUID().toString());

        // Utiliser GameFolder.FLOW_UPDATER prédéfini (assets, libraries, natives,
        // client.jar)
        fr.theshark34.openlauncherlib.minecraft.GameFolder gameFolder = fr.theshark34.openlauncherlib.minecraft.GameFolder.FLOW_UPDATER;

        NoFramework noFramework = new NoFramework(
                gameDir,
                authInfos,
                gameFolder);

        noFramework.getAdditionalVmArgs().add("-Xms" + cfg.minMemory + "M");

        // Fix LWJGL sur macOS
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            noFramework.getAdditionalVmArgs().add("-XstartOnFirstThread");
        }

        // Ajouter les arguments JVM personnalisés si présents (sanitisés)
        if (cfg.jvmArgs != null && !cfg.jvmArgs.trim().isEmpty()) {
            java.util.List<String> safeArgs = com.nexaria.launcher.security.JvmArgsSanitizer.sanitize(cfg.jvmArgs);
            noFramework.getAdditionalVmArgs().addAll(safeArgs);
        }

        // Configuration du chemin Java personnalisé via le callback
        if (customJavaPath != null) {
            noFramework.setLastCallback(launcher -> {
                launcher.setLaunchingEvent(pb -> {
                    java.util.List<String> command = pb.command();
                    if (!command.isEmpty()) {
                        command.set(0, customJavaPath);
                        pb.command(command);
                        logger.info("Java Path remplacé par: {}", customJavaPath);
                    } else {
                        logger.warn("Impossible de remplacer le chemin Java: commande vide");
                    }
                });
            });
        }

        // Anti-cheat: durcissement JVM minimal
        try {
            noFramework.getAdditionalVmArgs().addAll(new AntiCheatService().getJvmHardeningArgs());
        } catch (Exception e) {
            logger.warn("Echec application durcissement JVM anti-cheat", e);
        }

        logger.info("Arguments JVM: {}", noFramework.getAdditionalVmArgs());

        // Anti-cheat: détection basique de processus interdits
        try {
            new AntiCheatService().enforceOrThrowOnDetection();
        } catch (SecurityException se) {
            logger.error("Lancement bloqué par l'anti-cheat: {}", se.getMessage());
            throw se;
        } catch (Exception e) {
            logger.warn("Vérification anti-cheat incomplète", e);
        }

        // Lancer le jeu
        Process gameProcess = noFramework.launch(
                minecraftVersion,
                loaderVersion != null && !loaderVersion.isEmpty() ? loaderVersion : null,
                loader.contains("forge") ? NoFramework.ModLoader.FORGE
                        : loader.contains("fabric") ? NoFramework.ModLoader.FABRIC
                                : loader.contains("neo") ? NoFramework.ModLoader.NEO_FORGE
                                        : NoFramework.ModLoader.VANILLA);

        logger.info("Minecraft lancé avec succès (PID: {})", gameProcess.pid());
        return gameProcess;
    }

    private static void syncAllAssets(Path sourceAssets, Path targetAssets) throws IOException {
        if (!Files.exists(sourceAssets))
            return;
        Files.createDirectories(targetAssets);
        copyRecursive(sourceAssets, targetAssets);
        logger.info("Assets synchronisés vers {}", targetAssets);
    }

    private static void copyRecursive(Path src, Path dst) throws IOException {
        Files.createDirectories(dst);
        try (java.util.stream.Stream<Path> stream = Files.walk(src)) {
            stream.forEach(p -> {
                try {
                    Path rel = src.relativize(p);
                    Path out = dst.resolve(rel);
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(out);
                    } else {
                        Files.createDirectories(out.getParent());
                        Files.copy(p, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException ex) {
                    logger.warn("Échec copie: {}", p);
                }
            });
        }
    }
}
