package com.nexora.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GradientPanel extends JPanel {
    private Color startColor;
    private Color endColor;
    private int orientation; // 0 = vertical, 1 = horizontal

    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    public GradientPanel(Color startColor, Color endColor, int orientation) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.orientation = orientation;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (orientation == VERTICAL) {
            GradientPaint gp = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        } else {
            GradientPaint gp = new GradientPaint(0, 0, startColor, getWidth(), 0, endColor);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
