package com.nexora.launcher.ui;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {
    private Color startColor;
    private Color endColor;
    private int orientation; // 0 = vertical, 1 = horizontal, 2 = diagonal
    private int cornerRadius = 0;

    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;
    public static final int DIAGONAL = 2; // Top-Left to Bottom-Right

    public GradientPanel(Color startColor, Color endColor, int orientation) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.orientation = orientation;
    }

    public GradientPanel(Color startColor, Color endColor, int orientation, int cornerRadius) {
        this(startColor, endColor, orientation);
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (cornerRadius == 0) {
            super.paintComponent(g);
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gp;
        if (orientation == VERTICAL) {
            gp = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
        } else if (orientation == HORIZONTAL) {
            gp = new GradientPaint(0, 0, startColor, getWidth(), 0, endColor);
        } else { // DIAGONAL
            gp = new GradientPaint(0, 0, startColor, getWidth(), getHeight(), endColor);
        }

        g2d.setPaint(gp);

        if (cornerRadius > 0) {
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        } else {
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
