package com.nexaria.launcher.health;

import com.nexaria.launcher.logging.LoggingService;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de vérification de santé du launcher au démarrage.
 * Vérifie Java version, espace disque, mémoire disponible, etc.
 */
public class HealthCheckService {
    private static final LoggingService logger = LoggingService.getLogger(HealthCheckService.class);

    // Constantes de configuration
    private static final int MIN_JAVA_VERSION = 17;
    private static final long MIN_DISK_SPACE_MB = 500;
    private static final long MIN_MEMORY_MB = 1024;

    /**
     * Exécute tous les health checks et retourne le résultat.
     * 
     * @return Résultat des vérifications
     */
    public HealthCheckResult performHealthCheck() {
        String correlationId = logger.startOperation("health_check");

        List<HealthCheck> checks = new ArrayList<>();
        boolean allPassed = true;

        try {
            logger.info("Starting health check");

            // Vérification Java
            HealthCheck javaCheck = checkJavaVersion();
            checks.add(javaCheck);
            if (!javaCheck.isPassed()) allPassed = false;

            // Vérification espace disque
            HealthCheck diskCheck = checkDiskSpace();
            checks.add(diskCheck);
            if (!diskCheck.isPassed()) allPassed = false;

            // Vérification mémoire
            HealthCheck memoryCheck = checkAvailableMemory();
            checks.add(memoryCheck);
            if (!memoryCheck.isPassed()) allPassed = false;

            // Vérification OS
            HealthCheck osCheck = checkOperatingSystem();
            checks.add(osCheck);
            if (!osCheck.isPassed()) allPassed = false;

            // Vérification permissions d'écriture
            HealthCheck permissionsCheck = checkFilePermissions();
            checks.add(permissionsCheck);
            if (!permissionsCheck.isPassed()) allPassed = false;

            logger.eventBuilder()
                    .level(allPassed ? LoggingService.LogLevel.INFO : LoggingService.LogLevel.WARN)
                    .message("Health check completed")
                    .addContext("status", allPassed ? "PASSED" : "FAILED")
                    .addContext("totalChecks", checks.size())
                    .addContext("passedChecks", checks.stream().filter(HealthCheck::isPassed).count())
                    .log();

            return new HealthCheckResult(allPassed, checks);

        } finally {
            logger.endOperation("health_check");
        }
    }

    /**
     * Vérifie que la version de Java est suffisante.
     */
    private HealthCheck checkJavaVersion() {
        try {
            String javaVersion = System.getProperty("java.version");
            String javaVendor = System.getProperty("java.vendor");
            
            int majorVersion = extractMajorVersion(javaVersion);
            boolean passed = majorVersion >= MIN_JAVA_VERSION;

            String message = passed
                    ? String.format("Java %d (%s) - OK", majorVersion, javaVendor)
                    : String.format("Java %d détecté, mais Java %d ou supérieur est requis", 
                            majorVersion, MIN_JAVA_VERSION);

            logger.eventBuilder()
                    .level(passed ? LoggingService.LogLevel.INFO : LoggingService.LogLevel.ERROR)
                    .message("Java version check")
                    .addContext("version", javaVersion)
                    .addContext("vendor", javaVendor)
                    .addContext("majorVersion", majorVersion)
                    .addContext("required", MIN_JAVA_VERSION)
                    .addContext("passed", passed)
                    .log();

            return new HealthCheck(
                    "Java Version",
                    passed,
                    message,
                    passed ? Severity.INFO : Severity.CRITICAL
            );

        } catch (Exception e) {
            logger.error("Failed to check Java version", e);
            return new HealthCheck(
                    "Java Version",
                    false,
                    "Impossible de vérifier la version Java: " + e.getMessage(),
                    Severity.CRITICAL
            );
        }
    }

    /**
     * Extrait la version majeure de Java (ex: "17.0.1" -> 17).
     */
    private int extractMajorVersion(String version) {
        try {
            // Format moderne: "17.0.1", "21.0.2"
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            
            // Ancien format: "1.8.0_xxx" -> 8
            if (major == 1 && parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
            
            return major;
        } catch (Exception e) {
            logger.warn("Failed to parse Java version: {}", version);
            return 0;
        }
    }

    /**
     * Vérifie l'espace disque disponible.
     */
    private HealthCheck checkDiskSpace() {
        try {
            File root = new File(System.getProperty("user.home"));
            long freeSpaceMB = root.getFreeSpace() / 1024 / 1024;
            long totalSpaceMB = root.getTotalSpace() / 1024 / 1024;
            
            boolean passed = freeSpaceMB >= MIN_DISK_SPACE_MB;

            String message = String.format("Espace disque: %d MB libre / %d MB total", 
                    freeSpaceMB, totalSpaceMB);

            logger.eventBuilder()
                    .level(passed ? LoggingService.LogLevel.INFO : LoggingService.LogLevel.WARN)
                    .message("Disk space check")
                    .addContext("freeSpaceMB", freeSpaceMB)
                    .addContext("totalSpaceMB", totalSpaceMB)
                    .addContext("requiredMB", MIN_DISK_SPACE_MB)
                    .addContext("passed", passed)
                    .log();

            return new HealthCheck(
                    "Espace Disque",
                    passed,
                    message,
                    passed ? Severity.INFO : Severity.WARNING
            );

        } catch (Exception e) {
            logger.error("Failed to check disk space", e);
            return new HealthCheck(
                    "Espace Disque",
                    false,
                    "Impossible de vérifier l'espace disque: " + e.getMessage(),
                    Severity.WARNING
            );
        }
    }

    /**
     * Vérifie la mémoire disponible.
     */
    private HealthCheck checkAvailableMemory() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemoryMB = runtime.maxMemory() / 1024 / 1024;
            long totalMemoryMB = runtime.totalMemory() / 1024 / 1024;
            long freeMemoryMB = runtime.freeMemory() / 1024 / 1024;
            
            boolean passed = maxMemoryMB >= MIN_MEMORY_MB;

            String message = String.format("Mémoire JVM: max=%d MB, total=%d MB, libre=%d MB", 
                    maxMemoryMB, totalMemoryMB, freeMemoryMB);

            logger.eventBuilder()
                    .level(passed ? LoggingService.LogLevel.INFO : LoggingService.LogLevel.WARN)
                    .message("Memory check")
                    .addContext("maxMemoryMB", maxMemoryMB)
                    .addContext("totalMemoryMB", totalMemoryMB)
                    .addContext("freeMemoryMB", freeMemoryMB)
                    .addContext("requiredMB", MIN_MEMORY_MB)
                    .addContext("passed", passed)
                    .log();

            return new HealthCheck(
                    "Mémoire Disponible",
                    passed,
                    message,
                    passed ? Severity.INFO : Severity.WARNING
            );

        } catch (Exception e) {
            logger.error("Failed to check memory", e);
            return new HealthCheck(
                    "Mémoire Disponible",
                    false,
                    "Impossible de vérifier la mémoire: " + e.getMessage(),
                    Severity.WARNING
            );
        }
    }

    /**
     * Vérifie le système d'exploitation.
     */
    private HealthCheck checkOperatingSystem() {
        try {
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String osArch = System.getProperty("os.arch");

            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            int processors = osBean.getAvailableProcessors();

            String message = String.format("%s %s (%s) - %d processeurs", 
                    osName, osVersion, osArch, processors);

            logger.eventBuilder()
                    .level(LoggingService.LogLevel.INFO)
                    .message("Operating system check")
                    .addContext("osName", osName)
                    .addContext("osVersion", osVersion)
                    .addContext("osArch", osArch)
                    .addContext("processors", processors)
                    .log();

            return new HealthCheck(
                    "Système d'Exploitation",
                    true,
                    message,
                    Severity.INFO
            );

        } catch (Exception e) {
            logger.error("Failed to check OS", e);
            return new HealthCheck(
                    "Système d'Exploitation",
                    false,
                    "Impossible de vérifier l'OS: " + e.getMessage(),
                    Severity.INFO
            );
        }
    }

    /**
     * Vérifie les permissions d'écriture dans le répertoire de l'application.
     */
    private HealthCheck checkFilePermissions() {
        try {
            File userHome = new File(System.getProperty("user.home"));
            File testDir = new File(userHome, ".nexaria-launcher-test");
            
            boolean canWrite = testDir.mkdir() || testDir.exists();
            if (canWrite) {
                testDir.delete(); // Nettoyage
            }

            String message = canWrite
                    ? "Permissions d'écriture OK"
                    : "Pas de permission d'écriture dans " + userHome.getAbsolutePath();

            logger.eventBuilder()
                    .level(canWrite ? LoggingService.LogLevel.INFO : LoggingService.LogLevel.ERROR)
                    .message("File permissions check")
                    .addContext("directory", userHome.getAbsolutePath())
                    .addContext("canWrite", canWrite)
                    .log();

            return new HealthCheck(
                    "Permissions Fichiers",
                    canWrite,
                    message,
                    canWrite ? Severity.INFO : Severity.CRITICAL
            );

        } catch (Exception e) {
            logger.error("Failed to check file permissions", e);
            return new HealthCheck(
                    "Permissions Fichiers",
                    false,
                    "Impossible de vérifier les permissions: " + e.getMessage(),
                    Severity.CRITICAL
            );
        }
    }

    /**
     * Résultat d'un health check individuel.
     */
    public static class HealthCheck {
        private final String name;
        private final boolean passed;
        private final String message;
        private final Severity severity;

        public HealthCheck(String name, boolean passed, String message, Severity severity) {
            this.name = name;
            this.passed = passed;
            this.message = message;
            this.severity = severity;
        }

        public String getName() { return name; }
        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
        public Severity getSeverity() { return severity; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s (%s)", 
                    passed ? "✓" : "✗", name, message, severity);
        }
    }

    /**
     * Résultat global des health checks.
     */
    public static class HealthCheckResult {
        private final boolean allPassed;
        private final List<HealthCheck> checks;

        public HealthCheckResult(boolean allPassed, List<HealthCheck> checks) {
            this.allPassed = allPassed;
            this.checks = checks;
        }

        public boolean isAllPassed() { return allPassed; }
        public List<HealthCheck> getChecks() { return checks; }

        public List<HealthCheck> getFailedChecks() {
            return checks.stream()
                    .filter(check -> !check.isPassed())
                    .toList();
        }

        public boolean hasCriticalIssues() {
            return checks.stream()
                    .anyMatch(check -> !check.isPassed() && check.getSeverity() == Severity.CRITICAL);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Health Check Result: ").append(allPassed ? "PASSED" : "FAILED").append("\n");
            for (HealthCheck check : checks) {
                sb.append("  ").append(check).append("\n");
            }
            return sb.toString();
        }
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
}
