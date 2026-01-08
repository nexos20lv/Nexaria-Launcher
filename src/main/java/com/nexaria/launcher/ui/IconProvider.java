package com.nexaria.launcher.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;

public class IconProvider {
    // Icônes utilisées dans l'app
    public static Icon getIcon(String name, int size) {
        String path = null;
        if ("user".equals(name)) path = "com/formdev/flatlaf/icons/user.svg";
        else if ("settings".equals(name)) path = "com/formdev/flatlaf/icons/settings.svg";
        else if ("gear".equals(name)) path = "com/formdev/flatlaf/icons/settings.svg";
        else if ("memory".equals(name)) path = "com/formdev/flatlaf/icons/ram.svg";
        else if ("network".equals(name)) path = "com/formdev/flatlaf/icons/network.svg";
        else if ("folder".equals(name)) path = "com/formdev/flatlaf/icons/folder.svg";
        else if ("trash".equals(name)) path = "com/formdev/flatlaf/icons/trash.svg";
        else if ("download".equals(name)) path = "com/formdev/flatlaf/icons/download.svg";
        else if ("debug".equals(name)) path = "com/formdev/flatlaf/icons/debug.svg";
        else if ("test".equals(name)) path = "com/formdev/flatlaf/icons/checkCircle.svg";
        else if ("export".equals(name)) path = "com/formdev/flatlaf/icons/save.svg";
        else if ("security".equals(name)) path = "com/formdev/flatlaf/icons/lock.svg";
        else if ("skin".equals(name)) path = "com/formdev/flatlaf/icons/image.svg";
        else if ("info".equals(name)) path = "com/formdev/flatlaf/icons/info.svg";
        else if ("warning".equals(name)) path = "com/formdev/flatlaf/icons/warning.svg";
        else if ("error".equals(name)) path = "com/formdev/flatlaf/icons/error.svg";
        else if ("play".equals(name)) path = "com/formdev/flatlaf/icons/play.svg";
        else if ("refresh".equals(name)) path = "com/formdev/flatlaf/icons/refresh.svg";
        else if ("home".equals(name)) path = "com/formdev/flatlaf/icons/home.svg";
        else if ("logout".equals(name)) path = "com/formdev/flatlaf/icons/exit.svg";
        
        if (path == null) return null;
        try {
            return new FlatSVGIcon(path, size, size, IconProvider.class.getClassLoader());
        } catch (Exception e) {
            return null;
        }
    }

    // Utility: Unicode symbols as fallback
    public static String getSymbol(String name) {
        if ("user".equals(name)) return "👤";
        else if ("settings".equals(name)) return "⚙️";
        else if ("memory".equals(name)) return "💾";
        else if ("network".equals(name)) return "🌐";
        else if ("folder".equals(name)) return "📁";
        else if ("trash".equals(name)) return "🗑️";
        else if ("download".equals(name)) return "⬇️";
        else if ("debug".equals(name)) return "🐛";
        else if ("test".equals(name)) return "✓";
        else if ("export".equals(name)) return "💾";
        else if ("security".equals(name)) return "🔒";
        else if ("skin".equals(name)) return "🎨";
        else if ("info".equals(name)) return "ℹ️";
        else if ("play".equals(name)) return "▶️";
        else if ("home".equals(name)) return "🏠";
        else if ("logout".equals(name)) return "🚪";
        return "";
    }
}
