package com.nexaria.launcher.resilience;

import com.nexaria.launcher.exception.LauncherException;
import com.nexaria.launcher.logging.LoggingService;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * Exécuteur de retry avec backoff exponentiel et jitter.
 * Permet de retenter automatiquement des opérations qui peuvent échouer temporairement.
 */
public class RetryExecutor {
    private static final LoggingService logger = LoggingService.getLogger(RetryExecutor.class);

    private final RetryPolicy policy;

    public RetryExecutor(RetryPolicy policy) {
        this.policy = policy;
    }

    /**
     * Exécute une opération avec retry selon la politique définie.
     * 
     * @param operation L'opération à exécuter
     * @param operationName Nom de l'opération pour les logs
     * @return Le résultat de l'opération
     * @throws Exception Si toutes les tentatives échouent
     */
    public <T> T execute(Callable<T> operation, String operationName) throws Exception {
        int attempt = 0;
        Exception lastException = null;

        String correlationId = logger.startOperation("retry_" + operationName);

        try {
            while (attempt < policy.getMaxAttempts()) {
                attempt++;

                try {
                    logger.eventBuilder()
                            .level(LoggingService.LogLevel.DEBUG)
                            .message("Attempting operation: " + operationName)
                            .addContext("attempt", attempt)
                            .addContext("maxAttempts", policy.getMaxAttempts())
                            .log();

                    T result = operation.call();

                    if (attempt > 1) {
                        logger.eventBuilder()
                                .level(LoggingService.LogLevel.INFO)
                                .message("Operation succeeded after retry")
                                .addContext("operation", operationName)
                                .addContext("attempts", attempt)
                                .log();
                    }

                    return result;

                } catch (Exception e) {
                    lastException = e;

                    // Vérifier si cette exception est retryable
                    if (!policy.isRetryable(e)) {
                        logger.eventBuilder()
                                .level(LoggingService.LogLevel.WARN)
                                .message("Non-retryable exception encountered")
                                .addContext("operation", operationName)
                                .addContext("exceptionType", e.getClass().getSimpleName())
                                .throwable(e)
                                .log();
                        throw e;
                    }

                    // Si c'est la dernière tentative, on lève l'exception
                    if (attempt >= policy.getMaxAttempts()) {
                        logger.eventBuilder()
                                .level(LoggingService.LogLevel.ERROR)
                                .message("All retry attempts exhausted")
                                .addContext("operation", operationName)
                                .addContext("attempts", attempt)
                                .throwable(e)
                                .log();
                        break;
                    }

                    // Calculer le délai avec backoff exponentiel et jitter
                    long delayMs = calculateDelay(attempt);

                    logger.eventBuilder()
                            .level(LoggingService.LogLevel.WARN)
                            .message("Operation failed, will retry")
                            .addContext("operation", operationName)
                            .addContext("attempt", attempt)
                            .addContext("nextRetryInMs", delayMs)
                            .addContext("exceptionType", e.getClass().getSimpleName())
                            .log();

                    // Attendre avant la prochaine tentative
                    Thread.sleep(delayMs);
                }
            }

            // Si on arrive ici, toutes les tentatives ont échoué
            throw new LauncherException(
                    String.format("Operation '%s' failed after %d attempts", operationName, attempt),
                    lastException
            );

        } finally {
            logger.endOperation("retry_" + operationName);
        }
    }

    /**
     * Version simplifié pour les Runnable (opérations sans retour).
     */
    public void execute(Runnable operation, String operationName) throws Exception {
        execute(() -> {
            operation.run();
            return null;
        }, operationName);
    }

    /**
     * Calcule le délai avant la prochaine tentative avec backoff exponentiel.
     * Formule: min(initialDelay * (multiplier ^ (attempt - 1)) + jitter, maxDelay)
     */
    private long calculateDelay(int attempt) {
        long baseDelay = policy.getInitialDelay().toMillis();
        double multiplier = policy.getBackoffMultiplier();

        // Calcul du délai avec backoff exponentiel
        long delay = (long) (baseDelay * Math.pow(multiplier, attempt - 1));

        // Ajouter un jitter aléatoire (±25%) pour éviter les thundering herd
        if (policy.isJitterEnabled()) {
            double jitterFactor = 0.75 + (Math.random() * 0.5); // Entre 0.75 et 1.25
            delay = (long) (delay * jitterFactor);
        }

        // Appliquer la limite maximale
        return Math.min(delay, policy.getMaxDelay().toMillis());
    }

    /**
     * Crée un RetryExecutor avec une politique par défaut.
     */
    public static RetryExecutor withDefaults() {
        return new RetryExecutor(RetryPolicy.builder().build());
    }

    /**
     * Crée un RetryExecutor pour les opérations réseau (délais plus longs).
     */
    public static RetryExecutor forNetwork() {
        return new RetryExecutor(
                RetryPolicy.builder()
                        .maxAttempts(5)
                        .initialDelay(Duration.ofSeconds(2))
                        .maxDelay(Duration.ofSeconds(30))
                        .backoffMultiplier(2.0)
                        .retryOn(java.io.IOException.class)
                        .retryOn(java.net.SocketTimeoutException.class)
                        .build()
        );
    }

    /**
     * Crée un RetryExecutor pour les opérations rapides (délais courts).
     */
    public static RetryExecutor forQuickOperations() {
        return new RetryExecutor(
                RetryPolicy.builder()
                        .maxAttempts(3)
                        .initialDelay(Duration.ofMillis(100))
                        .maxDelay(Duration.ofSeconds(1))
                        .backoffMultiplier(2.0)
                        .build()
        );
    }
}
