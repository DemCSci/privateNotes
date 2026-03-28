package com.theblind.privatenotes.core.listener;

import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.theblind.privatenotes.core.PrivateNotesFactory;
import com.theblind.privatenotes.core.service.NoteFileService;
import com.theblind.privatenotes.core.util.PrivateNotesUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批量文件修改 监听
 *
 * @author theblind
 */
public class PrivateNotesBulkFileListener implements BulkFileListener {

    NoteFileService noteFileService = PrivateNotesFactory.getNoteFileService();
    private final Map<String, byte[]> previousFileContent = new ConcurrentHashMap<>();

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        events.stream()
                .filter(this::needFilter)
                .filter(VFileContentChangeEvent.class::isInstance)
                .map(VFileContentChangeEvent.class::cast)
                .forEach(this::beforeContentChange);
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        events.stream()
                .filter(this::needFilter)
                .forEach(fileEvent -> {
                    if (fileEvent instanceof VFilePropertyChangeEvent) {
                        this.afterPropertyChange(((VFilePropertyChangeEvent) fileEvent));
                    } else if (fileEvent instanceof VFileContentChangeEvent) {
                        this.afterContentChange(((VFileContentChangeEvent) fileEvent));
                    }
                });
    }

    private boolean needFilter(VFileEvent fileEvent) {
        if (fileEvent.getFile() == null || fileEvent.getFile().isDirectory()) {
            return false;
        }
        try {
            Path path = Paths.get(fileEvent.getFile().getPath());
            return Files.isRegularFile(path) && Files.isReadable(path);
        } catch (InvalidPathException e) {
            return false;
        }
    }


    private void afterPropertyChange(VFilePropertyChangeEvent changeEvent) {
        if (changeEvent.isRename()) {
            try {
                noteFileService.updateFileName(changeEvent.getNewPath(), (String) changeEvent.getOldValue());
            } catch (Exception e) {
                PrivateNotesUtil.errLog(e, null);
            }
        }
    }

    private void afterContentChange(VFileContentChangeEvent changeEvent) {
        String canonicalPath = changeEvent.getFile().getPath();
        try {
            byte[] previousBytes = previousFileContent.remove(canonicalPath);
            if (previousBytes != null && !PrivateNotesDocumentListener.consumeRecentDocumentChange(canonicalPath)) {
                noteFileService.updateVersion(canonicalPath, previousBytes);
                return;
            }
            noteFileService.updateVersion(canonicalPath);
        } catch (Exception e) {
            PrivateNotesUtil.errLog(e, null);
        }
    }

    private void beforeContentChange(VFileContentChangeEvent changeEvent) {
        String canonicalPath = changeEvent.getFile().getPath();
        try {
            if (!noteFileService.exist(canonicalPath, new java.io.File(canonicalPath))) {
                previousFileContent.remove(canonicalPath);
                return;
            }
            previousFileContent.put(canonicalPath, Files.readAllBytes(Paths.get(canonicalPath)));
        } catch (Exception e) {
            previousFileContent.remove(canonicalPath);
            PrivateNotesUtil.errLog(e, null);
        }
    }
}
