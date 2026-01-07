package com.nexora.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {
    private boolean isHovered = false;
    private Color normalColor;
    private Color hoverColor;
    private Color startGradient; // Optional gradient
    private Color endGradient; // Optional gradient

    // Pulse Effect
    private float pulseAlpha = 0f;
    private Timer pulseTimer;
    private boolean pulseGrowing = true;

    public ModernButton(String text) {
        this(text, DesignConstants.PURPLE_ACCENT, DesignConstants.PURPLE_ACCENT_DARK);
    }

    public ModernButton(String text, Color normalColor, Color hoverColor) {
        super(text);
        this.normalColor = normalColor;
        this.hoverColor = hoverColor;
        init();
    }

    // Gradient Constructor
    public ModernButton(String text, Color startGradient, Color endGradient, boolean gradient) {
        super(text);
        this.startGradient = startGradient;
        this.endGradient = endGradient;
        this.normalColor = startGradient;
        this.hoverColor = endGradient;
        init();
    }

    private void init() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(DesignConstants.FONT_REGULAR);
        setForeground(DesignConstants.TEXT_PRIMARY);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        pulseTimer = new Timer(50, e -> {
            if (isHovered) {
                if (pulseGrowing) {
                    pulseAlpha += 0.05f;
                    if (pulseAlpha >= 0.4f)
                        pulseGrowing = false;
                } else {
                    pulseAlpha -= 0.05f;
                    if (pulseAlpha <= 0f)
                        pulseGrowing = true;
                }
                repaint();
            } else {
                pulseAlpha = 0f;
                pulseTimer.stop();
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                pulseGrowing = true;
                pulseAlpha = 0f;
                pulseTimer.start();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                pulseTimer.stop();
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Pill Shape: Radius = Height
        int r = getHeight();

        // Background
        if (startGradient != null && endGradient != null) {
            GradientPaint gp = new GradientPaint(0, 0, startGradient, getWidth(), 0, endGradient);
            g2d.setPaint(gp);
        } else {
            g2d.setColor(isHovered ? hoverColor : normalColor);
        }
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);

        // Pulse / Glow Overlay
        if (isHovered && pulseAlpha > 0) {
            g2d.setColor(new Color(1f, 1f, 1f, pulseAlpha));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
        }

        // Border Glow
        if (isHovered) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, r, r);
        }

        super.paintComponent(g);
    }
}
