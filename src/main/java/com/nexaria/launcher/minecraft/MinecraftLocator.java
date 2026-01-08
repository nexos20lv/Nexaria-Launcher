package com.nexaria.launcher.minecraft;

import java.io.File;

public class MinecraftLocator {
    public static String getMinecraftDir() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            if (appdata == null || appdata.isBlank()) {
                appdata = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming";
            }
            return appdata + File.separator + ".minecraft";
        } else if (os.contains("mac")) {
            return System.getProperty("user.home") + "/Library/Application Support/minecraft";
        } else {
            return System.getProperty("user.home") + "/.minecraft";
        }
    }
}
