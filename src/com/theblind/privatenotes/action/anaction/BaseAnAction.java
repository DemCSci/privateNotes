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

public abstract class BaseAnAction extends AnAction {

    ActionHandle actionHandle;

    public BaseAnAction(ActionHandle.Operate operate) {
        actionHandle=ActionHandleFactory.getActionHandle(operate);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public void update(@NotNull AnActionEvent anActionEvent) {
        super.update(anActionEvent);
        Project project = CommonDataKeys.PROJECT.getData(anActionEvent.getDataContext());
        Editor editor = CommonDataKeys.EDITOR.getData(anActionEvent.getDataContext());
        VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        anActionEvent.getPresentation().setEnabledAndVisible(actionHandle.isVisible(project, editor, virtualFile));

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = CommonDataKeys.PROJECT.getData(anActionEvent.getDataContext());
        Editor editor = CommonDataKeys.EDITOR.getData(anActionEvent.getDataContext());
        VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());
        if (project == null || editor == null || virtualFile == null) {
            return;
        }
        actionHandle.execute(project, editor, virtualFile);
    }







}
