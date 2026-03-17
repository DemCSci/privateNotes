package com.theblind.privatenotes.action.anaction;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.theblind.privatenotes.action.ActionHandle;
import com.theblind.privatenotes.action.ActionHandleFactory;
import org.jetbrains.annotations.NotNull;

public class AddOrEditNoteAnAction extends AnAction {



    ActionHandle addHandle;
    ActionHandle editHandle;



    public AddOrEditNoteAnAction() {
        addHandle = ActionHandleFactory.getActionHandle(ActionHandle.Operate.ADD);
        editHandle = ActionHandleFactory.getActionHandle(ActionHandle.Operate.EDIT);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

        Project project = CommonDataKeys.PROJECT.getData(anActionEvent.getDataContext());
        Editor editor = CommonDataKeys.EDITOR.getData(anActionEvent.getDataContext());
        VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        if (project == null || editor == null || virtualFile == null) {
            return;
        }

        if (addHandle.isVisible(project, editor, virtualFile)) {
            addHandle.execute(project, editor, virtualFile);
            return;
        }
        editHandle.execute(project, editor, virtualFile);

    }

    @Override
    public void update(@NotNull AnActionEvent anActionEvent) {
        super.update(anActionEvent);
        Project project = CommonDataKeys.PROJECT.getData(anActionEvent.getDataContext());
        Editor editor = CommonDataKeys.EDITOR.getData(anActionEvent.getDataContext());
        VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        if (project == null || editor == null || virtualFile == null) {
            anActionEvent.getPresentation().setEnabledAndVisible(false);
            return;
        }
        String title = ActionHandle.Operate.EDIT.getTitle();
        if (addHandle.isVisible(project, editor, virtualFile)) {
            title = ActionHandle.Operate.ADD.getTitle();
        }
        anActionEvent.getPresentation().setText(title);
        anActionEvent.getPresentation().setEnabledAndVisible(true);
    }


}
