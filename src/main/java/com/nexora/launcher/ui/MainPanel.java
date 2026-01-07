package com.nexora.launcher.ui;

import com.nexora.launcher.auth.AzureAuthManager;

import javax.swing.*;
import java.awt.*;

public class MainPanel extends JPanel {
    private JButton downloadButton;
    private JButton logoutButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel userLabel;
    private Runnable downloadCallback;
    private Runnable logoutCallback;

    public MainPanel(Runnable downloadCallback, Runnable logoutCallback) {
        this.downloadCallback = downloadCallback;
        this.logoutCallback = logoutCallback;
        
        setLayout(new GridBagLayout());
        setBackground(new Color(45, 45, 48));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Titre
        JLabel titleLabel = new JLabel("NEXORA LAUNCHER");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 20, 10);
        add(titleLabel, gbc);

        // Label utilisateur
        userLabel = new JLabel("Utilisateur: Non connecté");
        userLabel.setForeground(new Color(100, 200, 100));
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(userLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Label Statut
        statusLabel = new JLabel("Prêt");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(statusLabel, gbc);

        // Barre de progression
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setBackground(new Color(60, 60, 65));
        progressBar.setForeground(new Color(0, 120, 215));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 20, 10);
        add(progressBar, gbc);

        // Bouton Télécharger
        downloadButton = new JButton("Télécharger & Lancer");
        downloadButton.setBackground(new Color(0, 120, 215));
        downloadButton.setForeground(Color.WHITE);
        downloadButton.setFont(new Font("Arial", Font.BOLD, 14));
        downloadButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        downloadButton.addActionListener(e -> downloadCallback.run());
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);
        add(downloadButton, gbc);

        // Bouton Déconnexion
        logoutButton = new JButton("Déconnexion");
        logoutButton.setBackground(new Color(200, 50, 50));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFont(new Font("Arial", Font.BOLD, 14));
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> logoutCallback.run());
        gbc.gridx = 1;
        gbc.gridy = 4;
        add(logoutButton, gbc);

        // Informations supplémentaires
        JTextArea infoArea = new JTextArea();
        infoArea.setText("Statut:\n• Mods: À jour\n• Configs: À jour\n• Dernière vérification: À l'instant");
        infoArea.setBackground(new Color(60, 60, 65));
        infoArea.setForeground(Color.WHITE);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        infoArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setBackground(new Color(60, 60, 65));
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        add(scrollPane, gbc);
    }

    public void setUserProfile(AzureAuthManager.UserProfile profile) {
        if (profile != null) {
            userLabel.setText("Utilisateur: " + profile.getDisplayName() + " (" + profile.getEmail() + ")");
        }
    }

    public void setProgress(int progress) {
        progressBar.setValue(progress);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public void setButtonsEnabled(boolean enabled) {
        downloadButton.setEnabled(enabled);
        logoutButton.setEnabled(enabled);
    }

    public void reset() {
        progressBar.setValue(0);
        statusLabel.setText("Prêt");
        userLabel.setText("Utilisateur: Non connecté");
        setButtonsEnabled(true);
    }
}
