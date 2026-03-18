package com.securefile.ui;

import com.securefile.crypto.FileCryptoService;

import javax.crypto.AEADBadTagException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public final class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color DEEP_NAVY = new Color(7, 18, 33);
    private static final Color MIDNIGHT = new Color(12, 34, 60);
    private static final Color TEAL = new Color(27, 140, 135);
    private static final Color GOLD = new Color(227, 183, 87);
    private static final Color FROST = new Color(241, 248, 252);
    private static final Color MIST = new Color(219, 232, 240);
    private static final Color GLASS = new Color(255, 255, 255, 58);
    private static final Color GLASS_BORDER = new Color(255, 255, 255, 90);
    private static final Color INPUT_BORDER = new Color(255, 255, 255, 64);
    private static final Color INPUT_BG = new Color(6, 20, 35, 185);
    private static final Color PANEL_TEXT_BG = new Color(6, 20, 35, 145);
    private static final Color TAB_SELECTED = new Color(19, 112, 124, 220);
    private static final Color TAB_IDLE = new Color(255, 255, 255, 28);

    private static final Font TITLE_FONT = new Font("Serif", Font.BOLD, 30);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 15);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    private final FileCryptoService cryptoService = new FileCryptoService();
    private final JTextArea statusArea = new JTextArea(6, 70);

    public MainFrame() {
        super("Secure File Encryptor");
        configureFrame();
        setContentPane(buildContent());
    }

    private void configureFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 690));
        setSize(1120, 760);
        setLocationByPlatform(true);
    }

    private JPanel buildContent() {
        AnimatedBackgroundPanel root = new AnimatedBackgroundPanel();
        root.setLayout(new BorderLayout(18, 18));
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 20, 22));
        root.add(buildHeroPanel(), BorderLayout.NORTH);
        root.add(buildCenterPanel(), BorderLayout.CENTER);
        root.add(buildStatusPanel(), BorderLayout.SOUTH);
        return root;
    }

    private JComponent buildHeroPanel() {
        GlassPanel hero = new GlassPanel(new BorderLayout(18, 0), 34);
        hero.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("SECURE DESKTOP TOOL");
        eyebrow.setForeground(GOLD);
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 12));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Encrypt real files without compromising the app itself.");
        title.setForeground(FROST);
        title.setFont(TITLE_FONT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("AES-256-GCM for confidentiality and integrity, PBKDF2-HMAC-SHA256 for password-based key derivation, and safe file handling throughout the workflow.");
        subtitle.setForeground(MIST);
        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(10));
        copy.add(title);
        copy.add(Box.createVerticalStrut(10));
        copy.add(subtitle);

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        metrics.setOpaque(false);
        metrics.add(createMetricChip("AES-256-GCM", "AEAD"));
        metrics.add(createMetricChip("PBKDF2", "310k"));
        metrics.add(createMetricChip("Writes", "Atomic"));
        metrics.add(createMetricChip("Tamper", "Detected"));

        hero.add(copy, BorderLayout.CENTER);
        hero.add(metrics, BorderLayout.EAST);
        return hero;
    }

    private JComponent buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        center.add(buildTabs(), BorderLayout.CENTER);
        center.add(buildSecurityNotes(), BorderLayout.EAST);
        return center;
    }

    private JComponent buildTabs() {
        GlassPanel wrapper = new GlassPanel(new BorderLayout(), 30);
        wrapper.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.setBackground(TAB_IDLE);
        tabs.setForeground(FROST);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("Encrypt", createScrollableTab(buildEncryptPanel()));
        tabs.addTab("Decrypt", createScrollableTab(buildDecryptPanel()));
        tabs.setFocusable(false);
        styleTabs(tabs);

        wrapper.add(tabs, BorderLayout.CENTER);
        return wrapper;
    }

    private JComponent createScrollableTab(JPanel content) {
        JScrollPane scroller = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(14);
        scroller.getHorizontalScrollBar().setUnitIncrement(14);
        return scroller;
    }

    private JComponent buildSecurityNotes() {
        GlassPanel notes = new GlassPanel(new BorderLayout(0, 16), 28);
        notes.setPreferredSize(new Dimension(280, 0));
        notes.setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));

        JLabel title = new JLabel("Built Securely");
        title.setForeground(FROST);
        title.setFont(new Font("Serif", Font.BOLD, 24));

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(createNoteCard("Tamper detection", "AES-GCM rejects modified ciphertext instead of returning corrupted plaintext."));
        list.add(Box.createVerticalStrut(10));
        list.add(createNoteCard("Safer writes", "Operations use a temporary file and move it into place after success."));
        list.add(Box.createVerticalStrut(10));
        list.add(createNoteCard("Password handling", "Passwords stay in char arrays and are wiped after background operations finish."));
        list.add(Box.createVerticalStrut(10));
        list.add(createNoteCard("Input validation", "The app checks file existence, writability, path validity, and prevents in-place overwrite."));

        notes.add(title, BorderLayout.NORTH);
        notes.add(list, BorderLayout.CENTER);
        return notes;
    }

    private JPanel buildStatusPanel() {
        GlassPanel panel = new GlassPanel(new BorderLayout(0, 10), 24);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Activity Log");
        title.setForeground(FROST);
        title.setFont(new Font("Serif", Font.BOLD, 20));

        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(true);
        statusArea.setBackground(PANEL_TEXT_BG);
        statusArea.setForeground(FROST);
        statusArea.setCaretColor(FROST);
        statusArea.setFont(MONO_FONT);
        statusArea.setText("Ready. This app writes authenticated .sfe files and refuses modified ciphertext during decryption.\n");

        JScrollPane scroller = new JScrollPane(statusArea);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(false);
        scroller.setOpaque(false);
        scroller.setPreferredSize(new Dimension(0, 170));

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 48), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        inner.add(scroller, BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildEncryptPanel() {
        OperationFields fields = new OperationFields(true);
        JButton encryptButton = createActionButton("Encrypt File", TEAL);
        encryptButton.addActionListener(event -> runEncrypt(fields, encryptButton));

        JPanel panel = buildOperationPanel(
                "Create a .sfe file with authenticated encryption.",
                "The password must be at least 12 characters. A fresh salt and IV are generated every time.",
                fields,
                encryptButton);

        fields.inputBrowseButton.addActionListener(event -> chooseInputFile(fields.inputField, true, fields.outputField));
        fields.outputBrowseButton.addActionListener(event -> chooseOutputFile(fields.outputField, true));
        return panel;
    }

    private JPanel buildDecryptPanel() {
        OperationFields fields = new OperationFields(false);
        JButton decryptButton = createActionButton("Decrypt File", GOLD.darker());
        decryptButton.addActionListener(event -> runDecrypt(fields, decryptButton));

        JPanel panel = buildOperationPanel(
                "Decrypt an existing .sfe file.",
                "Wrong passwords and modified files fail closed instead of producing output that looks valid.",
                fields,
                decryptButton);

        fields.inputBrowseButton.addActionListener(event -> chooseInputFile(fields.inputField, false, fields.outputField));
        fields.outputBrowseButton.addActionListener(event -> chooseOutputFile(fields.outputField, false));
        return panel;
    }

    private JPanel buildOperationPanel(String titleText, String description, OperationFields fields, JButton actionButton) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(titleText);
        title.setForeground(FROST);
        title.setFont(new Font("Serif", Font.BOLD, 24));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel body = new JLabel("<html><div style='width:520px;'>" + description + "</div></html>");
        body.setForeground(FROST);
        body.setFont(BODY_FONT);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(body);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        addRow(form, constraints, 0, "Input file", fields.inputField, fields.inputBrowseButton);
        addRow(form, constraints, 1, "Output file", fields.outputField, fields.outputBrowseButton);
        addRow(form, constraints, 2, "Password", fields.passwordField, null);
        if (fields.confirmPasswordField != null) {
            addRow(form, constraints, 3, "Confirm password", fields.confirmPasswordField, null);
        }

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(createHintLabel(fields.confirmPasswordField != null
                ? "Tip: output defaults to input + .sfe when you choose a source file."
                : "Tip: decryption suggests the original filename when the input ends with .sfe."), BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(actionButton);
        footer.add(actionPanel, BorderLayout.EAST);

        wrapper.add(heading, BorderLayout.NORTH);
        wrapper.add(form, BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);
        return wrapper;
    }

    private void addRow(JPanel panel, GridBagConstraints constraints, int row, String labelText,
                        JComponent field, JButton button) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(8, 0, 8, 14);

        JLabel label = new JLabel(labelText);
        label.setForeground(FROST);
        label.setFont(LABEL_FONT);
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.insets = new Insets(8, 0, 8, 12);
        panel.add(field, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.insets = new Insets(8, 0, 8, 0);
        if (button != null) {
            panel.add(button, constraints);
        } else {
            panel.add(Box.createHorizontalStrut(120), constraints);
        }
    }

    private JButton createActionButton(String text, Color accent) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(DEEP_NAVY);
        button.setBackground(accent);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.brighter(), 1, true),
                BorderFactory.createEmptyBorder(11, 20, 11, 20)));
        return button;
    }

    private JLabel createHintLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MIST);
        label.setFont(new Font("SansSerif", Font.ITALIC, 12));
        return label;
    }

    private JComponent createMetricChip(String title, String value) {
        GlassPanel chip = new GlassPanel(new BorderLayout(0, 2), 18);
        chip.setPreferredSize(new Dimension(110, 62));
        chip.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel top = new JLabel(title, SwingConstants.LEFT);
        top.setForeground(FROST);
        top.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel bottom = new JLabel(value, SwingConstants.LEFT);
        bottom.setForeground(FROST);
        bottom.setFont(new Font("Serif", Font.BOLD, 20));

        chip.add(top, BorderLayout.NORTH);
        chip.add(bottom, BorderLayout.CENTER);
        return chip;
    }

    private JComponent createNoteCard(String title, String body) {
        GlassPanel note = new GlassPanel(new BorderLayout(0, 6), 22);
        note.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel heading = new JLabel(title);
        heading.setForeground(FROST);
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel copy = new JLabel("<html><div style='width:220px;'>" + body + "</div></html>");
        copy.setForeground(FROST);
        copy.setFont(BODY_FONT);

        note.add(heading, BorderLayout.NORTH);
        note.add(copy, BorderLayout.CENTER);
        return note;
    }

    private void styleTabs(JTabbedPane tabs) {
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                highlight = TAB_SELECTED;
                lightHighlight = TAB_SELECTED;
                shadow = TAB_IDLE;
                darkShadow = TAB_IDLE;
                focus = TAB_SELECTED;
            }

            @Override
            protected void paintTabBackground(Graphics graphics, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Paint paint = isSelected
                        ? new GradientPaint(x, y, TAB_SELECTED, x + w, y + h, TEAL)
                        : new GradientPaint(x, y, TAB_IDLE, x + w, y + h, new Color(255, 255, 255, 18));
                g2.setPaint(paint);
                g2.fillRoundRect(x + 4, y + 4, w - 8, h - 6, 18, 18);
                g2.dispose();
            }

            @Override
            protected void paintText(Graphics graphics, int tabPlacement, Font font, java.awt.FontMetrics metrics,
                                     int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                graphics.setFont(font);
                graphics.setColor(FROST);
                graphics.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            }

            @Override
            protected void paintContentBorder(Graphics graphics, int tabPlacement, int selectedIndex) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 35));
                g2.drawRoundRect(4, calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight),
                        tabs.getWidth() - 9,
                        tabs.getHeight() - calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight) - 5,
                        22, 22);
                g2.dispose();
            }
        });
    }

    private void chooseInputFile(JTextField inputField, boolean encrypt, JTextField outputField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            inputField.setText(selected.toString());
            if (outputField.getText().trim().isEmpty()) {
                outputField.setText(encrypt ? suggestEncryptedOutput(selected) : suggestDecryptedOutput(selected));
            }
        }
    }

    private void chooseOutputFile(JTextField outputField, boolean encrypt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        if (encrypt && outputField.getText().trim().isEmpty()) {
            chooser.setSelectedFile(Paths.get("encrypted-output.sfe").toFile());
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputField.setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString());
        }
    }

    private void runEncrypt(OperationFields fields, JButton encryptButton) {
        char[] password = fields.passwordField.getPassword();
        char[] confirmPassword = fields.confirmPasswordField.getPassword();

        try {
            if (!Arrays.equals(password, confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            Path input = parsePath(fields.inputField.getText());
            Path output = parsePath(fields.outputField.getText());
            startWorker(
                    encryptButton,
                    "Encrypting file...",
                    () -> cryptoService.encryptFile(input, output, password),
                    "Encryption completed: " + output.toAbsolutePath(),
                    () -> {
                        Arrays.fill(password, '\0');
                        Arrays.fill(confirmPassword, '\0');
                    });
        } catch (RuntimeException exception) {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmPassword, '\0');
            showError(exception.getMessage());
        } finally {
            fields.passwordField.setText("");
            fields.confirmPasswordField.setText("");
        }
    }

    private void runDecrypt(OperationFields fields, JButton decryptButton) {
        char[] password = fields.passwordField.getPassword();

        try {
            Path input = parsePath(fields.inputField.getText());
            Path output = parsePath(fields.outputField.getText());
            startWorker(
                    decryptButton,
                    "Decrypting file...",
                    () -> cryptoService.decryptFile(input, output, password),
                    "Decryption completed: " + output.toAbsolutePath(),
                    () -> Arrays.fill(password, '\0'));
        } catch (RuntimeException exception) {
            Arrays.fill(password, '\0');
            showError(exception.getMessage());
        } finally {
            fields.passwordField.setText("");
        }
    }

    private void startWorker(JButton actionButton, String progressMessage, ThrowingRunnable task,
                             String successMessage, Runnable cleanup) {
        actionButton.setEnabled(false);
        appendStatus(progressMessage);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }

            @Override
            protected void done() {
                actionButton.setEnabled(true);
                try {
                    get();
                    appendStatus(successMessage);
                    JOptionPane.showMessageDialog(MainFrame.this, successMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("The operation was interrupted.");
                } catch (ExecutionException exception) {
                    showError(mapFailure(exception.getCause()));
                } finally {
                    cleanup.run();
                }
            }
        };
        worker.execute();
    }

    private String mapFailure(Throwable cause) {
        if (cause instanceof AEADBadTagException) {
            return "Decryption failed. The password is wrong or the file was modified.";
        }
        if (cause instanceof InvalidPathException) {
            return "One of the selected file paths is invalid.";
        }
        if (cause instanceof IllegalArgumentException || cause instanceof IOException || cause instanceof GeneralSecurityException) {
            return cause.getMessage();
        }
        return "The operation failed unexpectedly.";
    }

    private void showError(String message) {
        appendStatus("Failed: " + message);
        JOptionPane.showMessageDialog(this, message, "Operation Failed", JOptionPane.ERROR_MESSAGE);
    }

    private void appendStatus(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> statusArea.append("[" + timestamp + "] " + message + "\n"));
    }

    private Path parsePath(String rawPath) {
        String trimmed = rawPath == null ? "" : rawPath.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Select both input and output files.");
        }
        return Paths.get(trimmed);
    }

    private String suggestEncryptedOutput(Path inputPath) {
        Path fileName = inputPath.getFileName();
        String suggested = fileName == null ? "encrypted-output.sfe" : fileName.toString() + ".sfe";
        return inputPath.resolveSibling(suggested).toString();
    }

    private String suggestDecryptedOutput(Path inputPath) {
        Path fileName = inputPath.getFileName();
        String name = fileName == null ? "decrypted-output" : fileName.toString();
        String suggested = name.endsWith(".sfe") ? name.substring(0, name.length() - 4) : name + ".decrypted";
        return inputPath.resolveSibling(suggested).toString();
    }

    private static final class OperationFields {
        private final JTextField inputField;
        private final JTextField outputField;
        private final JPasswordField passwordField;
        private final JPasswordField confirmPasswordField;
        private final JButton inputBrowseButton;
        private final JButton outputBrowseButton;

        private OperationFields(boolean includeConfirmPassword) {
            inputField = createStaticTextField();
            outputField = createStaticTextField();
            passwordField = createStaticPasswordField();
            confirmPasswordField = includeConfirmPassword ? createStaticPasswordField() : null;
            inputBrowseButton = createStaticBrowseButton();
            outputBrowseButton = createStaticBrowseButton();
        }

        private static JTextField createStaticTextField() {
            JTextField field = new JTextField();
            field.setOpaque(true);
            field.setBackground(INPUT_BG);
            field.setForeground(FROST);
            field.setCaretColor(FROST);
            field.setSelectedTextColor(DEEP_NAVY);
            field.setSelectionColor(new Color(177, 225, 234));
            field.setFont(BODY_FONT);
            field.setColumns(28);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            Dimension preferred = field.getPreferredSize();
            field.setMinimumSize(new Dimension(120, preferred.height));
            return field;
        }

        private static JPasswordField createStaticPasswordField() {
            JPasswordField field = new JPasswordField();
            field.setOpaque(true);
            field.setBackground(INPUT_BG);
            field.setForeground(FROST);
            field.setCaretColor(FROST);
            field.setSelectedTextColor(DEEP_NAVY);
            field.setSelectionColor(new Color(177, 225, 234));
            field.setFont(BODY_FONT);
            field.setColumns(28);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            Dimension preferred = field.getPreferredSize();
            field.setMinimumSize(new Dimension(120, preferred.height));
            return field;
        }

        private static JButton createStaticBrowseButton() {
            JButton button = new JButton("Browse");
            button.setUI(new BasicButtonUI());
            button.setFocusPainted(false);
            button.setContentAreaFilled(true);
            button.setOpaque(true);
            button.setForeground(new Color(248, 252, 255));
            button.setBackground(new Color(8, 62, 80));
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(165, 226, 240, 220), 1, true),
                    BorderFactory.createEmptyBorder(9, 16, 9, 16)));
            return button;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private final class AnimatedBackgroundPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		private float phase;

        private AnimatedBackgroundPanel() {
            setOpaque(true);
            Timer timer = new Timer(40, event -> {
                phase += 0.0125f;
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Paint background = new LinearGradientPaint(
                    0, 0, getWidth(), getHeight(),
                    new float[] {0f, 0.45f, 1f},
                    new Color[] {DEEP_NAVY, MIDNIGHT, new Color(7, 56, 72)});
            g2.setPaint(background);
            g2.fillRect(0, 0, getWidth(), getHeight());
            paintOrb(g2, getWidth() * 0.12 + Math.sin(phase) * 30, getHeight() * 0.2 + Math.cos(phase * 0.7f) * 22, 220, new Color(45, 172, 163, 72));
            paintOrb(g2, getWidth() * 0.78 + Math.cos(phase * 0.8f) * 28, getHeight() * 0.18 + Math.sin(phase * 1.2f) * 16, 180, new Color(236, 190, 88, 60));
            paintOrb(g2, getWidth() * 0.68 + Math.sin(phase * 1.4f) * 25, getHeight() * 0.72 + Math.cos(phase * 0.9f) * 20, 280, new Color(130, 224, 255, 38));
            paintGrid(g2);
            g2.dispose();
        }

        private void paintOrb(Graphics2D g2, double centerX, double centerY, double size, Color color) {
            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(centerX - size / 2.0, centerY - size / 2.0, size, size));
        }

        private void paintGrid(Graphics2D g2) {
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255, 255, 255, 14));
            for (int x = -60; x < getWidth() + 60; x += 56) {
                g2.drawLine(x, 0, x - 90, getHeight());
            }
            for (int y = 28; y < getHeight(); y += 46) {
                g2.drawLine(0, y, getWidth(), y);
            }
        }
    }

    private static final class GlassPanel extends JPanel {


		private static final long serialVersionUID = 1L;
		private final int arc;

        private GlassPanel(BorderLayout layout, int arc) {
            super(layout);
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, GLASS, 0, getHeight(), new Color(255, 255, 255, 24)));
            g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc));
            g2.dispose();
            super.paintComponent(graphics);
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(GLASS_BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc));
            g2.dispose();
        }
    }
}