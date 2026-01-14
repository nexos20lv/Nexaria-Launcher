package com.nexaria.launcher.ui.notification;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Gestionnaire de notifications toast non-bloquantes pour l'interface utilisateur.
 * Affiche des notifications élégantes qui s'animent et disparaissent automatiquement.
 */
public class ToastNotificationManager {
    private static final int TOAST_WIDTH = 350;
    private static final int TOAST_HEIGHT = 80;
    private static final int TOAST_SPACING = 10;
    private static final int ANIMATION_DURATION_MS = 300;
    private static final int DISPLAY_DURATION_MS = 4000;

    private static ToastNotificationManager instance;
    private final ConcurrentLinkedQueue<ToastNotification> activeToasts = new ConcurrentLinkedQueue<>();
    private final Point screenPosition;

    private ToastNotificationManager() {
        // Positionner les toasts en bas à droite par défaut
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        Rectangle bounds = gd.getDefaultConfiguration().getBounds();
        
        this.screenPosition = new Point(
                bounds.x + bounds.width - TOAST_WIDTH - 20,
                bounds.y + bounds.height - TOAST_HEIGHT - 60
        );
    }

    public static ToastNotificationManager getInstance() {
        if (instance == null) {
            synchronized (ToastNotificationManager.class) {
                if (instance == null) {
                    instance = new ToastNotificationManager();
                }
            }
        }
        return instance;
    }

    /**
     * Affiche une notification d'information.
     */
    public void showInfo(String title, String message) {
        show(title, message, ToastType.INFO);
    }

    /**
     * Affiche une notification de succès.
     */
    public void showSuccess(String title, String message) {
        show(title, message, ToastType.SUCCESS);
    }

    /**
     * Affiche une notification d'avertissement.
     */
    public void showWarning(String title, String message) {
        show(title, message, ToastType.WARNING);
    }

    /**
     * Affiche une notification d'erreur.
     */
    public void showError(String title, String message) {
        show(title, message, ToastType.ERROR);
    }

    /**
     * Affiche une notification personnalisée.
     */
    public void show(String title, String message, ToastType type) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(title, message, type);
            activeToasts.add(toast);
            positionToast(toast);
            toast.show();
        });
    }

    /**
     * Positionne un toast en fonction des autres toasts actifs.
     */
    private void positionToast(ToastNotification toast) {
        int yOffset = 0;
        for (ToastNotification activeToast : activeToasts) {
            if (activeToast != toast && activeToast.isVisible()) {
                yOffset += TOAST_HEIGHT + TOAST_SPACING;
            }
        }
        
        int x = screenPosition.x;
        int y = screenPosition.y - yOffset;
        toast.setLocation(x, y);
    }

    /**
     * Retire un toast de la liste des toasts actifs.
     */
    private void removeToast(ToastNotification toast) {
        activeToasts.remove(toast);
        repositionToasts();
    }

    /**
     * Repositionne tous les toasts actifs.
     */
    private void repositionToasts() {
        SwingUtilities.invokeLater(() -> {
            int yOffset = 0;
            for (ToastNotification toast : activeToasts) {
                if (toast.isVisible()) {
                    int targetY = screenPosition.y - yOffset;
                    animateToPosition(toast, targetY);
                    yOffset += TOAST_HEIGHT + TOAST_SPACING;
                }
            }
        });
    }

    /**
     * Anime le déplacement d'un toast vers une nouvelle position.
     */
    private void animateToPosition(ToastNotification toast, int targetY) {
        Timer animator = new Timer(10, null);
        animator.addActionListener(e -> {
            int currentY = toast.getY();
            int diff = targetY - currentY;
            
            if (Math.abs(diff) < 2) {
                toast.setLocation(toast.getX(), targetY);
                animator.stop();
            } else {
                toast.setLocation(toast.getX(), currentY + diff / 5);
            }
        });
        animator.start();
    }

    /**
     * Représente une notification toast individuelle.
     */
    private class ToastNotification extends JWindow {
        private final String title;
        private final String message;
        private final ToastType type;
        private float opacity = 0.0f;

        public ToastNotification(String title, String message, ToastType type) {
            this.title = title;
            this.message = message;
            this.type = type;
            
            setSize(TOAST_WIDTH, TOAST_HEIGHT);
            setAlwaysOnTop(true);
            
            // Rendre la fenêtre transparente
            try {
                setOpacity(opacity);
            } catch (Exception e) {
                // Ignore si la transparence n'est pas supportée
            }
            
            initUI();
        }

        private void initUI() {
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BorderLayout(10, 5));
            contentPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(type.getBorderColor(), 2),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            contentPanel.setBackground(type.getBackgroundColor());

            // Icône
            JLabel iconLabel = new JLabel(type.getIcon());
            iconLabel.setFont(new Font("Arial", Font.BOLD, 24));
            iconLabel.setForeground(type.getForegroundColor());
            contentPanel.add(iconLabel, BorderLayout.WEST);

            // Contenu
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
            titleLabel.setForeground(type.getForegroundColor());
            textPanel.add(titleLabel);

            JLabel messageLabel = new JLabel("<html>" + message + "</html>");
            messageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            messageLabel.setForeground(type.getForegroundColor().brighter());
            textPanel.add(messageLabel);

            contentPanel.add(textPanel, BorderLayout.CENTER);

            // Bouton fermer
            JLabel closeLabel = new JLabel("✕");
            closeLabel.setFont(new Font("Arial", Font.BOLD, 16));
            closeLabel.setForeground(type.getForegroundColor());
            closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    hide();
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    closeLabel.setForeground(type.getForegroundColor().brighter());
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    closeLabel.setForeground(type.getForegroundColor());
                }
            });
            contentPanel.add(closeLabel, BorderLayout.EAST);

            setContentPane(contentPanel);
        }

        public void show() {
            setVisible(true);
            fadeIn();
            
            // Auto-hide après le délai
            Timer hideTimer = new Timer(DISPLAY_DURATION_MS, e -> hide());
            hideTimer.setRepeats(false);
            hideTimer.start();
        }

        private void fadeIn() {
            Timer fadeInTimer = new Timer(20, null);
            fadeInTimer.addActionListener(e -> {
                opacity += 0.05f;
                if (opacity >= 0.95f) {
                    opacity = 0.95f;
                    fadeInTimer.stop();
                }
                try {
                    setOpacity(opacity);
                } catch (Exception ex) {
                    // Ignore
                }
            });
            fadeInTimer.start();
        }

        public void hide() {
            Timer fadeOutTimer = new Timer(20, null);
            fadeOutTimer.addActionListener(e -> {
                opacity -= 0.1f;
                if (opacity <= 0.0f) {
                    opacity = 0.0f;
                    fadeOutTimer.stop();
                    dispose();
                    removeToast(this);
                }
                try {
                    setOpacity(opacity);
                } catch (Exception ex) {
                    // Ignore
                }
            });
            fadeOutTimer.start();
        }
    }

    /**
     * Types de notifications avec leurs couleurs.
     */
    public enum ToastType {
        INFO("ℹ", new Color(33, 150, 243), new Color(227, 242, 253), Color.WHITE),
        SUCCESS("✓", new Color(76, 175, 80), new Color(232, 245, 233), Color.WHITE),
        WARNING("⚠", new Color(255, 152, 0), new Color(255, 243, 224), Color.WHITE),
        ERROR("✗", new Color(244, 67, 54), new Color(255, 235, 238), Color.WHITE);

        private final String icon;
        private final Color borderColor;
        private final Color backgroundColor;
        private final Color foregroundColor;

        ToastType(String icon, Color borderColor, Color backgroundColor, Color foregroundColor) {
            this.icon = icon;
            this.borderColor = borderColor;
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
        }

        public String getIcon() { return icon; }
        public Color getBorderColor() { return borderColor; }
        public Color getBackgroundColor() { return backgroundColor; }
        public Color getForegroundColor() { return foregroundColor; }
    }
}
