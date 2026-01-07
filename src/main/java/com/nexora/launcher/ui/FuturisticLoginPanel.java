package com.nexora.launcher.ui;

import com.nexora.launcher.model.User;
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

    private JTextField usernameField;
    private JPasswordField passwordField;
    private ModernButton loginButton;
    private JLabel statusLabel;
    private Consumer<User> loginCallback;
    private AzAuthManager authManager;

    public FuturisticLoginPanel(Consumer<User> loginCallback) {
        this.loginCallback = loginCallback;
        this.authManager = new AzAuthManager(LauncherConfig.getInstance().getAzuriomUrl());

        setOpaque(false); // Transparent to show parent gradient
        setLayout(new GridBagLayout());

        // Glass Card for the Login Form
        GlassPanel glassCard = new GlassPanel(DesignConstants.ROUNDING);
        glassCard.setLayout(new BoxLayout(glassCard, BoxLayout.Y_AXIS));
        glassCard.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        // Title
        JLabel titleLabel = new JLabel("NEXORA");
        titleLabel.setFont(DesignConstants.FONT_TITLE.deriveFont(32f));
        titleLabel.setForeground(DesignConstants.BLUE_ACCENT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("LAUNCHER");
        subtitleLabel.setFont(DesignConstants.FONT_HEADER); // Smaller than title
        subtitleLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        glassCard.add(titleLabel);
        glassCard.add(subtitleLabel);
        glassCard.add(Box.createVerticalStrut(30));

        // Username
        JLabel userLabel = new JLabel("Username / Email");
        userLabel.setFont(DesignConstants.FONT_SMALL);
        userLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center alignment looks good on cards

        usernameField = createStyledTextField();
        usernameField.setMaximumSize(new Dimension(280, 40));

        glassCard.add(userLabel);
        glassCard.add(Box.createVerticalStrut(5));
        glassCard.add(usernameField);
        glassCard.add(Box.createVerticalStrut(15));

        // Password
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(DesignConstants.FONT_SMALL);
        passLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = createStyledPasswordField();
        passwordField.setMaximumSize(new Dimension(280, 40));

        glassCard.add(passLabel);
        glassCard.add(Box.createVerticalStrut(5));
        glassCard.add(passwordField);
        glassCard.add(Box.createVerticalStrut(30));

        // Submit Button
        loginButton = new ModernButton("LOGIN", DesignConstants.PURPLE_ACCENT, DesignConstants.PURPLE_ACCENT_DARK,
                true); // Gradient button
        loginButton.setMaximumSize(new Dimension(280, 45));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());

        glassCard.add(loginButton);
        glassCard.add(Box.createVerticalStrut(20));

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(DesignConstants.FONT_SMALL);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        glassCard.add(statusLabel);

        // Add Card to Panel
        add(glassCard);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setOpaque(true);
        field.setBackground(new Color(255, 255, 255, 20)); // Semi-transparent
        field.setForeground(DesignConstants.TEXT_PRIMARY);
        field.setCaretColor(DesignConstants.BLUE_ACCENT);
        field.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // No line border, just padding
        field.setFont(DesignConstants.FONT_REGULAR);
        return field;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setOpaque(true);
        field.setBackground(new Color(255, 255, 255, 20));
        field.setForeground(DesignConstants.TEXT_PRIMARY);
        field.setCaretColor(DesignConstants.BLUE_ACCENT);
        field.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        field.setFont(DesignConstants.FONT_REGULAR);
        return field;
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            setStatusMessage("Please enter your credentials.");
            return;
        }

        SwingWorker<User, String> worker = new SwingWorker<User, String>() {
            @Override
            protected User doInBackground() throws Exception {
                try {
                    publish("Authenticating...");
                    return authManager.authenticate(username, password, "");
                } catch (AuthenticationException e) {
                    logger.error("Auth Error", e);
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
                    setStatusMessage("Error: " + e.getMessage());
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
