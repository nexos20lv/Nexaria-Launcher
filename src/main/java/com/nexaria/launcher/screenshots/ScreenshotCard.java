package com.nexaria.launcher.screenshots;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import com.nexaria.launcher.ui.DesignConstants;

/**
 * Carte pour afficher un screenshot avec miniature et actions
 */
public class ScreenshotCard extends JPanel {
    private File screenshotFile;
    private BufferedImage thumbnail;
    private boolean hovered = false;
    private Runnable onView;
    private Runnable onShare;
    private Runnable onDelete;

    public ScreenshotCard(File file, Runnable onView, Runnable onShare, Runnable onDelete) {
        this.screenshotFile = file;
        this.onView = onView;
        this.onShare = onShare;
        this.onDelete = onDelete;

        setPreferredSize(new Dimension(240, 170)); // Format paysage plus large
        setMaximumSize(new Dimension(240, 170));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Charger la miniature
        loadThumbnail();

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

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onView != null) {
                    onView.run();
                }
            }
        });
    }

    private void loadThumbnail() {
        new Thread(() -> {
            try {
                BufferedImage original = ImageIO.read(screenshotFile);
                // Créer une miniature ~220px de large
                thumbnail = createThumbnail(original, 220, 130);
                SwingUtilities.invokeLater(() -> repaint());
            } catch (Exception e) {
                System.err.println("[ScreenshotCard] Erreur chargement: " + e.getMessage());
            }
        }).start();
    }

    private BufferedImage createThumbnail(BufferedImage original, int targetW, int targetH) {
        BufferedImage thumb = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = thumb.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Resize "cover" style (remplir tout)
        double ratioW = (double) targetW / original.getWidth();
        double ratioH = (double) targetH / original.getHeight();
        double ratio = Math.max(ratioW, ratioH);

        int w = (int) (original.getWidth() * ratio);
        int h = (int) (original.getHeight() * ratio);
        int x = (targetW - w) / 2;
        int y = (targetH - h) / 2;

        g2.drawImage(original, x, y, w, h, null);
        g2.dispose();

        return thumb;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Fond avec glassmorphism
        if (hovered) {
            g2.setColor(new Color(70, 50, 90, 220));
        } else {
            g2.setColor(new Color(40, 30, 50, 180));
        }
        g2.fillRoundRect(0, 0, width, height, 12, 12);

        // Bordure
        g2.setColor(hovered ? new Color(170, 80, 255, 180) : new Color(255, 255, 255, 40));
        g2.setStroke(new BasicStroke(hovered ? 1.5f : 1f));
        g2.drawRoundRect(0, 0, width - 1, height - 1, 12, 12);

        // Dessiner la miniature
        int thumbX = 10;
        int thumbY = 10;
        int thumbW = width - 20;
        int thumbH = height - 40; // Espace pour le texte en bas

        // Zone image
        Shape oldClip = g2.getClip();
        g2.setClip(new java.awt.geom.RoundRectangle2D.Float(thumbX, thumbY, thumbW, thumbH, 8, 8));

        if (thumbnail != null) {
            g2.drawImage(thumbnail, thumbX, thumbY, thumbW, thumbH, null);
        } else {
            // Placeholder
            g2.setColor(new Color(255, 255, 255, 20));
            g2.fillRect(thumbX, thumbY, thumbW, thumbH);
        }
        g2.setClip(oldClip);

        // Nom du fichier
        g2.setColor(Color.WHITE);
        g2.setFont(DesignConstants.FONT_REGULAR.deriveFont(11f));
        String name = screenshotFile.getName();

        // Truncate name logic
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(name) > width - 25) {
            while (fm.stringWidth(name + "...") > width - 25 && name.length() > 0) {
                name = name.substring(0, name.length() - 1);
            }
            name += "...";
        }

        g2.drawString(name, 12, height - 12);

        g2.dispose();
    }

    public File getFile() {
        return screenshotFile;
    }
}
