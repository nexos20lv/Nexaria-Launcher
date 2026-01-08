package com.nexaria.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticlePanel extends JPanel {
    private List<Particle> particles;
    private final int PARTICLE_COUNT = 50;
    private Timer aniTimer;

    public ParticlePanel() {
        setOpaque(false);
        particles = new ArrayList<>();

        // Spawn particles
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle());
        }

        aniTimer = new Timer(50, (ActionEvent e) -> {
            updateParticles();
            repaint();
        });
        aniTimer.start();
    }

    private void updateParticles() {
        for (Particle p : particles) {
            p.update();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // We assume a GradientPanel is behind this, or we just paint particles
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }

    private class Particle {
        float x, y;
        float vx, vy;
        float size;
        float alpha;

        // Target screen size placeholder
        int maxWidth = 1200;
        int maxHeight = 800;

        public Particle() {
            init(true);
        }

        private void init(boolean randomY) {
            Random rand = new Random();
            maxWidth = getWidth() > 0 ? getWidth() : 1200;
            maxHeight = getHeight() > 0 ? getHeight() : 800;

            x = rand.nextInt(maxWidth);
            y = randomY ? rand.nextInt(maxHeight) : maxHeight + 10;

            // Slower, calmer movement
            vx = (rand.nextFloat() - 0.5f) * 0.2f;
            vy = -(rand.nextFloat() * 0.3f + 0.1f); // Slow float up

            size = rand.nextFloat() * 2 + 1; // 1-3px size (smaller)
            alpha = rand.nextFloat() * 0.4f + 0.05f; // More transparent
        }

        public void update() {
            x += vx;
            y += vy;

            if (y < -10 || x < -10 || x > maxWidth + 10) {
                init(false); // Respawn at bottom
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(1f, 1f, 1f, alpha));
            g2d.fillOval((int) x, (int) y, (int) size, (int) size);
        }
    }
}
