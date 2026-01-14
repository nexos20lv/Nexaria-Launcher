package com.nexaria.launcher.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service de logging structuré avec corrélation d'événements et contexte enrichi.
 * Utilise SLF4J MDC (Mapped Diagnostic Context) pour le tracking des opérations.
 */
public class LoggingService {
    private static final String CORRELATION_ID = "correlationId";
    private static final String SESSION_ID = "sessionId";
    private static final String USER_ID = "userId";
    private static final String OPERATION = "operation";
    private static final String COMPONENT = "component";

    private final Logger logger;
    private final String component;

    private LoggingService(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
        this.component = clazz.getSimpleName();
    }

    public static LoggingService getLogger(Class<?> clazz) {
        return new LoggingService(clazz);
    }

    /**
     * Initialise un nouveau contexte de corrélation pour une opération.
     * Retourne le correlation ID généré.
     */
    public String startOperation(String operationName) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(OPERATION, operationName);
        MDC.put(COMPONENT, component);
        logger.info("Starting operation: {}", operationName);
        return correlationId;
    }

    /**
     * Termine une opération et nettoie le contexte MDC.
     */
    public void endOperation(String operationName) {
        logger.info("Completed operation: {}", operationName);
        MDC.remove(CORRELATION_ID);
        MDC.remove(OPERATION);
        MDC.remove(COMPONENT);
    }

    /**
     * Définit le contexte utilisateur pour les logs.
     */
    public void setUserContext(String userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * Définit l'ID de session pour tracer toute la session launcher.
     */
    public void setSessionId(String sessionId) {
        if (sessionId != null) {
            MDC.put(SESSION_ID, sessionId);
        }
    }

    /**
     * Nettoie tout le contexte MDC.
     */
    public void clearContext() {
        MDC.clear();
    }

    // === Méthodes de logging avec contexte ===

    public void trace(String message, Object... args) {
        logger.trace(message, args);
    }

    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public void error(String message, Object... args) {
        logger.error(message, args);
    }

    /**
     * Log d'un événement métier avec contexte enrichi.
     */
    public void logEvent(LogEvent event) {
        Map<String, String> previousContext = saveContext();
        
        try {
            // Enrichir le contexte avec les données de l'événement
            event.getContext().forEach(MDC::put);
            
            switch (event.getLevel()) {
                case TRACE:
                    logger.trace(event.getMessage());
                    break;
                case DEBUG:
                    logger.debug(event.getMessage());
                    break;
                case INFO:
                    logger.info(event.getMessage());
                    break;
                case WARN:
                    logger.warn(event.getMessage());
                    break;
                case ERROR:
                    if (event.getThrowable() != null) {
                        logger.error(event.getMessage(), event.getThrowable());
                    } else {
                        logger.error(event.getMessage());
                    }
                    break;
            }
        } finally {
            restoreContext(previousContext);
        }
    }

    /**
     * Crée un builder pour construire des événements de log complexes.
     */
    public LogEventBuilder eventBuilder() {
        return new LogEventBuilder(this);
    }

    // === Helpers pour sauvegarder/restaurer le contexte ===

    private Map<String, String> saveContext() {
        Map<String, String> context = new HashMap<>();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        if (mdcContext != null) {
            context.putAll(mdcContext);
        }
        return context;
    }

    private void restoreContext(Map<String, String> context) {
        MDC.clear();
        if (context != null && !context.isEmpty()) {
            MDC.setContextMap(context);
        }
    }

    /**
     * Builder pattern pour créer des événements de log structurés.
     */
    public static class LogEventBuilder {
        private final LoggingService loggingService;
        private LogLevel level = LogLevel.INFO;
        private String message;
        private Throwable throwable;
        private final Map<String, String> context = new HashMap<>();

        private LogEventBuilder(LoggingService loggingService) {
            this.loggingService = loggingService;
        }

        public LogEventBuilder level(LogLevel level) {
            this.level = level;
            return this;
        }

        public LogEventBuilder message(String message) {
            this.message = message;
            return this;
        }

        public LogEventBuilder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public LogEventBuilder addContext(String key, String value) {
            this.context.put(key, value);
            return this;
        }

        public LogEventBuilder addContext(String key, Object value) {
            if (value != null) {
                this.context.put(key, value.toString());
            }
            return this;
        }

        public void log() {
            LogEvent event = new LogEvent(level, message, throwable, context);
            loggingService.logEvent(event);
        }
    }

    /**
     * Représente un événement de log structuré.
     */
    private static class LogEvent {
        private final LogLevel level;
        private final String message;
        private final Throwable throwable;
        private final Map<String, String> context;

        public LogEvent(LogLevel level, String message, Throwable throwable, Map<String, String> context) {
            this.level = level;
            this.message = message;
            this.throwable = throwable;
            this.context = context;
        }

        public LogLevel getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public Map<String, String> getContext() {
            return context;
        }
    }

    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}
