package com.theblind.privatenotes.core.service;

import com.theblind.privatenotes.core.NoteFile;

import java.io.File;
import java.util.List;

public interface NoteFileService {

    NoteFile get(File file, Object... params) throws Exception;

    NoteFile get(String path, Object... params) throws Exception;

    String getNote(File file, int lineNumber, Object... params) throws Exception;

    String getNote(String path, int lineNumber, Object... params) throws Exception;

    boolean exist(File file, Object... params);

    boolean exist(String path, Object... params);

    boolean noteExist(File file, int lineNumber, Object... params) throws Exception;

    boolean noteExist(String path, int lineNumber, Object... params) throws Exception;

    void saveNote(NoteFile noteFile) throws Exception;

    void saveNote(String path, int lineNumber, String note, Object... params) throws Exception;

    void saveNote(File file, int lineNumber, String note, Object... params) throws Exception;

    void saveNote(List<NoteFile> noteFileList);

    void delNote(String path, int lineNumber, Object... params) throws Exception;

    void delNote(File file, int lineNumber, Object... params) throws Exception;

    void delNoteFile(NoteFile noteFile) throws Exception;

    void loadCache(String path, Object... params) throws Exception;

    void removeCache(String path, Object... params) throws Exception;

    void wrapDownNote(String path, int lineNumber, Object... params) throws Exception;

    void continueToWrapDown(String path, int lineNumber, int wrapCount, Object... params) throws Exception;

    void continueToWrapUp(String path, int lineNumber, int wrapCount,Object... params) throws Exception;


    void updateVersion(String path, Object... params) throws Exception;

    void updateFileName(String nowPath, String oldFileName, Object... params) throws Exception;

    String generateVersion(boolean cacheInvalid, Object... params) throws Exception;

    String generateVersionByCache(Object... params) throws Exception;

    void removeCache(long lastTime);

    String migratePathPrefix(String oldPrefix, String newPrefix) throws Exception;

}
