package com.theblind.privatenotes.core.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.IconButton;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class IdeaApiUtil {

    private static final String NOTIFICATION_GROUP_ID = "Private Notes";

    public static JBPopup showComponent(JComponent body, JComponent focusComponent, String title, Icon cancelIcon) {
        return JBPopupFactory.getInstance().createComponentPopupBuilder(body, focusComponent)
                .setCancelKeyEnabled(true)
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .setTitle(title)
                .setMinSize(new Dimension(200, 200))
                .setCancelButton(new IconButton("关闭", cancelIcon))
                .setResizable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(false)
                .setShowBorder(false)
                .setMovable(true)
                .createPopup();
    }

    public static JBPopup showPopup(JComponent body,
                                    JComponent focusComponent,
                                    String title,
                                    @Nullable JBPopupListener popupListener,
                                    Editor editor,
                                    boolean cancelOnClickOutside,
                                    Icon cancelIcon) {
        JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(body, focusComponent)
                .setCancelKeyEnabled(true)
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(cancelOnClickOutside)
                .setTitle(title)
                .setRequestFocus(true)
                .setResizable(true)
                .setMovable(true)
                .setMinSize(body.getPreferredSize())
                .setCancelButton(new IconButton("关闭", cancelIcon))
                .createPopup();
        if (popupListener != null) {
            popup.addListener(popupListener);
        }
        popup.showInBestPositionFor(editor);
        requestFocus(focusComponent);
        return popup;
    }

    public static void showComponent(String title, JComponent body, JComponent focusComponent, Editor editor, Icon cancelIcon) {
        JBPopup popup = showComponent(body, focusComponent, title, cancelIcon);
        popup.showInBestPositionFor(editor);
        requestFocus(focusComponent);
    }

    public static Integer getSelLineNumber(@NotNull Editor editor) {
        return editor.getDocument().getLineNumber(editor.getCaretModel().getOffset());
    }

    public static byte[] getBytes(@NotNull VirtualFile virtualFile) throws IOException {
        return virtualFile.contentsToByteArray(false);
    }

    public static void showNotification(String content, NotificationType type, Project project) {
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification("Private Notes Message", content, type);
        ApplicationManager.getApplication().invokeLater(() -> Notifications.Bus.notify(notification, project));
    }

    public static void showErrNotification(String content, Project project) {
        showNotification(content, NotificationType.ERROR, project);
    }

    public static void showInfoNotification(String content, Project project) {
        showNotification(content, NotificationType.INFORMATION, project);
    }

    public static void chooseColorListener(JComponent placement, JComponent lc) {
        lc.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                Color chooseColor = JColorChooser.showDialog(placement, "Choose color", lc.getForeground());
                if (chooseColor != null) {
                    lc.setForeground(chooseColor);
                }
            }
        });
    }

    public static void requestFocus(@NotNull JComponent component) {
        ApplicationManager.getApplication().invokeLater(() ->
                IdeFocusManager.getGlobalInstance().requestFocus(component, true));
    }

    public static void to(Project project, VirtualFile mapperFile, Integer lineNumber) {
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, mapperFile);
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
        if (editor == null) {
            return;
        }

        CaretModel caretModel = editor.getCaretModel();
        LogicalPosition logicalPosition = caretModel.getLogicalPosition();
        logicalPosition.leanForward(true);
        LogicalPosition logical = new LogicalPosition(lineNumber, logicalPosition.column);
        caretModel.moveToLogicalPosition(logical);
        SelectionModel selectionModel = editor.getSelectionModel();
        selectionModel.selectLineAtCaret();
    }
}
