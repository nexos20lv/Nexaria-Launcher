package com.nexora.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TitleBar extends JPanel {
    private final JFrame frame;
    private Point dragOffset;

    public TitleBar(JFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(20, 28, 48));
        setPreferredSize(new Dimension(1000, 40));

        JLabel title = new JLabel("  NEXORA LAUNCHER");
        title.setForeground(new Color(200, 210, 230));
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(title, BorderLayout.WEST);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 6));

        JButton minimize = new JButton(IconUtil.minimize(16, new Color(148,163,184)));
        JButton close = new JButton(IconUtil.close(16, new Color(239,68,68)));
        styleIconButton(minimize); styleIconButton(close);

        minimize.addActionListener(e -> frame.setState(Frame.ICONIFIED));
        close.addActionListener(e -> frame.dispose());

        actions.add(minimize); actions.add(close);
        add(actions, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragOffset = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                Point p = e.getLocationOnScreen();
                frame.setLocation(p.x - dragOffset.x, p.y - dragOffset.y);
            }
        };
        addMouseListener(drag); addMouseMotionListener(drag);
    }

    private void styleIconButton(JButton b) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
