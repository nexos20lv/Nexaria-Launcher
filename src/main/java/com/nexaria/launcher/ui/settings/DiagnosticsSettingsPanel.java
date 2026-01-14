package com.nexaria.launcher.ui.settings;

import com.nexaria.launcher.config.LauncherConfig;
import com.nexaria.launcher.services.support.LogUploadService;
import com.nexaria.launcher.ui.DesignConstants;
import com.nexaria.launcher.ui.ModernButton;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DiagnosticsSettingsPanel extends SettingsTabPanel {
    private final LauncherConfig cfg;

    public DiagnosticsSettingsPanel() {
        super("Diagnostics & Logs");
        this.cfg = LauncherConfig.getInstance();
        initUI();
    }

    private void initUI() {
        add(Box.createVerticalStrut(20));

        addSubsection("Journalisation");
        add(Box.createVerticalStrut(10));
        JPanel diagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        diagPanel.setOpaque(false);
        diagPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton exportLogsBtn = new ModernButton("EXPORTER LOGS (ZIP)", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        exportLogsBtn.setIcon(FontIcon.of(FontAwesomeSolid.FILE_ARCHIVE, 16, DesignConstants.TEXT_PRIMARY));
        exportLogsBtn.addActionListener(e -> {
            try {
                File zip = new File(LauncherConfig.getCacheDir(), "logs-" + System.currentTimeMillis() + ".zip");
                zipLogs(zip);
                JOptionPane.showMessageDialog(this, "Logs exportés: " + zip.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Échec export logs.", "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        ModernButton uploadLogsBtn = new ModernButton("ENVOYER LOGS (SUPPORT)", new Color(60, 140, 60),
                new Color(40, 100, 40), false);
        uploadLogsBtn.setIcon(FontIcon.of(FontAwesomeSolid.CLOUD_UPLOAD_ALT, 16, DesignConstants.TEXT_PRIMARY));
        uploadLogsBtn.addActionListener(e -> {
            handleLogUpload();
        });

        ModernButton testConnBtn = new ModernButton("TESTER CONNECTIVITÉ", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        testConnBtn.setIcon(FontIcon.of(FontAwesomeSolid.WIFI, 16, DesignConstants.TEXT_PRIMARY));
        testConnBtn.addActionListener(e -> testConnectivity());

        diagPanel.add(exportLogsBtn);
        diagPanel.add(uploadLogsBtn);
        diagPanel.add(testConnBtn);

        add(diagPanel);
        add(Box.createVerticalGlue());
    }

    private void handleLogUpload() {
        // Log file is typically at .nexaria/launcher.log (defined in logback.xml or by
        // convention)
        // We can get it from user.home/.nexaria/launcher.log or assume cache dir for
        // standard logs if redirected
        // Based on logback.xml seen: ${user.home}/.nexaria/launcher.log
        File logFile = new File(System.getProperty("user.home") + "/.nexaria/launcher.log");
        if (!logFile.exists()) {
            JOptionPane.showMessageDialog(this, "Aucun fichier de log trouvé.", "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int res = JOptionPane.showConfirmDialog(this,
                "Ceci va envoyer vos logs de launcher sur mclo.gs (service public).\n" +
                        "Le lien sera copié dans votre presse-papiers.\n\nContinuer ?",
                "Upload Logs", JOptionPane.YES_NO_OPTION);

        if (res != JOptionPane.YES_OPTION)
            return;

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                LogUploadService service = new LogUploadService();
                return service.uploadLog(logFile);
            }

            @Override
            protected void done() {
                try {
                    String url = get();
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
                    JOptionPane.showMessageDialog(DiagnosticsSettingsPanel.this,
                            "Upload réussi !\nLien copié: " + url, "Succès", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DiagnosticsSettingsPanel.this,
                            "Erreur lors de l'upload: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void zipLogs(File destZip) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZip))) {
            // Include main launcher log
            File logFile = new File(System.getProperty("user.home") + "/.nexaria/launcher.log");
            if (logFile.exists()) {
                addToZip(logFile, zos);
            }
            // Include game output log if exists
            File gameLog = new File(LauncherConfig.getGameDir(), "logs/latest.log");
            if (gameLog.exists()) {
                addToZip(gameLog, zos);
            }
        }
    }

    private void addToZip(File file, ZipOutputStream zos) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    private void testConnectivity() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try (Socket sock = new Socket()) {
                    sock.connect(new InetSocketAddress("google.com", 80), 3000);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(DiagnosticsSettingsPanel.this, "Connexion Internet OK ✅");
                    } else {
                        JOptionPane.showMessageDialog(DiagnosticsSettingsPanel.this, "Pas de connexion Internet ❌",
                                "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
}
