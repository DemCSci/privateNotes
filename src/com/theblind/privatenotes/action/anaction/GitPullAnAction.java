package com.theblind.privatenotes.action.anaction;

import cn.hutool.core.thread.ThreadUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.theblind.privatenotes.core.Config;
import com.theblind.privatenotes.core.PrivateNotesFactory;
import com.theblind.privatenotes.core.service.ConfigService;
import com.theblind.privatenotes.core.service.NoteFileService;
import com.theblind.privatenotes.core.util.GitUtil;
import com.theblind.privatenotes.core.util.PrivateNotesUtil;
import org.jetbrains.annotations.NotNull;

public class GitPullAnAction extends AnAction {

    NoteFileService noteFileService = PrivateNotesFactory.getNoteFileService();
    ConfigService configService = PrivateNotesFactory.getConfigService();

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public void update(@NotNull AnActionEvent anActionEvent) {
        super.update(anActionEvent);
        anActionEvent.getPresentation().setEnabledAndVisible(true);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = CommonDataKeys.PROJECT.getData(anActionEvent.getDataContext());
        Config config = configService.get();
        ThreadUtil.execute(() -> {
            try {
                long lastTime = System.currentTimeMillis();
                GitUtil.pull(config.getUserSavePath());
                noteFileService.removeCache(lastTime);
            } catch (Exception e) {
                PrivateNotesUtil.errLog(e, project);
            }
        });

    }
}
