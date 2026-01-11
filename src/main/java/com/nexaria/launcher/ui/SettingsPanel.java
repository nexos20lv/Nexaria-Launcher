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

    // Variables pour les contrôles (utilisées dans getUpdatedConfig)
    private final java.util.function.Supplier<String> accessTokenSupplier;
    private final java.util.function.Supplier<String> uuidSupplier;
    private final java.util.function.Supplier<String> usernameSupplier;
    private final java.util.function.Supplier<String> azuriomUrlSupplier;
    private final Runnable skinChangedCallback;
    private final Runnable logoutCallback;
    private final java.util.function.Consumer<com.nexaria.launcher.config.RememberStore.RememberSession> switchAccountCallback;
    private final Runnable addAccountCallback;
    private JCheckBox rememberDefault;
    private JSlider ramSlider;
    private JCheckBox autoUpdate;
    private JCheckBox debugMode;
    private JSpinner rateSpinner;
    private JTextField gameDirField;

    public SettingsPanel(java.util.function.Supplier<String> accessTokenSupplier,
            java.util.function.Supplier<String> uuidSupplier,
            java.util.function.Supplier<String> usernameSupplier,
            java.util.function.Supplier<String> azuriomUrlSupplier,
            Runnable skinChangedCallback,
            Runnable logoutCallback,
            java.util.function.Consumer<com.nexaria.launcher.config.RememberStore.RememberSession> switchAccountCallback,
            Runnable addAccountCallback) {
        this.accessTokenSupplier = accessTokenSupplier;
        this.uuidSupplier = uuidSupplier;
        this.usernameSupplier = usernameSupplier;
        this.azuriomUrlSupplier = azuriomUrlSupplier;
        this.skinChangedCallback = skinChangedCallback;
        this.logoutCallback = logoutCallback;
        this.switchAccountCallback = switchAccountCallback;
        this.addAccountCallback = addAccountCallback;
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
        general.add(Box.createVerticalStrut(20));

        // Carte Comportement
        JPanel behaviorCard = createCard();
        behaviorCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        behaviorCard.setMaximumSize(new Dimension(600, 100));

        JLabel behaviorTitle = new JLabel("Comportement de l'application");
        behaviorTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        behaviorTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        behaviorTitle.setIcon(FontIcon.of(FontAwesomeSolid.COG, 16, DesignConstants.PURPLE_ACCENT));
        behaviorCard.add(behaviorTitle);
        behaviorCard.add(Box.createVerticalStrut(15));

        JCheckBox minimizeOnLaunch = new JCheckBox("Minimiser le launcher lorsque le jeu démarre");
        minimizeOnLaunch.setOpaque(false);
        minimizeOnLaunch.setForeground(DesignConstants.TEXT_PRIMARY);
        minimizeOnLaunch.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
        minimizeOnLaunch.setSelected(cfg.minimizeOnLaunch);
        minimizeOnLaunch.setFocusPainted(false);
        minimizeOnLaunch.setBorderPainted(false);
        minimizeOnLaunch.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 18, new Color(255, 255, 255, 40)));
        minimizeOnLaunch.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 18, DesignConstants.PURPLE_ACCENT));
        minimizeOnLaunch.addActionListener(e -> cfg.minimizeOnLaunch = minimizeOnLaunch.isSelected());
        behaviorCard.add(minimizeOnLaunch);

        general.add(behaviorCard);
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

    // private JLabel skinPreviewLabel; // Removed

    private JPanel accountListContainer; // Container for the account list

    private JPanel createAccountTab() {
        JPanel p = createTabBase("Compte & Sécurité");
        p.add(Box.createVerticalStrut(20));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Colonne gauche: Contrôles
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        // Carte Préférences
        JPanel prefsCard = createCard();
        prefsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        prefsCard.setMaximumSize(new Dimension(500, 80));

        rememberDefault = new JCheckBox("Se souvenir de moi par défaut");
        rememberDefault.setOpaque(false);
        rememberDefault.setForeground(DesignConstants.TEXT_PRIMARY);
        rememberDefault.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
        rememberDefault.setSelected(cfg.rememberMeDefault);
        rememberDefault.setFocusPainted(false);
        rememberDefault.setBorderPainted(false);
        rememberDefault.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 18, new Color(255, 255, 255, 40)));
        rememberDefault.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 18, DesignConstants.PURPLE_ACCENT));
        prefsCard.add(rememberDefault);
        left.add(prefsCard);

        left.add(Box.createVerticalStrut(20));

        // Carte Actions
        JPanel actionsCard = createCard();
        actionsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionsCard.setMaximumSize(new Dimension(500, 140));

        JLabel actionsTitle = new JLabel("Actions du compte");
        actionsTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        actionsTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        actionsTitle.setIcon(FontIcon.of(FontAwesomeSolid.COG, 16, DesignConstants.PURPLE_ACCENT));
        actionsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionsCard.add(actionsTitle);
        actionsCard.add(Box.createVerticalStrut(15));

        ModernButton uploadSkinBtn = new ModernButton("CHANGER LE SKIN", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, true);
        uploadSkinBtn.setPreferredSize(new Dimension(200, 40));
        uploadSkinBtn.setMaximumSize(new Dimension(200, 40));
        uploadSkinBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        uploadSkinBtn.setIcon(FontIcon.of(FontAwesomeSolid.USER_EDIT, 16, DesignConstants.TEXT_PRIMARY));
        uploadSkinBtn.addActionListener(e -> handleSkinUpload());
        actionsCard.add(uploadSkinBtn);
        actionsCard.add(Box.createVerticalStrut(10));

        ModernButton logoutBtn = new ModernButton("SE DÉCONNECTER", new Color(180, 50, 50),
                new Color(140, 30, 30), true);
        logoutBtn.setPreferredSize(new Dimension(200, 40));
        logoutBtn.setMaximumSize(new Dimension(200, 40));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.setIcon(FontIcon.of(FontAwesomeSolid.SIGN_OUT_ALT, 16, DesignConstants.TEXT_PRIMARY));
        logoutBtn.addActionListener(e -> {
            if (logoutCallback != null)
                logoutCallback.run();
        });
        actionsCard.add(logoutBtn);
        left.add(actionsCard);

        left.add(Box.createVerticalGlue());

        /*
         * SECTION GESTION COMPTES
         */
        accountListContainer = new JPanel();
        accountListContainer.setLayout(new BoxLayout(accountListContainer, BoxLayout.Y_AXIS));
        accountListContainer.setOpaque(false);
        accountListContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DesignConstants.GLASS_BORDER),
                "Comptes enregistrés",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                DesignConstants.FONT_REGULAR,
                DesignConstants.TEXT_SECONDARY));

        rebuildAccountList();

        left.add(Box.createVerticalStrut(20));
        left.add(accountListContainer);
        left.add(Box.createVerticalGlue());

        content.add(left);
        content.add(Box.createHorizontalGlue());

        p.add(content);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void rebuildAccountList() {
        if (accountListContainer == null)
            return;
        accountListContainer.removeAll();

        java.util.List<com.nexaria.launcher.config.RememberStore.RememberSession> sessions = com.nexaria.launcher.config.RememberStore
                .loadSessions();
        if (sessions != null && !sessions.isEmpty()) {
            String current = usernameSupplier.get();

            for (com.nexaria.launcher.config.RememberStore.RememberSession s : sessions) {
                boolean isCurrent = current != null && current.equals(s.username);

                AccountCard card = new AccountCard(
                        s,
                        azuriomUrlSupplier.get(),
                        isCurrent,
                        isCurrent ? null : () -> {
                            if (switchAccountCallback != null)
                                switchAccountCallback.accept(s);
                        },
                        () -> {
                            com.nexaria.launcher.config.RememberStore.removeSession(s.username);
                            rebuildAccountList();
                            accountListContainer.revalidate();
                            accountListContainer.repaint();
                        });

                accountListContainer.add(card);
                accountListContainer.add(Box.createVerticalStrut(10));
            }
        } else {
            // Message si aucun compte
            JLabel noAccountLabel = new JLabel("Aucun compte enregistré");
            noAccountLabel.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
            noAccountLabel.setForeground(new Color(255, 255, 255, 120));
            noAccountLabel.setIcon(FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, 14, new Color(255, 255, 255, 120)));
            accountListContainer.add(noAccountLabel);
            accountListContainer.add(Box.createVerticalStrut(10));
        }

        ModernButton addAccountBtn = new ModernButton("AJOUTER UN COMPTE", DesignConstants.SUCCESS_COLOR,
                DesignConstants.SUCCESS_COLOR.darker(), true);
        addAccountBtn.setPreferredSize(new Dimension(220, 40));
        addAccountBtn.setMaximumSize(new Dimension(220, 40));
        addAccountBtn.setIcon(FontIcon.of(FontAwesomeSolid.PLUS, 14, DesignConstants.TEXT_PRIMARY));
        addAccountBtn.addActionListener(e -> {
            if (addAccountCallback != null)
                addAccountCallback.run();
        });

        accountListContainer.add(Box.createVerticalStrut(10));
        accountListContainer.add(addAccountBtn);
    }

    public void refreshUserProfile() {
        rebuildAccountList();
        if (accountListContainer != null) {
            accountListContainer.revalidate();
            accountListContainer.repaint();
        }
    }

    private JPanel createMemoryTab() {
        JPanel p = createTabBase("Allocation Mémoire Java");
        p.add(Box.createVerticalStrut(20));

        // Carte Allocation Mémoire
        JPanel ramCard = createCard();
        ramCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramCard.setMaximumSize(new Dimension(600, 150));

        int ram = cfg.getMaxMemory();
        JLabel ramValue = new JLabel(ram + " MB");
        ramValue.setForeground(DesignConstants.PURPLE_ACCENT);
        ramValue.setFont(DesignConstants.FONT_HEADER.deriveFont(28f));
        ramValue.setIcon(FontIcon.of(FontAwesomeSolid.MEMORY, 24, DesignConstants.PURPLE_ACCENT));
        ramCard.add(ramValue);
        ramCard.add(Box.createVerticalStrut(15));

        ramSlider = new JSlider(512, 16384, ram);
        ramSlider.setOpaque(false);
        ramSlider.setForeground(DesignConstants.PURPLE_ACCENT);
        ramSlider.setPaintTicks(true);
        ramSlider.setPaintLabels(true);
        ramSlider.setMajorTickSpacing(4096);
        ramSlider.setMinorTickSpacing(1024);
        ramSlider.setPreferredSize(new Dimension(500, 60));
        ramSlider.addChangeListener(e -> ramValue.setText(ramSlider.getValue() + " MB"));
        ramCard.add(ramSlider);

        JLabel ramHint = new JLabel("Recommandé: 4096-8192 MB pour une expérience optimale");
        ramHint.setFont(DesignConstants.FONT_REGULAR.deriveFont(11f));
        ramHint.setForeground(new Color(255, 255, 255, 120));
        ramCard.add(Box.createVerticalStrut(10));
        ramCard.add(ramHint);

        p.add(ramCard);

        p.add(Box.createVerticalStrut(20));

        // Graphique d'utilisation RAM en temps réel
        RAMUsageChart ramChart = new RAMUsageChart();
        ramChart.setAllocatedMemory(ram);
        ramChart.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramSlider.addChangeListener(e -> ramChart.setAllocatedMemory(ramSlider.getValue()));
        p.add(ramChart);

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

        // Carte Limite de débit
        JPanel rateCard = createCard();
        rateCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        rateCard.setMaximumSize(new Dimension(600, 120));

        JLabel rateTitle = new JLabel("Limite de débit de téléchargement");
        rateTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        rateTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        rateTitle.setIcon(FontIcon.of(FontAwesomeSolid.DOWNLOAD, 16, DesignConstants.PURPLE_ACCENT));
        rateCard.add(rateTitle);
        rateCard.add(Box.createVerticalStrut(15));

        JPanel ratePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        ratePanel.setOpaque(false);
        ratePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rateSpinner = new JSpinner(new SpinnerNumberModel(cfg.downloadRateLimitKBps, 0, 102400, 64));
        ((JSpinner.DefaultEditor) rateSpinner.getEditor()).getTextField().setPreferredSize(new Dimension(100, 32));
        ((JSpinner.DefaultEditor) rateSpinner.getEditor()).getTextField()
                .setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));

        JLabel rateLabel = new JLabel("KB/s (0 = illimité)");
        rateLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        rateLabel.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));

        ratePanel.add(rateSpinner);
        ratePanel.add(rateLabel);
        rateCard.add(ratePanel);

        p.add(rateCard);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createFoldersTab() {
        JPanel p = createTabBase("Dossiers & Cache");
        p.add(Box.createVerticalStrut(20));

        // Carte Répertoire de jeu
        JPanel dirCard = createCard();
        dirCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        dirCard.setMaximumSize(new Dimension(700, 120));

        JLabel dirTitle = new JLabel("Répertoire de jeu");
        dirTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        dirTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        dirTitle.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER, 16, DesignConstants.PURPLE_ACCENT));
        dirCard.add(dirTitle);
        dirCard.add(Box.createVerticalStrut(15));

        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pathPanel.setOpaque(false);
        pathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        gameDirField = new JTextField(LauncherConfig.getGameDir());
        gameDirField.setPreferredSize(new Dimension(400, 35));
        gameDirField.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));

        ModernButton chooseDirBtn = new ModernButton("PARCOURIR", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        chooseDirBtn.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 16, DesignConstants.TEXT_PRIMARY));
        chooseDirBtn.setPreferredSize(new Dimension(140, 35));
        chooseDirBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(LauncherConfig.getGameDir());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(SettingsPanel.this) == JFileChooser.APPROVE_OPTION) {
                File dir = fc.getSelectedFile();
                if (dir != null)
                    gameDirField.setText(dir.getAbsolutePath());
            }
        });

        pathPanel.add(gameDirField);
        pathPanel.add(chooseDirBtn);
        dirCard.add(pathPanel);
        p.add(dirCard);

        p.add(Box.createVerticalStrut(20));

        // Carte Gestion du cache
        JPanel cacheCard = createCard();
        cacheCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        cacheCard.setMaximumSize(new Dimension(700, 140));

        JLabel cacheTitle = new JLabel("Gestion du cache & maintenance");
        cacheTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        cacheTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        cacheTitle.setIcon(FontIcon.of(FontAwesomeSolid.DATABASE, 16, DesignConstants.PURPLE_ACCENT));
        cacheCard.add(cacheTitle);
        cacheCard.add(Box.createVerticalStrut(15));

        JPanel cachePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        cachePanel.setOpaque(false);
        cachePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton clearCacheBtn = new ModernButton("VIDER CACHE", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        clearCacheBtn.setIcon(FontIcon.of(FontAwesomeSolid.TRASH, 16, DesignConstants.TEXT_PRIMARY));
        clearCacheBtn.setPreferredSize(new Dimension(160, 40));
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

        ModernButton resetDataBtn = new ModernButton("RÉINIT. DONNÉES", new Color(200, 100, 50),
                new Color(160, 80, 30), false);
        resetDataBtn.setIcon(FontIcon.of(FontAwesomeSolid.BROOM, 16, DesignConstants.TEXT_PRIMARY));
        resetDataBtn.setPreferredSize(new Dimension(180, 40));
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

        ModernButton repairBtn = new ModernButton("RÉPARER LE JEU", DesignConstants.SUCCESS_COLOR,
                DesignConstants.SUCCESS_COLOR.darker(), false);
        repairBtn.setIcon(FontIcon.of(FontAwesomeSolid.TOOLS, 16, DesignConstants.TEXT_PRIMARY));
        repairBtn.setPreferredSize(new Dimension(170, 40));
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
        cacheCard.add(cachePanel);

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

        ModernButton cleanupBtn = new ModernButton("NETTOYER LE CACHE", new Color(180, 120, 60),
                new Color(160, 100, 40), false);
        cleanupBtn.setIcon(FontIcon.of(FontAwesomeSolid.BROOM, 14, DesignConstants.TEXT_PRIMARY));
        cleanupBtn.addActionListener(e -> performCleanup());
        diagPanel.add(cleanupBtn);

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

    private JPanel createCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fond avec effet glassmorphism
                g2.setColor(new Color(40, 30, 50, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Bordure subtile
                g2.setColor(new Color(170, 80, 255, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return card;
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
                        if (skinChangedCallback != null)
                            skinChangedCallback.run();
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

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performCleanup() {
        // Analyser d'abord ce qui peut être nettoyé
        com.nexaria.launcher.util.CacheCleanupService.CleanupResult analysis = com.nexaria.launcher.util.CacheCleanupService
                .analyzeCleanableFiles();

        double mb = analysis.bytesFreed / (1024.0 * 1024.0);
        String message = String.format(
                "Fichiers à nettoyer :\n\n" +
                        "• %d fichiers temporaires\n" +
                        "• %.2f MB d'espace disque\n\n" +
                        "Voulez-vous continuer ?",
                analysis.filesDeleted, mb);

        int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                "Nettoyage du cache",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            SwingWorker<com.nexaria.launcher.util.CacheCleanupService.CleanupResult, Void> worker = new SwingWorker<>() {
                @Override
                protected com.nexaria.launcher.util.CacheCleanupService.CleanupResult doInBackground() {
                    return com.nexaria.launcher.util.CacheCleanupService.cleanupFiles(true, true, true);
                }

                @Override
                protected void done() {
                    try {
                        com.nexaria.launcher.util.CacheCleanupService.CleanupResult result = get();
                        JOptionPane.showMessageDialog(
                                SettingsPanel.this,
                                result.getSummary(),
                                "Nettoyage terminé",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                SettingsPanel.this,
                                "Erreur lors du nettoyage: " + e.getMessage(),
                                "Erreur",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
}
