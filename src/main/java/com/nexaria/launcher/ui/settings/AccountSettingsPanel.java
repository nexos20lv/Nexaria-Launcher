package com.nexaria.launcher.ui.settings;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.ui.AccountCard;
import com.nexaria.launcher.ui.DesignConstants;
import com.nexaria.launcher.ui.ModernButton;
import com.nexaria.launcher.services.auth.AzAuthManager;
import com.nexaria.launcher.services.cache.ImageCache;
import com.nexaria.launcher.config.RememberStore;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AccountSettingsPanel extends SettingsTabPanel {
    private static final Logger logger = LoggerFactory.getLogger(AccountSettingsPanel.class);
    private final LauncherConfig cfg;

    private final java.util.function.Supplier<String> accessTokenSupplier;
    private final java.util.function.Supplier<String> uuidSupplier;
    private final java.util.function.Supplier<String> usernameSupplier;
    private final java.util.function.Supplier<String> azuriomUrlSupplier;
    private final Runnable skinChangedCallback;
    private final Runnable logoutCallback;
    private final java.util.function.Consumer<com.nexaria.launcher.config.RememberStore.RememberSession> switchAccountCallback;
    private final Runnable addAccountCallback;

    private JPanel accountListContainer;
    private JPanel activeAccountPanel;
    private JCheckBox rememberDefault;

    public AccountSettingsPanel(java.util.function.Supplier<String> accessTokenSupplier,
            java.util.function.Supplier<String> uuidSupplier,
            java.util.function.Supplier<String> usernameSupplier,
            java.util.function.Supplier<String> azuriomUrlSupplier,
            Runnable skinChangedCallback,
            Runnable logoutCallback,
            java.util.function.Consumer<com.nexaria.launcher.config.RememberStore.RememberSession> switchAccountCallback,
            Runnable addAccountCallback) {
        super("Compte & Sécurité");
        this.cfg = LauncherConfig.getInstance();
        this.accessTokenSupplier = accessTokenSupplier;
        this.uuidSupplier = uuidSupplier;
        this.usernameSupplier = usernameSupplier;
        this.azuriomUrlSupplier = azuriomUrlSupplier;
        this.skinChangedCallback = skinChangedCallback;
        this.logoutCallback = logoutCallback;
        this.switchAccountCallback = switchAccountCallback;
        this.addAccountCallback = addAccountCallback;
        initUI();
    }

    private void initUI() {
        // Vertical layout for the main container
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(0, 5, 20, 5));

        // Also align content left, though wrapper handles it mostly
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        // EXTRA SPACING requested by user
        content.add(Box.createVerticalStrut(30));

        // 1. ACTIVE SESSION SECTION
        JLabel activeTitle = new JLabel("Profil Actuel");
        activeTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        activeTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        activeTitle.setIcon(FontIcon.of(FontAwesomeSolid.ID_CARD, 16, DesignConstants.PURPLE_ACCENT));
        activeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(activeTitle);
        content.add(Box.createVerticalStrut(10));

        activeAccountPanel = new JPanel();
        activeAccountPanel.setLayout(new BoxLayout(activeAccountPanel, BoxLayout.Y_AXIS));
        activeAccountPanel.setOpaque(false);
        activeAccountPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(activeAccountPanel);

        content.add(Box.createVerticalStrut(25));

        // 2. PREFERENCES SECTION
        JLabel prefsTitle = new JLabel("Préférences");
        prefsTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        prefsTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        prefsTitle.setIcon(FontIcon.of(FontAwesomeSolid.COGS, 16, DesignConstants.PURPLE_ACCENT));
        prefsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(prefsTitle);
        content.add(Box.createVerticalStrut(10));

        JPanel prefsCard = createCard();
        prefsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        prefsCard.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10)); // Better alignment
        prefsCard.setMaximumSize(new Dimension(800, 50));

        rememberDefault = new JCheckBox("Se souvenir de moi par défaut");
        rememberDefault.setOpaque(false);
        rememberDefault.setForeground(DesignConstants.TEXT_PRIMARY);
        rememberDefault.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
        rememberDefault.setSelected(cfg.rememberMeDefault);
        rememberDefault.setFocusPainted(false);
        rememberDefault.setBorderPainted(false);
        rememberDefault.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 18, new Color(255, 255, 255, 40)));
        rememberDefault.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 18, DesignConstants.PURPLE_ACCENT));
        rememberDefault.addActionListener(e -> {
            cfg.setRememberMeDefault(rememberDefault.isSelected());
            cfg.saveConfig();
        });
        prefsCard.add(rememberDefault);
        content.add(prefsCard);

        content.add(Box.createVerticalStrut(25));

        // 3. SAVED ACCOUNTS SECTION
        JLabel accountsTitle = new JLabel("Comptes enregistrés");
        accountsTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        accountsTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        accountsTitle.setIcon(FontIcon.of(FontAwesomeSolid.USERS, 16, DesignConstants.PURPLE_ACCENT));
        accountsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(accountsTitle);
        content.add(Box.createVerticalStrut(10));

        accountListContainer = new JPanel();
        accountListContainer.setLayout(new BoxLayout(accountListContainer, BoxLayout.Y_AXIS));
        accountListContainer.setOpaque(false);
        accountListContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(accountListContainer);

        rebuildContent();

        // Wrap in a wrapper to align top-left
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        // CRITICAL FIX: Set alignment to LEFT so BoxLayout in parent respects it
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(content, BorderLayout.NORTH);

        add(wrapper);
    }

    private JPanel createCurrentSessionCard(String username, String accessToken) {
        // Special card with gradient background for the active session
        JPanel card = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Subtle purple gradient
                GradientPaint gp = new GradientPaint(0, 0, new Color(80, 40, 120, 80), getWidth(), getHeight(),
                        new Color(40, 20, 60, 60));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Border
                g2.setColor(new Color(138, 43, 226, 100)); // Purple Accent border
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        card.setMaximumSize(new Dimension(800, 110)); // Slightly taller

        // Left: Avatar
        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(72, 72)); // Bigger avatar

        String headUrl = "https://eclozionmc.ovh/api/skin-api/avatars/face/" + username;

        ImageCache.getInstance().get(headUrl).thenAccept(img -> {
            if (img != null) {
                SwingUtilities.invokeLater(() -> avatarLabel.setIcon(new ImageIcon(createRoundedImage(img, 72))));
            }
        });

        card.add(avatarLabel, BorderLayout.WEST);

        // Center: User Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(DesignConstants.FONT_HEADER.deriveFont(24f)); // Bigger Name
        nameLabel.setForeground(Color.WHITE);
        // Removed Glue to avoid spacing issues

        JLabel statusLabel = new JLabel("Compte Officiel Nexaria");
        statusLabel.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        statusLabel.setForeground(new Color(200, 200, 255));

        // Use strut instead of glue to force top alignment
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(statusLabel);
        infoPanel.add(Box.createVerticalGlue()); // Push to top

        card.add(infoPanel, BorderLayout.CENTER);

        // Right: Actions
        JPanel actionsPanel = new JPanel(new GridLayout(2, 1, 0, 10)); // Grid for stacked buttons
        actionsPanel.setOpaque(false);

        ModernButton skinBtn = new ModernButton("Changer Skin", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, true);
        skinBtn.setIcon(FontIcon.of(FontAwesomeSolid.TSHIRT, 14, Color.WHITE));
        skinBtn.setPreferredSize(new Dimension(140, 32));
        skinBtn.addActionListener(e -> handleSkinUpload());

        ModernButton logoutBtn = new ModernButton("Déconnexion", new Color(180, 50, 50, 200),
                new Color(140, 30, 30, 200), true);
        logoutBtn.setIcon(FontIcon.of(FontAwesomeSolid.SIGN_OUT_ALT, 14, Color.WHITE));
        logoutBtn.setPreferredSize(new Dimension(140, 32));
        logoutBtn.addActionListener(e -> {
            if (logoutCallback != null)
                logoutCallback.run();
        });

        actionsPanel.add(skinBtn);
        actionsPanel.add(logoutBtn);
        card.add(actionsPanel, BorderLayout.EAST);

        return card;
    }

    private Image createRoundedImage(BufferedImage image, int size) {
        BufferedImage rounded = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = rounded.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Circular Clip
        g2.setClip(new Ellipse2D.Float(0, 0, size, size));
        g2.drawImage(image, 0, 0, size, size, null);

        // Border Ring
        g2.setClip(null);
        g2.setColor(new Color(255, 255, 255, 50)); // Semi-transparent white ring
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(1, 1, size - 2, size - 2);

        g2.dispose();
        return rounded;
    }

    private void rebuildContent() {
        if (activeAccountPanel == null || accountListContainer == null)
            return;

        // 1. Refresh Active Account
        activeAccountPanel.removeAll();
        String current = "Voyageur";
        String token = "";

        if (usernameSupplier != null) {
            String s = usernameSupplier.get();
            if (s != null && !s.isEmpty() && !s.equals("null")) {
                current = s;
            }
        }
        if (accessTokenSupplier != null) {
            String t = accessTokenSupplier.get();
            if (t != null)
                token = t;
        }

        activeAccountPanel.add(createCurrentSessionCard(current, token));
        activeAccountPanel.revalidate();
        activeAccountPanel.repaint();

        // 2. Refresh Saved Accounts List
        accountListContainer.removeAll();
        List<RememberStore.RememberSession> sessions = RememberStore.loadSessions();

        if (sessions != null && !sessions.isEmpty()) {
            boolean hasOtherAccounts = false;
            for (RememberStore.RememberSession s : sessions) {
                if (current != null && current.equals(s.username))
                    continue;

                hasOtherAccounts = true;
                AccountCard card = new AccountCard(
                        s,
                        azuriomUrlSupplier.get(),
                        false,
                        () -> {
                            if (switchAccountCallback != null)
                                switchAccountCallback.accept(s);
                        },
                        () -> {
                            RememberStore.removeSession(s.username);
                            rebuildContent();
                        });
                card.setMaximumSize(new Dimension(800, 70));
                accountListContainer.add(card);
                accountListContainer.add(Box.createVerticalStrut(10));
            }
            if (!hasOtherAccounts) {
                addNoAccountLabel();
            }
        } else {
            addNoAccountLabel();
        }

        // Action button at bottom
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setOpaque(false);

        ModernButton addAccountBtn = new ModernButton("Ajouter un compte", DesignConstants.SUCCESS_COLOR,
                DesignConstants.SUCCESS_COLOR.darker(), true);
        addAccountBtn.setPreferredSize(new Dimension(200, 40));

        addAccountBtn.setIcon(FontIcon.of(FontAwesomeSolid.PLUS_CIRCLE, 16, DesignConstants.TEXT_PRIMARY));
        addAccountBtn.addActionListener(e -> {
            if (addAccountCallback != null)
                addAccountCallback.run();
        });

        btnPanel.add(addAccountBtn);

        accountListContainer.add(Box.createVerticalStrut(15));
        accountListContainer.add(btnPanel);

        accountListContainer.revalidate();
        accountListContainer.repaint();
    }

    private void addNoAccountLabel() {
        JLabel noAccountLabel = new JLabel("Aucun autre compte enregistré");
        noAccountLabel.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
        noAccountLabel.setForeground(new Color(255, 255, 255, 120));
        noAccountLabel.setIcon(FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, 14, new Color(255, 255, 255, 120)));
        noAccountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountListContainer.add(noAccountLabel);
        accountListContainer.add(Box.createVerticalStrut(10));
    }

    public void refreshUserProfile() {
        rebuildContent();
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
                    AzAuthManager authMgr = new AzAuthManager(url);
                    authMgr.uploadSkin(token, file);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("[SKIN] OK en {}ms", duration);
                        JOptionPane.showMessageDialog(AccountSettingsPanel.this, "Skin mis à jour!", "Succès",
                                JOptionPane.INFORMATION_MESSAGE);
                        if (skinChangedCallback != null)
                            skinChangedCallback.run();
                    } catch (Exception ex) {
                        logger.error("[SKIN] ERREUR: {}", ex.getMessage());
                        JOptionPane.showMessageDialog(AccountSettingsPanel.this, "Échec mise à jour.", "Erreur",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
}
