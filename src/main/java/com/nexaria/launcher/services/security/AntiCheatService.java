package com.nexaria.launcher.services.security;

import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AntiCheat côté launcher: vérifie des processus interdits basiques et
 * applique un durcissement JVM minimal au lancement.
 * Note: ceci ne remplace pas un anti-cheat serveur/ingame.
 */
public class AntiCheatService {
    private static final Logger logger = LoggerFactory.getLogger(AntiCheatService.class);

    private final LauncherConfig cfg = LauncherConfig.getInstance();

    public List<String> detectBannedProcesses() {
        Set<String> banned = parseCsv(cfg.bannedProcessesCsv);
        logger.debug("Processus interdits configurés: {}", banned);
        if (banned.isEmpty()) {
            logger.info("Liste de processus interdits vide, aucune vérification");
            return Collections.emptyList();
        }
        List<String> found = new ArrayList<>();
        long start = System.currentTimeMillis();

        try {
            for (ProcessHandle ph : ProcessHandle.allProcesses().toList()) {
                try {
                    // Les processus système peuvent lancer AccessDeniedException ici
                    if (!ph.info().command().isPresent())
                        continue;

                    Optional<String> cmd = ph.info().command();
                    String name = cmd.map(AntiCheatService::fileNameOnly).orElse("");
                    if (name.isEmpty())
                        continue;

                    String lower = name.toLowerCase(Locale.ROOT);
                    for (String b : banned) {
                        if (lower.equals(b) || lower.endsWith("/" + b) || lower.endsWith("\\" + b)) {
                            found.add(name + " (pid=" + ph.pid() + ")");
                            logger.warn("Processus interdit détecté: {} (pid={})", name, ph.pid());
                            break;
                        }
                    }
                } catch (SecurityException | UnsupportedOperationException e) {
                    // Ignorer les processus auxquels on n'a pas accès
                } catch (Exception e) {
                    logger.debug("Erreur scan processus {}: {}", ph.pid(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Impossible d'énumérer les processus (droits limités?)", e);
        }
        long elapsed = System.currentTimeMillis() - start;
        if (!found.isEmpty()) {
            logger.warn("Processus interdits détectés: {} ({}ms)", found, elapsed);
        } else {
            logger.info("Aucun processus interdit détecté ({}ms)", elapsed);
        }
        return found;
    }

    public void enforceOrThrowOnDetection() {
        List<String> found = detectBannedProcesses();
        if (!found.isEmpty() && cfg.enforceModPolicy) {
            throw new SecurityException("Processus interdits détectés: " + found);
        }
    }

    public List<String> getJvmHardeningArgs() {
        List<String> args = new ArrayList<>();
        // Empêche l'attach à chaud (jcmd/jattach/VirtualVM) pendant l'exécution
        args.add("-XX:+DisableAttachMechanism");
        return args;
    }

    private static Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank())
            return Collections.emptySet();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String fileNameOnly(String path) {
        try {
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return i >= 0 ? path.substring(i + 1) : path;
        }
    }
}
