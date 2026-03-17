package com.theblind.privatenotes.action.anaction;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.theblind.privatenotes.action.ActionHandle;
import com.theblind.privatenotes.core.PrivateNotesFactory;
import com.theblind.privatenotes.core.service.NoteFileService;
import com.theblind.privatenotes.core.util.PrivateNotesUtil;
import org.jetbrains.annotations.NotNull;

public class EditNoteAnActionByLine extends BaseAnAction {

    int selLineNumber;

    public static final NoteFileService noteFileService = PrivateNotesFactory.getNoteFileService();

    public EditNoteAnActionByLine(int line) {
        super(ActionHandle.Operate.EDIT);
        this.selLineNumber = line;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = CommonDataKeys.PROJECT.getData(anActionEvent.getDataContext());
        Editor editor = CommonDataKeys.EDITOR.getData(anActionEvent.getDataContext());
        VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        if (project == null || editor == null || virtualFile == null) {
            return;
        }
        try {
            ActionHandle.showNoteEditor(
                    project,
                    editor,
                    virtualFile,
                    selLineNumber,
                    ActionHandle.Operate.EDIT.getTitle(),
                    noteFileService.getNote(virtualFile.getPath(), selLineNumber, new java.io.File(virtualFile.getPath())),
                    null
            );
        } catch (Exception e) {
            PrivateNotesUtil.errLog(e, project);
        }
    }
}
