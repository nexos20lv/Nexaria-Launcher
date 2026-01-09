package com.nexaria.launcher.news;

import com.nexaria.launcher.ui.DesignConstants;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class NewsCard extends JPanel {
    private final RSSFeedParser.NewsItem newsItem;
    private boolean isHovered = false;
    private Color currentBorderColor = new Color(255, 255, 255, 30);
    private int borderWidth = 1;
    private static final int CORNER_RADIUS = 15;

    public NewsCard(RSSFeedParser.NewsItem item) {
        super();
        this.newsItem = item;
        setOpaque(false);
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(320, 160));
        setMinimumSize(new Dimension(280, 160));
        setMaximumSize(new Dimension(400, 160));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Panel principal
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BorderLayout(10, 10));

        // Titre avec icône
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        titlePanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(FontIcon.of(FontAwesomeSolid.NEWSPAPER, 18, DesignConstants.PURPLE_ACCENT));
        
        JLabel titleLabel = new JLabel("<html><b>" + truncate(item.getTitle(), 35) + "</b></html>");
        titleLabel.setFont(DesignConstants.FONT_HEADER.deriveFont(15f));
        titleLabel.setForeground(DesignConstants.TEXT_PRIMARY);
        
        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        // Description scrollable si nécessaire
        String desc = item.getDescription();
        if (desc == null || desc.isEmpty()) {
            desc = stripHtml(item.getContent());
        }
        
        JTextArea descArea = new JTextArea(desc);
        descArea.setFont(DesignConstants.FONT_SMALL.deriveFont(12f));
        descArea.setForeground(DesignConstants.TEXT_SECONDARY);
        descArea.setOpaque(false);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFocusable(false);
        descArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // Footer avec auteur et date
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        
        JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftFooter.setOpaque(false);
        
        if (item.getAuthor() != null && !item.getAuthor().isEmpty()) {
            JLabel authorLabel = new JLabel(item.getAuthor());
            authorLabel.setFont(DesignConstants.FONT_SMALL.deriveFont(10f));
            authorLabel.setForeground(new Color(200, 200, 200, 200));
            authorLabel.setIcon(FontIcon.of(FontAwesomeSolid.USER, 10, new Color(160, 120, 255, 180)));
            leftFooter.add(authorLabel);
        }
        
        JLabel dateLabel = new JLabel(item.getFormattedDate());
        dateLabel.setFont(DesignConstants.FONT_SMALL.deriveFont(10f));
        dateLabel.setForeground(new Color(200, 200, 200, 200));
        dateLabel.setIcon(FontIcon.of(FontAwesomeSolid.CALENDAR_ALT, 10, new Color(160, 120, 255, 180)));
        leftFooter.add(dateLabel);
        
        // Indicateur "Lire plus"
        JLabel readMoreLabel = new JLabel("Lire plus");
        readMoreLabel.setFont(DesignConstants.FONT_SMALL.deriveFont(Font.ITALIC, 10f));
        readMoreLabel.setForeground(DesignConstants.PURPLE_ACCENT);
        readMoreLabel.setIcon(FontIcon.of(FontAwesomeSolid.ARROW_RIGHT, 9, DesignConstants.PURPLE_ACCENT));
        readMoreLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        
        footerPanel.add(leftFooter, BorderLayout.WEST);
        footerPanel.add(readMoreLabel, BorderLayout.EAST);

        // Layout final
        contentPanel.add(titlePanel, BorderLayout.NORTH);
        contentPanel.add(descArea, BorderLayout.CENTER);
        contentPanel.add(footerPanel, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);

        // Animations au survol
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openLink(item.getLink());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                currentBorderColor = DesignConstants.PURPLE_ACCENT;
                borderWidth = 2;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                currentBorderColor = new Color(255, 255, 255, 30);
                borderWidth = 1;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background avec effet glassmorphism
        if (isHovered) {
            g2d.setColor(new Color(255, 255, 255, 30));
        } else {
            g2d.setColor(new Color(255, 255, 255, 15));
        }
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);

        // Gradient wash
        GradientPaint wash = new GradientPaint(
                0, 0, new Color(255, 255, 255, 20),
                getWidth(), getHeight(), new Color(255, 255, 255, 0));
        g2d.setPaint(wash);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);

        // Bordure arrondie
        g2d.setColor(currentBorderColor);
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.drawRoundRect(
            borderWidth / 2, 
            borderWidth / 2, 
            getWidth() - borderWidth, 
            getHeight() - borderWidth, 
            CORNER_RADIUS, 
            CORNER_RADIUS
        );

        g2d.dispose();
        super.paintComponent(g);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .trim();
    }

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            System.err.println("Impossible d'ouvrir le lien: " + url);
        }
    }
}
