package com.nexaria.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

public class IconUtil {
    public static class ShapeIcon implements Icon {
        private final int size;
        private final Color color;
        private final Painter painter;

        public interface Painter { void paint(Graphics2D g2, int s); }

        public ShapeIcon(int size, Color color, Painter painter) {
            this.size = size; this.color = color; this.painter = painter;
        }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(color);
            painter.paint(g2, size);
            g2.dispose();
        }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    public static Icon play(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            GeneralPath p = new GeneralPath();
            p.moveTo(s*0.2, s*0.1); p.lineTo(s*0.2, s*0.9); p.lineTo(s*0.85, s*0.5); p.closePath();
            g2.fill(p);
        });
    }
    public static Icon user(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            g2.fillOval((int)(s*0.25), (int)(s*0.1), (int)(s*0.5), (int)(s*0.5));
            g2.fillRoundRect((int)(s*0.15), (int)(s*0.6), (int)(s*0.7), (int)(s*0.3), (int)(s*0.2), (int)(s*0.2));
        });
    }
    public static Icon settings(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            int r = (int)(s*0.35); int cx = s/2; int cy = s/2;
            for (int i=0; i<8; i++) {
                double a = i*Math.PI/4;
                int x = (int)(cx + Math.cos(a)*r);
                int y = (int)(cy + Math.sin(a)*r);
                g2.fillOval(x- (int)(s*0.08), y- (int)(s*0.08), (int)(s*0.16), (int)(s*0.16));
            }
            g2.fillOval(cx- (int)(s*0.23), cy- (int)(s*0.23), (int)(s*0.46), (int)(s*0.46));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 160));
            g2.fillOval(cx- (int)(s*0.12), cy- (int)(s*0.12), (int)(s*0.24), (int)(s*0.24));
        });
    }

    public static Icon home(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.1, s * 0.55);
            p.lineTo(s * 0.5, s * 0.15);
            p.lineTo(s * 0.9, s * 0.55);
            p.lineTo(s * 0.9, s * 0.9);
            p.lineTo(s * 0.6, s * 0.9);
            p.lineTo(s * 0.6, s * 0.68);
            p.lineTo(s * 0.4, s * 0.68);
            p.lineTo(s * 0.4, s * 0.9);
            p.lineTo(s * 0.1, s * 0.9);
            p.closePath();
            g2.fill(p);
        });
    }
    public static Icon logout(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            g2.fillRoundRect((int)(s*0.1), (int)(s*0.2), (int)(s*0.45), (int)(s*0.6), (int)(s*0.15), (int)(s*0.15));
            GeneralPath p = new GeneralPath();
            p.moveTo(s*0.45, s*0.3); p.lineTo(s*0.9, s*0.5); p.lineTo(s*0.45, s*0.7);
            g2.setStroke(new BasicStroke((float)(s*0.08), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(p);
        });
    }
    public static Icon close(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            g2.setStroke(new BasicStroke((float)(s*0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int)(s*0.2),(int)(s*0.2),(int)(s*0.8),(int)(s*0.8));
            g2.drawLine((int)(s*0.2),(int)(s*0.8),(int)(s*0.8),(int)(s*0.2));
        });
    }
    public static Icon minimize(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            g2.fillRoundRect((int)(s*0.2), (int)(s*0.7), (int)(s*0.6), (int)(s*0.12), (int)(s*0.2), (int)(s*0.2));
        });
    }
    public static Icon mail(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            g2.drawRoundRect((int)(s*0.1), (int)(s*0.2), (int)(s*0.8), (int)(s*0.6), (int)(s*0.2), (int)(s*0.2));
            GeneralPath p = new GeneralPath();
            p.moveTo(s*0.1, s*0.2); p.lineTo(s*0.5, s*0.5); p.lineTo(s*0.9, s*0.2);
            g2.setStroke(new BasicStroke((float)(s*0.08), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(p);
        });
    }
    public static Icon lock(int size, Color color) {
        return new ShapeIcon(size, color, (g2, s) -> {
            g2.fillRoundRect((int)(s*0.2), (int)(s*0.45), (int)(s*0.6), (int)(s*0.4), (int)(s*0.2), (int)(s*0.2));
            g2.drawOval((int)(s*0.35), (int)(s*0.12), (int)(s*0.3), (int)(s*0.3));
        });
    }
}
