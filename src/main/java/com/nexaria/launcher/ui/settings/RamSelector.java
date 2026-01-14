package com.nexaria.launcher.ui.settings;

import com.nexaria.launcher.ui.DesignConstants;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;

/**
 * Composant intelligent pour la sélection de RAM
 * Détecte la RAM système et propose des paliers adaptés
 */
public class RamSelector extends JPanel {
    private final JSlider slider;
    private final JLabel valueLabel;
    private final long totalRamMB;

    private int valueMB;

    public RamSelector(int currentRamMB) {
        this.valueMB = currentRamMB;
        this.totalRamMB = getTotalSystemMemoryMB();

        setOpaque(false);
        setLayout(new BorderLayout(10, 5));

        // 1. Déterminer les bornes
        // Min: 1GB, Max: Total - 2GB (OS), mais plafonné à 16GB si beaucoup de RAM
        int minMB = 1024;
        int maxMB = (int) Math.max(2048, totalRamMB - 2048);

        // Bornes de sécurité
        int safeLimit = 4096; // 4GB est top pour le jeu
        int warnLimit = Math.min(8192, maxMB - 1024);

        if (maxMB > 16384)
            maxMB = 16384; // Cap visuel à 16Go pour pas avoir un slider illisible

        slider = new JSlider(minMB, maxMB, currentRamMB);
        setupSliderUI(slider);

        // Snap aux 512MB
        slider.setMajorTickSpacing(1024);
        slider.setMinorTickSpacing(512);
        slider.setSnapToTicks(true);
        slider.setPaintTicks(true); // Ticks gérés par UI custom pour plus de beauté

        valueLabel = new JLabel(formatSize(currentRamMB));
        valueLabel.setFont(DesignConstants.FONT_HEADER.deriveFont(18f));
        valueLabel.setForeground(DesignConstants.PURPLE_ACCENT);
        valueLabel.setPreferredSize(new Dimension(80, 40));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        slider.addChangeListener(e -> {
            valueMB = slider.getValue();
            valueLabel.setText(formatSize(valueMB));
            updateColorState(valueMB, safeLimit, warnLimit);
        });

        // Header avec infos système
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Mémoire allouée");
        title.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
        title.setForeground(DesignConstants.TEXT_SECONDARY);

        JLabel sysInfo = new JLabel("Système: " + (totalRamMB / 1024) + " GB");
        sysInfo.setFont(DesignConstants.FONT_REGULAR.deriveFont(12f));
        sysInfo.setForeground(new Color(255, 255, 255, 100));

        header.add(title, BorderLayout.WEST);
        header.add(sysInfo, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(slider, BorderLayout.CENTER);
        add(valueLabel, BorderLayout.EAST);

        // Initial color update
        updateColorState(currentRamMB, safeLimit, warnLimit);
    }

    private void updateColorState(int val, int safe, int warn) {
        if (val >= warn) {
            valueLabel.setForeground(new Color(255, 80, 80)); // Rouge alerte
            slider.setForeground(new Color(255, 80, 80));
        } else if (val >= safe) {
            valueLabel.setForeground(DesignConstants.SUCCESS_COLOR); // Vert optimal
            slider.setForeground(DesignConstants.SUCCESS_COLOR);
        } else {
            valueLabel.setForeground(DesignConstants.PURPLE_ACCENT); // Standard
            slider.setForeground(DesignConstants.PURPLE_ACCENT);
        }
    }

    private void setupSliderUI(JSlider s) {
        s.setOpaque(false);
        s.setUI(new BasicSliderUI(s) {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Rectangle trackRect = contentRect;

                int h = 6;
                int y = trackRect.y + (trackRect.height - h) / 2;

                // Fond
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(trackRect.x, y, trackRect.width, h, h, h);

                // Remplissage
                int fillW = xPositionForValue(s.getValue()) - trackRect.x;
                g2.setColor(s.getForeground());
                g2.fillRoundRect(trackRect.x, y, fillW, h, h, h);
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Rectangle knobRect = thumbRect;
                int w = 18;
                int h = 18;

                g2.translate(knobRect.x + knobRect.width / 2 - w / 2, knobRect.y + knobRect.height / 2 - h / 2);

                // Ombre
                g2.setColor(new Color(0, 0, 0, 100));
                g2.fillOval(1, 1, w, h);

                // Cercle
                g2.setColor(Color.WHITE);
                g2.fillOval(0, 0, w, h);

                // Centre coloré
                g2.setColor(s.getForeground());
                g2.fillOval(4, 4, w - 8, h - 8);

                g2.translate(-(knobRect.x + knobRect.width / 2 - w / 2), -(knobRect.y + knobRect.height / 2 - h / 2));
            }
        });
    }

    private long getTotalSystemMemoryMB() {
        try {
            java.lang.management.OperatingSystemMXBean osBean = java.lang.management.ManagementFactory
                    .getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) osBean).getTotalMemorySize() / (1024 * 1024);
            }
        } catch (Exception e) {
            // Fallback
        }
        return 8192; // 8GB par défaut si détection échoue
    }

    private String formatSize(int mb) {
        if (mb >= 1024 && mb % 1024 == 0) {
            return (mb / 1024) + " Go";
        }
        return mb + " Mo";
    }

    public int getValue() {
        return slider.getValue();
    }
}
