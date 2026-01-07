package com.nexora.launcher.ui;package com.nexora.launcher.ui;




























































































































































}    }        setButtonsEnabled(true);        userLabel.setText("Utilisateur: Non connecté");        statusLabel.setText("Prêt");        progressBar.setValue(0);    public void reset() {    }        logoutButton.setEnabled(enabled);        launchButton.setEnabled(enabled);    public void setButtonsEnabled(boolean enabled) {    }        versionLabel.setText(String.format("📦 Version: Minecraft %s - %s %s", minecraftVersion, loaderName, loaderVersion));    public void setVersion(String minecraftVersion, String loaderName, String loaderVersion) {    }        statusLabel.setText("⚙ " + message);    public void setStatus(String message) {    }        progressBar.setValue(progress);    public void setProgress(int progress) {    }        }            userLabel.setText("👤 Utilisateur: " + user.getUsername() + " (ID: " + user.getId() + ")");        if (user != null) {    public void setUserProfile(User user) {    }        add(mainPanel, gbc);        gbc.gridy = 0;        gbc.gridx = 0;        mainPanel.add(Box.createVerticalGlue());        mainPanel.add(buttonPanel);        buttonPanel.add(Box.createHorizontalGlue());        buttonPanel.add(logoutButton);        buttonPanel.add(Box.createHorizontalStrut(20));        buttonPanel.add(launchButton);        logoutButton.addActionListener(e -> logoutCallback.run());        logoutButton.setMaximumSize(new Dimension(200, 50));        logoutButton = new ModernButton("⏻ DÉCONNEXION", ACCENT_RED, new Color(220, 38, 38));        launchButton.addActionListener(e -> launchCallback.run());        launchButton.setMaximumSize(new Dimension(250, 50));        launchButton = new ModernButton("▶ LANCER LE JEU", ACCENT_GREEN, new Color(22, 163, 74));        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);        buttonPanel.setMaximumSize(new Dimension(500, 50));        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));        buttonPanel.setBackground(CARD_BG);        JPanel buttonPanel = new JPanel();        // Boutons        mainPanel.add(Box.createVerticalStrut(30));        mainPanel.add(progressBar);        });            }                super.paintIndeterminate(g2d, c);                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);                Graphics2D g2d = (Graphics2D) g;            protected void paintIndeterminate(Graphics g, JComponent c) {            @Override        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));        progressBar.setMaximumSize(new Dimension(500, 25));        progressBar.setPreferredSize(new Dimension(500, 25));        progressBar.setForeground(ACCENT_BLUE);        progressBar.setBackground(new Color(50, 61, 89));        progressBar.setStringPainted(true);        progressBar = new JProgressBar(0, 100);        // Barre de progression        mainPanel.add(Box.createVerticalStrut(15));        mainPanel.add(statusLabel);        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));        statusLabel.setForeground(TEXT_PRIMARY);        statusLabel = new JLabel("Prêt");        // Statut        mainPanel.add(Box.createVerticalStrut(20));        mainPanel.add(versionLabel);        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));        versionLabel.setForeground(TEXT_SECONDARY);        versionLabel = new JLabel("Version: Minecraft 1.20.1 - Forge 47.2.0");        // Version        mainPanel.add(Box.createVerticalStrut(10));        mainPanel.add(userLabel);        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));        userLabel.setForeground(ACCENT_GREEN);        userLabel = new JLabel("Utilisateur: Non connecté");        // Utilisateur        mainPanel.add(Box.createVerticalStrut(20));        mainPanel.add(titleLabel);        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);        titleLabel.setForeground(ACCENT_BLUE);        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));        JLabel titleLabel = new JLabel("NEXORA LAUNCHER");        // Titre        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));        mainPanel.setBackground(CARD_BG);        JPanel mainPanel = new JPanel();        // Panel principal        gbc.insets = new Insets(15, 15, 15, 15);        GridBagConstraints gbc = new GridBagConstraints();        setLayout(new GridBagLayout());        setBackground(DARK_BG);        this.logoutCallback = logoutCallback;        this.launchCallback = launchCallback;    public FuturisticMainPanel(Runnable launchCallback, Runnable logoutCallback) {    private Runnable logoutCallback;    private Runnable launchCallback;    private JLabel versionLabel;    private JLabel userLabel;    private JLabel statusLabel;    private JProgressBar progressBar;    private JButton logoutButton;    private JButton launchButton;    private static final Color CARD_BG = new Color(30, 41, 59);    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);    private static final Color TEXT_PRIMARY = new Color(241, 245, 249);    private static final Color ACCENT_RED = new Color(239, 68, 68);    private static final Color ACCENT_GREEN = new Color(34, 197, 94);    private static final Color ACCENT_BLUE = new Color(59, 130, 246);    private static final Color DARK_BG = new Color(15, 23, 42);public class FuturisticMainPanel extends JPanel {import java.awt.*;import javax.swing.*;import com.azuriom.auth.User;
import com.nexora.launcher.auth.AzAuthManager;
import com.nexora.launcher.auth.AuthenticationException;
import com.nexora.launcher.config.LauncherConfig;
import com.azuriom.auth.User;
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
        subtitleLabel.setFont(new Font("Segoe UI", Font.LIGHT, 24));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(titleLabel);
        centerPanel.add(subtitleLabel);
        centerPanel.add(Box.createVerticalStrut(40));

        // Champ Email
        JLabel emailLabel = new JLabel("Email ou Pseudo");
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
        JLabel passwordLabel = new JLabel("Mot de passe");
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
