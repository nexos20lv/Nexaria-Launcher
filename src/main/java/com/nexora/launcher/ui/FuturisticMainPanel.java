package com.nexora.launcher.ui;

import com.nexora.launcher.model.User;
import com.nexora.launcher.ui.IconUtil;
import javax.swing.*;
import java.awt.*;

public class FuturisticMainPanel extends JPanel {
    private static final Color DARK_BG = new Color(15, 23, 42);
    private static final Color CARD_BG = new Color(30, 41, 59);
    private static final Color TEXT_PRIMARY = new Color(241, 245, 249);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color ACCENT_BLUE = new Color(59, 130, 246);
    private static final Color ACCENT_GREEN = new Color(34, 197, 94);
    private static final Color ACCENT_RED = new Color(239, 68, 68);

    private Runnable launchCallback;
    private Runnable logoutCallback;

    private JLabel userLabel;
    private JLabel versionLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton launchButton;
    private JButton logoutButton;

    public FuturisticMainPanel(Runnable launchCallback, Runnable logoutCallback) {
        this.launchCallback = launchCallback;
        this.logoutCallback = logoutCallback;

        setBackground(DARK_BG);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.gridy = 0;

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(CARD_BG);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("NEXORA LAUNCHER", IconUtil.play(28, ACCENT_BLUE), SwingConstants.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(ACCENT_BLUE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        userLabel = new JLabel("Utilisateur: Non connecté", IconUtil.user(20, ACCENT_GREEN), SwingConstants.LEFT);
        userLabel.setForeground(ACCENT_GREEN);
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        versionLabel = new JLabel("Version: Minecraft 1.20.1 - Forge 47.2.0", IconUtil.settings(18, ACCENT_BLUE), SwingConstants.LEFT);
        versionLabel.setForeground(TEXT_SECONDARY);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel("Prêt", IconUtil.settings(16, TEXT_PRIMARY), SwingConstants.LEFT);
        statusLabel.setForeground(TEXT_PRIMARY);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(ACCENT_BLUE);
        progressBar.setBackground(new Color(50, 61, 89));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(500, 25));
        progressBar.setMaximumSize(new Dimension(500, 25));
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(CARD_BG);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setMaximumSize(new Dimension(500, 50));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        launchButton = new ModernButton(" LANCER LE JEU", ACCENT_GREEN, new Color(22, 163, 74));
        launchButton.setIcon(IconUtil.play(18, Color.WHITE));
        launchButton.setMaximumSize(new Dimension(250, 50));
        launchButton.addActionListener(e -> launchCallback.run());

        logoutButton = new ModernButton(" DÉCONNEXION", ACCENT_RED, new Color(220, 38, 38));
        logoutButton.setIcon(IconUtil.logout(18, Color.WHITE));
        logoutButton.setMaximumSize(new Dimension(200, 50));
        logoutButton.addActionListener(e -> logoutCallback.run());

        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(logoutButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(launchButton);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(userLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(versionLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(progressBar);
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createVerticalGlue());

        add(mainPanel, gbc);
    }

    public void setUserProfile(User user) {
        if (user != null) {
            userLabel.setText("👤 Utilisateur: " + user.getUsername() + " (ID: " + user.getId() + ")");
        } else {
            userLabel.setText("Utilisateur: Non connecté");
        }
    }

    public void setVersion(String minecraftVersion, String loaderName, String loaderVersion) {
        versionLabel.setText(String.format("📦 Version: Minecraft %s - %s %s", minecraftVersion, loaderName, loaderVersion));
    }

    public void setStatus(String message) {
        statusLabel.setText("⚙ " + message);
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
        setStatus("Prêt");
        setProgress(0);
    }
}
