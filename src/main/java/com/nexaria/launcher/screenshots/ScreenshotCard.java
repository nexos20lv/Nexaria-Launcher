package com.nexaria.launcher.screenshots;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

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

        setPreferredSize(new Dimension(200, 200));
        setMaximumSize(new Dimension(200, 200));
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
                // Créer une miniature 180x180
                thumbnail = createThumbnail(original, 180);
                SwingUtilities.invokeLater(() -> repaint());
            } catch (Exception e) {
                System.err.println("[ScreenshotCard] Erreur chargement: " + e.getMessage());
            }
        }).start();
    }

    private BufferedImage createThumbnail(BufferedImage original, int size) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Calculer les dimensions pour garder le ratio
        double ratio = (double) width / height;
        int thumbWidth, thumbHeight;

        if (ratio > 1) {
            thumbWidth = size;
            thumbHeight = (int) (size / ratio);
        } else {
            thumbHeight = size;
            thumbWidth = (int) (size * ratio);
        }

        BufferedImage thumb = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = thumb.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fond noir
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, size, size);

        // Centrer l'image
        int x = (size - thumbWidth) / 2;
        int y = (size - thumbHeight) / 2;
        g2.drawImage(original, x, y, thumbWidth, thumbHeight, null);
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
            g2.setColor(new Color(60, 40, 80, 200));
        } else {
            g2.setColor(new Color(40, 30, 50, 180));
        }
        g2.fillRoundRect(0, 0, width, height, 15, 15);

        // Bordure
        g2.setColor(hovered ? new Color(170, 80, 255, 150) : new Color(255, 255, 255, 50));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(1, 1, width - 2, height - 2, 15, 15);

        // Dessiner la miniature
        if (thumbnail != null) {
            int thumbX = (width - thumbnail.getWidth()) / 2;
            int thumbY = 10;
            g2.drawImage(thumbnail, thumbX, thumbY, null);
        } else {
            // Placeholder
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(10, 10, width - 20, 140, 10, 10);
            g2.setColor(new Color(255, 255, 255, 100));
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.drawString("Chargement...", 50, 85);
        }

        // Nom du fichier
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        String name = screenshotFile.getName();
        if (name.length() > 25) {
            name = name.substring(0, 22) + "...";
        }
        g2.drawString(name, 10, height - 10);

        g2.dispose();
    }

    public File getFile() {
        return screenshotFile;
    }
}
