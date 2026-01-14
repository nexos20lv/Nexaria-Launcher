package com.nexaria.launcher.ui;

import com.nexaria.launcher.ui.settings.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(SettingsPanel.class);

    private AccountSettingsPanel accountPanel;

    public SettingsPanel(java.util.function.Supplier<String> accessTokenSupplier,
            java.util.function.Supplier<String> uuidSupplier,
            java.util.function.Supplier<String> usernameSupplier,
            java.util.function.Supplier<String> azuriomUrlSupplier,
            Runnable skinChangedCallback,
            Runnable logoutCallback,
            java.util.function.Consumer<com.nexaria.launcher.config.RememberStore.RememberSession> switchAccountCallback,
            Runnable addAccountCallback) {
        
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(null); // Full width/height

        // 1. Sidebar (Left)
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
        sidebar.setOpaque(false);
        sidebar.setBackground(new Color(30, 20, 40, 240)); // Almost opaque
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, DesignConstants.GLASS_BORDER),
                BorderFactory.createEmptyBorder(20, 10, 20, 10)));
        sidebar.setPreferredSize(new Dimension(240, 0));

        // 2. Content (Right)
        CardLayout cardLayout = new CardLayout();
        JPanel contentResults = new JPanel(cardLayout);
        contentResults.setOpaque(false);
        contentResults.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        ButtonGroup group = new ButtonGroup();

        // Initialize Sub-Panels
        GeneralSettingsPanel generalPanel = new GeneralSettingsPanel();
        
        accountPanel = new AccountSettingsPanel(
                accessTokenSupplier, uuidSupplier, usernameSupplier, azuriomUrlSupplier,
                skinChangedCallback, logoutCallback, switchAccountCallback, addAccountCallback
        );
        
        JavaSettingsPanel javaPanel = new JavaSettingsPanel();
        NetworkSettingsPanel networkPanel = new NetworkSettingsPanel();
        FoldersSettingsPanel foldersPanel = new FoldersSettingsPanel();
        DiagnosticsSettingsPanel diagnosticsPanel = new DiagnosticsSettingsPanel();
        
        ConsolePanel consolePanel = new ConsolePanel();
        consolePanel.redirectSystemStreams();

        // Add Categories to Sidebar and Content
        addCategory(sidebar, contentResults, group, cardLayout, "Général", FontAwesomeSolid.COG, wrapInScroll(generalPanel), true);
        addCategory(sidebar, contentResults, group, cardLayout, "Compte", FontAwesomeSolid.USER, wrapInScroll(accountPanel), false);
        addCategory(sidebar, contentResults, group, cardLayout, "Mémoire", FontAwesomeSolid.MEMORY, wrapInScroll(javaPanel), false);
        addCategory(sidebar, contentResults, group, cardLayout, "Réseau", FontAwesomeSolid.WIFI, wrapInScroll(networkPanel), false);
        addCategory(sidebar, contentResults, group, cardLayout, "Dossiers", FontAwesomeSolid.FOLDER_OPEN, wrapInScroll(foldersPanel), false);
        addCategory(sidebar, contentResults, group, cardLayout, "Diagnostics", FontAwesomeSolid.STETHOSCOPE, wrapInScroll(diagnosticsPanel), false);
        addCategory(sidebar, contentResults, group, cardLayout, "Console", FontAwesomeSolid.TERMINAL, consolePanel, false);

        add(sidebar, BorderLayout.WEST);
        add(contentResults, BorderLayout.CENTER);
    }

    private void addCategory(JPanel sidebar, JPanel content, ButtonGroup group, CardLayout cards,
                             String title, FontAwesomeSolid icon, JComponent panel, boolean selected) {
        String id = title.toUpperCase();

        JToggleButton btn = new JToggleButton(title);
        btn.setIcon(FontIcon.of(icon, 18, DesignConstants.TEXT_SECONDARY));
        btn.setSelectedIcon(FontIcon.of(icon, 18, DesignConstants.PURPLE_ACCENT));
        btn.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
        btn.setForeground(DesignConstants.TEXT_SECONDARY);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(15);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(220, 50));
        btn.setPreferredSize(new Dimension(220, 50));

        btn.addChangeListener(e -> {
            if (btn.isSelected()) {
                btn.setForeground(DesignConstants.PURPLE_ACCENT);
            } else if (btn.getModel().isRollover()) {
                btn.setForeground(DesignConstants.TEXT_PRIMARY);
            } else {
                btn.setForeground(DesignConstants.TEXT_SECONDARY);
            }
        });

        btn.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JToggleButton b = (JToggleButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int r = 15;
                if (b.isSelected()) {
                    GradientPaint gp = new GradientPaint(0, 0, new Color(170, 80, 255, 40), b.getWidth(), 0,
                            new Color(170, 80, 255, 10));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), r, r);
                    g2.setColor(DesignConstants.PURPLE_ACCENT);
                    g2.fillRoundRect(0, 10, 4, b.getHeight() - 20, 4, 4);
                } else if (b.getModel().isRollover()) {
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
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public void refreshUserProfile() {
        if (accountPanel != null) {
            accountPanel.refreshUserProfile();
        }
    }
}
