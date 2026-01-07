package com.nexora.launcher.ui;

import com.nexora.launcher.model.User;
import javax.swing.*;
import java.awt.*;

public class FuturisticMainPanel extends JPanel {

    private Runnable launchCallback;
    private Runnable logoutCallback;

    private JLabel userLabel;
    private JLabel versionLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private ModernButton launchButton;
    private ModernButton logoutButton;

    public FuturisticMainPanel(Runnable launchCallback, Runnable logoutCallback) {
        this.launchCallback = launchCallback;
        this.logoutCallback = logoutCallback;

        setOpaque(false);
        setLayout(new GridBagLayout());

        // Glass Card
        GlassPanel glassCard = new GlassPanel(DesignConstants.ROUNDING);
        glassCard.setLayout(new BorderLayout());
        glassCard.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        glassCard.setPreferredSize(new Dimension(700, 450));

        // Top Section: User Info & Logout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        userLabel = new JLabel("Welcome, Player");
        userLabel.setFont(DesignConstants.FONT_HEADER);
        userLabel.setForeground(DesignConstants.TEXT_PRIMARY);
        userLabel.setIconTextGap(10);

        logoutButton = new ModernButton("LOGOUT", new Color(220, 38, 38), new Color(185, 28, 28)); // Red accents
        logoutButton.setPreferredSize(new Dimension(100, 35));
        logoutButton.addActionListener(e -> logoutCallback.run());

        topPanel.add(userLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);

        glassCard.add(topPanel, BorderLayout.NORTH);

        // Center Section: Info & Status
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        centerPanel.add(Box.createVerticalGlue());

        JLabel brandLabel = new JLabel("READY TO PLAY");
        brandLabel.setFont(DesignConstants.FONT_TITLE.deriveFont(40f));
        brandLabel.setForeground(DesignConstants.PURPLE_ACCENT);
        brandLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(brandLabel);

        centerPanel.add(Box.createVerticalStrut(20));

        versionLabel = new JLabel("Minecraft 1.20.1 - Forge");
        versionLabel.setFont(DesignConstants.FONT_REGULAR);
        versionLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(versionLabel);

        centerPanel.add(Box.createVerticalGlue());

        glassCard.add(centerPanel, BorderLayout.CENTER);

        // Bottom Section: Progress & Action
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        statusLabel = new JLabel("Idle");
        statusLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        statusLabel.setFont(DesignConstants.FONT_SMALL);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(DesignConstants.purple_ACCENT);
        progressBar.setBackground(new Color(255, 255, 255, 20));
        progressBar.setBorderPainted(false);
        progressBar.setMaximumSize(new Dimension(500, 6));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        launchButton = new ModernButton("PLAY NOW", DesignConstants.PURPLE_ACCENT, DesignConstants.PURPLE_ACCENT_DARK,
                true); // Gradient
        launchButton.setPreferredSize(new Dimension(250, 60)); // Big button
        launchButton.setMaximumSize(new Dimension(250, 60));
        launchButton.setFont(DesignConstants.FONT_TITLE);
        launchButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        launchButton.addActionListener(e -> launchCallback.run());

        bottomPanel.add(statusLabel);
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(progressBar);
        bottomPanel.add(Box.createVerticalStrut(20));
        bottomPanel.add(launchButton);

        glassCard.add(bottomPanel, BorderLayout.SOUTH);

        add(glassCard);
    }

    public void setUserProfile(User user) {
        if (user != null) {
            userLabel.setText("Welcome, " + user.getUsername());
        } else {
            userLabel.setText("Welcome, Player");
        }
    }

    public void setVersion(String minecraftVersion, String loaderName, String loaderVersion) {
        versionLabel
                .setText(String.format("Version: Minecraft %s - %s %s", minecraftVersion, loaderName, loaderVersion));
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public void setProgress(int progress) {
        progressBar.setValue(progress);
    }

    public void setButtonsEnabled(boolean enabled) {
        launchButton.setEnabled(enabled);
        logoutButton.setEnabled(enabled);
    }

    public void reset() {
        setButtonsEnabled(true);
        setUserProfile(null);
        setStatus("Ready");
        setProgress(0);
    }
}
