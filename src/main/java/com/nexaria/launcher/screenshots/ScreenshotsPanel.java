package com.nexaria.launcher.screenshots;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import com.nexaria.launcher.ui.DesignConstants;
import com.nexaria.launcher.ui.ModernButton;

/**
 * Panel pour afficher la galerie de screenshots
 */
public class ScreenshotsPanel extends JPanel {
    private JPanel gridPanel;
    private List<ScreenshotCard> cards;
    private Path screenshotsDir;
    private WatchService watchService;
    private Thread watchThread;

    public ScreenshotsPanel() {
        this.cards = new ArrayList<>();
        this.screenshotsDir = Paths.get("game/screenshots");

        setOpaque(false);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Galerie de Screenshots");
        title.setFont(DesignConstants.FONT_HEADER.deriveFont(20f));
        title.setForeground(DesignConstants.TEXT_PRIMARY);
        title.setIcon(FontIcon.of(FontAwesomeSolid.IMAGES, 22, DesignConstants.PURPLE_ACCENT));
        header.add(title, BorderLayout.WEST);

        ModernButton refreshBtn = new ModernButton("ACTUALISER", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        refreshBtn.setIcon(FontIcon.of(FontAwesomeSolid.SYNC, 14, DesignConstants.TEXT_PRIMARY));
        refreshBtn.addActionListener(e -> loadScreenshots());
        header.add(refreshBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Grille de screenshots
        gridPanel = new JPanel();
        gridPanel.setOpaque(false);
        gridPanel.setLayout(new GridLayout(0, 4, 15, 15)); // 4 colonnes
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Charger les screenshots
        loadScreenshots();

        // Démarrer la surveillance du dossier
        startWatching();
    }

    private void loadScreenshots() {
        gridPanel.removeAll();
        cards.clear();

        try {
            // Créer le dossier s'il n'existe pas
            Files.createDirectories(screenshotsDir);

            // Lister tous les fichiers PNG
            Files.list(screenshotsDir)
                    .filter(path -> path.toString().toLowerCase().endsWith(".png"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .forEach(path -> {
                        File file = path.toFile();
                        ScreenshotCard card = new ScreenshotCard(
                                file,
                                () -> viewScreenshot(file),
                                null, // Plus de partage Discord
                                () -> deleteScreenshot(file));
                        cards.add(card);
                        gridPanel.add(card);
                    });

            if (cards.isEmpty()) {
                JLabel noScreenshots = new JLabel("Aucun screenshot trouvé");
                noScreenshots.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
                noScreenshots.setForeground(new Color(255, 255, 255, 120));
                noScreenshots.setHorizontalAlignment(SwingConstants.CENTER);
                gridPanel.add(noScreenshots);
            }

        } catch (Exception e) {
            System.err.println("[ScreenshotsPanel] Erreur chargement: " + e.getMessage());
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void startWatching() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            screenshotsDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);

            watchThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        WatchKey key = watchService.take();
                        Thread.sleep(500); // Debounce
                        SwingUtilities.invokeLater(() -> loadScreenshots());
                        key.reset();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            watchThread.setDaemon(true);
            watchThread.start();

        } catch (Exception e) {
            System.err.println("[ScreenshotsPanel] Erreur surveillance: " + e.getMessage());
        }
    }

    private void viewScreenshot(File file) {
        // Créer une fenêtre lightbox pour afficher le screenshot en grand
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Screenshot", true);
        dialog.setLayout(new BorderLayout());

        try {
            ImageIcon icon = new ImageIcon(file.getAbsolutePath());

            // Redimensionner si trop grand
            int maxWidth = 1200;
            int maxHeight = 800;
            if (icon.getIconWidth() > maxWidth || icon.getIconHeight() > maxHeight) {
                Image img = icon.getImage();
                double ratio = Math.min((double) maxWidth / icon.getIconWidth(),
                        (double) maxHeight / icon.getIconHeight());
                int newWidth = (int) (icon.getIconWidth() * ratio);
                int newHeight = (int) (icon.getIconHeight() * ratio);
                icon = new ImageIcon(img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH));
            }

            JLabel imageLabel = new JLabel(icon);
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dialog.add(imageLabel, BorderLayout.CENTER);

            // Boutons d'action
            JPanel buttons = new JPanel(new FlowLayout());
            buttons.setBackground(new Color(30, 20, 40));

            ModernButton copyBtn = new ModernButton("COPIER CHEMIN", new Color(60, 120, 180),
                    new Color(40, 100, 160), true);
            copyBtn.setIcon(FontIcon.of(FontAwesomeSolid.COPY, 14, DesignConstants.TEXT_PRIMARY));
            copyBtn.addActionListener(e -> {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(file.getAbsolutePath()), null);
                JOptionPane.showMessageDialog(
                        dialog,
                        "Chemin copié dans le presse-papier !",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
            });
            buttons.add(copyBtn);

            ModernButton openFolderBtn = new ModernButton("OUVRIR DOSSIER", DesignConstants.PURPLE_ACCENT,
                    DesignConstants.PURPLE_ACCENT_DARK, true);
            openFolderBtn.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 14, DesignConstants.TEXT_PRIMARY));
            openFolderBtn.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(file.getParentFile());
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            dialog,
                            "Impossible d'ouvrir le dossier: " + ex.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            buttons.add(openFolderBtn);

            ModernButton deleteBtn = new ModernButton("SUPPRIMER", new Color(200, 60, 60),
                    new Color(160, 40, 40), true);
            deleteBtn.setIcon(FontIcon.of(FontAwesomeSolid.TRASH, 14, DesignConstants.TEXT_PRIMARY));
            deleteBtn.addActionListener(e -> {
                deleteScreenshot(file);
                dialog.dispose();
            });
            buttons.add(deleteBtn);

            dialog.add(buttons, BorderLayout.SOUTH);

        } catch (Exception e) {
            JLabel error = new JLabel("Erreur de chargement");
            error.setHorizontalAlignment(SwingConstants.CENTER);
            dialog.add(error, BorderLayout.CENTER);
        }

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void deleteScreenshot(File file) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Voulez-vous vraiment supprimer ce screenshot ?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            if (file.delete()) {
                loadScreenshots();
                JOptionPane.showMessageDialog(
                        this,
                        "Screenshot supprimé",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Impossible de supprimer le fichier",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void cleanup() {
        if (watchThread != null) {
            watchThread.interrupt();
        }
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (Exception e) {
            // Ignorer
        }
    }
}
