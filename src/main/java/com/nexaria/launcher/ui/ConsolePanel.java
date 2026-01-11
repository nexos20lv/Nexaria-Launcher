package com.nexaria.launcher.ui;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConsolePanel extends JPanel {
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public ConsolePanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.setOpaque(false);

        ModernButton copyBtn = new ModernButton("COPIER", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        copyBtn.setIcon(FontIcon.of(FontAwesomeSolid.COPY, 14, DesignConstants.TEXT_PRIMARY));
        copyBtn.addActionListener(e -> {
            try {
                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(
                        textArea.getText());
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            } catch (Exception ignored) {
            }
        });

        ModernButton clearBtn = new ModernButton("EFFACER", DesignConstants.PURPLE_ACCENT,
                DesignConstants.PURPLE_ACCENT_DARK, false);
        clearBtn.setIcon(FontIcon.of(FontAwesomeSolid.TRASH, 14, DesignConstants.TEXT_PRIMARY));
        clearBtn.addActionListener(e -> textArea.setText(""));

        toolbar.add(copyBtn);
        toolbar.add(clearBtn);
        add(toolbar, BorderLayout.NORTH);

        // Text Area
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(new Color(20, 20, 30));
        textArea.setForeground(new Color(200, 200, 200));
        textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        textArea.setMargin(new Insets(5, 5, 5, 5));

        // Auto scroll
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 40, 90), 1));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String time = dateFormat.format(new Date());
            textArea.append("[" + time + "] " + message + "\n");
            // Limit lines to avoid memory issues
            Document doc = textArea.getDocument();
            if (doc.getLength() > 100000) {
                try {
                    doc.remove(0, doc.getLength() - 80000);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void appendRaw(String text) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            // Auto-scroll
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    public void redirectSystemStreams() {
        java.io.OutputStream out = new java.io.OutputStream() {
            private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

            @Override
            public void write(int b) {
                synchronized (buffer) {
                    buffer.write(b);
                    if (b == '\n') {
                        flushBuffer();
                    }
                }
            }

            @Override
            public void write(byte[] b, int off, int len) {
                synchronized (buffer) {
                    buffer.write(b, off, len);
                    // Check for newline in the written chunk
                    for (int i = off; i < off + len; i++) {
                        if (b[i] == '\n') {
                            flushBuffer();
                            return;
                        }
                    }
                }
            }

            private void flushBuffer() {
                if (buffer.size() == 0)
                    return;
                final String text = buffer.toString(java.nio.charset.StandardCharsets.UTF_8);
                buffer.reset();
                SwingUtilities.invokeLater(() -> {
                    textArea.append(text);
                    // Limit text length occasionally
                    if (textArea.getDocument().getLength() > 100000) {
                        try {
                            textArea.getDocument().remove(0, textArea.getDocument().getLength() - 80000);
                        } catch (Exception ignored) {
                        }
                    }
                    textArea.setCaretPosition(textArea.getDocument().getLength());
                });
            }
        };

        System.setOut(new java.io.PrintStream(new TeeOutputStream(System.out, out), true,
                java.nio.charset.StandardCharsets.UTF_8));
        System.setErr(new java.io.PrintStream(new TeeOutputStream(System.err, out), true,
                java.nio.charset.StandardCharsets.UTF_8));
    }

    private static class TeeOutputStream extends java.io.OutputStream {
        private final java.io.OutputStream out1;
        private final java.io.OutputStream out2;

        public TeeOutputStream(java.io.OutputStream out1, java.io.OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }

        @Override
        public void write(int b) throws java.io.IOException {
            out1.write(b);
            out2.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            out1.write(b, off, len);
            out2.write(b, off, len);
        }

        @Override
        public void flush() throws java.io.IOException {
            out1.flush();
            out2.flush();
        }
    }
}
