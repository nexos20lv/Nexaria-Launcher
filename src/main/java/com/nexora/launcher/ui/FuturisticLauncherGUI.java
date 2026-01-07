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
import java.awt.geom.Ellipse2D;
import java.io.File;

public class FuturisticLauncherGUI extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(FuturisticLauncherGUI.class);
    private static final Color DARK_BG = new Color(15, 23, 42);
    private static final Color ACCENT_BLUE = new Color(59, 130, 246);
    private static final Color ACCENT_PURPLE = new Color(139, 92, 246);
    private static final Color TEXT_PRIMARY = new Color(241, 245, 249);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);

    private AzAuthManager authManager;
    private ModManager modManager;
    private FuturisticLoginPanel loginPanel;
    private FuturisticMainPanel mainPanel;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JLabel logoLabel;

    public FuturisticLauncherGUI() {
        setTitle("NEXORA LAUNCHER");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        // Rounded corners
        setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 1000, 700, 20, 20));

        LauncherConfig config = LauncherConfig.getInstance();
        this.authManager = new AzAuthManager(config.getAzuriomUrl());
        String gameDirectory = System.getProperty("user.home") + "/.nexora/game";
        this.modManager = new ModManager(gameDirectory, config.getServerUrl() + "/api/manifest");

        // Layout avec CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(DARK_BG);

        // Créer les panneaux
        loginPanel = new FuturisticLoginPanel(this::handleLogin);
        mainPanel = new FuturisticMainPanel(this::handleLaunch, this::handleLogout);

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
                    JOptionPane.showMessageDialog(FuturisticLauncherGUI.this,
                            "Jeu lancé avec succès!",
                            "Succès",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    mainPanel.setButtonsEnabled(true);
                    mainPanel.setStatus("Erreur: " + e.getMessage());
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
