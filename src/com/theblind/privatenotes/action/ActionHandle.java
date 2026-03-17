package com.theblind.privatenotes.action;


import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextArea;
import com.theblind.privatenotes.core.PrivateNotesFactory;
import com.theblind.privatenotes.core.service.NoteFileService;
import com.theblind.privatenotes.core.util.IdeaApiUtil;
import com.theblind.privatenotes.core.util.PrivateNotesUtil;
import com.theblind.privatenotes.ui.PrivateNotesEditorForm;
import com.theblind.privatenotes.ui.TextBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ActionHandle {

    public static final NoteFileService noteFileService = PrivateNotesFactory.getNoteFileService();
    private static final Icon CLOSE_ICON = IconLoader.getIcon("/icon/close.png", ActionHandle.class);

    protected Operate operate;

    public ActionHandle(Operate operate) {
        this.operate = operate;
    }

    public enum Operate {
        ADD("[Note] 添加"),
        DEL("[Note] 删除"),
        EDIT("[Note] 编辑"),
        WRAP("[Note] 换行"),
        COPY("[Note] 复制"),
        DETAILED("[Note] 详细");

        Operate(String title) {
            this.title = title;
        }

        private final String title;

        public String getTitle() {
            return title;
        }
    }

    public boolean isVisible(@NotNull Project project, Editor editor, VirtualFile virtualFile) {
        if (virtualFile == null || editor == null) {
            return false;
        }
        java.io.File ioFile = new java.io.File(virtualFile.getPath());
        if (!ioFile.exists()) {
            return false;
        }
        try {
            boolean noteExist = noteFileService.noteExist(
                    virtualFile.getPath(),
                    editor.getDocument().getLineNumber(editor.getCaretModel().getOffset()),
                    ioFile
            );
            return Operate.ADD == operate ? !noteExist : noteExist;
        } catch (Exception e) {
            PrivateNotesUtil.errLog(e, project);
        }
        return false;
    }

    public abstract void execute(@NotNull Project project, Editor editor, VirtualFile virtualFile);

    public static void showNoteEditor(@NotNull Project project,
                                      @NotNull Editor editor,
                                      @NotNull VirtualFile virtualFile,
                                      int lineNumber,
                                      @NotNull String title,
                                      @Nullable String initialText,
                                      @Nullable AtomicBoolean openState) {
        if (openState != null && !openState.compareAndSet(false, true)) {
            return;
        }

        PrivateNotesEditorForm editorForm = new PrivateNotesEditorForm();
        JBTextArea editorArea = editorForm.getEditorArea();
        if (initialText != null) {
            editorArea.setText(initialText);
            editorForm.top();
        }

        IdeaApiUtil.showPopup(editorForm.getPanel1(), editorArea, title, new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                if (openState != null) {
                    openState.set(false);
                }
                try {
                    noteFileService.saveNote(virtualFile.getPath(), lineNumber, editorArea.getText(), new java.io.File(virtualFile.getPath()));
                } catch (Exception e) {
                    PrivateNotesUtil.errLog(e, project);
                }
            }
        }, editor, true, CLOSE_ICON);
    }

    public static class AddActionHandle extends ActionHandle {

        static AtomicBoolean OPEN = new AtomicBoolean(false);

        public AddActionHandle() {
            super(Operate.ADD);
        }

        @Override
        public void execute(@NotNull Project project, Editor editor, VirtualFile virtualFile) {
            if (editor == null || virtualFile == null) {
                return;
            }
            showNoteEditor(project, editor, virtualFile, IdeaApiUtil.getSelLineNumber(editor), operate.getTitle(), null, OPEN);
        }
    }

    public static class EditActionHandle extends ActionHandle {
        static AtomicBoolean OPEN = new AtomicBoolean(false);

        public EditActionHandle() {
            super(Operate.EDIT);
        }

        @Override
        public void execute(@NotNull Project project, Editor editor, VirtualFile virtualFile) {
            if (editor == null || virtualFile == null) {
                return;
            }
            int lineNumber = IdeaApiUtil.getSelLineNumber(editor);
            try {
                showNoteEditor(
                        project,
                        editor,
                        virtualFile,
                        lineNumber,
                        Operate.EDIT.getTitle(),
                        noteFileService.getNote(virtualFile.getPath(), lineNumber, new java.io.File(virtualFile.getPath())),
                        OPEN
                );
            } catch (Exception e) {
                PrivateNotesUtil.errLog(e, project);
                OPEN.set(false);
            }
        }
    }

    public static class DetailedActionHandle extends ActionHandle {

        public DetailedActionHandle() {
            super(Operate.DETAILED);
        }

        @Override
        public void execute(@NotNull Project project, Editor editor, VirtualFile virtualFile) {
            if (editor == null || virtualFile == null) {
                return;
            }

            int lineNumber = IdeaApiUtil.getSelLineNumber(editor);
            TextBox textBox = new TextBox();
            JBTextArea editorArea = textBox.getEditorArea();
            try {
                editorArea.setText(noteFileService.getNote(virtualFile.getPath(), lineNumber, new java.io.File(virtualFile.getPath())));
            } catch (Exception e) {
                PrivateNotesUtil.errLog(e, project);
            }

            textBox.getNav().addActionListener(event -> IdeaApiUtil.to(project, virtualFile, lineNumber));
            textBox.getRefresh().addActionListener(event -> {
                try {
                    editorArea.setText(noteFileService.getNote(virtualFile.getPath(), lineNumber, new java.io.File(virtualFile.getPath())));
                } catch (Exception e) {
                    PrivateNotesUtil.errLog(e, project);
                }
            });

            editorArea.setSelectionStart(0);
            editorArea.setSelectionEnd(0);
            textBox.getPanel().setMinimumSize(new Dimension(200, 200));
            IdeaApiUtil.showComponent(
                    String.format("[Note] %s %s行", virtualFile.getName(), lineNumber),
                    textBox.getPanel(),
                    editorArea,
                    editor,
                    CLOSE_ICON
            );
        }
    }

    public static class CopyActionHandle extends ActionHandle {
        public CopyActionHandle() {
            super(Operate.COPY);
        }

        @Override
        public void execute(@NotNull Project project, Editor editor, VirtualFile virtualFile) {
            if (editor == null || virtualFile == null) {
                return;
            }
            try {
                CopyPasteManager.getInstance().setContents(new StringSelection(
                        noteFileService.getNote(virtualFile.getPath(), IdeaApiUtil.getSelLineNumber(editor), new java.io.File(virtualFile.getPath()))
                ));
            } catch (Exception e) {
                PrivateNotesUtil.errLog(e, project);
            }
        }
    }

    public static class DelActionHandle extends ActionHandle {

        public DelActionHandle() {
            super(Operate.DEL);
        }

        @Override
        public void execute(@NotNull Project project, Editor editor, VirtualFile virtualFile) {
            if (editor == null || virtualFile == null) {
                return;
            }
            try {
                noteFileService.delNote(virtualFile.getPath(), IdeaApiUtil.getSelLineNumber(editor), new java.io.File(virtualFile.getPath()));
            } catch (Exception e) {
                PrivateNotesUtil.errLog(e, project);
            }
        }
    }

    public static class WrapActionHandle extends ActionHandle {

        public WrapActionHandle() {
            super(Operate.WRAP);
        }

        @Override
        public void execute(@NotNull Project project, Editor editor, VirtualFile virtualFile) {
            if (editor == null || virtualFile == null) {
                return;
            }
            try {
                noteFileService.wrapDownNote(virtualFile.getPath(), IdeaApiUtil.getSelLineNumber(editor), new java.io.File(virtualFile.getPath()));
            } catch (Exception e) {
                PrivateNotesUtil.errLog(e, project);
            }
        }
    }
}
