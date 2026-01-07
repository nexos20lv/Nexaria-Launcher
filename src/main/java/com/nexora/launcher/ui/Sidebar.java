package com.nexora.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Sidebar extends JPanel {
    private String activeRoute = "ACCUEIL";
    private Consumer<String> onNavigate;
    private Map<String, ModernButton> buttons = new HashMap<>();

    public Sidebar(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(80, 750));
        // Standard balanced padding
        setBorder(BorderFactory.createEmptyBorder(40, 10, 40, 10));

        add(createNavButton("HOME", "ACCUEIL"));
        add(Box.createVerticalStrut(20));
        add(createNavButton("SETTINGS", "PARAMÈTRES"));

        add(Box.createVerticalGlue());
    }

    private JButton createNavButton(String text, String route) {
        String label = text.substring(0, 1);

        ModernButton btn = new ModernButton(label, new Color(255, 255, 255, 10), new Color(255, 255, 255, 40));
        btn.setMaximumSize(new Dimension(50, 50));
        btn.setPreferredSize(new Dimension(50, 50));
        btn.setFont(DesignConstants.FONT_HEADER);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT); // Important for BoxLayout

        btn.addActionListener(e -> {
            setActive(route);
            onNavigate.accept(route);
        });

        buttons.put(route, btn);
        return btn;
    }

    public void setActive(String route) {
        this.activeRoute = route;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Glass Dock - Full Height rounded strip
        g2d.setColor(DesignConstants.GLASS_BACKGROUND);
        g2d.fillRoundRect(10, 20, getWidth() - 20, getHeight() - 40, 30, 30);

        g2d.setColor(DesignConstants.GLASS_BORDER);
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRoundRect(10, 20, getWidth() - 20, getHeight() - 40, 30, 30);

        // Indicator
        ModernButton activeBtn = buttons.get(activeRoute);
        if (activeBtn != null) {
            Rectangle bounds = activeBtn.getBounds();
            int cy = bounds.y + bounds.height / 2;

            // Glow
            g2d.setColor(new Color(170, 80, 255, 180));
            g2d.fillOval(5, cy - 4, 8, 8);

            // Core
            g2d.setColor(Color.WHITE);
            g2d.fillOval(7, cy - 2, 4, 4);
        }

        super.paintComponent(g);
    }
}
