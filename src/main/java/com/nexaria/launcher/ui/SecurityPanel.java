package com.nexaria.launcher.ui;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.security.AntiCheatService;
import com.nexaria.launcher.security.ConfigVerificationService;
import com.nexaria.launcher.security.ModVerificationService;

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
        JButton regenBtn = new ModernButton("Regénérer manifests", new Color(255,255,255,20), new Color(255,255,255,40));
        JButton openQuarantineBtn = new ModernButton("Ouvrir Quarantaine", new Color(255,255,255,20), new Color(255,255,255,40));
        top.add(scanBtn);
        top.add(repairBtn);
        top.add(regenBtn);
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
        regenBtn.addActionListener(e -> runRegenerateManifests());

        runScan();
    }

    private void runScan() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            StringBuilder sb = new StringBuilder();
            @Override
            protected Void doInBackground() throws Exception {
                updateUI("Analyse en cours...", 0, true);

                // Anti-cheat: processus interdits
                AntiCheatService ac = new AntiCheatService();
                List<String> banned = ac.detectBannedProcesses();
                if (banned.isEmpty()) sb.append("[OK] Processus interdits: aucun détecté\n\n");
                else sb.append("[ERREUR] Processus interdits détectés: ").append(banned).append("\n\n");
                updateUI("Scan mods...", 33, true);

                // Mods verification
                ModVerificationService mvs = new ModVerificationService(Paths.get(LauncherConfig.getModsDir()));
                try {
                    ModVerificationService.VerificationResult mr = mvs.verify();
                    if (mr.isClean()) sb.append("[OK] Mods: intégrité OK\n");
                    else {
                        sb.append("[ERREUR] Mods:\n");
                        if (!mr.missingRequired.isEmpty()) sb.append("  - Manquants: ").append(mr.missingRequired).append("\n");
                        if (!mr.unexpected.isEmpty()) sb.append("  - Non attendus: ").append(mr.unexpected).append("\n");
                        if (!mr.hashMismatch.isEmpty()) sb.append("  - Hash invalide: ").append(mr.hashMismatch).append("\n");
                    }
                    sb.append("\n");
                } catch (Exception ex) {
                    sb.append("[ERREUR] Vérification mods: ").append(ex.getMessage()).append("\n\n");
                }
                updateUI("Scan configs...", 66, true);

                // Configs verification
                ConfigVerificationService cvs = new ConfigVerificationService(Paths.get(LauncherConfig.getConfigsDir()));
                try {
                    ConfigVerificationService.Result cr = cvs.verify();
                    if (cr.isClean()) sb.append("[OK] Configs: intégrité OK\n");
                    else {
                        sb.append("[ERREUR] Configs:\n");
                        if (!cr.missing.isEmpty()) sb.append("  - Manquantes: ").append(cr.missing).append("\n");
                        if (!cr.unexpected.isEmpty()) sb.append("  - Non attendues: ").append(cr.unexpected).append("\n");
                        if (!cr.badHash.isEmpty()) sb.append("  - Hash invalide: ").append(cr.badHash).append("\n");
                    }
                    sb.append("\n");
                } catch (Exception ex) {
                    sb.append("[ERREUR] Vérification configs: ").append(ex.getMessage()).append("\n\n");
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

    private void runRegenerateManifests() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            StringBuilder sb = new StringBuilder();
            @Override
            protected Void doInBackground() throws Exception {
                output.setText("Regénération des manifests...\n");
                try {
                    Path dataMods = Paths.get("data", "mods");
                    Path dataCfg = Paths.get("data", "configs");
                    Path outMods = Paths.get("data", "configs", "mods-manifest.json");
                    Path outCfg = Paths.get("data", "configs", "configs-manifest.json");
                    com.nexaria.launcher.security.ManifestGenerator.generateModsManifest(dataMods, outMods, true);
                    com.nexaria.launcher.security.ManifestGenerator.generateConfigsManifest(dataCfg, outCfg, true);

                    // Copier vers game/configs
                    Path gameCfg = Paths.get(LauncherConfig.getConfigsDir());
                    Files.createDirectories(gameCfg);
                    Files.copy(outMods, gameCfg.resolve("mods-manifest.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(outCfg, gameCfg.resolve("configs-manifest.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    sb.append("Manifests regénérés et copiés vers game/configs\n");
                } catch (Exception ex) {
                    sb.append("Erreur regénération manifests: ").append(ex.getMessage()).append("\n");
                }

                // Re-scan
                runScan();
                return null;
            }

            @Override
            protected void done() {
                output.append(sb.toString());
            }
        };
        worker.execute();
    }

    private void runRepair() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            StringBuilder sb = new StringBuilder();
            @Override
            protected Void doInBackground() throws Exception {
                output.setText("Réparation en cours...\n");
                updateUI("Réparation mods...", 0, true);

                Path modsDir = Paths.get(LauncherConfig.getModsDir());
                Path dataMods = Paths.get("data", "mods");
                Path cfgDir = Paths.get(LauncherConfig.getConfigsDir());
                Path dataCfg = Paths.get("data", "configs");

                // 1) Mods: mettre en quarantaine les inattendus/mismatch puis restaurer depuis data/
                try {
                    ModVerificationService mvs = new ModVerificationService(modsDir);
                    ModVerificationService.VerificationResult res = mvs.verify();

                    // Quarantaine pour inattendus et badhash
                    Path quarantine = modsDir.resolve("quarantine");
                    Files.createDirectories(quarantine);
                    for (String name : res.unexpected) move(modsDir.resolve(name), quarantine.resolve(name));
                    for (String name : res.hashMismatch) move(modsDir.resolve(name), quarantine.resolve(name + ".badhash"));

                    // Restaurer manquants et badhash depuis data/mods (si disponible)
                    for (String name : res.missingRequired) copyIfExists(dataMods.resolve(name), modsDir.resolve(name));
                    for (String name : res.hashMismatch) copyIfExists(dataMods.resolve(name), modsDir.resolve(name));

                    // Re-scan
                    ModVerificationService.VerificationResult after = mvs.verify();
                    if (after.isClean()) sb.append("[OK] Mods réparés: intégrité OK\n");
                    else sb.append("[AVERTISSEMENT] Mods: encore des écarts -> ")
                            .append("manquants=").append(after.missingRequired)
                            .append(", inattendus=").append(after.unexpected)
                            .append(", badhash=").append(after.hashMismatch).append("\n");
                } catch (Exception ex) {
                    sb.append("[ERREUR] Réparation mods: ").append(ex.getMessage()).append("\n");
                }
                updateUI("Réparation configs...", 50, true);

                // 2) Configs: déplacer inattendus vers quarantine et remplacer manquants/badhash depuis data/configs
                try {
                    ConfigVerificationService cvs = new ConfigVerificationService(cfgDir);
                    ConfigVerificationService.Result cr = cvs.verify();

                    Path quarantineCfg = cfgDir.resolve("quarantine");
                    Files.createDirectories(quarantineCfg);
                    for (String name : cr.unexpected) move(cfgDir.resolve(name), quarantineCfg.resolve(name));
                    for (String name : cr.badHash) move(cfgDir.resolve(name), quarantineCfg.resolve(name + ".badhash"));

                    for (String name : cr.missing) copyIfExists(dataCfg.resolve(name), cfgDir.resolve(name));
                    for (String name : cr.badHash) copyIfExists(dataCfg.resolve(name), cfgDir.resolve(name));

                    ConfigVerificationService.Result afterCfg = cvs.verify();
                    if (afterCfg.isClean()) sb.append("[OK] Configs réparées: intégrité OK\n");
                    else sb.append("[AVERTISSEMENT] Configs: encore des écarts -> ")
                            .append("manquantes=").append(afterCfg.missing)
                            .append(", inattendues=").append(afterCfg.unexpected)
                            .append(", badhash=").append(afterCfg.badHash).append("\n");
                } catch (Exception ex) {
                    sb.append("[ERREUR] Réparation configs: ").append(ex.getMessage()).append("\n");
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
                if (Files.exists(dst)) Files.delete(dst);
                Files.createDirectories(dst.getParent());
                Files.move(src, dst);
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
