package com.nexaria.launcher.resilience;

import com.nexaria.launcher.exception.LauncherException;
import com.nexaria.launcher.logging.LoggingService;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit Breaker Pattern pour protéger les appels réseau des défaillances en cascade.
 * 
 * États:
 * - CLOSED: Fonctionnement normal, tous les appels passent
 * - OPEN: Circuit ouvert, les appels sont immédiatement rejetés
 * - HALF_OPEN: Test de récupération, un appel test est autorisé
 */
public class CircuitBreaker {
    private static final LoggingService logger = LoggingService.getLogger(CircuitBreaker.class);

    private final String name;
    private final int failureThreshold;
    private final Duration timeout;
    private final Duration resetTimeout;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>();
    private final AtomicReference<Instant> openedAt = new AtomicReference<>();

    public CircuitBreaker(String name, int failureThreshold, Duration timeout, Duration resetTimeout) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeout = timeout;
        this.resetTimeout = resetTimeout;
    }

    /**
     * Exécute une opération protégée par le circuit breaker.
     */
    public <T> T execute(Callable<T> operation) throws Exception {
        // Vérifier si on peut tenter d'ouvrir le circuit
        if (state.get() == State.OPEN) {
            if (shouldAttemptReset()) {
                logger.info("Circuit breaker {} transitioning to HALF_OPEN", name);
                state.set(State.HALF_OPEN);
            } else {
                throw new CircuitBreakerOpenException(
                        String.format("Circuit breaker '%s' is OPEN. Too many failures detected.", name)
                );
            }
        }

        long startTime = System.currentTimeMillis();
        try {
            T result = operation.call();
            onSuccess(System.currentTimeMillis() - startTime);
            return result;

        } catch (Exception e) {
            onFailure(System.currentTimeMillis() - startTime);
            throw e;
        }
    }

    /**
     * Version pour les Runnable.
     */
    public void execute(Runnable operation) throws Exception {
        execute(() -> {
            operation.run();
            return null;
        });
    }

    /**
     * Appelé quand une opération réussit.
     */
    private void onSuccess(long executionTimeMs) {
        failureCount.set(0);
        lastFailureTime.set(null);

        if (state.get() == State.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            logger.eventBuilder()
                    .level(LoggingService.LogLevel.INFO)
                    .message("Circuit breaker recovering")
                    .addContext("circuitBreaker", name)
                    .addContext("successCount", successes)
                    .addContext("executionTimeMs", executionTimeMs)
                    .log();

            // Après quelques succès en HALF_OPEN, on ferme le circuit
            if (successes >= 2) {
                logger.info("Circuit breaker {} CLOSED after successful recovery", name);
                state.set(State.CLOSED);
                successCount.set(0);
            }
        }

        if (executionTimeMs > timeout.toMillis()) {
            logger.eventBuilder()
                    .level(LoggingService.LogLevel.WARN)
                    .message("Circuit breaker operation slow")
                    .addContext("circuitBreaker", name)
                    .addContext("executionTimeMs", executionTimeMs)
                    .addContext("timeoutMs", timeout.toMillis())
                    .log();
        }
    }

    /**
     * Appelé quand une opération échoue.
     */
    private void onFailure(long executionTimeMs) {
        lastFailureTime.set(Instant.now());
        int failures = failureCount.incrementAndGet();

        logger.eventBuilder()
                .level(LoggingService.LogLevel.WARN)
                .message("Circuit breaker operation failed")
                .addContext("circuitBreaker", name)
                .addContext("failureCount", failures)
                .addContext("threshold", failureThreshold)
                .addContext("state", state.get().name())
                .log();

        if (state.get() == State.HALF_OPEN) {
            // En HALF_OPEN, un seul échec suffit pour rouvrir le circuit
            logger.warn("Circuit breaker {} reopening after failure in HALF_OPEN state", name);
            tripCircuit();
        } else if (failures >= failureThreshold) {
            // Trop d'échecs, on ouvre le circuit
            logger.error("Circuit breaker {} OPENED after {} failures", name, failures);
            tripCircuit();
        }
    }

    /**
     * Ouvre le circuit.
     */
    private void tripCircuit() {
        state.set(State.OPEN);
        openedAt.set(Instant.now());
        successCount.set(0);
    }

    /**
     * Détermine si on doit tenter de réinitialiser le circuit.
     */
    private boolean shouldAttemptReset() {
        Instant opened = openedAt.get();
        if (opened == null) {
            return false;
        }

        Duration elapsed = Duration.between(opened, Instant.now());
        return elapsed.compareTo(resetTimeout) >= 0;
    }

    /**
     * Réinitialise manuellement le circuit breaker.
     */
    public void reset() {
        logger.info("Circuit breaker {} manually reset", name);
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        lastFailureTime.set(null);
        openedAt.set(null);
    }

    /**
     * Retourne l'état actuel du circuit breaker.
     */
    public State getState() {
        return state.get();
    }

    /**
     * Retourne le nombre d'échecs actuels.
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Retourne des statistiques sur le circuit breaker.
     */
    public CircuitBreakerStats getStats() {
        return new CircuitBreakerStats(
                name,
                state.get(),
                failureCount.get(),
                failureThreshold,
                lastFailureTime.get(),
                openedAt.get()
        );
    }

    public enum State {
        CLOSED,    // Fonctionnement normal
        OPEN,      // Circuit ouvert, rejette les appels
        HALF_OPEN  // Test de récupération
    }

    /**
     * Exception levée quand le circuit breaker est ouvert.
     */
    public static class CircuitBreakerOpenException extends LauncherException {
        public CircuitBreakerOpenException(String message) {
            super(message, ErrorSeverity.WARNING);
        }
    }

    /**
     * Statistiques d'un circuit breaker.
     */
    public static class CircuitBreakerStats {
        private final String name;
        private final State state;
        private final int failureCount;
        private final int failureThreshold;
        private final Instant lastFailureTime;
        private final Instant openedAt;

        public CircuitBreakerStats(String name, State state, int failureCount, 
                                  int failureThreshold, Instant lastFailureTime, Instant openedAt) {
            this.name = name;
            this.state = state;
            this.failureCount = failureCount;
            this.failureThreshold = failureThreshold;
            this.lastFailureTime = lastFailureTime;
            this.openedAt = openedAt;
        }

        public String getName() { return name; }
        public State getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getFailureThreshold() { return failureThreshold; }
        public Instant getLastFailureTime() { return lastFailureTime; }
        public Instant getOpenedAt() { return openedAt; }

        @Override
        public String toString() {
            return String.format("CircuitBreaker[%s] state=%s, failures=%d/%d", 
                    name, state, failureCount, failureThreshold);
        }
    }

    /**
     * Builder pour créer des circuit breakers avec configuration personnalisée.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private int failureThreshold = 5;
        private Duration timeout = Duration.ofSeconds(10);
        private Duration resetTimeout = Duration.ofSeconds(60);

        private Builder(String name) {
            this.name = name;
        }

        public Builder failureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder resetTimeout(Duration resetTimeout) {
            this.resetTimeout = resetTimeout;
            return this;
        }

        public CircuitBreaker build() {
            return new CircuitBreaker(name, failureThreshold, timeout, resetTimeout);
        }
    }
}
