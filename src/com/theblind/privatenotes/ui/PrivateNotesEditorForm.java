package com.theblind.privatenotes.ui;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PrivateNotesEditorForm {
    private final JBTextArea editorArea;
    private final JPanel panel1;
    private final JBScrollPane scrollPane;

    public PrivateNotesEditorForm() {
        panel1 = new JPanel(new BorderLayout());
        panel1.setPreferredSize(new Dimension(380, 180));

        editorArea = new JBTextArea(8, 32);
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);
        editorArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        scrollPane = new JBScrollPane(editorArea);
        scrollPane.setPreferredSize(new Dimension(400, 190));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel1.add(scrollPane, BorderLayout.CENTER);
    }

    public void top() {
        editorArea.setCaretPosition(0);
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.setValue(scrollBar.getMinimum());
    }

    public JBTextArea getEditorArea() {
        return editorArea;
    }

    public JBScrollPane getScrollPane() {
        return scrollPane;
    }

    public JPanel getPanel1() {
        return panel1;
    }
}
