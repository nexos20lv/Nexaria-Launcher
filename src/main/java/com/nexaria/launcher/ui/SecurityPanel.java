package com.nexaria.launcher.ui;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.security.AntiCheatService;
import com.nexaria.launcher.security.DataVerificationService;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class SecurityPanel extends JPanel {
    private final JTextArea output;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    public SecurityPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        JButton scanBtn = new ModernButton("Scanner", new Color(255,255,255,20), new Color(255,255,255,40));
        JButton repairBtn = new ModernButton("Réparer", new Color(255,255,255,20), new Color(255,255,255,40));
        JButton openQuarantineBtn = new ModernButton("Ouvrir Quarantaine", new Color(255,255,255,20), new Color(255,255,255,40));
        top.add(scanBtn);
        top.add(repairBtn);
        top.add(openQuarantineBtn);
        add(top, BorderLayout.NORTH);

        // Progress et status
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setOpaque(false);
        statusLabel = new JLabel("Prêt");
        statusLabel.setForeground(DesignConstants.TEXT_SECONDARY);
        statusLabel.setFont(DesignConstants.FONT_SMALL);
        progressBar = new JProgressBar();
        progressBar.setOpaque(false);
        progressBar.setForeground(new Color(170, 80, 255));
        progressBar.setString("");
        progressBar.setStringPainted(false);
        progressBar.setVisible(false);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(statusLabel, BorderLayout.EAST);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        add(progressPanel, BorderLayout.NORTH);

        output = new JTextArea();
        output.setEditable(false);
        output.setOpaque(false);
        output.setForeground(DesignConstants.TEXT_PRIMARY);
        output.setFont(DesignConstants.FONT_REGULAR);
        JScrollPane scroll = new JScrollPane(output);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        scanBtn.addActionListener(e -> runScan());
        openQuarantineBtn.addActionListener(e -> openQuarantine());
        repairBtn.addActionListener(e -> runRepair());

        runScan();
    }

    private void runScan() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            StringBuilder sb = new StringBuilder();
            @Override
            protected Void doInBackground() throws Exception {
                updateUI("Scan en cours...", 0, true);

                // Anti-cheat
                AntiCheatService ac = new AntiCheatService();
                List<String> banned = ac.detectBannedProcesses();
                if (banned.isEmpty()) sb.append("[OK] Processus: aucun interdit détecté\n\n");
                else sb.append("[ERREUR] Processus interdits: ").append(banned).append("\n\n");
                
                updateUI("Vérification data...", 50, true);

                // Data verification (mods + config)
                Path gameDir = Paths.get(LauncherConfig.getGameDir());
                DataVerificationService dvs = new DataVerificationService(gameDir, "strict");
                try {
                    DataVerificationService.Result dr = dvs.verify();
                    if (dr.ok) {
                        sb.append("[OK] Data: intégrité complète\n\n");
                    } else {
                        sb.append("[ERREUR] Data:\n");
                        if (!dr.missing.isEmpty()) sb.append("  - Manquants: ").append(dr.missing).append("\n");
                        if (!dr.unexpected.isEmpty()) sb.append("  - Non attendus: ").append(dr.unexpected).append("\n");
                        if (!dr.modified.isEmpty()) sb.append("  - Modifiés: ").append(dr.modified).append("\n");
                        sb.append("\n");
                    }
                } catch (Exception ex) {
                    sb.append("[ERREUR] Vérification data: ").append(ex.getMessage()).append("\n\n");
                }
                updateUI("Scan quarantaine...", 90, true);

                // Quarantaine
                Path quarantine = Paths.get(LauncherConfig.getModsDir(), "quarantine");
                try {
                    if (Files.isDirectory(quarantine)) {
                        var list = Files.list(quarantine).collect(Collectors.toList());
                        if (list.isEmpty()) sb.append("[OK] Quarantaine: vide\n");
                        else {
                            sb.append("[AVERTISSEMENT] Quarantaine (" + list.size() + " éléments):\n");
                            for (Path p : list) sb.append("  - ").append(p.getFileName()).append("\n");
                        }
                    } else sb.append("[OK] Quarantaine: dossier absent\n");
                } catch (Exception ex) {
                    sb.append("[ERREUR] Lecture quarantaine: ").append(ex.getMessage()).append("\n");
                }

                return null;
            }

            @Override
            protected void done() {
                output.setText(sb.toString());
                updateUI("Scan complété", 100, false);
            }
        };
        worker.execute();
    }

    private void updateUI(String status, int progress, boolean indeterminate) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            progressBar.setValue(progress);
            progressBar.setIndeterminate(indeterminate);
        });
    }

    private void openQuarantine() {
        try {
            Path q = Paths.get(LauncherConfig.getModsDir(), "quarantine");
            if (!Files.isDirectory(q)) Files.createDirectories(q);
            Desktop.getDesktop().open(q.toFile());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Impossible d'ouvrir la quarantaine: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runRepair() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            StringBuilder sb = new StringBuilder();
            @Override
            protected Void doInBackground() throws Exception {
                output.setText("Réparation en cours...\n");
                updateUI("Réparation data...", 0, true);

                Path gameDir = Paths.get(LauncherConfig.getGameDir());
                Path localDataDir = Paths.get("data");

                try {
                    DataVerificationService dvs = new DataVerificationService(gameDir, "strict");
                    DataVerificationService.Result res = dvs.verify();

                    // Quarantaine pour fichiers inattendus et modifiés
                    Path quarantine = gameDir.resolve("quarantine");
                    Files.createDirectories(quarantine);
                    for (String path : res.unexpected) {
                        Path f = gameDir.resolve(path);
                        if (Files.exists(f)) move(f, quarantine.resolve(path.replace("/", "_")));
                    }
                    for (String path : res.modified) {
                        Path f = gameDir.resolve(path);
                        if (Files.exists(f)) move(f, quarantine.resolve(path.replace("/", "_") + ".corrupt"));
                    }

                    // Restaurer fichiers manquants et modifiés depuis data/ local
                    for (String path : res.missing) {
                        Path src = localDataDir.resolve(path);
                        Path dst = gameDir.resolve(path);
                        copyIfExists(src, dst);
                    }
                    for (String path : res.modified) {
                        Path src = localDataDir.resolve(path);
                        Path dst = gameDir.resolve(path);
                        copyIfExists(src, dst);
                    }

                    // Re-scan
                    DataVerificationService.Result after = dvs.verify();
                    if (after.ok) sb.append("[OK] Data réparé: intégrité complète\n");
                    else sb.append("[AVERTISSEMENT] Data: ").append(after.missing.size())
                            .append(" manquants, ").append(after.unexpected.size())
                            .append(" inattendus, ").append(after.modified.size()).append(" modifiés\n");
                } catch (Exception ex) {
                    sb.append("[ERREUR] Réparation: ").append(ex.getMessage()).append("\n");
                }

                return null;
            }

            @Override
            protected void done() {
                output.setText(sb.toString());
                updateUI("Réparation terminée", 100, false);
            }
        };
        worker.execute();
    }

    private void move(Path src, Path dst) {
        try {
            if (Files.exists(src)) {
                Files.createDirectories(dst.getParent());
                Files.move(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) { }
    }

    private void copyIfExists(Path src, Path dst) {
        try {
            if (Files.exists(src)) {
                Files.createDirectories(dst.getParent());
                Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) { }
    }
}
