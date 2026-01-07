package com.nexora.launcher.ui;

import com.nexora.launcher.auth.AzAuthManager;
import com.nexora.launcher.downloader.ModManager;
import com.nexora.launcher.downloader.FileDownloader;
import com.nexora.launcher.config.LauncherConfig;
import com.azuriom.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class LauncherGUI extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(LauncherGUI.class);
    private AzAuthManager authManager;
    private ModManager modManager;
    private LoginPanel loginPanel;
    private MainPanel mainPanel;
    private CardLayout cardLayout;
    private JPanel contentPanel;

    public LauncherGUI() {
        setTitle("Nexora Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        LauncherConfig config = LauncherConfig.getInstance();
        this.authManager = new AzAuthManager(config.getAzuriomUrl());
        String gameDirectory = System.getProperty("user.home") + "/.nexora/game";
        this.modManager = new ModManager(gameDirectory, config.getServerUrl() + "/manifest.json");

        // Layout avec CardsPanel pour basculer entre les panneaux
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Créer les panneaux
        loginPanel = new LoginPanel(this::handleLogin);
        mainPanel = new MainPanel(this::handleLaunch, this::handleLogout);

        contentPanel.add(loginPanel, "LOGIN");
        contentPanel.add(mainPanel, "MAIN");

        add(contentPanel);
        
        // Afficher le panneau de connexion par défaut
        cardLayout.show(contentPanel, "LOGIN");
    }

    /**
     * Gérer la connexion
     */
    private void handleLogin(User user) {
        loginPanel.clearPassword();
        mainPanel.setUserProfile(user);
        mainPanel.setVersion(
            LauncherConfig.getInstance().getMinecraftVersion(),
            LauncherConfig.getInstance().getLoader(),
            LauncherConfig.getInstance().getLoaderVersion()
        );
        cardLayout.show(contentPanel, "MAIN");
    }

    /**
     * Gérer le lancement du jeu
     */
    private void handleLaunch() {
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    FileDownloader.DownloadProgressListener progressListener = (downloaded, total) -> {
                        int progress = (int) ((downloaded * 100) / total);
                        publish(progress);
                    };

                    mainPanel.setStatus("Téléchargement des fichiers...");
                    modManager.downloadAndInstallMods(progressListener);
                    
                    mainPanel.setStatus("Nettoyage des fichiers obsolètes...");
                    modManager.cleanupOldFiles();
                    
                    mainPanel.setStatus("Lancement du jeu...");
                    launchGame();
                    
                    return null;
                } catch (Exception e) {
                    logger.error("Erreur lors du téléchargement", e);
                    throw e;
                }
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                for (Integer progress : chunks) {
                    mainPanel.setProgress(progress);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    mainPanel.setButtonsEnabled(true);
                    JOptionPane.showMessageDialog(LauncherGUI.this,
                            "Jeu lancé avec succès!",
                            "Succès",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    mainPanel.setButtonsEnabled(true);
                    mainPanel.setStatus("Erreur: " + e.getMessage());
                    JOptionPane.showMessageDialog(LauncherGUI.this,
                            "Erreur: " + e.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        mainPanel.setButtonsEnabled(false);
        mainPanel.setProgress(0);
        worker.execute();
    }

    /**
     * Lancer le jeu Minecraft
     */
    private void launchGame() throws Exception {
        LauncherConfig config = LauncherConfig.getInstance();
        String gameDirectory = System.getProperty("user.home") + "/.nexora/game";
        
        // Exemple de lancement avec Java
        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "-Xmx" + config.getMaxMemory() + "M",
            "-Xms" + config.getMinMemory() + "M",
            "-Djava.library.path=" + gameDirectory + "/natives",
            "-cp", gameDirectory + "/mods/*:" + gameDirectory + "/libs/*",
            "net.minecraft.client.main.Main"
        );
        
        pb.directory(new File(gameDirectory));
        pb.start();
        
        logger.info("Jeu lancé avec succès");
    }

    /**
     * Gérer la déconnexion
     */
    private void handleLogout() {
        try {
            authManager.logout();
        } catch (Exception e) {
            logger.error("Erreur lors de la déconnexion", e);
        }
        mainPanel.reset();
        cardLayout.show(contentPanel, "LOGIN");
    }
}
