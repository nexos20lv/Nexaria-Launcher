package com.nexaria.launcher.ui;

import com.nexaria.launcher.auth.AzAuthManager;
import com.nexaria.launcher.downloader.GitHubModManager;
import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.model.User;
import com.nexaria.launcher.security.DataVerificationService;
import com.nexaria.launcher.rpc.DiscordPresenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.Taskbar;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Path;

public class LauncherWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(LauncherWindow.class);

    private AzAuthManager authManager;
    private GitHubModManager modManager;
    private LoginPanel loginPanel;
    private MainPanel mainPanel;
    private Sidebar sidebar;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private User currentUser;
    private Process currentGameProcess;

    public LauncherWindow() {
        setTitle("Nexaria Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        // Window Rounding
        setShape(new RoundRectangle2D.Double(0, 0, 1100, 750, 40, 40));

        // Icône application (fenêtre + dock)
        Image appIcon = loadAppIcon();
        if (appIcon != null) {
            try {
                setIconImage(appIcon);
            } catch (Exception ignore) {
            }
            try {
                Taskbar.getTaskbar().setIconImage(appIcon);
            } catch (UnsupportedOperationException | SecurityException ignore) {
            }
        }

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
        layeredPane.add(backgroundInfo, Integer.valueOf(0));

        // 2. Particle Layer
        ParticlePanel particles = new ParticlePanel();
        particles.setBounds(0, 0, 1100, 750);
        layeredPane.add(particles, Integer.valueOf(1));

        // 3. UI Content Layer
        JPanel uiRoot = new JPanel(new BorderLayout());
        uiRoot.setOpaque(false);
        uiRoot.setBounds(0, 0, 1100, 750);
        layeredPane.add(uiRoot, Integer.valueOf(2));

        uiRoot.add(new TitleBar(this), BorderLayout.NORTH);

        sidebar = new Sidebar(this::navigate, this::handleLogout);
        uiRoot.add(sidebar, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));

        loginPanel = new LoginPanel(this::handleLogin);
        mainPanel = new MainPanel(this::handleLaunch);
        LauncherConfig cfgInst = LauncherConfig.getInstance();
        JPanel settingsPanel = new SettingsPanel(
                () -> currentUser != null ? currentUser.getAccessToken() : null,
                () -> cfgInst.getAzuriomUrl(),
                () -> {
                    if (currentUser != null)
                        sidebar.setUserProfile(currentUser, cfgInst.getAzuriomUrl());
                });

        contentPanel.add(loginPanel, "CONNEXION");
        contentPanel.add(mainPanel, "ACCUEIL");
        contentPanel.add(settingsPanel, "PARAMÈTRES");
        contentPanel.add(new SecurityPanel(), "SÉCURITÉ");
        uiRoot.add(contentPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, "CONNEXION");
        sidebar.setVisible(false);

        attemptAutoLogin();

        // Démarrer RPC
        try {
            DiscordPresenter.start();
        } catch (Throwable e) {
            logger.warn("Discord RPC non disponible (bibliothèque native manquante ou erreur): {}", e.getMessage());
        }

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                DiscordPresenter.stop();
            }
        });
    }

    private Image loadAppIcon() {
        try {
            java.net.URL iconUrl = getClass().getResource("/logo.png");
            if (iconUrl != null) {
                return Toolkit.getDefaultToolkit().getImage(iconUrl);
            }
        } catch (Exception ignore) {
        }
        // Fallback: générer une icône violette simple "NX"
        try {
            int size = 128;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, DesignConstants.GRADIENT_MAIN_START, size, size,
                    DesignConstants.GRADIENT_MAIN_END);
            g2d.setPaint(gp);
            g2d.fillRoundRect(0, 0, size, size, 32, 32);
            g2d.setColor(new Color(255, 255, 255, 230));
            g2d.setFont(DesignConstants.FONT_TITLE.deriveFont(48f));
            String text = "NX";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (size - fm.stringWidth(text)) / 2;
            int y = (size + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(text, x, y);
            g2d.dispose();
            return img;
        } catch (Exception ignore) {
        }
        return null;
    }

    private void handleLogin(User user) {
        loginPanel.clearPassword();
        this.currentUser = user;

        DiscordPresenter.update("Dans le complexe", "Ingénieur : " + user.getUsername());

        mainPanel.setUserProfile(user);
        mainPanel.setVersion(
                LauncherConfig.getInstance().getMinecraftVersion(),
                LauncherConfig.getInstance().getLoader(),
                LauncherConfig.getInstance().getLoaderVersion());
        sidebar.setUserProfile(user, LauncherConfig.getInstance().getAzuriomUrl());
        mainPanel.startServerStatusAutoRefresh(LauncherConfig.getInstance().getServerHost(),
                LauncherConfig.getInstance().getServerPort(), LauncherConfig.getInstance().getServerName());

        // Charger les actualités
        mainPanel.loadNews(LauncherConfig.getInstance().getAzuriomUrl());

        sidebar.setVisible(true);
        cardLayout.show(contentPanel, "ACCUEIL");
    }

    private void handleLaunch() {
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            long startTime = System.currentTimeMillis();

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    logger.info("[LAUNCH] Debut du lancement");
                    mainPanel.setStatus("Synchronisation de data/...");
                    modManager.syncAllData();
                    logger.info("[LAUNCH] Data synchronise (mods + configs)");

                    // Vérifier immédiatement après la synchro pour éviter toute fenêtre de tir
                    verifyDataIntegrity();

                    mainPanel.setStatus("Préparation du lancement...");
                    SwingUtilities.invokeLater(() -> mainPanel.setIndeterminate(true));
                    mainPanel.setStatus("Lancement du jeu...");
                    launchGame();
                    return null;
                } catch (Exception e) {
                    logger.error("[LAUNCH] ERREUR: {}", e.getMessage(), e);
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
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("[LAUNCH] Succes en {}ms", duration);
                    mainPanel.setButtonsEnabled(true);
                    mainPanel.setIndeterminate(false);
                    mainPanel.setStatus("Jeu lancé !");
                    // Basculer le bouton en mode fermeture
                    mainPanel.setPlaying(true, () -> stopGame());
                    // Minimiser selon configuration
                    if (LauncherConfig.getInstance().minimizeOnLaunch) {
                        Timer minimizeTimer = new Timer(2000, e -> {
                            setState(JFrame.ICONIFIED);
                            logger.info("[LAUNCH] Launcher minimise - jeu en cours");
                        });
                        minimizeTimer.setRepeats(false);
                        minimizeTimer.start();
                    }
                } catch (Exception e) {
                    logger.error("[LAUNCH] ERREUR finale: {}", e.getMessage());
                    mainPanel.setButtonsEnabled(true);
                    mainPanel.setIndeterminate(false);
                    mainPanel.setStatus("Erreur : " + e.getMessage());
                }
            }
        };

        mainPanel.setButtonsEnabled(false);
        mainPanel.setProgress(0);
        worker.execute();
    }

    private void launchGame() throws Exception {
        // Configuration récupérée (variable locale supprimée pour éviter
        // l'avertissement unused)
        String gameDirectory = LauncherConfig.getGameDir();
        new File(gameDirectory).mkdirs();
        new File(LauncherConfig.getModsDir()).mkdirs();
        new File(LauncherConfig.getConfigDir()).mkdirs();

        // Bloquer les symlinks suspects pointant hors du gameDir
        if (LauncherConfig.getInstance().blockSymlinks) {
            Path gameDir = java.nio.file.Paths.get(LauncherConfig.getGameDir()).toAbsolutePath().normalize();
            enforceNoExternalSymlink(java.nio.file.Paths.get(LauncherConfig.getModsDir()), gameDir, "mods");
            enforceNoExternalSymlink(java.nio.file.Paths.get(LauncherConfig.getConfigDir()), gameDir, "config");
        }

        // Vérification d'intégrité déjà effectuée après la synchro
        // verifyDataIntegrity();

        // Utilisation d'OpenLauncherLib pour gérer tous les aspects du lancement
        com.nexaria.launcher.minecraft.OpenLauncherLibLauncher.ProgressListener progressListener = new com.nexaria.launcher.minecraft.OpenLauncherLibLauncher.ProgressListener() {
            @Override
            public void onStatus(String status) {
                SwingUtilities.invokeLater(() -> mainPanel.setStatus(status));
            }

            @Override
            public void onProgress(int percent) {
                SwingUtilities.invokeLater(() -> {
                    mainPanel.setProgress(percent);
                    mainPanel.setIndeterminate(false);
                });
            }
        };

        if (com.nexaria.launcher.java.JavaManager.getJavaHome() == null
                && !com.nexaria.launcher.java.JavaManager.isSystemJavaValid()) {
            logger.info("Java 17 manquant, demarrage du telechargement...");
            JDialog dialog = new JDialog(this, "Téléchargement de Java", true);
            dialog.setSize(400, 100);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());

            JProgressBar pb = new JProgressBar(0, 100);
            pb.setStringPainted(true);
            pb.setString("Préparation...");
            dialog.add(pb, BorderLayout.CENTER);

            SwingWorker<Void, Void> downloadWorker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    com.nexaria.launcher.java.JavaManager.downloadJava((status, percent) -> {
                        SwingUtilities.invokeLater(() -> {
                            pb.setString(status);
                            if (percent >= 0)
                                pb.setValue(percent);
                            else
                                pb.setIndeterminate(true);
                        });
                    });
                    return null;
                }

                @Override
                protected void done() {
                    dialog.dispose();
                }
            };
            downloadWorker.execute();
            dialog.setVisible(true);
        }

        // Vérifier et configurer Java
        File javaExec = com.nexaria.launcher.java.JavaManager.getJavaExecutable();
        if (javaExec != null) {
            logger.info("Utilisation de Java personnalisé: {}", javaExec.getAbsolutePath());
            // OpenLauncherLib utilise properties ou args, on doit voir comment lui passer
            // le java.
            // En fait, launchGame utilise params, mais n'accepte pas directement le path
            // java en argument simple ici
            // car launchGame est une abstraction de OpenLauncherLibLauncher.
            // On doit modifier OpenLauncherLibLauncher.launchGame pour accepter javaPath ou
            // le configurer.
            // Pour l'instant on suppose que OpenLauncherLibLauncher lit une config ou on le
            // modifie.
            com.nexaria.launcher.minecraft.OpenLauncherLibLauncher.setJavaPath(javaExec.getAbsolutePath());
        }

        currentGameProcess = com.nexaria.launcher.minecraft.OpenLauncherLibLauncher.launchGame(currentUser,
                progressListener);

        DiscordPresenter.update("Opération en cours",
                "Sur le secteur : " + LauncherConfig.getInstance().getServerName());

        // Surveiller le processus du jeu pour restaurer le launcher quand il se ferme
        if (currentGameProcess != null) {
            // Rediriger la sortie du jeu vers la console
            inheritIO(currentGameProcess.getInputStream(), "[GAME]");
            inheritIO(currentGameProcess.getErrorStream(), "[GAME ERROR]");

            Thread processMonitor = new Thread(() -> {
                try {
                    int exitCode = currentGameProcess.waitFor();
                    logger.info("Le jeu s'est fermé avec le code: {}", exitCode);

                    // Restaurer le launcher sur le thread Swing
                    SwingUtilities.invokeLater(() -> {
                        setState(JFrame.NORMAL);
                        toFront();
                        requestFocus();
                        mainPanel.setPlaying(false, null);
                        mainPanel.setStatus("Jeu fermé");
                        mainPanel.setButtonsEnabled(true);
                        currentGameProcess = null;
                        logger.info("Launcher restauré après fermeture du jeu");
                    });
                } catch (InterruptedException e) {
                    logger.debug("Surveillance du processus interrompue");
                }
            }, "GameProcessMonitor");
            processMonitor.setDaemon(true);
            processMonitor.start();
        }
    }

    private void enforceNoExternalSymlink(java.nio.file.Path path, java.nio.file.Path gameDir, String label) {
        try {
            java.nio.file.Path real = path.toRealPath();
            if (!real.startsWith(gameDir)) {
                String msg = "Le dossier " + label
                        + " semble être un lien symbolique externe au gameDir. Lancement bloqué.";
                if (LauncherConfig.getInstance().enforceModPolicy) {
                    throw new SecurityException(msg);
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, msg, "Sécurité symlink",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (java.io.IOException e) {
            logger.warn("Impossible de résoudre le chemin réel pour {}: {}", label, e.getMessage());
        }
    }

    private void stopGame() {
        try {
            if (currentGameProcess != null) {
                currentGameProcess.destroy();
                logger.info("Processus du jeu arrêté");
            }
        } catch (Exception e) {
            logger.warn("Échec arrêt du jeu", e);
        } finally {
            currentGameProcess = null;
            mainPanel.setPlaying(false, null);
            mainPanel.setStatus("Jeu arrêté");
            setState(JFrame.NORMAL);
            toFront();
        }
        DiscordPresenter.update("Dans le complexe", "Pause café...");
    }

    /**
     * Vérifie l'intégrité des data (mods + config) avec la politique configurée.
     * Appelée juste après la synchro et juste avant le lancement pour réduire au
     * minimum la fenêtre
     * pendant laquelle des modifications pourraient se produire.
     */
    private void verifyDataIntegrity() throws Exception {
        Path gameDir = java.nio.file.Paths.get(LauncherConfig.getGameDir());
        String policy = LauncherConfig.getInstance().enforceModPolicy ? "strict" : "warn";

        DataVerificationService dataVerifier = new DataVerificationService(gameDir, policy);
        DataVerificationService.Result dataResult = dataVerifier.verify();

        if (!dataResult.ok) {
            StringBuilder sb = new StringBuilder();
            if (!dataResult.missing.isEmpty())
                sb.append("Manquants: ").append(dataResult.missing).append("\n");
            if (!dataResult.modified.isEmpty())
                sb.append("Modifiés: ").append(dataResult.modified).append("\n");
            if (!dataResult.unexpected.isEmpty())
                sb.append("Non attendus: ").append(dataResult.unexpected).append("\n");

            String msg = "Vérification de l'intégrité data/: des écarts ont été détectés.\n" + sb;

            if (LauncherConfig.getInstance().enforceModPolicy) {
                dataVerifier.applyPolicy(dataResult);
                throw new SecurityException(msg);
            } else {
                javax.swing.SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(this, msg,
                        "Avertissement Intégrité Data", javax.swing.JOptionPane.WARNING_MESSAGE));
            }
        }
    }

    @SuppressWarnings("unused")
    private boolean ensureVanillaInitialized(String mcDir) {
        try {
            java.io.File profiles = new java.io.File(mcDir + java.io.File.separator + "launcher_profiles.json");
            if (profiles.exists())
                return true;
            SwingUtilities.invokeLater(() -> {
                String msg = "Forge nécessite que le dossier Minecraft soit initialisé par le launcher officiel.\n" +
                        "Ouvrez le Launcher Minecraft (Mojang/Microsoft) et lancez une fois le jeu vanilla.";
                int res = JOptionPane.showOptionDialog(this, msg, "Initialisation Minecraft requise",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                        null, new Object[] { "Télécharger le Launcher", "OK" }, "Télécharger le Launcher");
                if (res == 0) {
                    try {
                        String url = "https://www.minecraft.net/download";
                        if (Desktop.isDesktopSupported())
                            Desktop.getDesktop().browse(new java.net.URI(url));
                    } catch (Exception ignore) {
                    }
                }
            });
        } catch (Exception ignore) {
        }
        return false;
    }

    private void handleLogout() {
        try {
            authManager.logout();
        } catch (Exception e) {
            logger.error("Logout Error", e);
        }
        this.currentUser = null;
        mainPanel.reset();
        sidebar.clearUserProfile();
        loginPanel.resetState();
        cardLayout.show(contentPanel, "CONNEXION");
        cardLayout.show(contentPanel, "CONNEXION");
        sidebar.setVisible(false);
        DiscordPresenter.update("Hors ligne", "Fin de service");
    }

    @SuppressWarnings("unused")
    private boolean ensureJava17OrPrompt() {
        String java17 = com.nexaria.launcher.minecraft.JavaRuntimeLocator.getJavaExecutable(17);
        int current = parseMajor(System.getProperty("java.version"));
        boolean available = java17 != null && (current >= 17 || !"java".equals(java17));
        if (available)
            return true;
        SwingUtilities.invokeLater(() -> {
            String msg = "Java 17 est requis pour installer les loaders (Forge/NeoForge/Fabric).\n" +
                    "Veuillez installer un JDK 17 (Temurin recommandé).";
            int res = JOptionPane.showOptionDialog(this, msg, "Java 17 requis",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, new Object[] { "Installer Java 17", "OK" }, "Installer Java 17");
            if (res == 0) {
                try {
                    String url = "https://adoptium.net/fr/temurin/releases/?version=17";
                    if (Desktop.isDesktopSupported())
                        Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ignore) {
                }
            }
        });
        return false;
    }

    private int parseMajor(String v) {
        try {
            if (v == null)
                return 0;
            String[] parts = v.split("\\.");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private void navigate(String route) {
        if (("ACCUEIL".equals(route) || "HOME".equals(route)) && currentUser == null) {
            logger.warn("Tentative d'accès à ACCUEIL sans connexion");
            cardLayout.show(contentPanel, "CONNEXION");
            return;
        }
        String dest = "ACCUEIL";
        if ("ACCUEIL".equals(route) || "HOME".equals(route))
            dest = "ACCUEIL";
        else if ("SÉCURITÉ".equals(route) || "SECURITE".equals(route) || "SECURITY".equals(route))
            dest = "SÉCURITÉ";
        else if ("PARAMÈTRES".equals(route) || "SETTINGS".equals(route))
            dest = "PARAMÈTRES";
        else if ("CONNEXION".equals(route) || "LOGIN".equals(route))
            dest = "CONNEXION";
        cardLayout.show(contentPanel, dest);
        sidebar.setVisible(!"CONNEXION".equals(dest));
    }

    private void attemptAutoLogin() {
        com.nexaria.launcher.config.RememberStore.RememberSession s = com.nexaria.launcher.config.RememberStore
                .loadSession();
        if (s == null || s.accessToken == null || s.accessToken.isEmpty())
            return;
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            long startTime = System.currentTimeMillis();

            @Override
            protected Boolean doInBackground() throws Exception {
                logger.info("[AUTOLOGIN] Verification du token");
                return authManager.verifyAccessTokenRemote(s.accessToken);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("[AUTOLOGIN] OK en {}ms", duration);
                        User u = new User(s.id != null ? s.id : java.util.UUID.randomUUID().toString(),
                                s.username != null ? s.username : "Utilisateur", s.accessToken);
                        handleLogin(u);
                    } else {
                        logger.warn("[AUTOLOGIN] Token invalide");
                    }
                } catch (Exception e) {
                    logger.warn("[AUTOLOGIN] ERREUR: {}", e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void inheritIO(final java.io.InputStream src, final String prefix) {
        new Thread(() -> {
            try (java.util.Scanner sc = new java.util.Scanner(src)) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    System.out.println(prefix + " " + line);
                }
            } catch (Exception ignored) {
            }
        }, "Game-Output-Pipe").start();
    }
}