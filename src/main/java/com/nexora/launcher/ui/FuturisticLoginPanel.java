package com.nexora.launcher.ui;

import com.nexora.launcher.model.User;
import com.nexora.launcher.ui.IconUtil;
import com.nexora.launcher.auth.AzAuthManager;
import com.nexora.launcher.auth.AuthenticationException;
import com.nexora.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class FuturisticLoginPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(FuturisticLoginPanel.class);
    private static final Color DARK_BG = new Color(15, 23, 42);
    private static final Color ACCENT_BLUE = new Color(59, 130, 246);
    private static final Color ACCENT_PURPLE = new Color(139, 92, 246);
    private static final Color TEXT_PRIMARY = new Color(241, 245, 249);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color INPUT_BG = new Color(30, 41, 59);

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private Consumer<User> loginCallback;
    private AzAuthManager authManager;

    public FuturisticLoginPanel(Consumer<User> loginCallback) {
        this.loginCallback = loginCallback;
        this.authManager = new AzAuthManager(LauncherConfig.getInstance().getAzuriomUrl());

        setBackground(DARK_BG);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);

        // Panel central avec background
        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(new Color(25, 35, 65));
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Titre avec glow effect
        JLabel titleLabel = new JLabel("NEXORA");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        titleLabel.setForeground(ACCENT_BLUE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("LAUNCHER");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(titleLabel);
        centerPanel.add(subtitleLabel);
        centerPanel.add(Box.createVerticalStrut(40));

        // Champ Email
        JLabel emailLabel = new JLabel("Email ou Pseudo", IconUtil.mail(16, TEXT_PRIMARY), SwingConstants.LEFT);
        emailLabel.setForeground(TEXT_PRIMARY);
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = createStyledTextField();
        usernameField.setMaximumSize(new Dimension(300, 45));

        centerPanel.add(emailLabel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(usernameField);
        centerPanel.add(Box.createVerticalStrut(20));

        // Champ Mot de passe
        JLabel passwordLabel = new JLabel("Mot de passe", IconUtil.lock(16, TEXT_PRIMARY), SwingConstants.LEFT);
        passwordLabel.setForeground(TEXT_PRIMARY);
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = createStyledPasswordField();
        passwordField.setMaximumSize(new Dimension(300, 45));

        centerPanel.add(passwordLabel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(passwordField);
        centerPanel.add(Box.createVerticalStrut(30));

        // Bouton Connexion
        loginButton = new ModernButton("SE CONNECTER", ACCENT_BLUE, ACCENT_PURPLE);
        loginButton.setMaximumSize(new Dimension(300, 50));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());

        centerPanel.add(loginButton);
        centerPanel.add(Box.createVerticalStrut(20));

        // Label Statut
        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(248, 113, 113));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(statusLabel);

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(centerPanel, gbc);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_BLUE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(64, 89, 114), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return field;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_BLUE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(64, 89, 114), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return field;
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            setStatusMessage("Veuillez remplir tous les champs");
            return;
        }

        SwingWorker<User, String> worker = new SwingWorker<User, String>() {
            @Override
            protected User doInBackground() throws Exception {
                try {
                    publish("Authentification en cours...");
                    return authManager.authenticate(username, password, "");
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
                    passwordField.setText("");
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
    }

    public void clearPassword() {
        passwordField.setText("");
    }
}
