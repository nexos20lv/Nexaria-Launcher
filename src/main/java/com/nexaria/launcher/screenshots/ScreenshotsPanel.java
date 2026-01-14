package com.nexaria.launcher.screenshots;

import javax.swing.*;
import java.awt.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import com.nexaria.launcher.ui.DesignConstants;
import com.nexaria.launcher.ui.ModernButton;
import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.util.ImageSelection;

/**
 * Panel pour afficher la galerie de screenshots avec pagination (3x3)
 */
public class ScreenshotsPanel extends JPanel {
    private static final String VIEW_GRID = "GRID";
    private static final String VIEW_DETAIL = "DETAIL";
    private static final int ITEMS_PER_PAGE = 9;

    private CardLayout cardLayout;
    private JPanel mainContainer;

    // Grid View
    private JPanel gridViewPanel; // Container global de la vue grille
    private JPanel gridContent; // La grille 3x3
    private JLabel pageIndicator;
    private ModernButton prevBtn;
    private ModernButton nextBtn;

    // Data
    private List<File> allScreenshots;
    private int currentPage = 0;

    // Detail View
    private JPanel detailPanel;
    private JLabel detailImageLabel;
    private File currentDetailFile;

    private Path screenshotsDir;
    private WatchService watchService;
    private Thread watchThread;

    public ScreenshotsPanel() {
        this.allScreenshots = new ArrayList<>();
        this.screenshotsDir = Paths.get(LauncherConfig.getGameDir(), "screenshots");

        setOpaque(false);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setOpaque(false);

        initGridView();
        initDetailView();

        mainContainer.add(gridViewPanel, VIEW_GRID);
        mainContainer.add(detailPanel, VIEW_DETAIL);

        add(mainContainer, BorderLayout.CENTER);

        // Charger les screenshots
        loadScreenshots();

        // Démarrer la surveillance du dossier
        startWatching();
    }

    private void initGridView() {
        gridViewPanel = new JPanel(new BorderLayout());
        gridViewPanel.setOpaque(false);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 30)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

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

        gridViewPanel.add(header, BorderLayout.NORTH);

        // Grille content (3x3 fixe)
        gridContent = new JPanel(new GridLayout(3, 3, 15, 15));
        gridContent.setOpaque(false);
        gridContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));

        gridViewPanel.add(gridContent, BorderLayout.CENTER);

        // Pagination controls
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        paginationPanel.setOpaque(false);
        paginationPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));

        prevBtn = new ModernButton("Précédent", new Color(60, 60, 70), new Color(40, 40, 50), true);
        prevBtn.setIcon(FontIcon.of(FontAwesomeSolid.CHEVRON_LEFT, 12, Color.WHITE));
        prevBtn.addActionListener(e -> prevPage());
        paginationPanel.add(prevBtn);

        pageIndicator = new JLabel("Page 1 / 1");
        pageIndicator.setForeground(Color.WHITE);
        pageIndicator.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
        paginationPanel.add(pageIndicator);

        nextBtn = new ModernButton("Suivant", new Color(60, 60, 70), new Color(40, 40, 50), true);
        nextBtn.setIcon(FontIcon.of(FontAwesomeSolid.CHEVRON_RIGHT, 12, Color.WHITE));
        nextBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        nextBtn.addActionListener(e -> nextPage());
        paginationPanel.add(nextBtn);

        gridViewPanel.add(paginationPanel, BorderLayout.SOUTH);
    }

    private void initDetailView() {
        detailPanel = new JPanel(new BorderLayout());
        detailPanel.setOpaque(false);

        // Image display area
        detailImageLabel = new JLabel();
        detailImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        detailPanel.add(detailImageLabel, BorderLayout.CENTER);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        toolbar.setBackground(new Color(30, 20, 40, 200));

        // Back Button
        ModernButton backBtn = new ModernButton("RETOUR", new Color(100, 100, 100), new Color(80, 80, 80), true);
        backBtn.setIcon(FontIcon.of(FontAwesomeSolid.ARROW_LEFT, 14, DesignConstants.TEXT_PRIMARY));
        backBtn.addActionListener(e -> showGrid());
        toolbar.add(backBtn);

        // Actions
        ModernButton copyBtn = new ModernButton("COPIER IMAGE", new Color(60, 120, 180), new Color(40, 100, 160), true);
        copyBtn.setIcon(FontIcon.of(FontAwesomeSolid.COPY, 14, DesignConstants.TEXT_PRIMARY));
        copyBtn.addActionListener(e -> copyCurrentToClipboard());
        toolbar.add(copyBtn);

        ModernButton folderBtn = new ModernButton("DOSSIER", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, true);
        folderBtn.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 14, DesignConstants.TEXT_PRIMARY));
        folderBtn.addActionListener(e -> openCurrentFolder());
        toolbar.add(folderBtn);

        ModernButton deleteBtn = new ModernButton("SUPPRIMER", new Color(200, 60, 60), new Color(160, 40, 40), true);
        deleteBtn.setIcon(FontIcon.of(FontAwesomeSolid.TRASH, 14, DesignConstants.TEXT_PRIMARY));
        deleteBtn.addActionListener(e -> deleteCurrent());
        toolbar.add(deleteBtn);

        detailPanel.add(toolbar, BorderLayout.SOUTH);
    }

    private void showGrid() {
        cardLayout.show(mainContainer, VIEW_GRID);
        currentDetailFile = null;
        detailImageLabel.setIcon(null); // Clear memory
    }

    private void showDetail(File file) {
        currentDetailFile = file;

        SwingUtilities.invokeLater(() -> {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    // Fit to container logic (simplified)
                    int maxWidth = getWidth() - 40;
                    int maxHeight = getHeight() - 100;
                    if (maxWidth <= 0)
                        maxWidth = 800;
                    if (maxHeight <= 0)
                        maxHeight = 600;

                    Image scaled = getScaledImage(img, maxWidth, maxHeight);
                    detailImageLabel.setIcon(new ImageIcon(scaled));
                }
            } catch (Exception e) {
                detailImageLabel.setText("Erreur de chargement");
            }
        });

        cardLayout.show(mainContainer, VIEW_DETAIL);
    }

    private Image getScaledImage(BufferedImage src, int w, int h) {
        double ratio = Math.min((double) w / src.getWidth(), (double) h / src.getHeight());
        if (ratio >= 1.0)
            return src;
        int newW = (int) (src.getWidth() * ratio);
        int newH = (int) (src.getHeight() * ratio);
        return src.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
    }

    private void loadScreenshots() {
        try {
            Files.createDirectories(screenshotsDir);

            // Reload list from disk
            allScreenshots = Files.list(screenshotsDir)
                    .filter(path -> path.toString().toLowerCase().endsWith(".png"))
                    .map(Path::toFile)
                    .sorted((a, b) -> Long.compare(b.lastModified(), a.lastModified()))
                    .collect(Collectors.toList());

            // Reset to page 0 if out of bounds or just fresh load
            if (currentPage * ITEMS_PER_PAGE >= allScreenshots.size()) {
                currentPage = 0;
            }

            refreshGrid();

        } catch (Exception e) {
            System.err.println("[ScreenshotsPanel] Erreur chargement: " + e.getMessage());
            allScreenshots.clear();
            refreshGrid();
        }
    }

    private void refreshGrid() {
        gridContent.removeAll();

        int totalItems = allScreenshots.size();
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (totalPages == 0)
            totalPages = 1;

        if (currentPage >= totalPages)
            currentPage = totalPages - 1;
        if (currentPage < 0)
            currentPage = 0;

        pageIndicator.setText("Page " + (currentPage + 1) + " / " + totalPages);

        prevBtn.setEnabled(currentPage > 0);
        nextBtn.setEnabled(currentPage < totalPages - 1);

        if (allScreenshots.isEmpty()) {
            JLabel noScreenshots = new JLabel("Aucun screenshot trouvé");
            noScreenshots.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
            noScreenshots.setForeground(new Color(255, 255, 255, 120));
            noScreenshots.setHorizontalAlignment(SwingConstants.CENTER);
            gridContent.add(noScreenshots); // Will take 1st cell
            // Fill rest with empty to keep grid shape if needed, but GridLayout handles it
            // ok
        } else {
            int start = currentPage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

            for (int i = start; i < end; i++) {
                File file = allScreenshots.get(i);
                ScreenshotCard card = new ScreenshotCard(
                        file,
                        () -> showDetail(file),
                        null,
                        () -> deleteScreenshot(file));
                gridContent.add(card);
            }
        }

        gridContent.revalidate();
        gridContent.repaint();
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            refreshGrid();
        }
    }

    private void nextPage() {
        int totalPages = (int) Math.ceil((double) allScreenshots.size() / ITEMS_PER_PAGE);
        if (currentPage < totalPages - 1) {
            currentPage++;
            refreshGrid();
        }
    }

    private void copyCurrentToClipboard() {
        if (currentDetailFile == null)
            return;
        try {
            Image img = ImageIO.read(currentDetailFile);
            ImageSelection trans = new ImageSelection(img);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trans, null);
            JOptionPane.showMessageDialog(this, "Image copiée !", "Succès", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur copie: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCurrentFolder() {
        if (currentDetailFile == null)
            return;
        try {
            Desktop.getDesktop().open(currentDetailFile.getParentFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur ouverture dossier", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteCurrent() {
        if (currentDetailFile == null)
            return;
        int choice = JOptionPane.showConfirmDialog(this, "Supprimer ce screenshot ?", "Confirmation",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            if (currentDetailFile.delete()) {
                showGrid();
                loadScreenshots();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur suppression", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
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
                        SwingUtilities.invokeLater(() -> {
                            // Only reload if we are in grid view
                            if (currentDetailFile == null) {
                                loadScreenshots();
                            }
                        });
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

    // Kept for compatibility with ScreenshotCard calls if needed
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
