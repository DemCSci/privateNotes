package com.theblind.privatenotes.core.listener;

import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.DocumentUtil;
import com.theblind.privatenotes.core.NoteFile;
import com.theblind.privatenotes.core.PrivateNotesFactory;
import com.theblind.privatenotes.core.service.NoteFileService;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PrivateNotesDocumentListener implements DocumentListener {

    private static final long RECENT_CHANGE_WINDOW_MS = 10_000L;
    private static final Map<String, Long> RECENT_DOCUMENT_CHANGES = new ConcurrentHashMap<>();

    NoteFileService noteFileService = PrivateNotesFactory.getNoteFileService();

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
        try {
            Document document = event.getDocument();
            if (document.isInBulkUpdate()) {
                return;
            }

            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            if (virtualFile == null || virtualFile.isDirectory()) {
                return;
            }
            markDocumentChange(virtualFile.getPath());

            String newStr = String.valueOf(event.getNewFragment());
            String oldStr = String.valueOf(event.getOldFragment());
            int newLines = wrapCount(newStr);
            int oldLines = wrapCount(oldStr);
            int lineDelta = newLines - oldLines;
            if (lineDelta == 0) {
                return;
            }

            int offset = event.getOffset();
            int lineNumber = document.getLineNumber(offset);
            TextRange lineTextRange = DocumentUtil.getLineTextRange(document, lineNumber);
            int wrapLineNumber = wrapLineNumber(newStr, oldStr, lineNumber, lineTextRange, offset);
            String filePath = virtualFile.getPath();
            NoteFile noteFile = noteFileService.get(filePath, new java.io.File(filePath));
            if (noteFile == null) {
                return;
            }

            if (lineDelta > 0) {
                noteFileService.continueToWrapDown(filePath, wrapLineNumber, Math.abs(lineDelta), new java.io.File(filePath));
            } else {
                noteFileService.continueToWrapUp(filePath, wrapLineNumber, Math.abs(lineDelta), new java.io.File(filePath));
            }
        } catch (Exception ignored) {
        }
    }

    static void markDocumentChange(String path) {
        RECENT_DOCUMENT_CHANGES.put(path, System.currentTimeMillis());
    }

    static boolean consumeRecentDocumentChange(String path) {
        Long lastChangeTime = RECENT_DOCUMENT_CHANGES.remove(path);
        return lastChangeTime != null && System.currentTimeMillis() - lastChangeTime <= RECENT_CHANGE_WINDOW_MS;
    }

    int wrapCount(String text) {
        return StrUtil.count(text, "\n");
    }

    int wrapLineNumber(String newText, String oldText, int lineNumber, TextRange lineTextRange, int offset) {
        int wrapLineNumber = lineNumber;
        if (newText.length() < oldText.length()) {
            wrapLineNumber += 1;
        } else if (offset < lineTextRange.getEndOffset()) {
            return wrapLineNumber;
        } else if (lineTextRange.getLength() == 0) {
            return wrapLineNumber;
        } else if (offset == lineTextRange.getEndOffset()) {
            return wrapLineNumber + 1;
        } else if (!StrUtil.endWith(newText, "\n")) {
            return wrapLineNumber + 1;
        }
        return wrapLineNumber;
    }
}
