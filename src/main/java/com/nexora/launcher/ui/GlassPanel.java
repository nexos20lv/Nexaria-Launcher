package com.nexora.launcher.ui;

import javax.swing.*;
import java.awt.*;

public class GlassPanel extends JPanel {
    private int cornerRadius;
    private Color backgroundColor;
    private Color borderColor;

    public GlassPanel() {
        this(DesignConstants.ROUNDING);
    }

    public GlassPanel(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        this.backgroundColor = DesignConstants.GLASS_BACKGROUND;
        this.borderColor = DesignConstants.GLASS_BORDER;
        setOpaque(false);
    }

    public void setGlassColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background
        g2d.setColor(backgroundColor);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // Gradient Wash (Light Source from top-left)
        GradientPaint wash = new GradientPaint(
                0, 0, new Color(255, 255, 255, 20),
                getWidth(), getHeight(), new Color(255, 255, 255, 0));
        g2d.setPaint(wash);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // Draw border
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        g2d.dispose();
    }
}
