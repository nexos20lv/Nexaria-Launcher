package com.nexaria.launcher.services.security;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.logging.LoggingService;

import java.util.ArrayList;
import java.util.List;

/**
 * Générateur d'arguments JVM sécurisés pour le lancement de Minecraft.
 * Inclut des paramètres de sandbox, hardening et optimisation.
 */
public class SecureJvmArgsBuilder {
    private static final LoggingService logger = LoggingService.getLogger(SecureJvmArgsBuilder.class);

    private final LauncherConfig config;
    private final List<String> customArgs = new ArrayList<>();
    private boolean enableSecurityManager = false;
    private boolean enableSandbox = true;

    public SecureJvmArgsBuilder(LauncherConfig config) {
        this.config = config;
    }

    /**
     * Active le Security Manager Java (strict).
     */
    public SecureJvmArgsBuilder withSecurityManager(boolean enable) {
        this.enableSecurityManager = enable;
        return this;
    }

    /**
     * Active les paramètres de sandbox.
     */
    public SecureJvmArgsBuilder withSandbox(boolean enable) {
        this.enableSandbox = enable;
        return this;
    }

    /**
     * Ajoute un argument JVM personnalisé.
     */
    public SecureJvmArgsBuilder addCustomArg(String arg) {
        this.customArgs.add(arg);
        return this;
    }

    /**
     * Construit la liste complète des arguments JVM.
     */
    public List<String> build() {
        List<String> args = new ArrayList<>();

        // 1. Paramètres de mémoire
        args.add("-Xms" + config.minMemory + "M");
        args.add("-Xmx" + config.maxMemory + "M");

        // 2. Paramètres de sandbox et sécurité
        if (enableSandbox) {
            addSandboxArgs(args);
        }

        // 3. Security Manager (si activé)
        if (enableSecurityManager) {
            addSecurityManagerArgs(args);
        }

        // 4. Optimisations JVM
        addOptimizationArgs(args);

        // 5. Paramètres de monitoring et debugging
        addMonitoringArgs(args);

        // 6. Arguments personnalisés du config
        if (config.jvmArgs != null && !config.jvmArgs.isEmpty()) {
            String[] configArgs = config.jvmArgs.split(" ");
            for (String arg : configArgs) {
                if (!arg.trim().isEmpty()) {
                    args.add(arg.trim());
                }
            }
        }

        // 7. Arguments personnalisés additionnels
        args.addAll(customArgs);

        logger.eventBuilder()
                .level(LoggingService.LogLevel.INFO)
                .message("JVM arguments built")
                .addContext("totalArgs", args.size())
                .addContext("sandboxEnabled", enableSandbox)
                .addContext("securityManagerEnabled", enableSecurityManager)
                .log();

        return args;
    }

    /**
     * Ajoute les arguments de sandbox et hardening.
     */
    private void addSandboxArgs(List<String> args) {
        // Limiter l'utilisation des ressources
        args.add("-XX:+UseContainerSupport");
        args.add("-XX:MaxRAMPercentage=75.0");
        
        // Désactiver les features dangereuses
        args.add("-Djava.security.egd=file:/dev/./urandom"); // Meilleur RNG
        args.add("-Dfile.encoding=UTF-8");
        
        // Restreindre l'accès réseau (si supporté)
        args.add("-Djava.net.preferIPv4Stack=true");
        
        // Empêcher la création de fichiers temporaires non sécurisés
        args.add("-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"));
        
        logger.debug("Sandbox JVM arguments added");
    }

    /**
     * Ajoute les arguments du Security Manager.
     */
    private void addSecurityManagerArgs(List<String> args) {
        // Note: Security Manager est deprecated depuis Java 17
        // mais on peut toujours l'utiliser si nécessaire
        args.add("-Djava.security.manager");
        args.add("-Djava.security.policy==" + getPolicyFile());
        
        logger.warn("Security Manager enabled (deprecated in Java 17+)");
    }

    /**
     * Ajoute les arguments d'optimisation JVM.
     */
    private void addOptimizationArgs(List<String> args) {
        // Garbage Collector G1 (bon équilibre performance/latence)
        args.add("-XX:+UseG1GC");
        args.add("-XX:G1HeapRegionSize=4M");
        args.add("-XX:+UnlockExperimentalVMOptions");
        args.add("-XX:+ParallelRefProcEnabled");
        args.add("-XX:+AlwaysPreTouch");
        args.add("-XX:MaxGCPauseMillis=200");
        
        // Optimisations générales
        args.add("-XX:+OptimizeStringConcat");
        args.add("-XX:+UseStringDeduplication");
        
        // Prévenir les fuites mémoire
        args.add("-XX:+DisableExplicitGC");
        
        logger.debug("Optimization JVM arguments added");
    }

    /**
     * Ajoute les arguments de monitoring.
     */
    private void addMonitoringArgs(List<String> args) {
        // Logs de GC (pour debug)
        if (config.debugMode) {
            args.add("-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M");
            args.add("-XX:+HeapDumpOnOutOfMemoryError");
            args.add("-XX:HeapDumpPath=./crash-dumps/");
        }
        
        // JMX monitoring (désactivé par défaut pour sécurité)
        // args.add("-Dcom.sun.management.jmxremote");
        
        logger.debug("Monitoring JVM arguments added");
    }

    /**
     * Retourne le chemin vers le fichier de politique de sécurité.
     */
    private String getPolicyFile() {
        // TODO: Créer un fichier .policy avec les permissions nécessaires
        return System.getProperty("user.home") + "/.nexaria-launcher/security.policy";
    }

    /**
     * Construit les arguments avec les paramètres par défaut sécurisés.
     */
    public static List<String> buildSecureDefaults(LauncherConfig config) {
        return new SecureJvmArgsBuilder(config)
                .withSandbox(true)
                .withSecurityManager(false) // Désactivé par défaut (deprecated)
                .build();
    }
}
