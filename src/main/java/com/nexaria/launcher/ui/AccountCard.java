package com.nexaria.launcher.ui;

import com.nexaria.launcher.config.RememberStore;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URI;

/**
 * Carte visuelle pour afficher un compte utilisateur avec avatar
 */
public class AccountCard extends JPanel {
    private final RememberStore.RememberSession session;
    private final String azuriomUrl;
    private final boolean isCurrentAccount;
    private boolean hovered = false;
    private BufferedImage avatar;

    public AccountCard(RememberStore.RememberSession session, String azuriomUrl, boolean isCurrentAccount,
            Runnable onSwitch, Runnable onDelete) {
        this.session = session;
        this.azuriomUrl = azuriomUrl;
        this.isCurrentAccount = isCurrentAccount;

        setOpaque(false);
        setLayout(new BorderLayout(15, 0));
        setPreferredSize(new Dimension(450, 80));
        setMaximumSize(new Dimension(600, 80));
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Panel gauche avec avatar
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);

        // Charger l'avatar de manière asynchrone
        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(60, 60));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Afficher immédiatement l'avatar par défaut
        avatar = createDefaultAvatar();
        avatarLabel.setIcon(new ImageIcon(avatar));

        leftPanel.add(avatarLabel, BorderLayout.CENTER);

        // Charger l'avatar réel en arrière-plan
        loadAvatar(avatarLabel);

        // Panel central avec infos
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel usernameLabel = new JLabel(session.username);
        usernameLabel.setFont(DesignConstants.FONT_HEADER.deriveFont(16f));
        usernameLabel.setForeground(isCurrentAccount ? DesignConstants.PURPLE_ACCENT : DesignConstants.TEXT_PRIMARY);

        JLabel statusLabel = new JLabel(isCurrentAccount ? "Compte actif" : "Compte enregistré");
        statusLabel.setFont(DesignConstants.FONT_REGULAR.deriveFont(12f));
        statusLabel.setForeground(new Color(255, 255, 255, 150));
        statusLabel.setIcon(FontIcon.of(isCurrentAccount ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.USER,
                12, new Color(255, 255, 255, 150)));

        centerPanel.add(usernameLabel);
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(statusLabel);

        // Panel droit avec boutons d'action
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 15));
        rightPanel.setOpaque(false);

        if (!isCurrentAccount && onSwitch != null) {
            JButton switchBtn = createActionButton(FontAwesomeSolid.EXCHANGE_ALT, "Basculer",
                    DesignConstants.PURPLE_ACCENT);
            switchBtn.addActionListener(e -> onSwitch.run());
            rightPanel.add(switchBtn);
        }

        if (onDelete != null) {
            JButton deleteBtn = createActionButton(FontAwesomeSolid.TRASH, "Supprimer",
                    new Color(200, 60, 60));
            deleteBtn.addActionListener(e -> onDelete.run());
            rightPanel.add(deleteBtn);
        }

        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Effet hover
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    private void loadAvatar(JLabel avatarLabel) {
        new Thread(() -> {
            try {
                // Utiliser la même API que la sidebar
                String base = azuriomUrl != null ? azuriomUrl.replaceAll("/+$", "") : "";
                String url = base + "/api/skin-api/avatars/face/" + session.username;

                System.out.println("[AccountCard] Chargement avatar pour: " + session.username);

                // Utiliser le cache
                BufferedImage cachedImage = com.nexaria.launcher.cache.ImageCache.getInstance().get(url);

                if (cachedImage != null && cachedImage.getWidth() > 0 && cachedImage.getHeight() > 0) {
                    System.out.println(
                            "[AccountCard] Avatar chargé: " + cachedImage.getWidth() + "x" + cachedImage.getHeight());
                    BufferedImage realAvatar = createRoundedAvatar(cachedImage, 60);
                    SwingUtilities.invokeLater(() -> {
                        avatarLabel.setIcon(new ImageIcon(realAvatar));
                        System.out.println("[AccountCard] Avatar affiché pour: " + session.username);
                    });
                } else {
                    System.err.println("[AccountCard] Image invalide pour " + session.username);
                }
            } catch (Exception e) {
                // Garder l'avatar par défaut en cas d'erreur
                System.err.println(
                        "[AccountCard] Erreur chargement avatar pour " + session.username + ": " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private BufferedImage createRoundedAvatar(Image img, int size) {
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Clip circulaire
        g2.setClip(new RoundRectangle2D.Float(0, 0, size, size, size, size));
        g2.drawImage(img, 0, 0, size, size, null);

        // Bordure
        g2.setClip(null);
        g2.setColor(isCurrentAccount ? DesignConstants.PURPLE_ACCENT : new Color(255, 255, 255, 100));
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval(1, 1, size - 3, size - 3);

        g2.dispose();
        return output;
    }

    private BufferedImage createDefaultAvatar() {
        BufferedImage img = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fond dégradé
        GradientPaint gp = new GradientPaint(0, 0, new Color(100, 60, 140), 60, 60, new Color(140, 80, 180));
        g2.setPaint(gp);
        g2.fillOval(0, 0, 60, 60);

        // Initiale
        g2.setColor(Color.WHITE);
        g2.setFont(DesignConstants.FONT_HEADER.deriveFont(24f));
        String initial = session.username.substring(0, 1).toUpperCase();
        FontMetrics fm = g2.getFontMetrics();
        int x = (60 - fm.stringWidth(initial)) / 2;
        int y = ((60 - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(initial, x, y);

        g2.dispose();
        return img;
    }

    private JButton createActionButton(FontAwesomeSolid icon, String tooltip, Color color) {
        JButton btn = new JButton(FontIcon.of(icon, 16, color));
        btn.setToolTipText(tooltip);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(36, 36));

        // Effet hover
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setIcon(FontIcon.of(icon, 18, color.brighter()));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setIcon(FontIcon.of(icon, 16, color));
            }
        });

        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fond avec effet glassmorphism
        if (isCurrentAccount) {
            g2.setColor(new Color(80, 50, 110, 200));
        } else if (hovered) {
            g2.setColor(new Color(50, 40, 60, 200));
        } else {
            g2.setColor(new Color(40, 30, 50, 180));
        }
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

        // Bordure
        if (isCurrentAccount) {
            g2.setColor(DesignConstants.PURPLE_ACCENT);
            g2.setStroke(new BasicStroke(2f));
        } else if (hovered) {
            g2.setColor(new Color(170, 80, 255, 100));
            g2.setStroke(new BasicStroke(2f));
        } else {
            g2.setColor(new Color(170, 80, 255, 60));
            g2.setStroke(new BasicStroke(1.5f));
        }
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

        g2.dispose();
        super.paintComponent(g);
    }
}
