package com.nexaria.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fond animé avec particules flottantes pour un effet visuel premium
 */
public class ParticleBackground extends JPanel {
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private Timer animationTimer;
    private static final int PARTICLE_COUNT = 50;

    public ParticleBackground() {
        setOpaque(false);
        initializeParticles();
        startAnimation();
    }

    private void initializeParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle());
        }
    }

    private void startAnimation() {
        animationTimer = new Timer(16, e -> { // ~60 FPS
            updateParticles();
            repaint();
        });
        animationTimer.start();
    }

    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    private void updateParticles() {
        for (Particle p : particles) {
            p.update(getWidth(), getHeight());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dessiner les particules
        for (Particle p : particles) {
            p.draw(g2);
        }

        // Dessiner les connexions entre particules proches
        drawConnections(g2);

        g2.dispose();
    }

    private void drawConnections(Graphics2D g2) {
        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p1 = particles.get(i);
                Particle p2 = particles.get(j);

                double distance = Math.sqrt(
                        Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));

                if (distance < 150) {
                    int alpha = (int) (30 * (1 - distance / 150));
                    g2.setColor(new Color(170, 80, 255, alpha));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                }
            }
        }
    }

    private class Particle {
        double x, y;
        double vx, vy;
        double size;
        Color color;
        double opacity;

        Particle() {
            reset(random.nextInt(2000), random.nextInt(2000));
        }

        void reset(int width, int height) {
            x = random.nextDouble() * width;
            y = random.nextDouble() * height;
            vx = (random.nextDouble() - 0.5) * 0.5;
            vy = (random.nextDouble() - 0.5) * 0.5;
            size = 2 + random.nextDouble() * 4;

            // Variations de couleur autour du violet
            int r = 150 + random.nextInt(50);
            int g = 70 + random.nextInt(30);
            int b = 200 + random.nextInt(55);
            opacity = 0.3 + random.nextDouble() * 0.4;
            color = new Color(r, g, b, (int) (opacity * 255));
        }

        void update(int width, int height) {
            x += vx;
            y += vy;

            // Rebondir sur les bords
            if (x < 0 || x > width) {
                vx = -vx;
                x = Math.max(0, Math.min(width, x));
            }
            if (y < 0 || y > height) {
                vy = -vy;
                y = Math.max(0, Math.min(height, y));
            }
        }

        void draw(Graphics2D g2) {
            // Effet de glow
            for (int i = 3; i > 0; i--) {
                int alpha = (int) (opacity * 255 / (i + 1));
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                double glowSize = size + i * 2;
                g2.fill(new Ellipse2D.Double(x - glowSize / 2, y - glowSize / 2, glowSize, glowSize));
            }

            // Particule principale
            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
        }
    }
}
