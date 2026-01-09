package com.nexaria.launcher.ui;

import com.nexaria.launcher.model.User;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Sidebar extends JPanel {
    private String activeRoute = "ACCUEIL";
    private Consumer<String> onNavigate;
    @SuppressWarnings("unused")
    private Runnable logoutCallback;
    private Map<String, ModernButton> buttons = new HashMap<>();
    private JLabel avatarLabel;
    private User currentUser;

    public Sidebar(Consumer<String> onNavigate, Runnable logoutCallback) {
        this.onNavigate = onNavigate;
        this.logoutCallback = logoutCallback;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(90, 750));
        setBorder(BorderFactory.createEmptyBorder(40, 15, 40, 15));

        Icon homeIcon = FontIcon.of(FontAwesomeSolid.HOME, 22, DesignConstants.TEXT_PRIMARY);
        add(createNavButton("ACCUEIL", homeIcon, "Accueil"));
        add(Box.createVerticalStrut(18));
        Icon shieldIcon = FontIcon.of(FontAwesomeSolid.SHIELD_ALT, 22, DesignConstants.TEXT_PRIMARY);
        add(createNavButton("SÉCURITÉ", shieldIcon, "Sécurité"));
        add(Box.createVerticalStrut(18));
        Icon settingsIcon = FontIcon.of(FontAwesomeSolid.COG, 22, DesignConstants.TEXT_PRIMARY);
        add(createNavButton("PARAMÈTRES", settingsIcon, "Paramètres"));

        add(Box.createVerticalGlue());

        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(56, 56));
        avatarLabel.setMaximumSize(new Dimension(56, 56));
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        avatarLabel.setOpaque(false);
        avatarLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        avatarLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (currentUser == null) return;
                JPopupMenu menu = new JPopupMenu();
                
                // Nom d'utilisateur avec rôle
                String title = currentUser.getUsername();
                if (currentUser.getRoleName() != null && !currentUser.getRoleName().isEmpty()) {
                    title += " (" + currentUser.getRoleName() + ")";
                }
                JMenuItem info = new JMenuItem(title);
                info.setEnabled(false);
                info.setIcon(FontIcon.of(FontAwesomeSolid.USER, 14, DesignConstants.TEXT_SECONDARY));
                menu.add(info);
                
                // ID
                JMenuItem idItem = new JMenuItem("ID : " + currentUser.getId());
                idItem.setEnabled(false);
                idItem.setIcon(FontIcon.of(FontAwesomeSolid.ID_CARD, 14, DesignConstants.TEXT_SECONDARY));
                menu.add(idItem);
                
                // Argent
                JMenuItem moneyItem = new JMenuItem(String.format("Argent : %.2f", currentUser.getMoney()));
                moneyItem.setEnabled(false);
                moneyItem.setIcon(FontIcon.of(FontAwesomeSolid.COINS, 14, new Color(255, 215, 0)));
                menu.add(moneyItem);
                
                menu.addSeparator();
                
                // Déconnexion
                JMenuItem logout = new JMenuItem("Se déconnecter");
                logout.setIcon(FontIcon.of(FontAwesomeSolid.SIGN_OUT_ALT, 14, DesignConstants.PURPLE_ACCENT));
                logout.addActionListener(ev -> {
                    if (logoutCallback != null) logoutCallback.run();
                });
                menu.add(logout);
                
                menu.show(avatarLabel, e.getX(), e.getY());
            }
        });

        add(avatarLabel);
    }

    private JButton createNavButton(String route, Icon icon, String tooltip) {
        ModernButton btn = new ModernButton("", new Color(255, 255, 255, 14), new Color(255, 255, 255, 40));
        btn.setIcon(icon);
        btn.setToolTipText(tooltip);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setVerticalTextPosition(SwingConstants.CENTER);
        btn.setMaximumSize(new Dimension(56, 56));
        btn.setPreferredSize(new Dimension(56, 56));
        btn.setFont(DesignConstants.FONT_HEADER);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn.addActionListener(e -> {
            setActive(route);
            onNavigate.accept(route);
        });

        buttons.put(route, btn);
        return btn;
    }

    public void setActive(String route) {
        this.activeRoute = route;
        repaint();
    }

    public void setUserProfile(User user, String azuriomUrl) {
        this.currentUser = user;
        if (user == null) {
            avatarLabel.setIcon(null);
            avatarLabel.setText("");
            return;
        }
        String url = buildAvatarUrl(azuriomUrl, user);
        try {
            ImageIcon raw = new ImageIcon(URI.create(url).toURL());
            BufferedImage rounded = createRoundedAvatar(raw.getImage(), 48);
            avatarLabel.setIcon(new ImageIcon(rounded));
            avatarLabel.setText("");
        } catch (Exception ex) {
            avatarLabel.setIcon(null);
            avatarLabel.setText(user.getUsername());
            avatarLabel.setToolTipText("Avatar indisponible");
        }
    }

    private BufferedImage createRoundedAvatar(Image img, int size) {
        // Créer une image carrée avec canal alpha
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        
        // Anti-aliasing pour des bords lisses
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Dessiner un cercle blanc comme masque
        g2.setColor(Color.WHITE);
        g2.fillOval(0, 0, size, size);
        
        // Utiliser le masque pour ne garder que la partie circulaire de l'image
        g2.setComposite(AlphaComposite.SrcIn);
        g2.drawImage(img, 0, 0, size, size, null);
        
        g2.dispose();
        return output;
    }

    public void clearUserProfile() {
        this.currentUser = null;
        avatarLabel.setIcon(null);
        avatarLabel.setText("");
    }

    private String buildAvatarUrl(String baseUrl, User user) {
        String base = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "";
        String username = user.getUsername();
        String identifier = (username != null && !username.isBlank()) ? username : user.getId();
        return base + "/api/skin-api/avatars/face/" + identifier;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Glass Dock - Full Height rounded strip
        g2d.setColor(DesignConstants.GLASS_BACKGROUND);
        g2d.fillRoundRect(10, 20, getWidth() - 20, getHeight() - 40, 30, 30);

        g2d.setColor(DesignConstants.GLASS_BORDER);
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRoundRect(10, 20, getWidth() - 20, getHeight() - 40, 30, 30);

        // Indicator
        ModernButton activeBtn = buttons.get(activeRoute);
        if (activeBtn != null) {
            Container parent = activeBtn.getParent();
            if (parent != null) {
                int btnW = activeBtn.getWidth();
                int btnH = activeBtn.getHeight();
                if (btnW > 0 && btnH > 0) {
                    // Position du bouton dans la sidebar (insets inclus)
                    Point btnOrigin = SwingUtilities.convertPoint(parent, activeBtn.getLocation(), this);
                    int cy = btnOrigin.y + btnH / 2;
                    int cx = getWidth() / 2; // centrage horizontal exact dans la sidebar

                    int radius = Math.max(btnW, btnH) / 2 + 10;
                    int ringR = Math.max(radius - 6, 6);

                    try {
                        // Cercle dégradé (halo)
                        java.awt.RadialGradientPaint rg = new java.awt.RadialGradientPaint(
                                new Point(cx, cy),
                                radius,
                                new float[]{0f, 0.65f, 1f},
                                new Color[]{
                                        new Color(255, 255, 255, 220),
                                        new Color(170, 80, 255, 90),
                                        new Color(170, 80, 255, 0)
                                });
                        g2d.setPaint(rg);
                        g2d.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
                    } catch (IllegalArgumentException ignored) {
                        // Ignore gradient issues to avoid breaking UI when size is transiently zero
                    }

                    // Anneau fin
                    g2d.setColor(new Color(255, 255, 255, 140));
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawOval(cx - ringR, cy - ringR, ringR * 2, ringR * 2);

                    // Point central
                    g2d.setColor(new Color(255, 255, 255, 230));
                    g2d.fillOval(cx - 6, cy - 6, 12, 12);
                }
            }
        }

        super.paintComponent(g);
    }
}
