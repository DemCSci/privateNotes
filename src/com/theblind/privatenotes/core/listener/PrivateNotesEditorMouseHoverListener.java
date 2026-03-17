package com.theblind.privatenotes.core.listener;

import cn.hutool.core.util.StrUtil;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextArea;
import com.theblind.privatenotes.core.Config;
import com.theblind.privatenotes.core.PrivateNotesFactory;
import com.theblind.privatenotes.core.service.ConfigService;
import com.theblind.privatenotes.core.service.NoteFileService;
import com.theblind.privatenotes.core.util.PrivateNotesUtil;
import com.theblind.privatenotes.ui.PrivateNotesEditorForm;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.KeyEvent;
import java.awt.*;

public class PrivateNotesEditorMouseHoverListener implements EditorMouseMotionListener, EditorMouseListener {

    private static JDialog currentDialog;
    private static Editor currentEditor;
    private static String currentFilePath;
    private static int currentLine = -1;
    private static String currentNote;
    private static int currentCloseDelayMs = Config.DEFAULT_HOVER_POPUP_CLOSE_DELAY_MS;
    private static boolean popupMouseInside;
    private static Timer hideTimer;

    private final NoteFileService noteFileService = PrivateNotesFactory.getNoteFileService();
    private final ConfigService configService = PrivateNotesFactory.getConfigService();

    @Override
    public void mouseMoved(@NotNull EditorMouseEvent event) {
        Editor editor = event.getEditor();
        try {
            if (!EditorUtil.isRealFileEditor(editor) || editor.isDisposed() || editor.isOneLineMode()) {
                hideHint();
                return;
            }
            if (event.getArea() != EditorMouseEventArea.EDITING_AREA) {
                scheduleHide(currentCloseDelayMs);
                return;
            }

            VirtualFile virtualFile = editor.getVirtualFile();
            if (virtualFile == null || virtualFile.isDirectory()) {
                hideHint();
                return;
            }

            int lineNumber = event.getLogicalPosition().line;
            if (lineNumber < 0 || lineNumber >= editor.getDocument().getLineCount()) {
                hideHint();
                return;
            }

            Config config = configService.get();
            currentCloseDelayMs = sanitizeCloseDelay(config.getHoverPopupCloseDelayMs());
            String note = noteFileService.getNote(virtualFile.getPath(), lineNumber, new java.io.File(virtualFile.getPath()));
            if (StrUtil.isBlank(note)) {
                scheduleHide(currentCloseDelayMs);
                return;
            }

            Integer maxCharNum = config.getMaxCharNum();
            boolean sameTarget = isSameHintTarget(editor, virtualFile.getPath(), lineNumber, note);
            String visibleNote = PrivateNotesEditorLinePainter.buildVisibleNote(note, maxCharNum);
            if (!isHoveringOnPrivateNote(event, config, visibleNote)) {
                if (sameTarget && currentDialog != null && currentDialog.isVisible()) {
                    scheduleHide(currentCloseDelayMs);
                    return;
                }
                scheduleHide(currentCloseDelayMs);
                return;
            }

            cancelScheduledHide();
            Point point = event.getMouseEvent().getPoint();
            if (sameTarget) {
                if (currentDialog != null && currentDialog.isVisible()) {
                    return;
                }
            }

            showHint(editor, virtualFile.getPath(), lineNumber, note, point);
        } catch (Exception e) {
            hideHint();
            PrivateNotesUtil.errLog(e, editor.getProject());
        }
    }

    @Override
    public void mousePressed(@NotNull EditorMouseEvent event) {
        hideHintUnlessPopupInteracting();
    }

    @Override
    public void mouseClicked(@NotNull EditorMouseEvent event) {
        hideHintUnlessPopupInteracting();
    }

    @Override
    public void mouseReleased(@NotNull EditorMouseEvent event) {
        hideHintUnlessPopupInteracting();
    }

    @Override
    public void mouseExited(@NotNull EditorMouseEvent event) {
        scheduleHide(currentCloseDelayMs);
    }

    private boolean isHoveringOnPrivateNote(EditorMouseEvent event, Config config, String visibleNote) {
        Editor editor = event.getEditor();
        int lineNumber = event.getLogicalPosition().line;
        int lineEndOffset = editor.getDocument().getLineEndOffset(lineNumber);
        Point extensionStart = editor.offsetToXY(lineEndOffset);
        Point mousePoint = event.getMouseEvent().getPoint();

        if (mousePoint.y < extensionStart.y || mousePoint.y > extensionStart.y + editor.getLineHeight()) {
            return false;
        }

        String prefix = String.format(" %s ", config.getMark());
        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
        FontMetrics metrics = editor.getContentComponent().getFontMetrics(font);
        int extensionWidth = metrics.stringWidth(prefix + visibleNote) + 4;
        return mousePoint.x >= extensionStart.x && mousePoint.x <= extensionStart.x + extensionWidth;
    }

    private void showHint(Editor editor, String filePath, int lineNumber, String note, Point point) {
        hideHint();

        HintPopupContent popupContent = createHintComponent(editor, note);
        installPopupHoverTracking(popupContent.panel());
        JDialog dialog = createHintDialog(editor, popupContent, point);
        dialog.setVisible(true);

        currentDialog = dialog;
        currentEditor = editor;
        currentFilePath = filePath;
        currentLine = lineNumber;
        currentNote = note;
        popupMouseInside = false;
    }

    private HintPopupContent createHintComponent(Editor editor, String note) {
        PrivateNotesEditorForm form = new PrivateNotesEditorForm();
        JBTextArea textArea = form.getEditorArea();
        textArea.setText(note);
        textArea.setEditable(false);
        textArea.setFocusable(true);
        textArea.setFont(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
        textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        textArea.setCaretPosition(0);
        textArea.setSelectionStart(0);
        textArea.setSelectionEnd(0);
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                popupMouseInside = true;
                cancelScheduledHide();
                requestPopupFocus(textArea);
            }
        });
        textArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                popupMouseInside = true;
                cancelScheduledHide();
            }
        });
        installCopySupport(textArea);

        int popupWidth = 420;
        textArea.setSize(new Dimension(popupWidth, Integer.MAX_VALUE));
        Dimension preferredTextSize = textArea.getPreferredSize();
        int popupHeight = Math.min(Math.max(preferredTextSize.height + 12, editor.getLineHeight() * 3), editor.getLineHeight() * 12);
        form.getPanel1().setBorder(HintUtil.createHintBorder());
        form.getPanel1().setPreferredSize(new Dimension(popupWidth, popupHeight));
        form.getScrollPane().setPreferredSize(new Dimension(popupWidth, popupHeight));
        form.top();
        return new HintPopupContent(form.getPanel1(), textArea);
    }

    private JDialog createHintDialog(Editor editor, HintPopupContent popupContent, Point point) {
        Window owner = SwingUtilities.getWindowAncestor(editor.getContentComponent());
        JDialog dialog = owner instanceof Frame frame
                ? new JDialog(frame)
                : new JDialog();
        dialog.setModal(false);
        dialog.setUndecorated(true);
        dialog.setFocusableWindowState(true);
        dialog.setAutoRequestFocus(false);
        dialog.setContentPane(popupContent.panel());
        dialog.pack();

        Point screenPoint = new Point(point);
        SwingUtilities.convertPointToScreen(screenPoint, editor.getContentComponent());
        dialog.setLocation(screenPoint.x + 12, screenPoint.y + editor.getLineHeight());
        dialog.getRootPane().registerKeyboardAction(
                event -> hideHint(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        return dialog;
    }

    private void installCopySupport(JTextArea textArea) {
        KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        KeyStroke selectAllKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(copyKeyStroke, DefaultEditorKit.copyAction);
        inputMap.put(selectAllKeyStroke, DefaultEditorKit.selectAllAction);

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("复制");
        copyItem.addActionListener(event -> textArea.copy());
        JMenuItem selectAllItem = new JMenuItem("全选");
        selectAllItem.addActionListener(event -> textArea.selectAll());
        popupMenu.add(copyItem);
        popupMenu.add(selectAllItem);
        textArea.setComponentPopupMenu(popupMenu);
    }

    private boolean isSameHintTarget(Editor editor, String filePath, int lineNumber, String note) {
        return currentDialog != null
                && currentEditor == editor
                && filePath.equals(currentFilePath)
                && currentLine == lineNumber
                && note.equals(currentNote);
    }

    private void installPopupHoverTracking(Component root) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                popupMouseInside = true;
                cancelScheduledHide();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                popupMouseInside = isPointerOverPopup();
                if (popupMouseInside) {
                    cancelScheduledHide();
                    return;
                }
                scheduleHide(currentCloseDelayMs);
            }
        };
        MouseMotionAdapter motionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                popupMouseInside = true;
                cancelScheduledHide();
            }
        };
        installPopupHoverTracking(root, mouseAdapter, motionAdapter);
    }

    private void installPopupHoverTracking(Component component,
                                           MouseAdapter mouseAdapter,
                                           MouseMotionAdapter motionAdapter) {
        component.addMouseListener(mouseAdapter);
        component.addMouseMotionListener(motionAdapter);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installPopupHoverTracking(child, mouseAdapter, motionAdapter);
            }
        }
    }

    private static int sanitizeCloseDelay(Integer closeDelayMs) {
        if (closeDelayMs == null) {
            return Config.DEFAULT_HOVER_POPUP_CLOSE_DELAY_MS;
        }
        return Math.max(closeDelayMs, 0);
    }

    private static void cancelScheduledHide() {
        if (hideTimer != null) {
            hideTimer.stop();
        }
    }

    private static void scheduleHide(int delayMs) {
        if (currentDialog == null || !currentDialog.isDisplayable()) {
            return;
        }
        if (popupMouseInside || isPointerOverPopup()) {
            popupMouseInside = true;
            return;
        }
        cancelScheduledHide();
        if (delayMs <= 0) {
            hideHintIfPointerOutside();
            return;
        }
        hideTimer = new Timer(delayMs, e -> hideHintIfPointerOutside());
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    private static boolean isPointerOverPopup() {
        Window popupWindow = getPopupWindow();
        if (popupWindow == null || !popupWindow.isShowing()) {
            return false;
        }
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            return false;
        }

        Point pointerLocation = pointerInfo.getLocation();
        Rectangle bounds = popupWindow.getBounds();
        return bounds.contains(pointerLocation);
    }

    private static Window getPopupWindow() {
        if (currentDialog == null || !currentDialog.isDisplayable()) {
            return null;
        }
        return currentDialog;
    }

    private static void hideHintIfPointerOutside() {
        if (isPointerOverPopup()) {
            popupMouseInside = true;
            cancelScheduledHide();
            return;
        }
        popupMouseInside = false;
        hideHint();
    }

    private static void hideHint() {
        cancelScheduledHide();
        if (currentDialog != null) {
            currentDialog.dispose();
        }
        currentDialog = null;
        currentEditor = null;
        currentFilePath = null;
        currentLine = -1;
        currentNote = null;
        popupMouseInside = false;
    }

    private static void hideHintUnlessPopupInteracting() {
        if (popupMouseInside || isPointerOverPopup()) {
            return;
        }
        hideHint();
    }

    private static void requestPopupFocus(JComponent component) {
        SwingUtilities.invokeLater(component::requestFocusInWindow);
    }

    private record HintPopupContent(JComponent panel, JTextArea textArea) {
    }
}
