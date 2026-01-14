package com.nexaria.launcher.services.security;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Filtre les arguments JVM fournis via la config pour éviter les injections d'agents,
 * de bootclasspath, d'ouvertures de modules, etc. Conserve uniquement une liste sûre.
 */
public final class JvmArgsSanitizer {
    private JvmArgsSanitizer() {}

    private static final List<Pattern> BANNED_PREFIXES = List.of(
            Pattern.compile("^-javaagent:.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^-agentlib:.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^-agentpath:.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^-Xbootclasspath(:|/).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^--add-opens=.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^--add-exports=.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^--patch-module=.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^-Xdebug$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^-Xrunjdwp:.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^-XX:.*AllowAttach.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^-Djdk.attach.allowAttachSelf=.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^-Dcom.sun.management.jmxremote.*", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> ALLOWED_PREFIXES = List.of(
            Pattern.compile("^-Xmx\\d+[kKmMgG]$"),
            Pattern.compile("^-Xms\\d+[kKmMgG]$"),
            Pattern.compile("^-XX:.*"),
            Pattern.compile("^-D[a-zA-Z0-9_.-]+(=.+)?$")
    );

    public static List<String> sanitize(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        String[] split = raw.trim().split("\\s+");
        List<String> out = new ArrayList<>();
        outer:
        for (String arg : split) {
            if (arg.isBlank()) continue;
            for (Pattern p : BANNED_PREFIXES) {
                if (p.matcher(arg).matches()) {
                    continue outer; // rejeté
                }
            }
            boolean allowed = false;
            for (Pattern p : ALLOWED_PREFIXES) {
                if (p.matcher(arg).matches()) { allowed = true; break; }
            }
            if (allowed) out.add(arg);
        }
        // Dé-duplicate tout en gardant l'ordre
        return out.stream().distinct().collect(Collectors.toList());
    }
}
