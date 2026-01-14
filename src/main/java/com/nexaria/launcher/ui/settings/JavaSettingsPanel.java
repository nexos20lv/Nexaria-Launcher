package com.nexaria.launcher.ui.settings;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.ui.RAMUsageChart;

import javax.swing.*;
import java.awt.*;

public class JavaSettingsPanel extends SettingsTabPanel {
    private final LauncherConfig cfg;

    public JavaSettingsPanel() {
        super("Allocation Mémoire Java");
        this.cfg = LauncherConfig.getInstance();
        initUI();
    }

    private void initUI() {
        add(Box.createVerticalStrut(20));

        JPanel ramCard = createCard();
        ramCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramCard.setMaximumSize(new Dimension(600, 150));

        RamSelector ramSelector = new RamSelector(cfg.getMaxMemory());
        ramSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramCard.add(ramSelector);

        // Listener pour sauvegarder la config quand le slider change
        javax.swing.Timer saveTimer = new javax.swing.Timer(500, e -> {
            // Note: Ideally RamSelector should expose a precise change listener
            // For now assuming default usage
            int val = ramSelector.getValue();
            if (val != cfg.getMaxMemory()) {
                cfg.setMaxMemory(val);
                cfg.saveConfig();
            }
        });
        saveTimer.setRepeats(true);
        saveTimer.start();

        // Cleanup timer when panel removed? For now it's okay as it's a singleton app
        // mostly.

        add(ramCard);
        add(Box.createVerticalStrut(20));

        // Graphique d'utilisation RAM en temps réel
        RAMUsageChart ramChart = new RAMUsageChart();
        ramChart.setAllocatedMemory(cfg.getMaxMemory());
        ramChart.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Hack: poll la valeur du selector périodiquement pour mettre à jour le graph
        new javax.swing.Timer(100, e -> {
            int val = ramSelector.getValue();
            if (val != cfg.getMaxMemory()) {
                // Already handled by saveTimer mostly, but here for UI sync
                ramChart.setAllocatedMemory(val);
            }
            // Also need to push setAllocatedMemory if config changed elsewhere?
            // Simplified:
            ramChart.setAllocatedMemory(ramSelector.getValue());
        }).start();

        add(ramChart);
        add(Box.createVerticalGlue());
    }
}
