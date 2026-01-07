package com.nexora.launcher.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge la configuration depuis un fichier YAML simple
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    
    private Map<String, String> config = new HashMap<>();

    public void loadFromFile(String filePath) throws Exception {
        File configFile = new File(filePath);
        if (!configFile.exists()) {
            throw new FileNotFoundException("Config file not found: " + filePath);
        }

        logger.info("Chargement de la configuration depuis: {}", filePath);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Ignorer les commentaires et les lignes vides
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parser les paires clé: valeur
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    config.put(key, value);
                }
            }
        }
        
        logger.info("Configuration chargée avec {} paramètres", config.size());
    }

    public String getString(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public String getString(String key) {
        return config.get(key);
    }

    public int getInt(String key, int defaultValue) {
        String value = config.get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = config.get(key);
        if (value == null) return defaultValue;
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes");
    }

    public Map<String, String> getAll() {
        return new HashMap<>(config);
    }
}

class FileNotFoundException extends Exception {
    public FileNotFoundException(String message) {
        super(message);
    }
}
