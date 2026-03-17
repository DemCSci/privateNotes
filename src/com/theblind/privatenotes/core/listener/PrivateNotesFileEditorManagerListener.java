package com.theblind.privatenotes.core.listener;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.theblind.privatenotes.core.PrivateNotesFactory;
import com.theblind.privatenotes.core.service.NoteFileService;
import com.theblind.privatenotes.core.util.PrivateNotesUtil;
import org.jetbrains.annotations.NotNull;

public class PrivateNotesFileEditorManagerListener implements FileEditorManagerListener {

    NoteFileService noteFileService = PrivateNotesFactory.getNoteFileService();

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile newFile = event.getNewFile();
        if (newFile == null || newFile.isDirectory()) {
            return;
        }
        try {
            noteFileService.loadCache(newFile.getPath(), new java.io.File(newFile.getPath()));
        } catch (Exception e) {
            PrivateNotesUtil.errLog(e, event.getManager().getProject());
        }
    }
}
