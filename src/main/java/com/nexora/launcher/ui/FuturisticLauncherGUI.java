package com.nexora.launcher.ui;

import com.nexora.launcher.auth.AzAuthManager;
import com.nexora.launcher.downloader.GitHubModManager;
import com.nexora.launcher.config.LauncherConfig;
import com.nexora.launcher.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

public class FuturisticLauncherGUI extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(FuturisticLauncherGUI.class);

    private AzAuthManager authManager;
    private GitHubModManager modManager;
    private FuturisticLoginPanel loginPanel;
    private FuturisticMainPanel mainPanel;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private User currentUser;

    public FuturisticLauncherGUI() {
        setTitle("NEXORA LAUNCHER");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        // Window Rounding
        setShape(new RoundRectangle2D.Double(0, 0, 1100, 750, 40, 40));

        try {
            java.net.URL iconUrl = getClass().getResource("/icon.png");
            if (iconUrl != null) {
                setIconImage(Toolkit.getDefaultToolkit().getImage(iconUrl));
            }
        } catch (Exception e) {
            /* Ignore */ }

        LauncherConfig config = LauncherConfig.getInstance();
        this.authManager = new AzAuthManager(config.getAzuriomUrl());
        this.modManager = new GitHubModManager(null);

        // Layered Pane for Background + Particles + UI
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(1100, 750));
        setContentPane(layeredPane);

        // 1. Background Layer (Gradient)
        GradientPanel backgroundInfo = new GradientPanel(
                DesignConstants.GRADIENT_MAIN_START,
                DesignConstants.GRADIENT_MAIN_END,
                GradientPanel.DIAGONAL);
        backgroundInfo.setBounds(0, 0, 1100, 750);
        layeredPane.add(backgroundInfo, Integer.valueOf(0)); // Bottom

        // 2. Particle Layer
        ParticlePanel particles = new ParticlePanel();
        particles.setBounds(0, 0, 1100, 750);
        layeredPane.add(particles, Integer.valueOf(1));

        // 3. UI Content Layer
        JPanel uiRoot = new JPanel(new BorderLayout());
        uiRoot.setOpaque(false);
        uiRoot.setBounds(0, 0, 1100, 750);
        layeredPane.add(uiRoot, Integer.valueOf(2));

        // -- Construct UI within root --
        uiRoot.add(new TitleBar(this), BorderLayout.NORTH);

        Sidebar sidebar = new Sidebar(this::navigate);
        uiRoot.add(sidebar, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);
        // Standard Balanced Padding
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));

        loginPanel = new FuturisticLoginPanel(this::handleLogin);
        mainPanel = new FuturisticMainPanel(this::handleLaunch, this::handleLogout);
        JPanel settingsPanel = new SettingsPanel();

        contentPanel.add(loginPanel, "LOGIN");
        contentPanel.add(mainPanel, "ACCUEIL");
        contentPanel.add(settingsPanel, "PARAMÈTRES");

        uiRoot.add(contentPanel, BorderLayout.CENTER);

        // Default View
        cardLayout.show(contentPanel, "LOGIN");
    }

    private void handleLogin(User user) {
        loginPanel.clearPassword();
        this.currentUser = user;
        mainPanel.setUserProfile(user);
        mainPanel.setVersion(
                LauncherConfig.getInstance().getMinecraftVersion(),
                LauncherConfig.getInstance().getLoader(),
                LauncherConfig.getInstance().getLoaderVersion());
        cardLayout.show(contentPanel, "ACCUEIL");
    }

    private void handleLaunch() {
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    mainPanel.setStatus("Syncing mods...");
                    modManager.syncLocalMods();

                    mainPanel.setStatus("Syncing configs...");
                    modManager.syncLocalConfigs();

                    mainPanel.setStatus("Launching Game...");
                    launchGame();

                    return null;
                } catch (Exception e) {
                    logger.error("Launch Error", e);
                    throw e;
                }
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
            }

            @Override
            protected void done() {
                try {
                    get();
                    mainPanel.setButtonsEnabled(true);
                    mainPanel.setStatus("Game Launched!");
                } catch (Exception e) {
                    mainPanel.setButtonsEnabled(true);
                    mainPanel.setStatus("Error: " + e.getMessage());
                }
            }
        };

        mainPanel.setButtonsEnabled(false);
        mainPanel.setProgress(0);
        worker.execute();
    }

    private void launchGame() throws Exception {
        LauncherConfig config = LauncherConfig.getInstance();
        String gameDirectory = System.getProperty("user.home") + "/.nexora/game";

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Xmx" + config.getMaxMemory() + "M",
                "-Xms" + config.getMinMemory() + "M",
                "-Djava.library.path=" + gameDirectory + "/natives",
                "-cp", gameDirectory + "/mods/*:" + gameDirectory + "/libs/*",
                "net.minecraft.client.main.Main");

        pb.directory(new File(gameDirectory));
        pb.start();
        logger.info("Game Process Started");
    }

    private void handleLogout() {
        try {
            authManager.logout();
        } catch (Exception e) {
            logger.error("Logout Error", e);
        }
        this.currentUser = null;
        mainPanel.reset();
        cardLayout.show(contentPanel, "LOGIN");
    }

    private void navigate(String route) {
        // Vérifier si l'utilisateur est connecté avant d'accéder à ACCUEIL
        if (("ACCUEIL".equals(route) || "HOME".equals(route)) && currentUser == null) {
            logger.warn("Tentative d'accès à ACCUEIL sans connexion");
            cardLayout.show(contentPanel, "LOGIN");
            return;
        }

        String dest = "ACCUEIL";
        if ("ACCUEIL".equals(route) || "HOME".equals(route))
            dest = "ACCUEIL";
        else if ("PARAMÈTRES".equals(route) || "SETTINGS".equals(route))
            dest = "PARAMÈTRES";

        cardLayout.show(contentPanel, dest);
    }
}
