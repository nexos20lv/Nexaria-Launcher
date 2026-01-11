package com.nexaria.launcher.ui;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

/**
 * Bouton de lancement avec animations (hover scale + pulse)
 */
public class AnimatedLaunchButton extends ModernButton {
    private float scale = 1.0f;
    private float targetScale = 1.0f;
    private Timer scaleTimer;
    private Timer pulseTimer;
    private float pulseAlpha = 0.0f;
    private boolean pulseEnabled = false;

    public AnimatedLaunchButton(String text, Color bgColor, Color hoverColor, boolean filled) {
        super(text, bgColor, hoverColor, filled);

        // Timer pour l'animation de scale
        scaleTimer = new Timer(16, e -> { // ~60 FPS
            if (Math.abs(scale - targetScale) > 0.01f) {
                scale += (targetScale - scale) * 0.3f;
                repaint();
            }
        });
        scaleTimer.start();

        // Timer pour l'animation de pulse
        pulseTimer = new Timer(30, e -> {
            if (pulseEnabled) {
                pulseAlpha += 0.05f;
                if (pulseAlpha > 1.0f) {
                    pulseAlpha = 0.0f;
                }
                repaint();
            }
        });
        pulseTimer.start();

        // Effet hover
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    targetScale = 1.05f;
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                targetScale = 1.0f;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    targetScale = 0.98f;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled() && contains(e.getPoint())) {
                    targetScale = 1.05f;
                } else {
                    targetScale = 1.0f;
                }
            }
        });
    }

    /**
     * Active/désactive l'effet de pulse (quand le jeu est prêt)
     */
    public void setPulseEnabled(boolean enabled) {
        this.pulseEnabled = enabled;
        if (!enabled) {
            pulseAlpha = 0.0f;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Appliquer le scale
        if (scale != 1.0f) {
            AffineTransform at = new AffineTransform();
            at.translate(width / 2.0, height / 2.0);
            at.scale(scale, scale);
            at.translate(-width / 2.0, -height / 2.0);
            g2.setTransform(at);
        }

        // Dessiner l'effet de pulse (glow externe)
        if (pulseEnabled && pulseAlpha > 0) {
            int glowSize = (int) (10 * pulseAlpha);
            int alpha = (int) (100 * (1 - pulseAlpha));
            g2.setColor(new Color(170, 80, 255, alpha));
            g2.setStroke(new BasicStroke(glowSize));
            g2.drawRoundRect(
                    -glowSize / 2,
                    -glowSize / 2,
                    width + glowSize,
                    height + glowSize,
                    20,
                    20);
        }

        g2.dispose();

        // Dessiner le bouton normal
        super.paintComponent(g);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            targetScale = 1.0f;
            scale = 1.0f;
            pulseEnabled = false;
        }
    }

    /**
     * Nettoyer les timers quand le composant est détruit
     */
    public void cleanup() {
        if (scaleTimer != null) {
            scaleTimer.stop();
        }
        if (pulseTimer != null) {
            pulseTimer.stop();
        }
    }
}
