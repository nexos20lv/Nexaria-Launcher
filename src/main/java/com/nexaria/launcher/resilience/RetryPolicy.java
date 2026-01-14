package com.nexaria.launcher.resilience;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Politique de retry définissant le comportement des tentatives automatiques.
 */
public class RetryPolicy {
    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final boolean jitterEnabled;
    private final Set<Class<? extends Exception>> retryableExceptions;
    private final Predicate<Exception> customRetryPredicate;

    private RetryPolicy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.jitterEnabled = builder.jitterEnabled;
        this.retryableExceptions = builder.retryableExceptions;
        this.customRetryPredicate = builder.customRetryPredicate;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public boolean isJitterEnabled() {
        return jitterEnabled;
    }

    /**
     * Détermine si une exception doit déclencher un retry.
     */
    public boolean isRetryable(Exception exception) {
        // Si un prédicat personnalisé est défini, l'utiliser en priorité
        if (customRetryPredicate != null) {
            return customRetryPredicate.test(exception);
        }

        // Si aucune exception spécifique n'est définie, tout est retryable
        if (retryableExceptions.isEmpty()) {
            return true;
        }

        // Vérifier si l'exception ou une de ses super-classes est dans la liste
        for (Class<? extends Exception> retryableClass : retryableExceptions) {
            if (retryableClass.isInstance(exception)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Builder pour créer une RetryPolicy personnalisée.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(500);
        private Duration maxDelay = Duration.ofSeconds(10);
        private double backoffMultiplier = 2.0;
        private boolean jitterEnabled = true;
        private Set<Class<? extends Exception>> retryableExceptions = new HashSet<>();
        private Predicate<Exception> customRetryPredicate = null;

        /**
         * Nombre maximum de tentatives (1 = pas de retry, 2 = 1 retry, etc.)
         */
        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Délai initial avant le premier retry.
         */
        public Builder initialDelay(Duration initialDelay) {
            if (initialDelay.isNegative()) {
                throw new IllegalArgumentException("initialDelay must be positive");
            }
            this.initialDelay = initialDelay;
            return this;
        }

        /**
         * Délai maximum entre deux tentatives.
         */
        public Builder maxDelay(Duration maxDelay) {
            if (maxDelay.isNegative()) {
                throw new IllegalArgumentException("maxDelay must be positive");
            }
            this.maxDelay = maxDelay;
            return this;
        }

        /**
         * Multiplicateur pour le backoff exponentiel (généralement 2.0).
         */
        public Builder backoffMultiplier(double multiplier) {
            if (multiplier < 1.0) {
                throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
            }
            this.backoffMultiplier = multiplier;
            return this;
        }

        /**
         * Active ou désactive le jitter aléatoire.
         */
        public Builder jitter(boolean enabled) {
            this.jitterEnabled = enabled;
            return this;
        }

        /**
         * Ajoute un type d'exception qui doit déclencher un retry.
         */
        public Builder retryOn(Class<? extends Exception> exceptionClass) {
            this.retryableExceptions.add(exceptionClass);
            return this;
        }

        /**
         * Définit un prédicat personnalisé pour déterminer si un retry doit être effectué.
         */
        public Builder retryIf(Predicate<Exception> predicate) {
            this.customRetryPredicate = predicate;
            return this;
        }

        /**
         * Configure pour ne retenter que sur des exceptions réseau courantes.
         */
        public Builder forNetworkErrors() {
            retryOn(java.io.IOException.class);
            retryOn(java.net.SocketTimeoutException.class);
            retryOn(java.net.UnknownHostException.class);
            retryOn(java.net.ConnectException.class);
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
