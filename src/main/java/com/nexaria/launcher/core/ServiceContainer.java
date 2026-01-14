package com.nexaria.launcher.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Conteneur d'injection de dépendances léger pour le launcher.
 * Permet de découpler les composants et faciliter les tests.
 */
public class ServiceContainer {
    private static ServiceContainer instance;
    
    private final Map<Class<?>, Object> singletons = new HashMap<>();
    private final Map<Class<?>, Supplier<?>> factories = new HashMap<>();

    private ServiceContainer() {}

    public static ServiceContainer getInstance() {
        if (instance == null) {
            synchronized (ServiceContainer.class) {
                if (instance == null) {
                    instance = new ServiceContainer();
                }
            }
        }
        return instance;
    }

    /**
     * Enregistre un singleton.
     */
    public <T> void registerSingleton(Class<T> type, T instance) {
        singletons.put(type, instance);
    }

    /**
     * Enregistre une factory pour créer des instances à la demande.
     */
    public <T> void registerFactory(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
    }

    /**
     * Résout une dépendance (singleton ou factory).
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        // Vérifier d'abord les singletons
        if (singletons.containsKey(type)) {
            return (T) singletons.get(type);
        }

        // Ensuite les factories
        if (factories.containsKey(type)) {
            return (T) factories.get(type).get();
        }

        throw new IllegalArgumentException("No registration found for type: " + type.getName());
    }

    /**
     * Vérifie si un type est enregistré.
     */
    public boolean isRegistered(Class<?> type) {
        return singletons.containsKey(type) || factories.containsKey(type);
    }

    /**
     * Réinitialise le conteneur (utile pour les tests).
     */
    public void reset() {
        singletons.clear();
        factories.clear();
    }
}
