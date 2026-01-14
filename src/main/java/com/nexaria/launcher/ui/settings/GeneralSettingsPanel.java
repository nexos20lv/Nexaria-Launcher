package com.nexaria.launcher.ui.settings;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.ui.DesignConstants;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;

public class GeneralSettingsPanel extends SettingsTabPanel {
    private final LauncherConfig cfg;

    public GeneralSettingsPanel() {
        super("Paramètres généraux");
        this.cfg = LauncherConfig.getInstance();
        initUI();
    }

    private void initUI() {
        add(Box.createVerticalStrut(20));

        // Carte Comportement
        JPanel behaviorCard = createCard();
        behaviorCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        behaviorCard.setMaximumSize(new Dimension(600, 100));

        JLabel behaviorTitle = new JLabel("Comportement de l'application");
        behaviorTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        behaviorTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        behaviorTitle.setIcon(FontIcon.of(FontAwesomeSolid.COG, 16, DesignConstants.PURPLE_ACCENT));
        behaviorCard.add(behaviorTitle);
        behaviorCard.add(Box.createVerticalStrut(15));

        JCheckBox minimizeOnLaunch = new JCheckBox("Minimiser le launcher lorsque le jeu démarre");
        minimizeOnLaunch.setOpaque(false);
        minimizeOnLaunch.setForeground(DesignConstants.TEXT_PRIMARY);
        minimizeOnLaunch.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
        minimizeOnLaunch.setSelected(cfg.minimizeOnLaunch);
        minimizeOnLaunch.setFocusPainted(false);
        minimizeOnLaunch.setBorderPainted(false);
        minimizeOnLaunch.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 18, new Color(255, 255, 255, 40)));
        minimizeOnLaunch.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 18, DesignConstants.PURPLE_ACCENT));
        minimizeOnLaunch.addActionListener(e -> {
            cfg.minimizeOnLaunch = minimizeOnLaunch.isSelected();
            cfg.saveConfig();
        });
        behaviorCard.add(minimizeOnLaunch);

        add(behaviorCard);

        add(Box.createVerticalStrut(20));

        // Updates & Debug
        addSubsection("Mises à jour & Débogage");
        add(Box.createVerticalStrut(10));

        JPanel debugCard = createCard();
        debugCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        debugCard.setMaximumSize(new Dimension(600, 120));

        JCheckBox autoUpdate = new JCheckBox("Mettre à jour automatiquement le launcher");
        autoUpdate.setOpaque(false);
        autoUpdate.setForeground(DesignConstants.TEXT_SECONDARY);
        autoUpdate.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        autoUpdate.setSelected(cfg.isAutoUpdate());
        autoUpdate.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoUpdate.setFocusPainted(false);
        autoUpdate.setBorderPainted(false);
        autoUpdate.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 16, new Color(255, 255, 255, 40)));
        autoUpdate.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 16, DesignConstants.PURPLE_ACCENT));
        autoUpdate.addActionListener(e -> {
            cfg.setAutoUpdate(autoUpdate.isSelected());
            cfg.saveConfig();
        });

        JCheckBox debugMode = new JCheckBox("Mode Débogage (logs détaillés)");
        debugMode.setOpaque(false);
        debugMode.setForeground(DesignConstants.TEXT_SECONDARY);
        debugMode.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        debugMode.setSelected(cfg.debugMode);
        debugMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        debugMode.setFocusPainted(false);
        debugMode.setBorderPainted(false);
        debugMode.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 16, new Color(255, 255, 255, 40)));
        debugMode.setSelectedIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 16, DesignConstants.PURPLE_ACCENT));
        debugMode.addActionListener(e -> {
            cfg.setDebugMode(debugMode.isSelected());
            cfg.saveConfig();
        });

        debugCard.add(autoUpdate);
        debugCard.add(Box.createVerticalStrut(10));
        debugCard.add(debugMode);

        add(debugCard);
        add(Box.createVerticalGlue());
    }
}
