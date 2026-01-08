package com.nexaria.launcher.ui;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.model.User;
import com.nexaria.launcher.news.NewsCard;
import com.nexaria.launcher.news.RSSFeedParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.*;
import java.util.List;

public class MainPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MainPanel.class);

    @SuppressWarnings("unused")
    private Runnable launchCallback;

    private ServerStatusCard serverCard;
    private JLabel greetingLabel;
    private JLabel versionLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private ModernButton launchButton;
    private javax.swing.Timer refreshTimer;
    private boolean playing = false;
    private Runnable onStopCallback;
    private JPanel newsContainer;

    public MainPanel(Runnable launchCallback) {
        this.launchCallback = launchCallback;

        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        greetingLabel = new JLabel("Bonjour, Joueur");
        greetingLabel.setIcon(FontIcon.of(FontAwesomeSolid.HOME, 18, DesignConstants.TEXT_PRIMARY));
        greetingLabel.setFont(DesignConstants.FONT_HEADER);
        greetingLabel.setForeground(DesignConstants.TEXT_PRIMARY);
        greetingLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topBar.add(greetingLabel, BorderLayout.WEST);

        serverCard = new ServerStatusCard();
        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        rightWrap.setOpaque(false);
        rightWrap.add(serverCard);
        topBar.add(rightWrap, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Zone centrale avec news
        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));

        // Section News
        JLabel newsTitle = new JLabel("📰  Actualités");
        newsTitle.setFont(DesignConstants.FONT_HEADER.deriveFont(20f));
        newsTitle.setForeground(DesignConstants.TEXT_PRIMARY);
        newsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        newsTitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        mainContent.add(newsTitle);

        // Container pour les cartes de news avec GridLayout adaptatif
        newsContainer = new JPanel();
        newsContainer.setOpaque(false);
        newsContainer.setLayout(new GridLayout(0, 3, 15, 15)); // 3 colonnes maximum
        newsContainer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Wrapper avec scroll si nécessaire
        JPanel newsWrapper = new JPanel(new BorderLayout());
        newsWrapper.setOpaque(false);
        newsWrapper.add(newsContainer, BorderLayout.CENTER);
        newsWrapper.setMaximumSize(new Dimension(1050, 400));
        
        mainContent.add(newsWrapper);
        mainContent.add(Box.createVerticalStrut(15));

        add(mainContent, BorderLayout.CENTER);

        // Bottom panel avec bouton de lancement
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Version label caché mais conservé pour la compatibilité
        versionLabel = new JLabel("");
        versionLabel.setVisible(false);

        statusLabel = new JLabel("En attente");
        statusLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        statusLabel.setFont(DesignConstants.FONT_SMALL);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(statusLabel);
        centerPanel.add(Box.createVerticalStrut(8));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);  // Désactiver l'affichage du pourcentage
        progressBar.setForeground(DesignConstants.PURPLE_ACCENT);
        progressBar.setBackground(new Color(255, 255, 255, 20));
        progressBar.setBorderPainted(false);
        progressBar.setMaximumSize(new Dimension(500, 6));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setVisible(false);
        centerPanel.add(progressBar);
        centerPanel.add(Box.createVerticalStrut(20));

        launchButton = new ModernButton("JOUER", DesignConstants.PURPLE_ACCENT,
            DesignConstants.PURPLE_ACCENT_DARK, true);
        launchButton.setPreferredSize(new Dimension(250, 60));
        launchButton.setMaximumSize(new Dimension(250, 60));
        launchButton.setFont(DesignConstants.FONT_TITLE);
        launchButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        launchButton.setIcon(FontIcon.of(FontAwesomeSolid.PLAY, 20, DesignConstants.TEXT_PRIMARY));
        launchButton.addActionListener(e -> {
            if (playing) {
                if (onStopCallback != null) onStopCallback.run();
            } else {
                launchCallback.run();
            }
        });
        centerPanel.add(launchButton);

        JPanel bottomWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomWrapper.setOpaque(false);
        bottomWrapper.add(centerPanel);
        add(bottomWrapper, BorderLayout.SOUTH);
    }

    public void setUserProfile(User user) {
        String pseudo = (user != null) ? user.getUsername() : "Inconnu";
        int hour = java.time.LocalTime.now().getHour();
        String salutation = (hour >= 6 && hour < 18) ? "Bonjour" : "Bonsoir";
        greetingLabel.setText("  " + salutation + ", " + pseudo);
    }

    public void setVersion(String minecraftVersion, String loaderName, String loaderVersion) {
        versionLabel.setText(String.format("Version : Minecraft %s - %s %s", minecraftVersion, loaderName, loaderVersion));
    }

    public void setStatus(String message) { statusLabel.setText(message); }
    public void setProgress(int progress) { progressBar.setValue(progress); }

    public void setButtonsEnabled(boolean enabled) {
        launchButton.setEnabled(enabled);
        launchButton.setVisible(enabled);
        progressBar.setVisible(!enabled);
    }

    public void setIndeterminate(boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
        progressBar.setVisible(indeterminate || !launchButton.isEnabled());
    }

    public void setPlaying(boolean playing, Runnable onStop) {
        this.playing = playing;
        this.onStopCallback = onStop;
        if (playing) {
            launchButton.setText("FERMER LE JEU");
            launchButton.setIcon(FontIcon.of(FontAwesomeSolid.TIMES, 18, DesignConstants.TEXT_PRIMARY));
            launchButton.setBackground(DesignConstants.PURPLE_ACCENT_DARK);
        } else {
            launchButton.setText("JOUER");
            launchButton.setIcon(FontIcon.of(FontAwesomeSolid.PLAY, 18, DesignConstants.TEXT_PRIMARY));
            launchButton.setBackground(DesignConstants.PURPLE_ACCENT);
        }
    }

    public void refreshServerStatus(String host, int port, String name) {
        SwingWorker<com.nexaria.launcher.model.ServerStatusInfo, Void> worker = new SwingWorker<com.nexaria.launcher.model.ServerStatusInfo, Void>(){
            @Override protected com.nexaria.launcher.model.ServerStatusInfo doInBackground() throws Exception {
                return com.nexaria.launcher.downloader.MinecraftServerPing.ping(host, port, name);
            }
            @Override protected void done() {
                try { serverCard.update(get()); } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    public void startServerStatusAutoRefresh(String host, int port, String name) {
        stopServerStatusAutoRefresh();
        refreshServerStatus(host, port, name);
        refreshTimer = new javax.swing.Timer(30_000, e -> refreshServerStatus(host, port, name));
        refreshTimer.setRepeats(true);
        refreshTimer.start();
    }

    public void stopServerStatusAutoRefresh() {
        if (refreshTimer != null) { refreshTimer.stop(); refreshTimer = null; }
    }

    public void reset() {
        setButtonsEnabled(true);
        setUserProfile(null);
        setStatus("Prêt");
        setProgress(0);
        setPlaying(false, null);
        stopServerStatusAutoRefresh();
    }

    public void loadNews(String azuriomUrl) {
        SwingWorker<List<RSSFeedParser.NewsItem>, Void> worker = new SwingWorker<List<RSSFeedParser.NewsItem>, Void>() {
            @Override
            protected List<RSSFeedParser.NewsItem> doInBackground() {
                String rssUrl = azuriomUrl + "/api/rss";
                logger.info("Chargement des actualités depuis: {}", rssUrl);
                return RSSFeedParser.fetchNews(rssUrl);
            }

            @Override
            protected void done() {
                try {
                    List<RSSFeedParser.NewsItem> news = get();
                    newsContainer.removeAll();

                    if (news.isEmpty()) {
                        // Message "Aucune actualité"
                        newsContainer.setLayout(new FlowLayout(FlowLayout.CENTER));
                        
                        JPanel noNewsPanel = new JPanel();
                        noNewsPanel.setOpaque(false);
                        noNewsPanel.setLayout(new BoxLayout(noNewsPanel, BoxLayout.Y_AXIS));
                        
                        JLabel icon = new JLabel();
                        icon.setIcon(FontIcon.of(FontAwesomeSolid.INBOX, 48, new Color(255, 255, 255, 100)));
                        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
                        
                        JLabel noNews = new JLabel("Aucune actualité disponible");
                        noNews.setFont(DesignConstants.FONT_REGULAR.deriveFont(14f));
                        noNews.setForeground(DesignConstants.TEXT_SECONDARY);
                        noNews.setAlignmentX(Component.CENTER_ALIGNMENT);
                        
                        noNewsPanel.add(icon);
                        noNewsPanel.add(Box.createVerticalStrut(10));
                        noNewsPanel.add(noNews);
                        
                        newsContainer.add(noNewsPanel);
                    } else {
                        // Adapter le layout en fonction du nombre de news
                        int columns = Math.min(news.size(), 3); // Max 3 colonnes
                        newsContainer.setLayout(new GridLayout(0, columns, 15, 15));
                        
                        for (RSSFeedParser.NewsItem item : news) {
                            newsContainer.add(new NewsCard(item));
                        }
                    }

                    newsContainer.revalidate();
                    newsContainer.repaint();
                    logger.info("Actualités chargées: {} news affichées", news.size());
                } catch (Exception e) {
                    logger.error("Erreur lors du chargement des actualités", e);
                    
                    // Afficher un message d'erreur
                    newsContainer.removeAll();
                    newsContainer.setLayout(new FlowLayout(FlowLayout.CENTER));
                    
                    JLabel errorLabel = new JLabel("Erreur de chargement des actualités");
                    errorLabel.setFont(DesignConstants.FONT_SMALL);
                    errorLabel.setForeground(new Color(255, 100, 100));
                    errorLabel.setIcon(FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 14, new Color(255, 100, 100)));
                    
                    newsContainer.add(errorLabel);
                    newsContainer.revalidate();
                    newsContainer.repaint();
                }
            }
        };
        worker.execute();
    }
}
