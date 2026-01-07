package com.nexora.launcher.ui;

import com.nexora.launcher.auth.AzAuthManager;
import com.nexora.launcher.downloader.GitHubModManager;
import com.nexora.launcher.config.LauncherConfig;
import com.nexora.launcher.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FuturisticLauncherGUI extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(FuturisticLauncherGUI.class);
    private static final Color DARK_BG = new Color(15, 23, 42);
    private static final Color ACCENT_BLUE = new Color(59, 130, 246);
    private static final Color ACCENT_PURPLE = new Color(139, 92, 246);
    private static final Color TEXT_PRIMARY = new Color(241, 245, 249);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);

    private AzAuthManager authManager;
    private GitHubModManager modManager;
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

        LauncherConfig config = LauncherConfig.getInstance();
        this.authManager = new AzAuthManager(config.getAzuriomUrl());
        this.modManager = new GitHubModManager(null);
        getContentPane().setLayout(new BorderLayout());

        // Title bar & sidebar
        getContentPane().add(new TitleBar(this), BorderLayout.NORTH);
        Sidebar sidebar = new Sidebar(this::navigate);
        getContentPane().add(sidebar, BorderLayout.WEST);

        // Card area
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(DARK_BG);

        // Créer les panneaux
        loginPanel = new FuturisticLoginPanel(this::handleLogin);
        mainPanel = new FuturisticMainPanel(this::handleLaunch, this::handleLogout);
        JPanel settingsPanel = new SettingsPanel();

        contentPanel.add(loginPanel, "LOGIN");
        contentPanel.add(mainPanel, "ACCUEIL");
        contentPanel.add(settingsPanel, "PARAMÈTRES");

        getContentPane().add(contentPanel, BorderLayout.CENTER);

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
        cardLayout.show(contentPanel, "ACCUEIL");
    }

    /**
     * Gérer le lancement du jeu
     */
    private void handleLaunch() {
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    mainPanel.setStatus("Synchronisation des mods locaux...");
                    modManager.syncLocalMods();

                    mainPanel.setStatus("Synchronisation des configs locales...");
                    modManager.syncLocalConfigs();

                    mainPanel.setStatus("Lancement du jeu...");
                    launchGame();

                    return null;
                } catch (Exception e) {
                    logger.error("Erreur lors du téléchargement", e);
                    throw e;
                }
            }

            @Override
            protected void process(java.util.List<Integer> chunks) { }

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

    private void navigate(String route) {
        if ("ACCUEIL".equals(route)) cardLayout.show(contentPanel, "ACCUEIL");
        else if ("PARAMÈTRES".equals(route)) cardLayout.show(contentPanel, "PARAMÈTRES");
        else cardLayout.show(contentPanel, "ACCUEIL");
    }
}
