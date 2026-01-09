package com.nexaria.launcher.ui;

import javax.swing.*;

/**
 * Classe de test pour le Splash Screen
 * Simule les différents états de mise à jour
 */
public class UpdateSplashScreenTest {
    
    public static void main(String[] args) {
        // Afficher un menu de sélection
        String[] options = {
            "Test 1: Vérification (success)",
            "Test 2: Mise à jour disponible",
            "Test 3: Pas de mise à jour",
            "Test 4: Erreur réseau",
            "Test 5: Téléchargement avec progression"
        };
        
        int choice = JOptionPane.showOptionDialog(
            null,
            "Sélectionnez un test:",
            "Splash Screen Test",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        switch (choice) {
            case 0:
            case 1:
                testUpdateAvailable();
                break;
            case 2:
                testNoUpdate();
                break;
            case 3:
                testError();
                break;
            case 4:
                testDownloadProgress();
                break;
            default:
                System.exit(0);
        }
    }
    
    /**
     * Test 1: Mise à jour disponible avec téléchargement
     */
    private static void testUpdateAvailable() {
        System.out.println("=== Test: Mise à jour disponible ===\n");
        
        UpdateSplashScreen splash = new UpdateSplashScreen();
        
        // Simuler les étapes
        new Thread(() -> {
            try {
                // Étape 1: Vérification
                Thread.sleep(1000);
                splash.statusLabel.setText("✅ Mise à jour disponible: v1.0.6");
                splash.subStatusLabel.setText("Téléchargement en cours...");
                splash.progressBar.setValue(30);
                
                // Étape 2: Téléchargement
                Thread.sleep(1000);
                for (int i = 30; i <= 90; i += 10) {
                    splash.progressBar.setValue(i);
                    splash.subStatusLabel.setText(String.format("%.1f MB / 15.2 MB", i * 0.15));
                    Thread.sleep(500);
                }
                
                // Étape 3: Installation
                Thread.sleep(1000);
                splash.statusLabel.setText("📦 Installation en cours...");
                splash.subStatusLabel.setText("Préparation du redémarrage...");
                splash.progressBar.setValue(95);
                
                Thread.sleep(2000);
                splash.closeScreen();
                System.out.println("Test complété!\n");
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Test 2: Pas de mise à jour disponible
     */
    private static void testNoUpdate() {
        System.out.println("=== Test: Pas de mise à jour ===\n");
        
        UpdateSplashScreen splash = new UpdateSplashScreen();
        
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                splash.statusLabel.setText("✅ Launcher à jour");
                splash.subStatusLabel.setText("Vous disposez déjà de la dernière version");
                splash.progressBar.setValue(100);
                splash.progressBar.setForeground(new java.awt.Color(76, 175, 80));
                
                Thread.sleep(2000);
                splash.closeScreen();
                System.out.println("Test complété!\n");
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Test 3: Erreur lors de la vérification
     */
    private static void testError() {
        System.out.println("=== Test: Erreur ===\n");
        
        UpdateSplashScreen splash = new UpdateSplashScreen();
        
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                splash.statusLabel.setText("⚠️ Erreur lors de la vérification");
                splash.subStatusLabel.setText("Repo GitHub inexistant: Nexaria/launcher");
                splash.progressBar.setValue(100);
                splash.progressBar.setForeground(new java.awt.Color(200, 100, 100));
                
                Thread.sleep(3000);
                splash.closeScreen();
                System.out.println("Test complété!\n");
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Test 4: Simulation réaliste du téléchargement avec progression
     */
    private static void testDownloadProgress() {
        System.out.println("=== Test: Téléchargement avec progression ===\n");
        
        UpdateSplashScreen splash = new UpdateSplashScreen();
        
        new Thread(() -> {
            try {
                // Étape 1: Vérification
                splash.statusLabel.setText("🔍 Vérification des mises à jour...");
                splash.subStatusLabel.setText("Connexion à GitHub API...");
                splash.progressBar.setValue(10);
                Thread.sleep(2000);
                
                // Étape 2: Mise à jour trouvée
                splash.statusLabel.setText("✅ Mise à jour disponible: v1.0.6");
                splash.subStatusLabel.setText("Préparation du téléchargement...");
                splash.progressBar.setValue(30);
                Thread.sleep(1500);
                
                // Étape 3: Téléchargement progressif
                splash.statusLabel.setText("⬇️ Téléchargement de v1.0.6");
                splash.progressBar.setForeground(new java.awt.Color(0, 120, 215));
                
                long totalSize = 16 * 1024 * 1024; // 16 MB
                long downloaded = 0;
                
                while (downloaded < totalSize) {
                    // Simuler le téléchargement par chunks
                    long chunkSize = 1024 * 1024; // 1 MB
                    downloaded += chunkSize;
                    
                    int progress = (int) ((downloaded * 60) / totalSize) + 40;
                    splash.progressBar.setValue(Math.min(100, progress));
                    
                    String downloadedMB = String.format("%.1f", downloaded / (1024.0 * 1024.0));
                    String totalMB = String.format("%.1f", totalSize / (1024.0 * 1024.0));
                    splash.subStatusLabel.setText(downloadedMB + " MB / " + totalMB + " MB");
                    
                    Thread.sleep(300); // Simuler le temps de téléchargement
                }
                
                // Étape 4: Installation
                Thread.sleep(1000);
                splash.statusLabel.setText("📦 Installation en cours...");
                splash.subStatusLabel.setText("Préparation du redémarrage...");
                splash.progressBar.setValue(95);
                
                Thread.sleep(2000);
                splash.statusLabel.setText("✅ Installation complète");
                splash.subStatusLabel.setText("Redémarrage du launcher...");
                splash.progressBar.setValue(100);
                
                Thread.sleep(2000);
                splash.closeScreen();
                System.out.println("Test complété!\n");
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
