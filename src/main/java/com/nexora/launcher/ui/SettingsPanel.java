package com.nexora.launcher.ui;

import com.nexora.launcher.config.LauncherConfig;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class SettingsPanel extends JPanel {
    private LauncherConfig cfg;

    public SettingsPanel() {
        cfg = LauncherConfig.getInstance();
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Glass Container
        GlassPanel content = new GlassPanel(40); // Big round corners
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Title
        JLabel title = new JLabel("LAUNCHER SETTINGS");
        title.setFont(DesignConstants.FONT_TITLE);
        title.setForeground(DesignConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(30));

        // Memory Settings Section
        addSectionTitle(content, "JAVA MEMORY");
        content.add(Box.createVerticalStrut(10));

        // RAM Slider
        int ram = cfg.getMaxMemory();
        JLabel ramValue = new JLabel(ram + " MB");
        ramValue.setForeground(DesignConstants.purple_ACCENT);
        ramValue.setFont(DesignConstants.FONT_HEADER);
        ramValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider ramSlider = new JSlider(512, 16384, ram);
        ramSlider.setOpaque(false);
        ramSlider.setForeground(DesignConstants.PURPLE_ACCENT);
        ramSlider.setBackground(DesignConstants.GLASS_BACKGROUND);
        ramSlider.setPaintTicks(true);
        ramSlider.setMajorTickSpacing(2048);
        ramSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        ramSlider.setMaximumSize(new Dimension(500, 50));
        ramSlider.addChangeListener(e -> ramValue.setText(ramSlider.getValue() + " MB"));

        content.add(ramValue);
        content.add(ramSlider);
        content.add(Box.createVerticalStrut(30));

        // System Config Section
        addSectionTitle(content, "SYSTEM");
        content.add(Box.createVerticalStrut(10));

        JCheckBox autoUpdate = new JCheckBox("Enable Auto-Updates");
        autoUpdate.setOpaque(false);
        autoUpdate.setForeground(DesignConstants.TEXT_SECONDARY);
        autoUpdate.setFont(DesignConstants.FONT_REGULAR);
        autoUpdate.setSelected(cfg.isAutoUpdate());
        autoUpdate.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Custom icon for checkbox could be added here

        content.add(autoUpdate);
        content.add(Box.createVerticalGlue());

        // Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionPanel.setMaximumSize(new Dimension(800, 60));

        ModernButton saveBtn = new ModernButton("SAVE CHANGES", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, true);
        saveBtn.setPreferredSize(new Dimension(180, 45));
        saveBtn.addActionListener(e -> {
            cfg.setMaxMemory(ramSlider.getValue());
            cfg.setAutoUpdate(autoUpdate.isSelected());
            cfg.saveConfig();
            JOptionPane.showMessageDialog(this, "Settings Saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        actionPanel.add(saveBtn);
        content.add(actionPanel);

        add(content, BorderLayout.CENTER);
    }

    private void addSectionTitle(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignConstants.FONT_SMALL);
        label.setForeground(DesignConstants.CYAN_ACCENT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }
}
