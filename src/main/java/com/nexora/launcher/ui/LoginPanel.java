package com.nexora.launcher.ui;

import com.nexora.launcher.auth.AzAuthManager;
import com.nexora.launcher.auth.AuthenticationException;
import com.nexora.launcher.config.LauncherConfig;
import com.azuriom.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class LoginPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(LoginPanel.class);
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField twoFAField;
    private JButton loginButton;
    private JLabel statusLabel;
    private JLabel azuriomLabel;
    private JCheckBox rememberCheckBox;
    private Consumer<User> loginCallback;
    private AzAuthManager authManager;
    private boolean requires2FA = false;

    public LoginPanel(Consumer<User> loginCallback) {
        this.loginCallback = loginCallback;
        this.authManager = new AzAuthManager(LauncherConfig.getInstance().getAzuriomUrl());
        
        setLayout(new GridBagLayout());
        setBackground(new Color(45, 45, 48));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Logo/Titre
        JLabel titleLabel = new JLabel("NEXORA LAUNCHER");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(30, 10, 20, 10);
        add(titleLabel, gbc);

        // Label Azuriom
        azuriomLabel = new JLabel("Connecté à: " + LauncherConfig.getInstance().getAzuriomUrl());
        azuriomLabel.setForeground(new Color(150, 150, 150));
        azuriomLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 10, 20, 10);
        add(azuriomLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Label Email
        JLabel emailLabel = new JLabel("Email/Pseudo:");
        emailLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        add(emailLabel, gbc);

        // Champ Email
        usernameField = new JTextField(20);
        usernameField.setBackground(new Color(60, 60, 65));
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.WHITE);
        gbc.gridx = 1;
        gbc.gridy = 2;
        add(usernameField, gbc);

        // Label Mot de passe
        JLabel passwordLabel = new JLabel("Mot de passe:");
        passwordLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(passwordLabel, gbc);

        // Champ Mot de passe
        passwordField = new JPasswordField(20);
        passwordField.setBackground(new Color(60, 60, 65));
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);
        gbc.gridx = 1;
        gbc.gridy = 3;
        add(passwordField, gbc);

        // Label 2FA (caché par défaut)
        JLabel twoFALabel = new JLabel("Code 2FA:");
        twoFALabel.setForeground(Color.WHITE);
        twoFALabel.setVisible(false);
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(twoFALabel, gbc);

        // Champ 2FA (caché par défaut)
        twoFAField = new JTextField(20);
        twoFAField.setBackground(new Color(60, 60, 65));
        twoFAField.setForeground(Color.WHITE);
        twoFAField.setCaretColor(Color.WHITE);
        twoFAField.setVisible(false);
        gbc.gridx = 1;
        gbc.gridy = 4;
        add(twoFAField, gbc);

        // Remember me
        rememberCheckBox = new JCheckBox("Se souvenir de moi");
        rememberCheckBox.setBackground(new Color(45, 45, 48));
        rememberCheckBox.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        add(rememberCheckBox, gbc);

        // Bouton Connexion
        loginButton = new JButton("Se connecter");
        loginButton.setBackground(new Color(0, 120, 215));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> performLogin());
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 20, 10);
        add(loginButton, gbc);

        // Label Statut
        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        add(statusLabel, gbc);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String twoFACode = twoFAField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            setStatusMessage("Veuillez remplir tous les champs");
            return;
        }

        SwingWorker<User, String> worker = new SwingWorker<User, String>() {
            @Override
            protected User doInBackground() throws Exception {
                try {
                    publish("Authentification en cours...");
                    User user = authManager.authenticate(username, password, twoFACode);
                    return user;
                } catch (AuthenticationException e) {
                    logger.error("Erreur d'authentification", e);
                    throw e;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    setStatusMessage(message);
                }
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    setStatusMessage("Bienvenue, " + user.getUsername() + "!");
                    passwordField.setText("");
                    twoFAField.setText("");
                    loginCallback.accept(user);
                } catch (Exception e) {
                    setStatusMessage("Erreur: " + e.getMessage());
                    setButtonsEnabled(true);
                }
            }
        };

        setButtonsEnabled(false);
        worker.execute();
    }

    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    public void setButtonsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        twoFAField.setEnabled(enabled);
    }

    public void clearPassword() {
        passwordField.setText("");
        twoFAField.setText("");
    }

    public void setAzuriomUrl(String url) {
        authManager.setAzuriomUrl(url);
        azuriomLabel.setText("Connecté à: " + url);
    }
}
