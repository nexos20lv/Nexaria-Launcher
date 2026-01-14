package com.nexaria.launcher.ui.settings;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.ui.DesignConstants;
import com.nexaria.launcher.ui.ModernButton;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FoldersSettingsPanel extends SettingsTabPanel {
    private final LauncherConfig cfg;

    public FoldersSettingsPanel() {
        super("Dossiers & Cache");
        this.cfg = LauncherConfig.getInstance();
        initUI();
    }

    private void initUI() {
        add(Box.createVerticalStrut(20));

        // Carte Répertoire de jeu
        JPanel dirCard = createCard();
        dirCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        dirCard.setMaximumSize(new Dimension(700, 120));

        JLabel dirTitle = new JLabel("Répertoire de jeu");
        dirTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        dirTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        dirTitle.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER, 16, DesignConstants.PURPLE_ACCENT));
        dirCard.add(dirTitle);
        dirCard.add(Box.createVerticalStrut(15));

        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pathPanel.setOpaque(false);
        pathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField gameDirField = new JTextField(LauncherConfig.getGameDir());
        gameDirField.setPreferredSize(new Dimension(400, 35));
        gameDirField.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        gameDirField.setEditable(false);

        ModernButton chooseDirBtn = new ModernButton("PARCOURIR", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        chooseDirBtn.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 16, DesignConstants.TEXT_PRIMARY));
        chooseDirBtn.setPreferredSize(new Dimension(140, 35));
        chooseDirBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(LauncherConfig.getGameDir());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File dir = fc.getSelectedFile();
                if (dir != null) {
                    gameDirField.setText(dir.getAbsolutePath());
                    cfg.setGameDir(dir.getAbsolutePath());
                    cfg.saveConfig();
                }
            }
        });

        pathPanel.add(gameDirField);
        pathPanel.add(chooseDirBtn);
        dirCard.add(pathPanel);
        add(dirCard);

        add(Box.createVerticalStrut(20));

        // Carte Gestion du cache
        JPanel cacheCard = createCard();
        cacheCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        cacheCard.setMaximumSize(new Dimension(700, 140));

        JLabel cacheTitle = new JLabel("Gestion du cache & maintenance");
        cacheTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        cacheTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        cacheTitle.setIcon(FontIcon.of(FontAwesomeSolid.DATABASE, 16, DesignConstants.PURPLE_ACCENT));
        cacheCard.add(cacheTitle);
        cacheCard.add(Box.createVerticalStrut(15));

        JPanel cachePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        cachePanel.setOpaque(false);
        cachePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton clearCacheBtn = new ModernButton("VIDER CACHE", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        clearCacheBtn.setIcon(FontIcon.of(FontAwesomeSolid.TRASH, 16, DesignConstants.TEXT_PRIMARY));
        clearCacheBtn.setPreferredSize(new Dimension(160, 40));
        clearCacheBtn.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(this, "Vider le cache?", "Confirmation",
                    JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                try {
                    deleteRecursive(new File(LauncherConfig.getCacheDir()));
                    JOptionPane.showMessageDialog(this, "Cache vidé.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Échec du vidage du cache.", "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        ModernButton resetDataBtn = new ModernButton("RÉINIT. DONNÉES", new Color(200, 100, 50),
                new Color(160, 80, 30), false);
        resetDataBtn.setIcon(FontIcon.of(FontAwesomeSolid.BROOM, 16, DesignConstants.TEXT_PRIMARY));
        resetDataBtn.setPreferredSize(new Dimension(180, 40));
        resetDataBtn.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(this, "Réinitialiser toutes les données?",
                    "Zone dangereuse", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res == JOptionPane.YES_OPTION) {
                try {
                    deleteRecursive(new File(LauncherConfig.getDataFolder()));
                    JOptionPane.showMessageDialog(this, "Données réinitialisées.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Échec de la réinitialisation.", "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        ModernButton repairBtn = new ModernButton("RÉPARER LE JEU", DesignConstants.SUCCESS_COLOR,
                DesignConstants.SUCCESS_COLOR.darker(), false);
        repairBtn.setIcon(FontIcon.of(FontAwesomeSolid.TOOLS, 16, DesignConstants.TEXT_PRIMARY));
        repairBtn.setPreferredSize(new Dimension(170, 40));
        repairBtn.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(this,
                    "Ceci va forcer la re-vérification complète de tous les fichiers au prochain lancement.\n" +
                            "Le démarrage sera plus long.\n\nContinuer ?",
                    "Mode Réparation", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                cfg.setForceRepair(true);
                cfg.saveConfig();
                JOptionPane.showMessageDialog(this,
                        "Mode réparation activé !\nRelancez le jeu pour effectuer la vérification.",
                        "Information", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        cachePanel.add(clearCacheBtn);
        cachePanel.add(resetDataBtn);
        cachePanel.add(repairBtn);
        cacheCard.add(cachePanel);

        add(cacheCard);
        add(Box.createVerticalGlue());
    }

    private void deleteRecursive(File f) {
        if (f == null || !f.exists())
            return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null)
                for (File k : kids)
                    deleteRecursive(k);
        }
        f.delete();
    }
}
