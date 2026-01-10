package com.nexaria.launcher.ui;

import com.nexaria.launcher.updater.UpdateManager;
import com.nexaria.launcher.updater.GitHubUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

/**
 * Écran de démarrage (Splash Screen) pour les mises à jour automatiques
 * Affiche le statut de la vérification/téléchargement avec animation
 */
public class UpdateSplashScreen extends JWindow {
    private static final Logger logger = LoggerFactory.getLogger(UpdateSplashScreen.class);
    
    private JPanel contentPanel;
    public JLabel titleLabel;
    public JLabel statusLabel;
    public JProgressBar progressBar;
    public JLabel subStatusLabel;
    private AnimatedLoadingSpinner spinner;
    private boolean updateFound = false;
    private GitHubUpdater.GitHubRelease latestRelease;
    private Runnable onFinished;
    
    private static final int WIDTH = 500;
    private static final int HEIGHT = 300;
    // Utiliser les couleurs du thème violet de l'app
    private static final Color BACKGROUND_COLOR = DesignConstants.BACKGROUND_DARK;
    private static final Color GRADIENT_START = DesignConstants.GRADIENT_MAIN_START;
    private static final Color GRADIENT_END = DesignConstants.GRADIENT_MAIN_END;
    private static final Color PRIMARY_COLOR = DesignConstants.PURPLE_ACCENT;
    private static final Color TEXT_COLOR = DesignConstants.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = DesignConstants.TEXT_SECONDARY;
    private static final Color ERROR_COLOR = new Color(200, 80, 100);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);

    public UpdateSplashScreen() {
        setAlwaysOnTop(true); // s'assurer que le splash est visible au-dessus
        initializeUI();
        centerOnScreen();
        setVisible(true);
    }

    public void startUpdateCheck(UpdateManager updateManager, Runnable onFinished) {
        this.onFinished = onFinished;
        startUpdateCheck(updateManager);
    }

    private void initializeUI() {
        // Créer le panel principal avec gradient
        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Fond dégradé avec le thème violet
                GradientPaint gradient = new GradientPaint(
                    0, 0, GRADIENT_START,
                    0, getHeight(), GRADIENT_END
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                super.paintComponent(g);
            }
        };
        contentPanel.setLayout(null);
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.setOpaque(true);
        add(contentPanel);
        
        // Titre du launcher avec le style de l'app
        titleLabel = new JLabel("Nexaria Launcher");
        titleLabel.setFont(DesignConstants.FONT_TITLE);
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setBounds(50, 30, 400, 40);
        contentPanel.add(titleLabel);
        
        // Spinner de chargement
        spinner = new AnimatedLoadingSpinner();
        spinner.setBounds(50, 90, 40, 40);
        contentPanel.add(spinner);
        
        // Label de statut principal
        statusLabel = new JLabel("Vérification des mises à jour...");
        statusLabel.setFont(DesignConstants.FONT_REGULAR);
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setBounds(100, 100, 350, 25);
        contentPanel.add(statusLabel);
        
        // Barre de progression avec le thème violet
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setBounds(50, 150, 400, 8);
        progressBar.setOpaque(false);
        progressBar.setUI(new CustomProgressBarUI());
        progressBar.setForeground(PRIMARY_COLOR);
        progressBar.setBackground(new Color(80, 50, 120, 100));
        contentPanel.add(progressBar);
        
        // Label de sous-statut
        subStatusLabel = new JLabel("Connexion à GitHub API...");
        subStatusLabel.setFont(DesignConstants.FONT_SMALL);
        subStatusLabel.setForeground(TEXT_SECONDARY);
        subStatusLabel.setBounds(50, 170, 400, 20);
        contentPanel.add(subStatusLabel);
        
        // Panel info
        JLabel infoLabel = new JLabel("Ne pas fermer cette fenêtre");
        infoLabel.setFont(DesignConstants.FONT_SMALL);
        infoLabel.setForeground(new Color(100, 100, 120));
        infoLabel.setBounds(50, 260, 400, 15);
        contentPanel.add(infoLabel);
        
        // Fenêtre
        setBounds(0, 0, WIDTH, HEIGHT);
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 15, 15));
        setContentPane(contentPanel);
        
        // Ajouter une bordure
        contentPanel.addMouseListener(new MouseListener() {
            @Override public void mouseClicked(MouseEvent e) {}
            @Override public void mousePressed(MouseEvent e) {}
            @Override public void mouseReleased(MouseEvent e) {}
            @Override public void mouseEntered(MouseEvent e) {}
            @Override public void mouseExited(MouseEvent e) {}
        });
    }

    private void centerOnScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - WIDTH) / 2;
        int y = (screenSize.height - HEIGHT) / 2;
        setLocation(x, y);
    }

    public void startUpdateCheck(UpdateManager updateManager) {
        logger.debug("Splash update: démarrage des callbacks");
        updateManager.setCallback(new UpdateManager.UpdateCallback() {
            @Override
            public void onCheckStart() {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Vérification des mises à jour...");
                    statusLabel.setIcon(IconUtil.getSearchIcon(PRIMARY_COLOR));
                    subStatusLabel.setText("Connexion à GitHub API...");
                    progressBar.setValue(10);
                });
            }

            @Override
            public void onCheckComplete(boolean hasUpdate, GitHubUpdater.GitHubRelease release, String error) {
                SwingUtilities.invokeLater(() -> {
                    if (error != null) {
                        statusLabel.setText("Erreur lors de la vérification");
                        statusLabel.setIcon(IconUtil.getErrorIcon(ERROR_COLOR));
                        subStatusLabel.setText(error);
                        progressBar.setValue(100);
                        progressBar.setForeground(ERROR_COLOR);
                        
                        // Fermer après 2 secondes
                        Timer timer = new Timer(2000, e -> closeScreen());
                        timer.setRepeats(false);
                        timer.start();
                    } else if (hasUpdate) {
                        updateFound = true;
                        latestRelease = release;
                        statusLabel.setText("Mise à jour disponible: " + release.tagName);
                        statusLabel.setIcon(IconUtil.getDownloadIcon(SUCCESS_COLOR));
                        subStatusLabel.setText("Téléchargement en cours...");
                        progressBar.setValue(30);
                        progressBar.setForeground(SUCCESS_COLOR);
                    } else {
                        statusLabel.setText("Launcher à jour");
                        statusLabel.setIcon(IconUtil.getCheckIcon(SUCCESS_COLOR));
                        subStatusLabel.setText("Vous disposez déjà de la dernière version");
                        progressBar.setValue(100);
                        progressBar.setForeground(SUCCESS_COLOR);
                        
                        // Fermer après 1 seconde
                        Timer timer = new Timer(1000, e -> closeScreen());
                        timer.setRepeats(false);
                        timer.start();
                    }
                });
            }

            @Override
            public void onDownloadStart(String version) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Téléchargement de " + version);
                    statusLabel.setIcon(IconUtil.getDownloadIcon(PRIMARY_COLOR));
                    subStatusLabel.setText("Récupération depuis GitHub...");
                    progressBar.setValue(40);
                    progressBar.setForeground(PRIMARY_COLOR);
                });
            }

            @Override
            public void onDownloadProgress(long bytesDownloaded, long totalBytes) {
                SwingUtilities.invokeLater(() -> {
                    if (totalBytes > 0) {
                        int progress = (int) ((bytesDownloaded * 60) / totalBytes) + 40; // 40-100%
                        progressBar.setValue(Math.min(100, progress));
                        
                        String downloaded = formatBytes(bytesDownloaded);
                        String total = formatBytes(totalBytes);
                        subStatusLabel.setText(downloaded + " / " + total);
                    }
                });
            }

            @Override
            public void onDownloadComplete() {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Installation en cours...");
                    statusLabel.setIcon(IconUtil.getCheckIcon(PRIMARY_COLOR));
                    subStatusLabel.setText("Préparation du redémarrage...");
                    progressBar.setValue(95);
                });
            }

            @Override
            public void onError(String message) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur");
                    statusLabel.setIcon(IconUtil.getErrorIcon(ERROR_COLOR));
                    subStatusLabel.setText(message);
                    progressBar.setValue(100);
                    progressBar.setForeground(new Color(200, 100, 100));
                    
                    // Fermer après 3 secondes
                    Timer timer = new Timer(3000, e -> closeScreen());
                    timer.setRepeats(false);
                    timer.start();
                });
            }
        });

        // Lancer la vérification + téléchargement avec callbacks (cache désactivé pour afficher l'état)
        updateManager.autoUpdateWithCallbacks(false);
    }

    public void closeScreen() {
        spinner.stop();
        setVisible(false);
        dispose();
        if (onFinished != null) {
            try { onFinished.run(); } catch (Exception e) { logger.debug("onFinished callback failed", e); }
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Spinner animé personnalisé
     */
    private static class AnimatedLoadingSpinner extends JComponent {
        private float rotation = 0;
        private Timer timer;

        public AnimatedLoadingSpinner() {
            timer = new Timer(50, e -> {
                rotation += 10;
                if (rotation >= 360) rotation = 0;
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight());
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            AffineTransform original = g2d.getTransform();
            g2d.translate(centerX, centerY);
            g2d.rotate(Math.toRadians(rotation));

            // Dessiner un spinner circulaire avec gradient violet
            Stroke stroke = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g2d.setStroke(stroke);

            for (int i = 0; i < 8; i++) {
                float alpha = (float) (i / 8.0);
                // Gradient purple -> cyan
                Color segmentColor = new Color(170, 80, 255, (int) (255 * alpha));
                g2d.setColor(segmentColor);

                double angle = (i * 45) * Math.PI / 180;
                int x1 = (int) (Math.cos(angle) * (size / 2 - 5));
                int y1 = (int) (Math.sin(angle) * (size / 2 - 5));
                int x2 = (int) (Math.cos(angle) * (size / 2 - 2));
                int y2 = (int) (Math.sin(angle) * (size / 2 - 2));

                g2d.drawLine(x1, y1, x2, y2);
            }

            g2d.setTransform(original);
        }

        public void stop() {
            timer.stop();
        }
    }

    /**
     * UI personnalisée pour la barre de progression
     */
    private static class CustomProgressBarUI extends javax.swing.plaf.basic.BasicProgressBarUI {
        @Override
        protected void paintDeterminate(Graphics g, JComponent c) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = c.getWidth();
            int height = c.getHeight();

            // Fond arrondi avec couleur assortie au thème
            g2d.setColor(new Color(80, 50, 120, 100));
            g2d.fillRoundRect(0, 0, width, height, height, height);

            // Barre de progression avec gradient violet -> cyan
            JProgressBar pbar = (JProgressBar) c;
            int filledWidth = (int) (width * pbar.getPercentComplete());
            
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(170, 80, 255),
                filledWidth, height, new Color(50, 220, 255)
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, filledWidth, height, height, height);
        }
    }
}
