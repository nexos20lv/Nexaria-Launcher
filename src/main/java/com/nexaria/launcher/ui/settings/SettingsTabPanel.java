package com.nexaria.launcher.ui.settings;

import com.nexaria.launcher.ui.DesignConstants;

import javax.swing.*;
import java.awt.*;

public abstract class SettingsTabPanel extends JPanel {

    public SettingsTabPanel(String title) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(DesignConstants.FONT_TITLE.deriveFont(18f));
        titleLabel.setForeground(DesignConstants.PURPLE_ACCENT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel);
    }

    protected JPanel createCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fond avec effet glassmorphism
                g2.setColor(new Color(40, 30, 50, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Bordure subtile
                g2.setColor(new Color(170, 80, 255, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return card;
    }

    protected void addSubsection(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignConstants.FONT_REGULAR.deriveFont(13f));
        label.setForeground(DesignConstants.PURPLE_ACCENT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(label);
    }
}
