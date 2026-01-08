package com.nexaria.launcher.minecraft;

import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

public class ForgeInstaller {
    private static final Logger logger = LoggerFactory.getLogger(ForgeInstaller.class);

    public static boolean ensureInstalled(String mcVersion, String forgeVersion) throws Exception {
        String mcDir = MinecraftLocator.getMinecraftDir();
        new File(mcDir).mkdirs();
        String versionsDir = mcDir + "/versions";
        String versionId = com.nexaria.launcher.minecraft.VersionJsonLauncher.detectForgeVersionId(versionsDir, mcVersion, forgeVersion);
        if (versionId != null) {
            logger.info("Profil Forge détecté: {}", versionId);
            return true;
        }
        // Télécharger l'installer Forge
        String installerName = String.format("forge-%s-%s-installer.jar", mcVersion, forgeVersion);
        String url = String.format("https://maven.minecraftforge.net/net/minecraftforge/forge/%s-%s/%s", mcVersion, forgeVersion, installerName);
        String cachePath = LauncherConfig.getCacheDir() + File.separator + installerName;
        download(url, cachePath);
        // Exécuter l'installation headless dans .minecraft
        String javaExe = JavaRuntimeLocator.getJavaExecutable(17);
        if (javaExe == null) {
            logger.error("Java 17 introuvable pour l'installation de Forge");
            return false;
        }
        ProcessBuilder pb = new ProcessBuilder(javaExe, "-jar", cachePath, "--installClient");
        pb.directory(new File(mcDir));
        Process p = pb.start();
        String stdout = new String(p.getInputStream().readAllBytes());
        String stderr = new String(p.getErrorStream().readAllBytes());
        int exit = p.waitFor();
        logger.info("Forge installer terminé avec code {}", exit);
        if (!stdout.isBlank()) logger.info("Forge stdout:\n{}", stdout);
        if (!stderr.isBlank()) logger.warn("Forge stderr:\n{}", stderr);
        // Re-détecter
        versionId = com.nexaria.launcher.minecraft.VersionJsonLauncher.detectForgeVersionId(versionsDir, mcVersion, forgeVersion);
        return versionId != null;
    }

    private static void download(String urlStr, String dest) throws Exception {
        logger.info("Téléchargement Forge: {} -> {}", urlStr, dest);
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
