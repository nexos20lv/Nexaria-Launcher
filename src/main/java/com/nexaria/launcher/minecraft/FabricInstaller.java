package com.nexaria.launcher.minecraft;

import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

public class FabricInstaller {
    private static final Logger logger = LoggerFactory.getLogger(FabricInstaller.class);
    private static final String INSTALLER_VERSION = "1.0.0"; // peut être rendu configurable

    public static boolean ensureInstalled(String mcVersion, String loaderVersion) throws Exception {
        String mcDir = MinecraftLocator.getMinecraftDir();
        new File(mcDir).mkdirs();
        String versionsDir = mcDir + "/versions";
        String versionId = com.nexaria.launcher.minecraft.VersionJsonLauncher.detectFabricVersionId(versionsDir, mcVersion, loaderVersion);
        if (versionId != null) {
            logger.info("Profil Fabric détecté: {}", versionId);
            return true;
        }
        // Télécharger Fabric installer
        String installerName = String.format("fabric-installer-%s.jar", INSTALLER_VERSION);
        String url = String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/%s", INSTALLER_VERSION, installerName);
        String cachePath = LauncherConfig.getCacheDir() + File.separator + installerName;
        download(url, cachePath);
        // Exécuter l'installation client
        String javaExe = JavaRuntimeLocator.getJavaExecutable(17);
        if (javaExe == null) {
            logger.error("Java 17 introuvable pour l'installation de Fabric");
            return false;
        }
        ProcessBuilder pb = new ProcessBuilder(javaExe, "-jar", cachePath, "client", "-mcversion", mcVersion, "-loader", loaderVersion);
        pb.directory(new File(mcDir));
        Process p = pb.start();
        String stdout = new String(p.getInputStream().readAllBytes());
        String stderr = new String(p.getErrorStream().readAllBytes());
        int exit = p.waitFor();
        logger.info("Fabric installer terminé avec code {}", exit);
        if (!stdout.isBlank()) logger.info("Fabric stdout:\n{}", stdout);
        if (!stderr.isBlank()) logger.warn("Fabric stderr:\n{}", stderr);
        // Re-détecter
        versionId = com.nexaria.launcher.minecraft.VersionJsonLauncher.detectFabricVersionId(versionsDir, mcVersion, loaderVersion);
        return versionId != null;
    }

    private static void download(String urlStr, String dest) throws Exception {
        logger.info("Téléchargement Fabric: {} -> {}", urlStr, dest);
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setRequestProperty("User-Agent", "NexariaLauncher");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
    }
}
