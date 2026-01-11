package com.nexaria.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Graphique en temps réel de l'utilisation de la RAM
 */
public class RAMUsageChart extends JPanel {
    private final List<Double> usageHistory = new ArrayList<>();
    private final int maxDataPoints = 60; // 60 secondes d'historique
    private Timer updateTimer;
    private long maxMemory;
    private long allocatedMemory;

    public RAMUsageChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(500, 150));
        setMaximumSize(new Dimension(600, 150));

        // Initialiser avec la mémoire système
        Runtime runtime = Runtime.getRuntime();
        maxMemory = runtime.maxMemory();

        // Démarrer les mises à jour
        startUpdating();
    }

    public void setAllocatedMemory(long allocated) {
        this.allocatedMemory = allocated * 1024 * 1024; // Convertir MB en bytes
    }

    private void startUpdating() {
        updateTimer = new Timer(1000, e -> {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            double usagePercent = (double) used / maxMemory * 100;

            synchronized (usageHistory) {
                usageHistory.add(usagePercent);
                if (usageHistory.size() > maxDataPoints) {
                    usageHistory.remove(0);
                }
            }

            repaint();
        });
        updateTimer.start();
    }

    public void stopUpdating() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 20;
        int graphWidth = width - 2 * padding;
        int graphHeight = height - 2 * padding;

        // Fond du graphique
        g2.setColor(new Color(20, 15, 30, 200));
        g2.fillRoundRect(padding, padding, graphWidth, graphHeight, 10, 10);

        // Grille
        g2.setColor(new Color(255, 255, 255, 30));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 5 }, 0));
        for (int i = 0; i <= 4; i++) {
            int y = padding + (graphHeight * i / 4);
            g2.drawLine(padding, y, padding + graphWidth, y);
        }

        // Ligne d'allocation si définie
        if (allocatedMemory > 0) {
            double allocPercent = (double) allocatedMemory / maxMemory * 100;
            int allocY = padding + graphHeight - (int) (graphHeight * allocPercent / 100);
            g2.setColor(new Color(255, 200, 0, 100));
            g2.setStroke(
                    new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 10, 5 }, 0));
            g2.drawLine(padding, allocY, padding + graphWidth, allocY);

            // Label allocation
            g2.setColor(new Color(255, 200, 0, 200));
            g2.setFont(DesignConstants.FONT_REGULAR.deriveFont(10f));
            g2.drawString("Alloué: " + (allocatedMemory / 1024 / 1024) + " MB", padding + 5, allocY - 5);
        }

        // Dessiner la courbe d'utilisation
        synchronized (usageHistory) {
            if (usageHistory.size() > 1) {
                g2.setStroke(new BasicStroke(2f));

                // Dégradé pour la zone sous la courbe
                int[] xPoints = new int[usageHistory.size() + 2];
                int[] yPoints = new int[usageHistory.size() + 2];

                for (int i = 0; i < usageHistory.size(); i++) {
                    double usage = usageHistory.get(i);
                    int x = padding + (graphWidth * i / (maxDataPoints - 1));
                    int y = padding + graphHeight - (int) (graphHeight * usage / 100);
                    xPoints[i] = x;
                    yPoints[i] = y;
                }

                // Fermer le polygone
                xPoints[usageHistory.size()] = padding + graphWidth;
                yPoints[usageHistory.size()] = padding + graphHeight;
                xPoints[usageHistory.size() + 1] = padding;
                yPoints[usageHistory.size() + 1] = padding + graphHeight;

                // Remplir avec dégradé
                GradientPaint gp = new GradientPaint(
                        0, padding, new Color(170, 80, 255, 100),
                        0, padding + graphHeight, new Color(170, 80, 255, 20));
                g2.setPaint(gp);
                g2.fillPolygon(xPoints, yPoints, usageHistory.size() + 2);

                // Dessiner la ligne
                g2.setColor(DesignConstants.PURPLE_ACCENT);
                for (int i = 0; i < usageHistory.size() - 1; i++) {
                    double usage1 = usageHistory.get(i);
                    double usage2 = usageHistory.get(i + 1);

                    int x1 = padding + (graphWidth * i / (maxDataPoints - 1));
                    int y1 = padding + graphHeight - (int) (graphHeight * usage1 / 100);
                    int x2 = padding + (graphWidth * (i + 1) / (maxDataPoints - 1));
                    int y2 = padding + graphHeight - (int) (graphHeight * usage2 / 100);

                    g2.drawLine(x1, y1, x2, y2);
                }
            }
        }

        // Afficher les valeurs actuelles
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long usedMB = used / 1024 / 1024;
        long maxMB = maxMemory / 1024 / 1024;
        double usagePercent = (double) used / maxMemory * 100;

        g2.setColor(DesignConstants.TEXT_PRIMARY);
        g2.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        String usageText = String.format("Utilisation: %d MB / %d MB (%.1f%%)", usedMB, maxMB, usagePercent);
        g2.drawString(usageText, padding + 5, padding - 5);

        // Bordure
        g2.setColor(new Color(170, 80, 255, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(padding, padding, graphWidth, graphHeight, 10, 10);

        g2.dispose();
    }
}
