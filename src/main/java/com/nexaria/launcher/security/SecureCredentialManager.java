package com.nexaria.launcher.security;

import com.nexaria.launcher.logging.LoggingService;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.Base64;

/**
 * Gestionnaire sécurisé des credentials utilisant Java Keystore et chiffrement AES-256-GCM.
 * Stocke les tokens et mots de passe de manière sécurisée au lieu du plain text.
 */
public class SecureCredentialManager {
    private static final LoggingService logger = LoggingService.getLogger(SecureCredentialManager.class);
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    
    private final Path keystorePath;
    private final SecretKey encryptionKey;

    public SecureCredentialManager(String keystorePath) throws Exception {
        this.keystorePath = Paths.get(keystorePath);
        this.encryptionKey = loadOrCreateKey();
        
        logger.info("SecureCredentialManager initialized with keystore: {}", keystorePath);
    }

    /**
     * Charge ou crée une nouvelle clé de chiffrement.
     */
    private SecretKey loadOrCreateKey() throws Exception {
        File keyFile = keystorePath.toFile();
        
        if (keyFile.exists()) {
            logger.debug("Loading existing encryption key");
            return loadKey(keyFile);
        } else {
            logger.info("Creating new encryption key");
            SecretKey key = generateKey();
            saveKey(key, keyFile);
            return key;
        }
    }

    /**
     * Génère une nouvelle clé AES-256.
     */
    private SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGen.init(KEY_SIZE, new SecureRandom());
        return keyGen.generateKey();
    }

    /**
     * Sauvegarde la clé de manière sécurisée.
     */
    private void saveKey(SecretKey key, File keyFile) throws Exception {
        // S'assurer que le répertoire parent existe
        File parent = keyFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // Encoder la clé en Base64 et la sauvegarder
        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
        
        try (FileWriter writer = new FileWriter(keyFile)) {
            writer.write(encodedKey);
        }

        // Rendre le fichier accessible uniquement par l'utilisateur (Unix)
        try {
            keyFile.setReadable(false, false);
            keyFile.setReadable(true, true);
            keyFile.setWritable(false, false);
            keyFile.setWritable(true, true);
        } catch (Exception e) {
            logger.warn("Failed to set file permissions (not critical on Windows)", e);
        }

        logger.debug("Encryption key saved securely");
    }

    /**
     * Charge une clé depuis un fichier.
     */
    private SecretKey loadKey(File keyFile) throws Exception {
        String encodedKey = new String(Files.readAllBytes(keyFile.toPath()));
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, KEY_ALGORITHM);
    }

    /**
     * Chiffre des données sensibles.
     * 
     * @param plaintext Texte en clair à chiffrer
     * @return Texte chiffré encodé en Base64
     */
    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }

        logger.debug("Encrypting data");

        // Générer un IV aléatoire
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Configurer le cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, parameterSpec);

        // Chiffrer
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Combiner IV + ciphertext
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        // Encoder en Base64
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Déchiffre des données.
     * 
     * @param ciphertext Texte chiffré encodé en Base64
     * @return Texte en clair
     */
    public String decrypt(String ciphertext) throws Exception {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return "";
        }

        logger.debug("Decrypting data");

        // Décoder depuis Base64
        byte[] combined = Base64.getDecoder().decode(ciphertext);

        // Extraire IV et ciphertext
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] actualCiphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, actualCiphertext, 0, actualCiphertext.length);

        // Configurer le cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, parameterSpec);

        // Déchiffrer
        byte[] plaintext = cipher.doFinal(actualCiphertext);
        return new String(plaintext, "UTF-8");
    }

    /**
     * Stocke un credential de manière sécurisée.
     * 
     * @param key Identifiant du credential (ex: "azuriom_token")
     * @param value Valeur à stocker
     */
    public void storeCredential(String key, String value) throws Exception {
        logger.eventBuilder()
                .level(LoggingService.LogLevel.DEBUG)
                .message("Storing credential")
                .addContext("key", key)
                .log();

        String encrypted = encrypt(value);
        
        // Stocker dans un fichier dédié
        Path credFile = keystorePath.getParent().resolve(key + ".cred");
        Files.writeString(credFile, encrypted);

        // Sécuriser les permissions
        try {
            File file = credFile.toFile();
            file.setReadable(false, false);
            file.setReadable(true, true);
            file.setWritable(false, false);
            file.setWritable(true, true);
        } catch (Exception e) {
            logger.warn("Failed to set credential file permissions", e);
        }
    }

    /**
     * Récupère un credential stocké.
     * 
     * @param key Identifiant du credential
     * @return Valeur déchiffrée ou null si absent
     */
    public String retrieveCredential(String key) throws Exception {
        logger.debug("Retrieving credential: {}", key);

        Path credFile = keystorePath.getParent().resolve(key + ".cred");
        
        if (!Files.exists(credFile)) {
            logger.debug("Credential file does not exist: {}", key);
            return null;
        }

        String encrypted = Files.readString(credFile);
        return decrypt(encrypted);
    }

    /**
     * Supprime un credential.
     */
    public void deleteCredential(String key) throws Exception {
        logger.info("Deleting credential: {}", key);
        
        Path credFile = keystorePath.getParent().resolve(key + ".cred");
        Files.deleteIfExists(credFile);
    }

    /**
     * Vérifie si un credential existe.
     */
    public boolean hasCredential(String key) {
        Path credFile = keystorePath.getParent().resolve(key + ".cred");
        return Files.exists(credFile);
    }

    /**
     * Nettoie tous les credentials (pour logout complet).
     */
    public void clearAllCredentials() throws Exception {
        logger.warn("Clearing all stored credentials");
        
        File[] credFiles = keystorePath.getParent().toFile().listFiles(
                (dir, name) -> name.endsWith(".cred")
        );

        if (credFiles != null) {
            for (File file : credFiles) {
                Files.delete(file.toPath());
            }
        }
    }

    /**
     * Crée un gestionnaire avec le chemin par défaut.
     */
    public static SecureCredentialManager createDefault() throws Exception {
        String userHome = System.getProperty("user.home");
        Path defaultPath = Paths.get(userHome, ".nexaria-launcher", "secure", "keystore.key");
        
        // S'assurer que le répertoire existe
        Files.createDirectories(defaultPath.getParent());
        
        return new SecureCredentialManager(defaultPath.toString());
    }
}
