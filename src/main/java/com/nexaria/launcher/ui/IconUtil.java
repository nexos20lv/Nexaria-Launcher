package com.nexaria.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

public class IconUtil {
    public static class ShapeIcon implements Icon {
        private final int size;
        private final Color color;
        private final ShapePainter painter;

        @FunctionalInterface
        public interface ShapePainter { void paint(Graphics2D g2, int s); }

        public ShapeIcon(int size, Color color, ShapePainter painter) {
            this.size = size;
            this.color = color;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                g2.setColor(color);
                painter.paint(g2, size);
            } finally {
                g2.dispose();
            }
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    public static Icon lock(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect((int)(s*0.2), (int)(s*0.45), (int)(s*0.6), (int)(s*0.4), (int)(s*0.2), (int)(s*0.2));
            g2.drawOval((int)(s*0.35), (int)(s*0.12), (int)(s*0.3), (int)(s*0.3));
        });
    }

    // Icônes personnalisées pour le système de mise à jour
    public static Icon getCheckIcon(Color color) {
        return new ShapeIcon(20, color, (g2, s) -> {
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval((int)(s*0.1), (int)(s*0.1), (int)(s*0.8), (int)(s*0.8));
            g2.drawLine((int)(s*0.3), (int)(s*0.5), (int)(s*0.45), (int)(s*0.65));
            g2.drawLine((int)(s*0.45), (int)(s*0.65), (int)(s*0.75), (int)(s*0.3));
        });
    }

    public static Icon getDownloadIcon(Color color) {
        return new ShapeIcon(20, color, (g2, s) -> {
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = s / 2;
            g2.drawLine(cx, (int)(s*0.2), cx, (int)(s*0.55));
            g2.drawLine((int)(s*0.3), (int)(s*0.5), cx, (int)(s*0.65));
            g2.drawLine(cx, (int)(s*0.65), (int)(s*0.7), (int)(s*0.5));
            g2.drawRect((int)(s*0.2), (int)(s*0.7), (int)(s*0.6), (int)(s*0.15));
        });
    }

    public static Icon getSearchIcon(Color color) {
        return new ShapeIcon(20, color, (g2, s) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval((int)(s*0.15), (int)(s*0.15), (int)(s*0.5), (int)(s*0.5));
            g2.drawLine((int)(s*0.6), (int)(s*0.6), (int)(s*0.85), (int)(s*0.85));
        });
    }

    public static Icon getErrorIcon(Color color) {
        return new ShapeIcon(20, color, (g2, s) -> {
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval((int)(s*0.1), (int)(s*0.1), (int)(s*0.8), (int)(s*0.8));
            g2.drawLine((int)(s*0.3), (int)(s*0.3), (int)(s*0.7), (int)(s*0.7));
            g2.drawLine((int)(s*0.7), (int)(s*0.3), (int)(s*0.3), (int)(s*0.7));
        });
    }
}
