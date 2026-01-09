package com.nexaria.launcher.ui;

import com.nexaria.launcher.model.User;
import com.nexaria.launcher.auth.AzAuthManager;
import com.nexaria.launcher.auth.AuthenticationException;
import com.nexaria.launcher.config.LauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class LoginPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(LoginPanel.class);

    private JTextField usernameField;
    private JPasswordField passwordField;
    private ModernButton loginButton;
    private JCheckBox rememberCheck;
    private JLabel statusLabel;
    private Consumer<User> loginCallback;
    private AzAuthManager authManager;
    private JPanel twoFaContainer;

    public LoginPanel(Consumer<User> loginCallback) {
        this.loginCallback = loginCallback;
        this.authManager = new AzAuthManager(LauncherConfig.getInstance().getAzuriomUrl());

        setOpaque(false);
        setLayout(new GridBagLayout());

        GlassPanel glassCard = new GlassPanel(DesignConstants.ROUNDING);
        glassCard.setLayout(new BoxLayout(glassCard, BoxLayout.Y_AXIS));
        glassCard.setBorder(BorderFactory.createEmptyBorder(40, 70, 60, 70));
        glassCard.setPreferredSize(new Dimension(500, 550));

        // Logo centré en haut
        JLabel logoLabel;
        try {
            ImageIcon rawIcon = new ImageIcon(getClass().getResource("/logo.png"));
            Image scaled = rawIcon.getImage().getScaledInstance(180, -1, Image.SCALE_SMOOTH);
            logoLabel = new JLabel(new ImageIcon(scaled));
        } catch (Exception e) {
            logoLabel = new JLabel("Nexaria Launcher");
            logoLabel.setForeground(DesignConstants.PURPLE_ACCENT);
            logoLabel.setFont(DesignConstants.FONT_TITLE.deriveFont(36f));
        }
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        glassCard.add(logoLabel);
        glassCard.add(Box.createVerticalStrut(30));

        JLabel userLabel = new JLabel("Nom d'utilisateur / Email");
        userLabel.setIcon(FontIcon.of(FontAwesomeSolid.USER, 16, DesignConstants.TEXT_PRIMARY));
        userLabel.setFont(DesignConstants.FONT_SMALL);
        userLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        usernameField = createStyledTextField();
        usernameField.setMaximumSize(new Dimension(360, 50));

        glassCard.add(userLabel);
        glassCard.add(Box.createVerticalStrut(8));
        glassCard.add(usernameField);
        glassCard.add(Box.createVerticalStrut(20));

        JLabel passLabel = new JLabel("Mot de passe");
        passLabel.setIcon(FontIcon.of(FontAwesomeSolid.LOCK, 16, DesignConstants.TEXT_PRIMARY));
        passLabel.setFont(DesignConstants.FONT_SMALL);
        passLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = createStyledPasswordField();
        passwordField.setMaximumSize(new Dimension(360, 50));

        glassCard.add(passLabel);
        glassCard.add(Box.createVerticalStrut(8));
        glassCard.add(passwordField);
        glassCard.add(Box.createVerticalStrut(20));

        rememberCheck = new JCheckBox("Se souvenir de moi");
        rememberCheck.setOpaque(false);
        rememberCheck.setForeground(DesignConstants.TEXT_SECONDARY);
        rememberCheck.setFont(DesignConstants.FONT_REGULAR);
        rememberCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
        String remembered = com.nexaria.launcher.config.RememberStore.loadUsername();
        if (remembered != null && !remembered.isEmpty()) {
            usernameField.setText(remembered);
            rememberCheck.setSelected(true);
        }
        glassCard.add(rememberCheck);
        glassCard.add(Box.createVerticalStrut(20));

        loginButton = new ModernButton("CONNEXION", DesignConstants.PURPLE_ACCENT,
            DesignConstants.PURPLE_ACCENT_DARK, true);
        loginButton.setMaximumSize(new Dimension(360, 55));
        loginButton.setFont(DesignConstants.FONT_HEADER.deriveFont(18f));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setIcon(FontIcon.of(FontAwesomeSolid.SIGN_IN_ALT, 18, DesignConstants.TEXT_PRIMARY));
        loginButton.addActionListener(e -> performLogin());

        glassCard.add(loginButton);
        glassCard.add(Box.createVerticalStrut(20));

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(DesignConstants.FONT_SMALL);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        glassCard.add(statusLabel);

        twoFaContainer = new JPanel(new CardLayout());
        twoFaContainer.setOpaque(false);
        twoFaContainer.add(glassCard, "LOGIN");
        add(twoFaContainer);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setOpaque(true);
        field.setBackground(new Color(60, 40, 90, 255));
        field.setForeground(DesignConstants.TEXT_PRIMARY);
        field.setCaretColor(DesignConstants.PURPLE_ACCENT);
        field.setSelectionColor(new Color(DesignConstants.PURPLE_ACCENT.getRed(), DesignConstants.PURPLE_ACCENT.getGreen(), DesignConstants.PURPLE_ACCENT.getBlue(), 80));
        field.setSelectedTextColor(DesignConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        field.setFont(DesignConstants.FONT_REGULAR);
        return field;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setOpaque(true);
        field.setBackground(new Color(60, 40, 90, 255));
        field.setForeground(DesignConstants.TEXT_PRIMARY);
        field.setCaretColor(DesignConstants.PURPLE_ACCENT);
        field.setSelectionColor(new Color(DesignConstants.PURPLE_ACCENT.getRed(), DesignConstants.PURPLE_ACCENT.getGreen(), DesignConstants.PURPLE_ACCENT.getBlue(), 80));
        field.setSelectedTextColor(DesignConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        field.setFont(DesignConstants.FONT_REGULAR);
        return field;
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) { setStatusMessage("Veuillez saisir vos identifiants."); return; }
        String u = username; String p = password;
        SwingWorker<User, String> worker = new SwingWorker<User, String>() {
            long startTime = System.currentTimeMillis();
            @Override protected User doInBackground() throws Exception {
                try {
                    logger.info("[LOGIN] Debut authentification pour: {}", u);
                    publish("Authentification en cours...");
                    return authManager.authenticate(u, p, "");
                }
                catch (AuthenticationException e) {
                    logger.error("[LOGIN] ERREUR: {}", e.getMessage(), e);
                    throw e;
                }
            }
            @Override protected void process(java.util.List<String> chunks) { for (String m : chunks) setStatusMessage(m); }
            @Override protected void done() {
                try {
                    User user = get();
                    long duration = System.currentTimeMillis() - startTime;
                    passwordField.setText("");
                    logger.info("[LOGIN] Succes en {}ms", duration);
                    
                    // Vérifier que l'email est vérifié
                    if (!user.isEmailVerified()) {
                        logger.warn("[LOGIN] Email non verifie pour: {}", u);
                        setStatusMessage("Erreur : Vous devez vérifier votre email pour vous connecter");
                        setButtonsEnabled(true);
                        JOptionPane.showMessageDialog(
                            LoginPanel.this,
                            "Veuillez vérifier votre adresse email avant de vous connecter au launcher.\nConsultez vos emails et cliquez sur le lien de vérification.",
                            "Email non vérifié",
                            JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                    
                    if (rememberCheck.isSelected()) com.nexaria.launcher.config.RememberStore.saveSession(user.getId(), user.getUsername(), user.getAccessToken());
                    else com.nexaria.launcher.config.RememberStore.clear();
                    loginCallback.accept(user);
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof com.nexaria.launcher.auth.TwoFactorRequiredException) {
                        logger.info("[LOGIN] 2FA requis pour: {}", u);
                        showTwoFactor(u, p);
                    } else {
                        logger.error("[LOGIN] ERREUR finale: {}", e.getMessage());
                        setStatusMessage("Erreur : " + e.getMessage());
                        setButtonsEnabled(true);
                    }
                }
            }
        };
        setButtonsEnabled(false);
        worker.execute();
    }

    private void showTwoFactor(String username, String password) {
        TwoFactorPanel twofa = new TwoFactorPanel(
                authManager,
                username,
                password,
                user -> {
                    // Vérifier que l'email est vérifié
                    if (!user.isEmailVerified()) {
                        CardLayout cl = (CardLayout) twoFaContainer.getLayout();
                        cl.show(twoFaContainer, "LOGIN");
                        setStatusMessage("Erreur : Vous devez vérifier votre email pour vous connecter");
                        setButtonsEnabled(true);
                        JOptionPane.showMessageDialog(
                            LoginPanel.this,
                            "Veuillez vérifier votre adresse email avant de vous connecter au launcher.\nConsultez vos emails et cliquez sur le lien de vérification.",
                            "Email non vérifié",
                            JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                    
                    CardLayout cl = (CardLayout) twoFaContainer.getLayout();
                    cl.show(twoFaContainer, "LOGIN");
                    passwordField.setText("");
                    if (rememberCheck.isSelected()) com.nexaria.launcher.config.RememberStore.saveSession(user.getId(), user.getUsername(), user.getAccessToken());
                    else com.nexaria.launcher.config.RememberStore.clear();
                    loginCallback.accept(user);
                },
                () -> {
                    CardLayout cl = (CardLayout) twoFaContainer.getLayout();
                    cl.show(twoFaContainer, "LOGIN");
                    setButtonsEnabled(true);
                    setStatusMessage("2FA annulée.");
                }
        );
        twoFaContainer.add(twofa, "2FA");
        CardLayout cl = (CardLayout) twoFaContainer.getLayout();
        cl.show(twoFaContainer, "2FA");
    }

    public void setStatusMessage(String message) { statusLabel.setText(message); }
    public void setButtonsEnabled(boolean enabled) { loginButton.setEnabled(enabled); usernameField.setEnabled(enabled); passwordField.setEnabled(enabled); }
    public void clearPassword() { passwordField.setText(""); }
    public void resetState() { setButtonsEnabled(true); setStatusMessage(" "); clearPassword(); usernameField.setText(""); }
}
