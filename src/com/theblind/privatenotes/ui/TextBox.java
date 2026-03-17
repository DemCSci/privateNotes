package com.theblind.privatenotes.ui;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TextBox {
    private final JBTextArea editorArea;
    private final JPanel panel;
    private final JBScrollPane scroll;
    private final JPanel topPanel;
    private final JButton nav;
    private final JButton refresh;

    public TextBox() {
        panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(420, 240));
        panel.setBorder(BorderFactory.createEmptyBorder());

        topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topPanel.setOpaque(false);

        refresh = createToolbarButton("刷新", "/icon/refresh.png");
        nav = createToolbarButton("定位", "/icon/location.png");
        topPanel.add(refresh);
        topPanel.add(nav);

        editorArea = new JBTextArea(10, 36);
        editorArea.setEditable(false);
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);
        editorArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        scroll = new JBScrollPane(editorArea);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
    }

    private JButton createToolbarButton(String text, String iconPath) {
        JButton button = new JButton(text, IconLoader.getIcon(iconPath, TextBox.class));
        button.setFocusable(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return button;
    }

    public JBTextArea getEditorArea() {
        return editorArea;
    }

    public JPanel getPanel() {
        return panel;
    }

    public JBScrollPane getScroll() {
        return scroll;
    }

    public JButton getNav() {
        return nav;
    }

    public JButton getRefresh() {
        return refresh;
    }
}
