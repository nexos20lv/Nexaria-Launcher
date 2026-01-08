package com.nexaria.launcher.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VersionJsonLauncher {

    public static class LaunchSpec {
        public String mainClass;
        public List<String> jvmArgs = new ArrayList<>();
        public List<String> gameArgs = new ArrayList<>();
        public List<String> classpath = new ArrayList<>();
        public String assetsIndex;
        public String versionType;
        public int javaMajor = 0;
        public String librariesDir;
    }

    public static String detectForgeVersionId(String versionsDir, String mcVersion, String forgeVersion) {
        File dir = new File(versionsDir);
        File[] kids = dir.listFiles((d, name) -> name.toLowerCase().contains("forge") && name.contains(mcVersion) && name.contains(forgeVersion));
        if (kids != null && kids.length > 0) return kids[0].getName();
        // Try common naming
        String candidate = mcVersion + "-forge-" + forgeVersion;
        if (Files.exists(Paths.get(versionsDir, candidate, candidate + ".json"))) return candidate;
        // Fallback: pick any forge json
        kids = dir.listFiles((d, name) -> new File(d, name + "/" + name + ".json").exists() && name.toLowerCase().contains("forge"));
        if (kids != null && kids.length > 0) return kids[0].getName();
        return null;
    }

    public static String detectNeoForgeVersionId(String versionsDir, String mcVersion, String neoForgeVersion) {
        File dir = new File(versionsDir);
        File[] kids = dir.listFiles((d, name) -> name.toLowerCase().contains("neoforge") && name.contains(mcVersion));
        if (kids != null && kids.length > 0) return kids[0].getName();
        // Common naming may be just neoforge-<version>
        String candidate = "neoforge-" + neoForgeVersion;
        if (Files.exists(Paths.get(versionsDir, candidate, candidate + ".json"))) return candidate;
        kids = dir.listFiles((d, name) -> new File(d, name + "/" + name + ".json").exists() && name.toLowerCase().contains("neoforge"));
        if (kids != null && kids.length > 0) return kids[0].getName();
        return null;
    }

    public static String detectFabricVersionId(String versionsDir, String mcVersion, String loaderVersion) {
        File dir = new File(versionsDir);
        // Fabric often uses: fabric-loader-<loaderVersion>-<mcVersion>
        File[] kids = dir.listFiles((d, name) -> name.toLowerCase().contains("fabric") && name.contains(mcVersion));
        if (kids != null && kids.length > 0) return kids[0].getName();
        String candidate = "fabric-loader-" + loaderVersion + "-" + mcVersion;
        if (Files.exists(Paths.get(versionsDir, candidate, candidate + ".json"))) return candidate;
        kids = dir.listFiles((d, name) -> new File(d, name + "/" + name + ".json").exists() && name.toLowerCase().contains("fabric"));
        if (kids != null && kids.length > 0) return kids[0].getName();
        return null;
    }

    public static LaunchSpec parseVersion(String minecraftDir, String versionId) throws Exception {
        String versionsDir = minecraftDir + "/versions";
        String jsonPath = versionsDir + "/" + versionId + "/" + versionId + ".json";
        try (FileReader reader = new FileReader(jsonPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            LaunchSpec spec = new LaunchSpec();
            spec.mainClass = root.get("mainClass").getAsString();
            spec.librariesDir = minecraftDir + "/libraries";
            if (root.has("assets")) {
                spec.assetsIndex = root.get("assets").getAsString();
            }
            if (root.has("type")) {
                spec.versionType = root.get("type").getAsString();
            }
            if (root.has("javaVersion")) {
                JsonObject jv = root.getAsJsonObject("javaVersion");
                if (jv != null && jv.has("majorVersion")) {
                    try { spec.javaMajor = jv.get("majorVersion").getAsInt(); } catch (Exception ignore) {}
                }
            }

            // Arguments (supporte primitives et objets avec rules/value)
            if (root.has("arguments")) {
                JsonObject args = root.getAsJsonObject("arguments");
                if (args.has("jvm")) {
                    parseArgsWithRules(args.getAsJsonArray("jvm"), spec.jvmArgs);
                }
                if (args.has("game")) {
                    parseArgsWithRules(args.getAsJsonArray("game"), spec.gameArgs);
                }
            }

            // Classpath
            JsonArray libraries = root.getAsJsonArray("libraries");
            String libsDir = spec.librariesDir;
            for (JsonElement el : libraries) {
                JsonObject lib = el.getAsJsonObject();
                if (lib.has("downloads")) {
                    JsonObject dl = lib.getAsJsonObject("downloads");
                    if (dl.has("artifact")) {
                        JsonObject art = dl.getAsJsonObject("artifact");
                        String path = art.get("path").getAsString();
                        String cpEntry = libsDir + "/" + path;
                        if (Files.exists(Paths.get(cpEntry))) spec.classpath.add(cpEntry);
                    }
                } else if (lib.has("name")) {
                    // Build maven path
                    String name = lib.get("name").getAsString();
                    String[] parts = name.split(":");
                    if (parts.length >= 3) {
                        String group = parts[0].replace('.', '/');
                        String artifact = parts[1];
                        String version = parts[2];
                        String jar = libsDir + "/" + group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
                        if (Files.exists(Paths.get(jar))) spec.classpath.add(jar);
                    }
                }
            }

            // Add client jar
            String clientJar = versionsDir + "/" + versionId + "/" + versionId + ".jar";
            if (Files.exists(Paths.get(clientJar))) spec.classpath.add(clientJar);

            return spec;
        }
    }

    private static void parseArgsWithRules(JsonArray arr, List<String> out) {
        String osKey = getOsKey();
        for (JsonElement el : arr) {
            if (el.isJsonPrimitive()) {
                out.add(el.getAsString());
            } else if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (!isAllowedByRules(obj, osKey)) continue;
                if (obj.has("value")) {
                    JsonElement val = obj.get("value");
                    if (val.isJsonArray()) {
                        for (JsonElement vEl : val.getAsJsonArray()) {
                            if (vEl.isJsonPrimitive()) out.add(vEl.getAsString());
                        }
                    } else if (val.isJsonPrimitive()) {
                        out.add(val.getAsString());
                    }
                }
            }
        }
    }

    private static boolean isAllowedByRules(JsonObject obj, String osKey) {
        if (!obj.has("rules")) return true;
        boolean allowed = false; // dernière règle gagnante
        JsonArray rules = obj.getAsJsonArray("rules");
        for (JsonElement rEl : rules) {
            JsonObject rule = rEl.getAsJsonObject();
            String action = rule.has("action") ? rule.get("action").getAsString() : "allow";
            boolean matches = true;
            if (rule.has("os")) {
                JsonObject osObj = rule.getAsJsonObject("os");
                if (osObj.has("name")) {
                    String name = osObj.get("name").getAsString();
                    matches = osKey.equalsIgnoreCase(name);
                }
            }
            if (matches) {
                allowed = "allow".equalsIgnoreCase(action);
            }
        }
        return allowed;
    }

    private static String getOsKey() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }
}
