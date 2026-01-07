package com.nexora.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class Sidebar extends JPanel {
    public Sidebar(Consumer<String> onNavigate) {
        setBackground(new Color(24, 34, 60));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(220, 700));

        add(Box.createVerticalStrut(20));
        add(makeItem("Accueil", IconUtil.play(20, new Color(59,130,246)), onNavigate));
        add(makeItem("Mods", IconUtil.settings(20, new Color(59,130,246)), onNavigate));
        add(makeItem("Configs", IconUtil.settings(20, new Color(59,130,246)), onNavigate));
        add(makeItem("Paramètres", IconUtil.settings(20, new Color(59,130,246)), onNavigate));
        add(Box.createVerticalGlue());
    }

    private JComponent makeItem(String label, Icon icon, Consumer<String> onNavigate) {
        JButton btn = new JButton(label, icon);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 42));
        btn.setBackground(new Color(30, 41, 59));
        btn.setForeground(new Color(241, 245, 249));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onNavigate.accept(label.toUpperCase()));
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new FlowLayout(FlowLayout.LEFT));
        wrap.add(btn);
        return wrap;
    }
}
