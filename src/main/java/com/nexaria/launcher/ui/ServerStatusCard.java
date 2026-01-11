package com.nexaria.launcher.ui;

import com.nexaria.launcher.model.ServerStatusInfo;

import javax.swing.*;
import java.awt.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class ServerStatusCard extends JPanel {
    private JLabel nameLabel;
    private JLabel statusLabel;
    private JLabel playersLabel;
    private JLabel iconLabel;

    public ServerStatusCard() {
        setOpaque(false);
        setLayout(new BorderLayout());

        GlassPanel card = new GlassPanel(18);
        card.setOpaque(false);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(40, 40));
        iconLabel.setOpaque(false);
        card.add(iconLabel, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        nameLabel = new JLabel("Serveur");
        nameLabel.setForeground(DesignConstants.TEXT_PRIMARY);
        nameLabel.setFont(DesignConstants.FONT_REGULAR);
        statusLabel = new JLabel("Statut");
        statusLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        statusLabel.setFont(DesignConstants.FONT_SMALL);
        playersLabel = new JLabel("Joueurs: -/-");
        playersLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        playersLabel.setFont(DesignConstants.FONT_SMALL);
        center.add(nameLabel);
        center.add(statusLabel);
        center.add(playersLabel);
        card.add(center, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    public void update(ServerStatusInfo info) {
        if (info == null)
            return;
        nameLabel.setText(info.name);
        if (info.online) {
            statusLabel.setText("En ligne • " + info.pingMs + " ms");
            statusLabel.setForeground(new Color(180, 255, 200));
        } else {
            statusLabel.setText("Hors ligne");
            statusLabel.setForeground(new Color(255, 180, 180));
        }
        if (info.playersOnline >= 0 && info.playersMax >= 0) {
            playersLabel.setText("Joueurs: " + info.playersOnline + "/" + info.playersMax);
        } else {
            playersLabel.setText("Joueurs: N/A");
        }
        if (info.favicon != null) {
            Image scaled = info.favicon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaled));
        } else {
            // Icône serveur par défaut
            iconLabel.setIcon(FontIcon.of(FontAwesomeSolid.SERVER, 20, DesignConstants.TEXT_PRIMARY));
        }

        // Construction du tooltip détaillé
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        if (info.description != null && !info.description.isEmpty()) {
            sb.append("<b>Message:</b><br/>");
            // Basic handling of color codes not implemented effectively in tooltip without
            // parsing,
            // but we display raw text or sanitized text.
            sb.append(info.description.replace("\n", "<br/>")).append("<br/><br/>");
        }

        sb.append("<b>Joueurs connectés:</b><br/>");
        if (info.playerList != null && !info.playerList.isEmpty()) {
            int count = 0;
            for (String p : info.playerList) {
                sb.append("• ").append(p).append("<br/>");
                count++;
                if (count >= 10) {
                    sb.append("<i>...et ").append(info.playersOnline - 10).append(" autres</i>");
                    break;
                }
            }
        } else {
            sb.append("<i>Aucun joueur listé</i>");
        }
        sb.append("</html>");

        setToolTipText(sb.toString());
    }
}
