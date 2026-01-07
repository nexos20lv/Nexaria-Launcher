package com.nexora.launcher.ui;

import com.nexora.launcher.config.LauncherConfig;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {
    public SettingsPanel() {
        setBackground(new Color(15, 23, 42));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Paramètres du Launcher");
        title.setForeground(new Color(241,245,249));
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2; add(title, gbc);

        LauncherConfig cfg = LauncherConfig.getInstance();

        // Mémoire
        gbc.gridwidth=1; gbc.gridy=1; gbc.gridx=0;
        JLabel memoryLabel = new JLabel("Mémoire max (Mo)");
        memoryLabel.setForeground(new Color(148,163,184));
        add(memoryLabel, gbc);
        gbc.gridx=1; JSpinner memMax = new JSpinner(new SpinnerNumberModel(cfg.getMaxMemory(), 512, 16384, 256)); add(memMax, gbc);

        gbc.gridy=2; gbc.gridx=0; JLabel memMinLabel = new JLabel("Mémoire min (Mo)"); memMinLabel.setForeground(new Color(148,163,184)); add(memMinLabel, gbc);
        gbc.gridx=1; JSpinner memMin = new JSpinner(new SpinnerNumberModel(cfg.getMinMemory(), 256, 8192, 256)); add(memMin, gbc);

        // Auto-update
        gbc.gridy=3; gbc.gridx=0; JLabel updateLabel = new JLabel("Mises à jour automatiques"); updateLabel.setForeground(new Color(148,163,184)); add(updateLabel, gbc);
        gbc.gridx=1; JCheckBox autoUpd = new JCheckBox(); autoUpd.setSelected(cfg.isAutoUpdate()); add(autoUpd, gbc);

        // Boutons
        gbc.gridy=4; gbc.gridx=0; JButton save = new JButton("Enregistrer"); add(save, gbc);
        gbc.gridx=1; JButton reset = new JButton("Réinitialiser"); add(reset, gbc);
    }
}
