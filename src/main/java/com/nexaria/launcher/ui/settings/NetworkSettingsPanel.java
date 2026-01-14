package com.nexaria.launcher.ui.settings;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.ui.DesignConstants;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;

public class NetworkSettingsPanel extends SettingsTabPanel {
    private final LauncherConfig cfg;

    public NetworkSettingsPanel() {
        super("Réseau & Téléchargements");
        this.cfg = LauncherConfig.getInstance();
        initUI();
    }

    private void initUI() {
        add(Box.createVerticalStrut(20));

        JPanel rateCard = createCard();
        rateCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        rateCard.setMaximumSize(new Dimension(600, 120));

        JLabel rateTitle = new JLabel("Limite de débit de téléchargement");
        rateTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(14f));
        rateTitle.setForeground(DesignConstants.PURPLE_ACCENT);
        rateTitle.setIcon(FontIcon.of(FontAwesomeSolid.DOWNLOAD, 16, DesignConstants.PURPLE_ACCENT));
        rateCard.add(rateTitle);
        rateCard.add(Box.createVerticalStrut(15));

        JPanel ratePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        ratePanel.setOpaque(false);
        ratePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSpinner rateSpinner = new JSpinner(new SpinnerNumberModel(cfg.downloadRateLimitKBps, 0, 102400, 64));
        ((JSpinner.DefaultEditor) rateSpinner.getEditor()).getTextField().setPreferredSize(new Dimension(100, 32));
        ((JSpinner.DefaultEditor) rateSpinner.getEditor()).getTextField()
                .setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));

        rateSpinner.addChangeListener(e -> {
            cfg.setDownloadRateLimitKBps((Integer) rateSpinner.getValue());
            cfg.saveConfig();
        });

        JLabel rateLabel = new JLabel("KB/s (0 = illimité)");
        rateLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        rateLabel.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));

        ratePanel.add(rateSpinner);
        ratePanel.add(rateLabel);
        rateCard.add(ratePanel);

        add(rateCard);
        add(Box.createVerticalGlue());
    }
}
