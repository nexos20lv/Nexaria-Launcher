package com.nexaria.launcher.java;

import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JavaManager {
    private static final Logger logger = LoggerFactory.getLogger(JavaManager.class);
    private static final String JAVA_VERSION = "17";
    private static final String RUNTIME_DIR = "java-runtime";

    public interface ProgressCallback {
        void onProgress(String status, int percent);
    }

    public static File getJavaHome() {
        File runtimeDir = new File(LauncherConfig.getDataFolder(), RUNTIME_DIR);
        File binDir = new File(runtimeDir, getBinPath());
        if (binDir.exists() && new File(binDir, getExecutableName()).exists()) {
            return runtimeDir;
        }
        return null; // Pas de runtime local
    }

    public static File getJavaExecutable() {
        File home = getJavaHome();
        if (home != null) {
            return new File(home, getBinPath() + File.separator + getExecutableName());
        }
        // Fallback: Java système
        return new File(System.getProperty("java.home"), "bin" + File.separator + getExecutableName());
    }

    public static void downloadJava(ProgressCallback callback) throws Exception {
        File runtimeDir = new File(LauncherConfig.getDataFolder(), RUNTIME_DIR);
        if (runtimeDir.exists())
            return; // Déjà là

        String os = getOS();
        String arch = getArch();
        String ext = os.equals("windows") ? "zip" : "tar.gz";
        String url = String.format("https://api.adoptium.net/v3/binary/latest/%s/ga/%s/%s/jdk/hotspot/normal/eclipse",
                JAVA_VERSION, os, arch);

        logger.info("[JAVA] Téléchargement depuis: {}", url);
        callback.onProgress("Téléchargement de Java 17...", 0);

        File archive = new File(LauncherConfig.getDataFolder(), "java." + ext);
        downloadFile(url, archive, callback);

        callback.onProgress("Extraction de Java...", 90);

        if (ext.equals("zip")) {
            unzip(archive, runtimeDir);
        } else {
            untargz(archive, runtimeDir);
        }

        archive.delete();
        logger.info("[JAVA] Installation terminée.");
    }

    private static void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    if (getOS().equals("mac") || getOS().equals("linux")) {
                        if (newFile.getName().equals("java"))
                            newFile.setExecutable(true);
                    }
                }
            }
        }
    }

    private static void untargz(File tarFile, File destDir) throws IOException {
        destDir.mkdirs();
        try (InputStream fi = new FileInputStream(tarFile);
                InputStream bi = new BufferedInputStream(fi);
                org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream gzi = new org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(
                        bi);
                org.apache.commons.compress.archivers.tar.TarArchiveInputStream ti = new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(
                        gzi)) {

            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
            while ((entry = ti.getNextTarEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = ti.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    // Restaurer les permissions d'exécution pour le binaire java
                    if (entry.getMode() != 0) { // simple check, plus précis serait mieux
                        if (newFile.getName().equals("java") || newFile.getName().endsWith(".sh")) {
                            newFile.setExecutable(true);
                        }
                    }
                }
            }
        }
    }

    private static void downloadFile(String url, File target, ProgressCallback callback) throws Exception {
        URL u = new URL(url);
        try (InputStream in = new BufferedInputStream(u.openStream());
                FileOutputStream out = new FileOutputStream(target)) {
            byte[] data = new byte[1024];
            int count;
            long total = 0; // On ne connait pas toujours la taille
            while ((count = in.read(data, 0, 1024)) != -1) {
                out.write(data, 0, count);
                total += count;
                if (total % 1024000 == 0)
                    callback.onProgress("Téléchargement (" + (total / 1024 / 1024) + " MB)...", -1);
            }
        }
    }

    private static String getOS() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win"))
            return "windows";
        if (os.contains("mac"))
            return "mac";
        return "linux";
    }

    private static String getArch() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64"))
            return "aarch64";
        return "x64";
    }

    private static String getBinPath() {
        if (getOS().equals("mac"))
            return "Contents/Home/bin";
        return "bin";
    }

    private static String getExecutableName() {
        return getOS().equals("windows") ? "java.exe" : "java";
    }

    public static boolean isSystemJavaValid() {
        String v = System.getProperty("java.version");
        return v.startsWith("17") || v.startsWith("21");
    }
}
