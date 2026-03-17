package com.theblind.privatenotes.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;
import com.theblind.privatenotes.core.Config;
import com.theblind.privatenotes.core.PrivateNotesFactory;
import com.theblind.privatenotes.core.service.ConfigService;
import com.theblind.privatenotes.core.service.NoteFileService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GonfigForm implements SearchableConfigurable {
    private final ConfigService configService = PrivateNotesFactory.getConfigService();
    private final NoteFileService noteFileService = PrivateNotesFactory.getNoteFileService();

    private JPanel mainPane;
    private JBTextField markText;
    private JBTextField userText;
    private JBTextField migrateOldPrefixText;
    private JBTextField migrateNewPrefixText;
    private JLabel markLabSel;
    private JLabel noteLabSel;
    private JSpinner maxCharNum;
    private JSpinner hoverPopupCloseDelayMs;
    private UiState persistedState;

    public GonfigForm() {
    }

    @NotNull
    @Override
    public String getId() {
        return "plugins.privateNotes";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "私人注释";
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return userText;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ensureUi();
        reset();
        return mainPane;
    }

    @Override
    public boolean isModified() {
        ensureUi();
        UiState baseline = persistedState != null ? persistedState : readConfigState(configService.get());
        return !baseline.equals(readUiState());
    }

    @Override
    public void apply() throws ConfigurationException {
        Config config = configService.get();
        UiState uiState = readUiState();
        config.setUser(uiState.user());
        config.setMark(uiState.mark());
        config.setNoteColor(uiState.noteColor());
        config.setMarkColor(uiState.markColor());
        config.setMaxCharNum(uiState.maxCharNum());
        config.setHoverPopupCloseDelayMs(uiState.hoverPopupCloseDelayMs());
        configService.save(config);
        persistedState = uiState;
    }

    @Override
    public void reset() {
        ensureUi();
        UiState uiState = readConfigState(configService.get());
        userText.setText(uiState.user());
        markText.setText(uiState.mark());
        markLabSel.setText(uiState.mark());
        markLabSel.setForeground(Config.asColor(uiState.markColor()));
        noteLabSel.setForeground(Config.asColor(uiState.noteColor()));
        maxCharNum.setValue(uiState.maxCharNum());
        hoverPopupCloseDelayMs.setValue(uiState.hoverPopupCloseDelayMs());
        if (migrateOldPrefixText != null) {
            migrateOldPrefixText.setText("");
        }
        if (migrateNewPrefixText != null) {
            migrateNewPrefixText.setText("");
        }
        persistedState = uiState;
    }

    @Override
    public void disposeUIResources() {
        mainPane = null;
        userText = null;
        markText = null;
        migrateOldPrefixText = null;
        migrateNewPrefixText = null;
        markLabSel = null;
        noteLabSel = null;
        maxCharNum = null;
        hoverPopupCloseDelayMs = null;
        persistedState = null;
    }

    private void buildUi() {
        mainPane = new JPanel(new BorderLayout(0, 12));
        mainPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("属性设置");
        mainPane.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 12));

        JPanel userPanel = new JPanel(new BorderLayout(8, 0));
        userPanel.add(new JLabel("用户:"), BorderLayout.WEST);
        userText = new JBTextField();
        userPanel.add(userText, BorderLayout.CENTER);
        content.add(userPanel, BorderLayout.NORTH);

        content.add(createNoteSettingsPanel(), BorderLayout.CENTER);

        mainPane.add(content, BorderLayout.CENTER);
    }

    private JComponent createNoteSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JPanel markPanel = new JPanel(new BorderLayout(8, 0));
        markPanel.add(new JLabel("标识:"), BorderLayout.WEST);
        markText = new JBTextField();
        markPanel.add(markText, BorderLayout.CENTER);
        panel.add(markPanel, gbc);

        gbc.gridy++;
        JPanel previewPanel = new JPanel(new BorderLayout(8, 0));
        previewPanel.add(new JLabel("效果:"), BorderLayout.WEST);
        JPanel previewValue = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        markLabSel = new JLabel("标识");
        noteLabSel = new JLabel("注释内容");
        previewValue.add(markLabSel);
        previewValue.add(noteLabSel);
        previewPanel.add(previewValue, BorderLayout.CENTER);
        panel.add(previewPanel, gbc);

        gbc.gridy++;
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        colorPanel.add(new JLabel("颜色:"));
        colorPanel.add(createColorButton("标识颜色", markLabSel));
        colorPanel.add(createColorButton("注释颜色", noteLabSel));
        panel.add(colorPanel, gbc);

        gbc.gridy++;
        JPanel maxCharPanel = new JPanel(new BorderLayout(8, 0));
        maxCharPanel.add(new JLabel("展示:"), BorderLayout.WEST);
        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        maxCharNum = new JSpinner(new SpinnerNumberModel(30, 1, 500, 1));
        spinnerPanel.add(maxCharNum);
        spinnerPanel.add(new JLabel("字符数"));
        maxCharPanel.add(spinnerPanel, BorderLayout.CENTER);
        panel.add(maxCharPanel, gbc);

        gbc.gridy++;
        JPanel hoverDelayPanel = new JPanel(new BorderLayout(8, 0));
        hoverDelayPanel.add(new JLabel("悬浮:"), BorderLayout.WEST);
        JPanel hoverDelaySpinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hoverPopupCloseDelayMs = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_HOVER_POPUP_CLOSE_DELAY_MS, 0, 10000, 100));
        hoverDelaySpinnerPanel.add(hoverPopupCloseDelayMs);
        hoverDelaySpinnerPanel.add(new JLabel("关闭延迟(毫秒)"));
        hoverDelayPanel.add(hoverDelaySpinnerPanel, BorderLayout.CENTER);
        panel.add(hoverDelayPanel, gbc);

        gbc.gridy++;
        JPanel migratePanel = new JPanel(new BorderLayout(8, 8));
        migratePanel.add(new JLabel("迁移:"), BorderLayout.WEST);
        JPanel migrateFieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints migrateGbc = new GridBagConstraints();
        migrateGbc.gridx = 0;
        migrateGbc.gridy = 0;
        migrateGbc.insets = new Insets(0, 0, 6, 8);
        migrateGbc.anchor = GridBagConstraints.WEST;
        migrateFieldsPanel.add(new JLabel("旧前缀"), migrateGbc);

        migrateGbc.gridx = 1;
        migrateGbc.weightx = 1.0;
        migrateGbc.fill = GridBagConstraints.HORIZONTAL;
        migrateOldPrefixText = new JBTextField();
        migrateFieldsPanel.add(migrateOldPrefixText, migrateGbc);

        migrateGbc.gridx = 0;
        migrateGbc.gridy = 1;
        migrateGbc.weightx = 0;
        migrateGbc.fill = GridBagConstraints.NONE;
        migrateFieldsPanel.add(new JLabel("新前缀"), migrateGbc);

        migrateGbc.gridx = 1;
        migrateGbc.weightx = 1.0;
        migrateGbc.fill = GridBagConstraints.HORIZONTAL;
        migrateNewPrefixText = new JBTextField();
        migrateFieldsPanel.add(migrateNewPrefixText, migrateGbc);

        migrateGbc.gridx = 2;
        migrateGbc.gridy = 0;
        migrateGbc.gridheight = 2;
        migrateGbc.weightx = 0;
        migrateGbc.fill = GridBagConstraints.NONE;
        JButton migrateButton = new JButton("执行迁移");
        migrateButton.addActionListener(event -> runPathMigration());
        migrateFieldsPanel.add(migrateButton, migrateGbc);

        migratePanel.add(migrateFieldsPanel, BorderLayout.CENTER);
        panel.add(migratePanel, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        markLabSel.setForeground(JBColor.GRAY);
        noteLabSel.setForeground(JBColor.GRAY);
        markText.getDocument().addDocumentListener(new DocumentListener() {
            private void updatePreview() {
                markLabSel.setText(markText.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreview();
            }
        });
        return panel;
    }

    private void ensureUi() {
        if (mainPane == null) {
            buildUi();
        }
    }

    private JButton createColorButton(String text, JLabel previewLabel) {
        JButton button = new JButton(text);
        button.addActionListener((ActionEvent e) -> {
            Color chooseColor = JColorChooser.showDialog(mainPane, text, previewLabel.getForeground());
            if (chooseColor != null) {
                previewLabel.setForeground(chooseColor);
            }
        });
        return button;
    }

    private void runPathMigration() {
        try {
            if (isModified()) {
                apply();
            }
            String result = noteFileService.migratePathPrefix(
                    nullToEmpty(migrateOldPrefixText.getText()),
                    nullToEmpty(migrateNewPrefixText.getText())
            );
            showMigrationResult(result, JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "路径迁移失败" : e.getMessage();
            showMigrationResult(message, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMigrationResult(String message, int messageType) {
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setCaretPosition(0);
        textArea.setRows(18);
        textArea.setColumns(72);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(760, 420));
        JOptionPane.showMessageDialog(mainPane, scrollPane, "私人注释", messageType);
    }

    private UiState readConfigState(Config config) {
        Config defaults = new Config();
        return new UiState(
                nullToEmpty(config.getUser()),
                valueOrDefault(config.getMark(), defaults.getMark()),
                valueOrDefault(config.getNoteColor(), defaults.getNoteColor()),
                valueOrDefault(config.getMarkColor(), defaults.getMarkColor()),
                config.getMaxCharNum() == null ? defaults.getMaxCharNum() : config.getMaxCharNum(),
                config.getHoverPopupCloseDelayMs() == null ? defaults.getHoverPopupCloseDelayMs() : config.getHoverPopupCloseDelayMs()
        );
    }

    private UiState readUiState() {
        return new UiState(
                nullToEmpty(userText.getText()),
                nullToEmpty(markText.getText()),
                Config.byColor(noteLabSel.getForeground()),
                Config.byColor(markLabSel.getForeground()),
                ((Number) maxCharNum.getValue()).intValue(),
                ((Number) hoverPopupCloseDelayMs.getValue()).intValue()
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private record UiState(String user,
                           String mark,
                           String noteColor,
                           String markColor,
                           int maxCharNum,
                           int hoverPopupCloseDelayMs) {
    }
}
