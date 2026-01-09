package com.nexaria.launcher.ui;

import com.nexaria.launcher.auth.AzAuthManager;
import com.nexaria.launcher.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class TwoFactorPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(TwoFactorPanel.class);
    private final AzAuthManager authManager;
    private final String username;
    private final String password;
    private final Consumer<User> onSuccess;
    @SuppressWarnings("unused")
    private final Runnable onCancel;

    private JTextField codeField;
    private ModernButton confirmButton;
    private ModernButton cancelButton;
    private JLabel statusLabel;

    public TwoFactorPanel(AzAuthManager authManager, String username, String password,
                          Consumer<User> onSuccess, Runnable onCancel) {
        this.authManager = authManager;
        this.username = username;
        this.password = password;
        this.onSuccess = onSuccess;
        this.onCancel = onCancel;

        setOpaque(false);
        setLayout(new GridBagLayout());

        GlassPanel card = new GlassPanel(DesignConstants.ROUNDING);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        card.setPreferredSize(new Dimension(500, 300));

        JLabel title = new JLabel("VÉRIFICATION 2FA");
        title.setFont(DesignConstants.FONT_TITLE);
        title.setForeground(DesignConstants.PURPLE_ACCENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SHIELD_ALT, 
            24, 
            DesignConstants.PURPLE_ACCENT
        ));
        card.add(title);
        card.add(Box.createVerticalStrut(20));

        JLabel info = new JLabel("Entrez le code de votre application d'authentification.");
        info.setFont(DesignConstants.FONT_SMALL);
        info.setForeground(DesignConstants.TEXT_SECONDARY);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(info);
        card.add(Box.createVerticalStrut(12));

        codeField = new JTextField();
        codeField.setOpaque(true);
        codeField.setBackground(new Color(60, 40, 90, 255));
        codeField.setForeground(DesignConstants.TEXT_PRIMARY);
        codeField.setCaretColor(DesignConstants.PURPLE_ACCENT);
        codeField.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        codeField.setMaximumSize(new Dimension(300, 40));
        codeField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(codeField);
        card.add(Box.createVerticalStrut(16));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actions.setOpaque(false);
        confirmButton = new ModernButton("VALIDER", DesignConstants.PURPLE_ACCENT, DesignConstants.PURPLE_ACCENT_DARK, true);
        confirmButton.setPreferredSize(new Dimension(140, 40));
        confirmButton.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHECK, 
            16, 
            DesignConstants.TEXT_PRIMARY
        ));
        confirmButton.addActionListener(e -> submit());
        cancelButton = new ModernButton("RETOUR", new Color(255,255,255,30), new Color(255,255,255,50));
        cancelButton.setPreferredSize(new Dimension(120, 40));
        cancelButton.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.ARROW_LEFT, 
            16, 
            DesignConstants.TEXT_PRIMARY
        ));
        cancelButton.addActionListener(e -> onCancel.run());
        actions.add(confirmButton);
        actions.add(cancelButton);
        card.add(actions);

        card.add(Box.createVerticalStrut(10));
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(DesignConstants.FONT_SMALL);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(statusLabel);

        add(card);
    }

    private void submit() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) { statusLabel.setText("Veuillez entrer le code."); return; }
        confirmButton.setEnabled(false);
        SwingWorker<User, Void> worker = new SwingWorker<User, Void>(){
            long startTime = System.currentTimeMillis();
            @Override protected User doInBackground() throws Exception {
                logger.info("[2FA] Verification du code");
                return authManager.authenticate(username, password, code);
            }
            @Override protected void done() {
                try {
                    User user = get();
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("[2FA] OK en {}ms", duration);
                    onSuccess.accept(user);
                }
                catch (Exception e) {
                    logger.error("[2FA] ERREUR: {}", e.getMessage());
                    statusLabel.setText("Code invalide ou erreur. Reessayez.");
                    confirmButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }
}