package com.nexaria.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TitleBar extends JPanel {
    @SuppressWarnings("unused")
    private final JFrame frame;
    private Point dragOffset;

    public TitleBar(JFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setOpaque(false); // Transparent to show gradient
        setPreferredSize(new Dimension(1000, 40));

        JLabel title = new JLabel("NEXARIA LAUNCHER");
        title.setForeground(DesignConstants.TEXT_SECONDARY);
        title.setFont(DesignConstants.FONT_SMALL);
        title.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0)); // Décalage à droite
        add(title, BorderLayout.WEST);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        // Use FlowLayout for buttons
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5)); // Plus d'espacement à gauche
        actions.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10)); // Décalage à gauche

        // Create simple text buttons for window controls if icons fail, or simple
        // colored dots (Mac style)
        JButton minimize = createWindowControlButton(new Color(255, 189, 46)); // Yellow
        JButton close = createWindowControlButton(new Color(255, 95, 87)); // Red

        minimize.addActionListener(e -> frame.setState(Frame.ICONIFIED));
        close.addActionListener(e -> System.exit(0)); // Start proper shutdown

        actions.add(minimize);
        actions.add(close);
        add(actions, BorderLayout.EAST);

        // Drag functionality
        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = e.getLocationOnScreen();
                frame.setLocation(p.x - dragOffset.x, p.y - dragOffset.y);
            }
        };
        addMouseListener(drag);
        addMouseMotionListener(drag);
    }

    private JButton createWindowControlButton(Color color) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(15, 15));
        btn.setBackground(color);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false); // We'll paint it ourselves
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
        }); // Handled by caller layout, but keeps UI responsive

        // Custom painting for circle
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color);
                g2d.fillOval(2, 2, 12, 12);
            }
        });

        return btn;
    }
}
