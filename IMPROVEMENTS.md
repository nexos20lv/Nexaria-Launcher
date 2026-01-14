# 🎯 Améliorations Architecture v1.1.0

Ce document détaille toutes les améliorations majeures implémentées dans le Nexaria Launcher.

## 📋 Table des Matières

- [Architecture & Qualité](#architecture--qualit%C3%A9)
- [Gestion d'Erreurs & Robustesse](#gestion-derreurs--robustesse)
- [Performance](#performance)
- [UI/UX](#uiux)
- [Sécurité](#s%C3%A9curit%C3%A9)
- [Monitoring & CI/CD](#monitoring--cicd)

---

## 🏗️ Architecture & Qualité

### 1. Logging Structuré (`LoggingService`)

**Emplacement**: `com.nexaria.launcher.logging.LoggingService`

Service de logging avancé avec corrélation d'événements et contexte enrichi.

**Fonctionnalités**:
- Corrélation d'événements avec `correlationId`
- Contexte de session et utilisateur
- Builder pattern pour logs complexes
- Support MDC (Mapped Diagnostic Context)

**Usage**:
```java
LoggingService logger = LoggingService.getLogger(MyClass.class);

// Démarrer une opération
String correlationId = logger.startOperation("user_login");

// Log avec contexte enrichi
logger.eventBuilder()
    .level(LogLevel.INFO)
    .message("User authenticated")
    .addContext("username", username)
    .addContext("ip", ipAddress)
    .log();

// Terminer l'opération
logger.endOperation("user_login");
```

### 2. Injection de Dépendances (`ServiceContainer`)

**Emplacement**: `com.nexaria.launcher.core.ServiceContainer`

Conteneur DI léger pour découpler les composants.

**Usage**:
```java
ServiceContainer container = ServiceContainer.getInstance();

// Enregistrer un singleton
container.registerSingleton(LauncherConfig.class, config);

// Enregistrer une factory
container.registerFactory(UpdateManager.class, () -> new UpdateManager(...));

// Résoudre une dépendance
LauncherConfig config = container.resolve(LauncherConfig.class);
```

### 3. Point d'Entrée Unifié (`NexariaLauncherMain`)

**Emplacement**: `com.nexaria.launcher.NexariaLauncherMain`

Remplace `LauncherApp` et `NexariaLauncher` pour une architecture unifiée.

**Nouvelles fonctionnalités**:
- Health checks au démarrage
- Gestion du cycle de vie complet
- Shutdown hooks pour cleanup
- Session tracking

---

## ⚠️ Gestion d'Erreurs & Robustesse

### 1. Hiérarchie d'Exceptions

**Emplacement**: `com.nexaria.launcher.exception.*`

Exceptions métier typées avec contexte enrichi.

**Classes**:
- `LauncherException` - Exception de base avec severité
- `NetworkException` - Erreurs réseau (endpoint, status code)
- `AuthenticationException` - Erreurs d'auth (type, username)
- `DownloadException` - Erreurs de téléchargement (URL, progression)
- `SecurityException` - Violations de sécurité

**Usage**:
```java
throw new DownloadException(
    "Failed to download mod",
    cause,
    url,
    destination,
    bytesDownloaded,
    totalBytes
);
```

### 2. Retry avec Backoff Exponentiel

**Emplacement**: `com.nexaria.launcher.resilience.RetryExecutor`

Système de retry automatique configurable.

**Usage**:
```java
RetryExecutor executor = RetryExecutor.forNetwork();

String result = executor.execute(() -> {
    return downloadFile(url);
}, "download_mod");
```

**Configurations pré-définies**:
- `withDefaults()` - 3 tentatives, délai 500ms
- `forNetwork()` - 5 tentatives, délai 2s-30s
- `forQuickOperations()` - 3 tentatives, délai 100ms-1s

### 3. Circuit Breaker Pattern

**Emplacement**: `com.nexaria.launcher.resilience.CircuitBreaker`

Protection contre les défaillances en cascade.

**Usage**:
```java
CircuitBreaker breaker = CircuitBreaker.builder("github")
    .failureThreshold(5)
    .timeout(Duration.ofSeconds(10))
    .resetTimeout(Duration.ofMinutes(5))
    .build();

String data = breaker.execute(() -> callGitHubApi());
```

**États**:
- `CLOSED` - Fonctionnement normal
- `OPEN` - Circuit ouvert, rejette les appels
- `HALF_OPEN` - Test de récupération

---

## 🚀 Performance

### 1. Cache Intelligent

**Emplacement**: `com.nexaria.launcher.cache.CacheManager`

Cache avec TTL et nettoyage automatique.

**Usage**:
```java
CacheManager<String, Object> cache = CacheFactory.getGitHubApiCache();

Object result = cache.get(key, () -> {
    return expensiveOperation();
});

// Statistiques
CacheStats stats = cache.getStats();
System.out.println("Hit rate: " + stats.getHitRate());
```

**Caches pré-configurés**:
- `getGitHubApiCache()` - TTL 5min, évite rate limiting
- `getIntegrityCache()` - TTL 1h, checksums
- `getFileMetadataCache()` - TTL 30min
- `getImageCache()` - TTL 24h

### 2. Téléchargements Parallèles

**Emplacement**: `com.nexaria.launcher.downloader.ParallelDownloadManager`

Télécharge plusieurs fichiers simultanément avec CompletableFuture.

**Usage**:
```java
ParallelDownloadManager manager = new ParallelDownloadManager(4);

List<DownloadTask> tasks = List.of(
    new DownloadTask(url1, dest1, "mod1.jar"),
    new DownloadTask(url2, dest2, "mod2.jar")
);

manager.setProgressCallback(progress -> {
    System.out.println(progress.getFormattedSpeed());
});

DownloadResult result = manager.downloadAll(tasks);
```

---

## 🎨 UI/UX

### 1. Toast Notifications

**Emplacement**: `com.nexaria.launcher.ui.notification.ToastNotificationManager`

Notifications non-bloquantes élégantes.

**Usage**:
```java
ToastNotificationManager.getInstance().showSuccess(
    "Téléchargement terminé",
    "Les mods ont été installés avec succès!"
);

// Types disponibles
showInfo(title, message);
showSuccess(title, message);
showWarning(title, message);
showError(title, message);
```

### 2. Barre de Progression Améliorée

**Emplacement**: `com.nexaria.launcher.ui.progress.ProgressManager`

Progression détaillée avec vitesse, ETA, et fichier en cours.

**Usage**:
```java
ProgressManager progress = new ProgressManager("Téléchargement", 10, 1024000);

progress.addListener(update -> {
    System.out.println(update.getFormattedSpeed()); // "2.5 MB/s"
    System.out.println(update.getFormattedETA());   // "30s"
    System.out.println(update.getCurrentFileName()); // "mod.jar"
});

progress.updateBytes(512000, "mod.jar");
```

---

## 🔒 Sécurité

### 1. Chiffrement des Credentials

**Emplacement**: `com.nexaria.launcher.security.SecureCredentialManager`

Stockage sécurisé avec AES-256-GCM.

**Usage**:
```java
SecureCredentialManager manager = SecureCredentialManager.createDefault();

// Stocker
manager.storeCredential("azuriom_token", token);

// Récupérer
String token = manager.retrieveCredential("azuriom_token");

// Supprimer
manager.deleteCredential("azuriom_token");
```

**Sécurité**:
- AES-256-GCM avec IV aléatoire
- Clé stockée séparément avec permissions restrictives
- Protection contre la falsification avec GCM tag

### 2. Arguments JVM Sécurisés

**Emplacement**: `com.nexaria.launcher.security.SecureJvmArgsBuilder`

Génère des arguments JVM hardened pour Minecraft.

**Usage**:
```java
List<String> args = new SecureJvmArgsBuilder(config)
    .withSandbox(true)
    .withSecurityManager(false)
    .addCustomArg("-XX:+UseZGC")
    .build();
```

**Paramètres inclus**:
- Sandbox et resource limiting
- G1GC optimisé
- String deduplication
- Heap dump on OOM
- Security policies

---

## 📊 Monitoring & CI/CD

### 1. Health Check

**Emplacement**: `com.nexaria.launcher.health.HealthCheckService`

Vérifications au démarrage.

**Vérifications**:
- Java version (≥17)
- Espace disque (≥500 MB)
- Mémoire disponible (≥1024 MB)
- Système d'exploitation
- Permissions fichiers

**Usage**:
```java
HealthCheckService healthCheck = new HealthCheckService();
HealthCheckResult result = healthCheck.performHealthCheck();

if (result.hasCriticalIssues()) {
    // Afficher avertissement
}
```

### 2. Auto-Rollback

**Emplacement**: `com.nexaria.launcher.updater.RollbackManager`

Restaure l'ancienne version si crash après update.

**Fonctionnalités**:
- Backup automatique avant update
- Détection de crash au démarrage
- Rollback automatique
- Garde les 5 dernières versions

**Usage**:
```java
RollbackManager rollback = RollbackManager.createDefault();

// Avant update
rollback.backupCurrentVersion(currentJar, version);

// Au démarrage
if (rollback.checkAndRollbackIfNeeded()) {
    logger.warn("Rolled back to previous version");
}

// Marquer succès
rollback.clearCrashMarker();
```

### 3. GitHub Actions CI/CD

**Emplacement**: `.github/workflows/ci-cd.yml`

Pipeline automatisé multi-plateformes.

**Fonctionnalités**:
- Build sur Ubuntu, Windows, macOS
- Tests automatiques
- Scan de sécurité OWASP
- Génération de checksums SHA-256
- Signature GPG optionnelle
- Release automatique sur tag
- Support Docker

**Tags**:
```bash
git tag v1.1.0
git push origin v1.1.0
# → Déclenche build + release automatique
```

---

## 🎓 Exemples d'Intégration

### Exemple complet d'utilisation

```java
public class ExampleUsage {
    private static final LoggingService logger = LoggingService.getLogger(ExampleUsage.class);
    
    public void downloadModsWithResilience() {
        // 1. Circuit Breaker pour GitHub
        CircuitBreaker githubBreaker = CircuitBreaker.builder("github")
            .failureThreshold(5)
            .build();
        
        // 2. Retry avec backoff
        RetryExecutor retry = RetryExecutor.forNetwork();
        
        // 3. Cache pour éviter re-téléchargements
        CacheManager<String, File> cache = CacheFactory.getFileMetadataCache();
        
        // 4. Progression avec listener
        ProgressManager progress = new ProgressManager("Download", 5, 50000000);
        progress.addListener(update -> {
            ToastNotificationManager.getInstance().showInfo(
                "Téléchargement",
                update.getCurrentFileName() + " - " + update.getFormattedSpeed()
            );
        });
        
        try {
            // Télécharger avec toutes les protections
            File result = githubBreaker.execute(() -> 
                retry.execute(() -> 
                    cache.get("mod-key", () -> downloadMod())
                , "download_mod")
            );
            
            logger.info("Download completed successfully");
            
        } catch (CircuitBreaker.CircuitBreakerOpenException e) {
            logger.error("GitHub API is down, circuit breaker opened", e);
            ToastNotificationManager.getInstance().showError(
                "Service indisponible",
                "GitHub API temporairement inaccessible"
            );
        }
    }
}
```

---

## 📝 Migration depuis l'ancienne version

### Point d'entrée

**Avant**:
```java
public static void main(String[] args) {
    // Dans LauncherApp ou NexariaLauncher
}
```

**Après**:
```java
public static void main(String[] args) {
    NexariaLauncherMain.main(args);
}
```

### Configuration Maven

Mettre à jour `pom.xml`:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <configuration>
                <archive>
                    <manifest>
                        <mainClass>com.nexaria.launcher.NexariaLauncherMain</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 🔧 Configuration

Ajoutez ces propriétés à votre `config.yml`:

```yaml
# Sécurité
security:
  enableSandbox: true
  enableSecurityManager: false
  
# Cache
cache:
  githubApiTtl: 300  # 5 minutes
  integrityTtl: 3600 # 1 heure

# Téléchargements
downloads:
  maxConcurrent: 4
  retryAttempts: 5
  
# Monitoring
monitoring:
  enableHealthCheck: true
  enableMetrics: true
```

---

## 📚 Documentation Complète

Chaque composant est documenté avec Javadoc détaillée. Consultez:
- Code source pour les détails d'implémentation
- Tests unitaires (à venir) pour exemples d'usage
- Ce README pour vue d'ensemble

---

## 🤝 Contribution

Pour contribuer à ces améliorations:
1. Fork le projet
2. Créer une branche feature
3. Suivre les patterns établis
4. Ajouter des tests
5. Soumettre une PR

---

**Version**: 1.1.0  
**Date**: Janvier 2026  
**Auteur**: Nexaria Team
