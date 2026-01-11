package com.nexaria.launcher.ui;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.downloader.GitHubModManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class SettingsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(SettingsPanel.class);
    private LauncherConfig cfg;

    @SuppressWarnings("unused")
    private java.util.function.Supplier<String> accessTokenSupplier;
    @SuppressWarnings("unused")
    private java.util.function.Supplier<String> azuriomUrlSupplier;
    @SuppressWarnings("unused")
    private Runnable onSkinChanged;

    // Variables pour les contrôles (utilisées dans getUpdatedConfig)
    private JSlider ramSlider;
    private JCheckBox autoUpdate;
    private JCheckBox debugMode;
    private JSpinner rateSpinner;
    private JCheckBox rememberDefault;
    private JTextField gameDirField;

    public SettingsPanel(java.util.function.Supplier<String> accessTokenSupplier,
            java.util.function.Supplier<String> azuriomUrlSupplier,
            Runnable onSkinChanged) {
        this.accessTokenSupplier = accessTokenSupplier;
        this.azuriomUrlSupplier = azuriomUrlSupplier;
        this.onSkinChanged = onSkinChanged;
        cfg = LauncherConfig.getInstance();
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Layout principal
        setLayout(new BorderLayout());
        setBorder(null); // Full width/height

        // 1. Sidebar (Gauche)
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false); // Important pour la transparence
        sidebar.setBackground(new Color(30, 20, 40, 240)); // Almost opaque for stability
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, DesignConstants.GLASS_BORDER), // Right divider
                BorderFactory.createEmptyBorder(20, 10, 20, 10)));
        sidebar.setPreferredSize(new Dimension(240, 0)); // Slightly wider

        // 2. Content (Droite)
        CardLayout cardLayout = new CardLayout();
        JPanel contentResults = new JPanel(cardLayout);
        contentResults.setOpaque(false);
        contentResults.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0)); // Add spacing from sidebar

        // Groupe pour les boutons (radio behavior)
        ButtonGroup group = new ButtonGroup();

        // Création du panel Général
        JPanel general = createTabBase("Paramètres généraux");
        general.add(Box.createVerticalStrut(10));
        JCheckBox minimizeOnLaunch = new JCheckBox("Minimiser le launcher lorsque le jeu démarre");
        minimizeOnLaunch.setOpaque(false);
        minimizeOnLaunch.setForeground(DesignConstants.TEXT_SECONDARY);
        minimizeOnLaunch.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        minimizeOnLaunch.setSelected(cfg.minimizeOnLaunch);
        minimizeOnLaunch.setAlignmentX(Component.LEFT_ALIGNMENT);
        minimizeOnLaunch.setFocusPainted(false);
        minimizeOnLaunch.setBorderPainted(false);
        minimizeOnLaunch.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 16, new Color(255, 255, 255, 40)));
        minimizeOnLaunch.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 16, DesignConstants.PURPLE_ACCENT));
        minimizeOnLaunch.addActionListener(e -> cfg.minimizeOnLaunch = minimizeOnLaunch.isSelected());
        general.add(minimizeOnLaunch);
        general.add(Box.createVerticalGlue());

        // Définition des catégories
        addCategory(sidebar, contentResults, group, cardLayout, "Général", FontAwesomeSolid.COG, wrapInScroll(general),
                true);
        addCategory(sidebar, contentResults, group, cardLayout, "Compte", FontAwesomeSolid.USER,
                wrapInScroll(createAccountTab()), false);
        addCategory(sidebar, contentResults, group, cardLayout, "Mémoire", FontAwesomeSolid.MEMORY,
                wrapInScroll(createMemoryTab()), false);
        addCategory(sidebar, contentResults, group, cardLayout, "Réseau", FontAwesomeSolid.WIFI,
                wrapInScroll(createNetworkTab()), false);
        addCategory(sidebar, contentResults, group, cardLayout, "Dossiers", FontAwesomeSolid.FOLDER_OPEN,
                wrapInScroll(createFoldersTab()), false);
        addCategory(sidebar, contentResults, group, cardLayout, "Diagnostics", FontAwesomeSolid.STETHOSCOPE,
                wrapInScroll(createDiagnosticsTab()), false);

        ConsolePanel consolePanel = new ConsolePanel();
        consolePanel.redirectSystemStreams();
        addCategory(sidebar, contentResults, group, cardLayout, "Console", FontAwesomeSolid.TERMINAL, consolePanel,
                false);

        // Assemblage
        add(sidebar, BorderLayout.WEST);
        add(contentResults, BorderLayout.CENTER);
    }

    private void addCategory(JPanel sidebar, JPanel content, ButtonGroup group, CardLayout cards,
            String title, FontAwesomeSolid icon, JComponent panel, boolean selected) {
        String id = title.toUpperCase();

        // Bouton Custom
        JToggleButton btn = new JToggleButton(title);
        btn.setIcon(FontIcon.of(icon, 18, DesignConstants.TEXT_SECONDARY)); // Slightly larger icon
        btn.setSelectedIcon(FontIcon.of(icon, 18, DesignConstants.PURPLE_ACCENT));
        btn.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
        btn.setForeground(DesignConstants.TEXT_SECONDARY);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(15);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(220, 50)); // Taller for better hit area
        btn.setPreferredSize(new Dimension(220, 50));

        // ChangeListener pour gérer les couleurs (évite l'appel setForeground dans
        // paint)
        btn.addChangeListener(e -> {
            if (btn.isSelected()) {
                btn.setForeground(DesignConstants.PURPLE_ACCENT);
            } else if (btn.getModel().isRollover()) {
                btn.setForeground(DesignConstants.TEXT_PRIMARY);
            } else {
                btn.setForeground(DesignConstants.TEXT_SECONDARY);
            }
        });

        // Custom UI pour l'effet de sélection (Pill shape à la ModernButton)
        btn.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JToggleButton b = (JToggleButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int r = 15; // Corner radius

                if (b.isSelected()) {
                    // Fond actif (Gradient subtil)
                    GradientPaint gp = new GradientPaint(0, 0, new Color(170, 80, 255, 40), b.getWidth(), 0,
                            new Color(170, 80, 255, 10));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), r, r);

                    // Bordure gauche plus "design"
                    g2.setColor(DesignConstants.PURPLE_ACCENT);
                    g2.fillRoundRect(0, 10, 4, b.getHeight() - 20, 4, 4);
                } else if (b.getModel().isRollover()) {
                    // Hover glass
                    g2.setColor(new Color(255, 255, 255, 20));
                    g2.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), r, r);
                }

                super.paint(g2, c);
                g2.dispose();
            }
        });

        btn.addActionListener(e -> cards.show(content, id));

        group.add(btn);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(5));

        content.add(panel, id);

        if (selected) {
            btn.setSelected(true);
        }
    }

    private JScrollPane wrapInScroll(JPanel panel) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        return scroll;
    }

    private JPanel createAccountTab() {
        JPanel p = createTabBase("Compte & Sécurité");
        p.add(Box.createVerticalStrut(20));

        rememberDefault = new JCheckBox("Se souvenir de moi par défaut");
        rememberDefault.setOpaque(false);
        rememberDefault.setForeground(DesignConstants.TEXT_SECONDARY);
        rememberDefault.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        rememberDefault.setSelected(cfg.rememberMeDefault);
        rememberDefault.setAlignmentX(Component.LEFT_ALIGNMENT);
        rememberDefault.setFocusPainted(false);
        rememberDefault.setBorderPainted(false);
        rememberDefault.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 16, new Color(255, 255, 255, 40)));
        rememberDefault.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 16, DesignConstants.PURPLE_ACCENT));
        p.add(rememberDefault);

        p.add(Box.createVerticalStrut(30));
        ModernButton uploadSkinBtn = new ModernButton("CHANGER LE SKIN", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, true);
        uploadSkinBtn.setPreferredSize(new Dimension(220, 45));
        uploadSkinBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        uploadSkinBtn.setIcon(FontIcon.of(FontAwesomeSolid.USER, 18, DesignConstants.TEXT_PRIMARY));
        uploadSkinBtn.addActionListener(e -> handleSkinUpload());
        p.add(uploadSkinBtn);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createMemoryTab() {
        JPanel p = createTabBase("Allocation Mémoire Java");
        p.add(Box.createVerticalStrut(20));

        int ram = cfg.getMaxMemory();
        JLabel ramValue = new JLabel(ram + " MB");
        ramValue.setForeground(DesignConstants.PURPLE_ACCENT);
        ramValue.setFont(DesignConstants.FONT_HEADER.deriveFont(24f));
        ramValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramValue.setIcon(FontIcon.of(FontAwesomeSolid.MEMORY, 20, DesignConstants.PURPLE_ACCENT));

        ramSlider = new JSlider(512, 16384, ram);
        ramSlider.setOpaque(false);
        ramSlider.setForeground(DesignConstants.PURPLE_ACCENT);
        ramSlider.setPaintTicks(true);
        ramSlider.setMajorTickSpacing(2048);
        ramSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramSlider.setMaximumSize(new Dimension(500, 50));
        ramSlider.addChangeListener(e -> ramValue.setText(ramSlider.getValue() + " MB"));

        p.add(ramValue);
        p.add(Box.createVerticalStrut(15));
        p.add(ramSlider);

        p.add(Box.createVerticalStrut(40));
        addSubsection(p, "Mises à jour & Débogague");
        p.add(Box.createVerticalStrut(10));

        autoUpdate = new JCheckBox("Mettre à jour automatiquement le launcher");
        autoUpdate.setOpaque(false);
        autoUpdate.setForeground(DesignConstants.TEXT_SECONDARY);
        autoUpdate.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        autoUpdate.setSelected(cfg.isAutoUpdate());
        autoUpdate.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoUpdate.setFocusPainted(false);
        autoUpdate.setBorderPainted(false);
        autoUpdate.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 16, new Color(255, 255, 255, 40)));
        autoUpdate.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 16, DesignConstants.PURPLE_ACCENT));

        debugMode = new JCheckBox("Mode Débogage (logs détaillés)");
        debugMode.setOpaque(false);
        debugMode.setForeground(DesignConstants.TEXT_SECONDARY);
        debugMode.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        debugMode.setSelected(cfg.debugMode);
        debugMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        debugMode.setFocusPainted(false);
        debugMode.setBorderPainted(false);
        debugMode.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 16, new Color(255, 255, 255, 40)));
        debugMode.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 16, DesignConstants.PURPLE_ACCENT));

        p.add(autoUpdate);
        p.add(Box.createVerticalStrut(10));
        p.add(debugMode);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createNetworkTab() {
        JPanel p = createTabBase("Réseau & Téléchargements");
        p.add(Box.createVerticalStrut(20));

        addSubsection(p, "Limite de débit");
        p.add(Box.createVerticalStrut(10));
        JPanel ratePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ratePanel.setOpaque(false);
        JLabel rateLabel = new JLabel("KB/s:");
        rateLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        rateLabel.setFont(DesignConstants.FONT_REGULAR.deriveFont(12f));
        rateLabel.setIcon(FontIcon.of(FontAwesomeSolid.DOWNLOAD, 14, DesignConstants.TEXT_SECONDARY));
        rateSpinner = new JSpinner(new SpinnerNumberModel(cfg.downloadRateLimitKBps, 0, 102400, 64));
        ((JSpinner.DefaultEditor) rateSpinner.getEditor()).getTextField().setPreferredSize(new Dimension(80, 28));
        ratePanel.add(rateLabel);
        ratePanel.add(rateSpinner);
        ratePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(ratePanel);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createFoldersTab() {
        JPanel p = createTabBase("Dossiers & Cache");
        p.add(Box.createVerticalStrut(20));

        addSubsection(p, "Répertoire de jeu");
        p.add(Box.createVerticalStrut(10));
        gameDirField = new JTextField(LauncherConfig.getGameDir());
        gameDirField.setPreferredSize(new Dimension(360, 32));
        gameDirField.setMaximumSize(new Dimension(500, 32));
        ModernButton chooseDirBtn = new ModernButton("PARCOURIR", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        chooseDirBtn.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 16, DesignConstants.TEXT_PRIMARY));
        chooseDirBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(LauncherConfig.getGameDir());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(SettingsPanel.this) == JFileChooser.APPROVE_OPTION) {
                File dir = fc.getSelectedFile();
                if (dir != null)
                    gameDirField.setText(dir.getAbsolutePath());
            }
        });
        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pathPanel.setOpaque(false);
        pathPanel.add(gameDirField);
        pathPanel.add(chooseDirBtn);
        pathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(pathPanel);

        p.add(Box.createVerticalStrut(30));
        addSubsection(p, "Gestion du cache");
        p.add(Box.createVerticalStrut(10));
        JPanel cachePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cachePanel.setOpaque(false);
        ModernButton clearCacheBtn = new ModernButton("VIDER CACHE", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        clearCacheBtn.setIcon(FontIcon.of(FontAwesomeSolid.TRASH, 16, DesignConstants.TEXT_PRIMARY));
        clearCacheBtn.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(SettingsPanel.this, "Vider le cache?", "Confirmation",
                    JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                try {
                    deleteRecursive(new File(LauncherConfig.getCacheDir()));
                    JOptionPane.showMessageDialog(SettingsPanel.this, "Cache vidé.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SettingsPanel.this, "Échec du vidage du cache.", "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        ModernButton resetDataBtn = new ModernButton("RÉINIT. DONNÉES", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        resetDataBtn.setIcon(FontIcon.of(FontAwesomeSolid.BROOM, 16, DesignConstants.TEXT_PRIMARY));
        resetDataBtn.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(SettingsPanel.this, "Réinitialiser toutes les données?",
                    "Zone dangereuse", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res == JOptionPane.YES_OPTION) {
                try {
                    deleteRecursive(new File(LauncherConfig.getDataFolder()));
                    JOptionPane.showMessageDialog(SettingsPanel.this, "Données réinitialisées.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SettingsPanel.this, "Échec de la réinitialisation.", "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        ModernButton repairBtn = new ModernButton("RÉPARER LE JEU", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK,
                false);
        repairBtn.setIcon(FontIcon.of(FontAwesomeSolid.TOOLS, 16, DesignConstants.TEXT_PRIMARY));
        repairBtn.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(SettingsPanel.this,
                    "Ceci va forcer la re-synchronisation de tous les fichiers du jeu.\nContinuer ?",
                    "Réparation", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        new GitHubModManager(null).syncAllData();
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            JOptionPane.showMessageDialog(SettingsPanel.this, "Jeu réparé avec succès !");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(SettingsPanel.this,
                                    "Erreur durant la réparation: " + ex.getMessage(), "Erreur",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            }
        });

        cachePanel.add(clearCacheBtn);
        cachePanel.add(resetDataBtn);
        cachePanel.add(repairBtn);
        cachePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(cachePanel);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createDiagnosticsTab() {
        JPanel p = createTabBase("Diagnostics & Logs");
        p.add(Box.createVerticalStrut(20));

        addSubsection(p, "Journalisation");
        p.add(Box.createVerticalStrut(10));
        JPanel diagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        diagPanel.setOpaque(false);
        ModernButton exportLogsBtn = new ModernButton("EXPORTER LOGS", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        exportLogsBtn.setIcon(FontIcon.of(FontAwesomeSolid.FILE_ARCHIVE, 16, DesignConstants.TEXT_PRIMARY));
        exportLogsBtn.addActionListener(e -> {
            try {
                File zip = new File(LauncherConfig.getCacheDir(), "logs-" + System.currentTimeMillis() + ".zip");
                zipLogs(zip);
                JOptionPane.showMessageDialog(SettingsPanel.this, "Logs exportés: " + zip.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(SettingsPanel.this, "Échec export logs.", "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        ModernButton testConnBtn = new ModernButton("TESTER CONNECTIVITÉ", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        testConnBtn.setIcon(FontIcon.of(FontAwesomeSolid.WIFI, 16, DesignConstants.TEXT_PRIMARY));
        testConnBtn.addActionListener(e -> testConnectivity());
        diagPanel.add(exportLogsBtn);
        diagPanel.add(testConnBtn);
        diagPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(diagPanel);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createTabBase(String title) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(DesignConstants.FONT_TITLE.deriveFont(18f));
        titleLabel.setForeground(DesignConstants.PURPLE_ACCENT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(titleLabel);
        return p;
    }

    private void addSubsection(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        label.setForeground(DesignConstants.PURPLE_ACCENT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }

    private void handleSkinUpload() {
        String token = accessTokenSupplier != null ? accessTokenSupplier.get() : null;
        String url = azuriomUrlSupplier != null ? azuriomUrlSupplier.get() : null;
        if (token == null || token.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez vous connecter.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Sélectionner un skin PNG");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file == null || !file.exists())
                return;
            if (!file.getName().toLowerCase().endsWith(".png")) {
                JOptionPane.showMessageDialog(this, "Doit être un PNG.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                long startTime = System.currentTimeMillis();

                @Override
                protected Void doInBackground() throws Exception {
                    logger.info("[SKIN] Upload de: {}", file.getName());
                    com.nexaria.launcher.auth.AzAuthManager authMgr = new com.nexaria.launcher.auth.AzAuthManager(url);
                    authMgr.uploadSkin(token, file);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("[SKIN] OK en {}ms", duration);
                        JOptionPane.showMessageDialog(SettingsPanel.this, "Skin mis à jour!", "Succès",
                                JOptionPane.INFORMATION_MESSAGE);
                        if (onSkinChanged != null)
                            onSkinChanged.run();
                    } catch (Exception ex) {
                        logger.error("[SKIN] ERREUR: {}", ex.getMessage());
                        JOptionPane.showMessageDialog(SettingsPanel.this, "Échec mise à jour.", "Erreur",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void deleteRecursive(File f) {
        if (f == null || !f.exists())
            return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null)
                for (File k : kids)
                    deleteRecursive(k);
        }
        try {
            f.delete();
        } catch (Exception ignore) {
        }
    }

    private void zipLogs(File zipFile) throws Exception {
        String base = System.getProperty("user.home") + "/.nexaria";
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        try {
            File dir = new File(base);
            File[] files = dir.listFiles((d, name) -> name.startsWith("launcher") && name.endsWith(".log"));
            if (files != null) {
                for (File f : files) {
                    ZipEntry e = new ZipEntry(f.getName());
                    zos.putNextEntry(e);
                    try (FileInputStream in = new FileInputStream(f)) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = in.read(buf)) != -1)
                            zos.write(buf, 0, r);
                    }
                    zos.closeEntry();
                }
            }
        } finally {
            zos.close();
        }
    }

    private void testConnectivity() {
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            long startTime = System.currentTimeMillis();

            @Override
            protected Void doInBackground() throws Exception {
                logger.info("[CONNECTIVITY] Test debut");
                String host = LauncherConfig.getInstance().getServerHost();
                int port = LauncherConfig.getInstance().getServerPort();
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, port), 3000);
                    logger.info("[CONNECTIVITY] Serveur Minecraft: OK");
                    publish("Serveur Minecraft: OK");
                } catch (Exception e) {
                    logger.warn("[CONNECTIVITY] Serveur Minecraft: KO - {}", e.getMessage());
                    publish("Serveur Minecraft: KO");
                }

                String az = LauncherConfig.getInstance().getAzuriomUrl();
                try {
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) java.net.URI.create(az).toURL()
                            .openConnection();
                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(3000);
                    conn.connect();
                    logger.info("[CONNECTIVITY] AzAuth: OK");
                    publish("AzAuth: OK");
                } catch (Exception e) {
                    logger.warn("[CONNECTIVITY] AzAuth: KO - {}", e.getMessage());
                    publish("AzAuth: KO");
                }
                long duration = System.currentTimeMillis() - startTime;
                logger.info("[CONNECTIVITY] Test termine en {}ms", duration);
                return null;
            }

            @Override
            protected void process(java.util.List<String> msgs) {
                if (!msgs.isEmpty())
                    JOptionPane.showMessageDialog(SettingsPanel.this, String.join("\n", msgs));
            }
        };
        worker.execute();
    }

    public LauncherConfig getUpdatedConfig() {
        if (ramSlider != null)
            cfg.setMaxMemory(ramSlider.getValue());
        if (autoUpdate != null)
            cfg.setAutoUpdate(autoUpdate.isSelected());
        if (debugMode != null)
            cfg.setDebugMode(debugMode.isSelected());
        if (rateSpinner != null)
            cfg.setDownloadRateLimitKBps((Integer) rateSpinner.getValue());
        if (rememberDefault != null)
            cfg.setRememberMeDefault(rememberDefault.isSelected());
        if (gameDirField != null) {
            String newDir = gameDirField.getText().trim();
            if (!newDir.isEmpty())
                cfg.setGameDir(newDir);
        }
        return cfg;
    }
}
