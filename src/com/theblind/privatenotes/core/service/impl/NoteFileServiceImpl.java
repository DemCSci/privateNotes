package com.theblind.privatenotes.core.service.impl;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.theblind.privatenotes.core.Config;
import com.theblind.privatenotes.core.NoteFile;
import com.theblind.privatenotes.core.PrivateNotesFactory;
import com.theblind.privatenotes.core.service.ConfigService;
import com.theblind.privatenotes.core.service.NoteFileService;
import com.theblind.privatenotes.core.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public class NoteFileServiceImpl implements NoteFileService {

    ConfigService configService = PrivateNotesFactory.getConfigService();

    Map<String, String> versionCache = new ConcurrentHashMap<>();

    LRUCache<String, NoteFile> noteFileCache = CacheUtil.newLRUCache(32);

    @Override
    public NoteFile get(File file, Object... params) throws Exception {
        Config config = configService.get();
        String version = generateVersionByCache(params.length == 0 ? new Object[]{file} : params);
        if (StrUtil.isBlank(version)) {
            return null;
        }
        File expectedStorage = getAbsolutePath(config, file, version).toFile();
        File storageFile = resolveStorageFile(config, file, version, expectedStorage);
        if (!storageFile.exists()) {
            return null;
        }

        NoteFile noteFile;
        String cacheKey = storageFile.getAbsolutePath();
        synchronized (NoteFileService.class) {
            noteFile = noteFileCache.get(cacheKey, () -> readNoteFile(storageFile));
        }
        if (noteFile == null) {
            return null;
        }
        return migrateIfNeeded(file, version, storageFile, expectedStorage, noteFile);
    }

    @Override
    public NoteFile get(String path, Object... params) throws Exception {
        if (params.length == 0) {
            params = new Object[]{new File(path)};
        }
        return get(new File(path), params);
    }

    public File getStorage(Config config, File file, String version) throws Exception {
        return getAbsolutePath(config, file, version).toFile();
    }

    @Override
    public String getNote(File file, int lineNumber, Object... params) throws Exception {
        NoteFile noteFile = get(file, params);
        if (noteFile == null) {
            return null;
        }
        return noteFile.getNode(lineNumber);
    }

    @Override
    public String getNote(String path, int lineNumber, Object... params) throws Exception {
        return getNote(new File(path), lineNumber, params);
    }

    @Override
    public boolean exist(String path, Object... params) {
        return exist(new File(path), params);
    }

    @Override
    public boolean exist(File file, Object... params) {
        try {
            return get(file, params) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean noteExist(File file, int lineNumber, Object... params) throws Exception {
        return getNote(file, lineNumber, params) != null;
    }

    @Override
    public boolean noteExist(String path, int lineNumber, Object... params) throws Exception {
        return noteExist(new File(path), lineNumber, params);
    }

    @Override
    public String generateVersion(boolean cacheInvalid, Object... params) throws Exception {
        if (params == null || params.length == 0 || !(params[0] instanceof File)) {
            throw new RuntimeException("params[0] not instanceof File");
        }
        File file = (File) params[0];
        String canonicalPath = file.getCanonicalPath();
        String version;
        if (!cacheInvalid && (version = versionCache.get(canonicalPath)) != null) {
            return version;
        }

        byte[] contentBytes = findContentBytes(params);
        if (canDigestFromFile(file)) {
            try {
                version = md5Hex16(file);
            } catch (IOException ignored) {
                if (contentBytes == null) {
                    return versionCache.get(canonicalPath);
                }
                version = generateVersion(contentBytes);
            }
        } else {
            if (contentBytes == null) {
                return versionCache.get(canonicalPath);
            }
            version = generateVersion(contentBytes);
        }
        versionCache.put(canonicalPath, version);
        return version;
    }

    @Override
    public String generateVersionByCache(Object... params) throws Exception {
        return generateVersion(false, params);
    }

    @Override
    public void saveNote(NoteFile noteFile) throws Exception {
        Config config = configService.get();
        File storageFile = getAbsolutePath(config, noteFile).toFile();
        FileUtil.touch(storageFile);
        FileWriter fileWriter = new FileWriter(storageFile);
        fileWriter.write(JsonUtil.toJson(noteFile));
        noteFileCache.put(storageFile.getAbsolutePath(), noteFile);
    }

    @Override
    public void saveNote(String path, int lineNumber, String note, Object... params) throws Exception {
        saveNote(new File(path), lineNumber, note, params);
    }

    @Override
    public void saveNote(File file, int lineNumber, String note, Object... params) throws Exception {
        if (StrUtil.isBlank(note)) {
            return;
        }

        if (params.length == 0) {
            params = new Object[]{file};
        }
        NoteFile noteFile = get(file, params);
        if (noteFile == null) {
            noteFile = new NoteFile();
            populateIdentity(noteFile, file, generateVersionByCache(params));
        }
        noteFile.setNode(lineNumber, note.trim());
        saveNote(noteFile);
    }

    @Override
    public void saveNote(List<NoteFile> noteFileList) {
    }

    @Override
    public void delNote(String path, int lineNumber, Object... params) throws Exception {
        delNote(new File(path), lineNumber, params);
    }

    @Override
    public void delNote(File file, int lineNumber, Object... params) throws Exception {
        if (params.length == 0) {
            params = new Object[]{file};
        }
        NoteFile noteFile = get(file, params);
        if (noteFile == null) {
            return;
        }
        if (noteFile.getNodeSize() == 1) {
            delNoteFile(noteFile);
            return;
        }
        noteFile.removeNode(lineNumber);
        saveNote(noteFile);
    }

    @Override
    public void delNoteFile(NoteFile noteFile) throws Exception {
        Config config = configService.get();
        File storageFile = getAbsolutePath(config, noteFile).toFile();
        synchronized (NoteFileService.class) {
            FileUtil.del(storageFile);
            noteFileCache.remove(storageFile.getAbsolutePath());
        }
    }

    @Override
    public void loadCache(String path, Object... params) throws Exception {
        get(path, params);
    }

    @Override
    public void removeCache(String path, Object... params) {
        try {
            File file = new File(path);
            versionCache.remove(file.getCanonicalPath());

            String newPrefix = fileNamingPrefix(file);
            String legacyGitPrefix = legacyGitFileNamingPrefix(file);
            String legacyPrefix = legacyFileNamingPrefix(file.getName());
            noteFileCache.keySet().stream()
                    .filter(cachePath -> {
                        String storageName = new File(cachePath).getName();
                        return storageName.startsWith(newPrefix)
                                || (StrUtil.isNotBlank(legacyGitPrefix) && storageName.startsWith(legacyGitPrefix))
                                || storageName.startsWith(legacyPrefix);
                    })
                    .collect(Collectors.toList())
                    .forEach(noteFileCache::remove);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void wrapDownNote(String path, int lineNumber, Object... params) throws Exception {
        updateNoteLineNumber(path, lineNumber, lineNumber + 1, params);
    }

    @Override
    public void continueToWrapDown(String path, int lineNumber, int wrapCount, Object... params) throws Exception {
        NoteFile noteFile = get(path, params);
        if (noteFile == null) {
            return;
        }
        Map<Integer, String> notes = noteFile.getNotes();
        if (notes == null || notes.isEmpty()) {
            return;
        }

        TreeMap<Integer, String> sort = MapUtil.sort(notes, (k1, k2) -> k2 - k1);
        SortedMap<Integer, String> subMap = sort.subMap(sort.firstKey(), lineNumber - 1);
        subMap.forEach((key, value) -> {
            notes.remove(key);
            notes.put(key + wrapCount, value);
        });
        saveNote(noteFile);
    }

    @Override
    public void continueToWrapUp(String path, int lineNumber, int wrapCount, Object... params) throws Exception {
        NoteFile noteFile = get(path, params);
        if (noteFile == null) {
            return;
        }
        Map<Integer, String> notes = noteFile.getNotes();
        if (notes == null || notes.isEmpty()) {
            return;
        }

        TreeMap<Integer, String> sort = MapUtil.sort(notes);
        SortedMap<Integer, String> subMap = sort.subMap(lineNumber, sort.lastKey() + 1);
        subMap.forEach((key, value) -> {
            notes.remove(key);
            notes.put(key - wrapCount, value);
        });
        saveNote(noteFile);
    }

    public void updateNoteLineNumber(String path, int lineNumber, int newLineNumber, Object... params) throws Exception {
        NoteFile noteFile = get(path, params);
        if (noteFile == null) {
            return;
        }
        String node = noteFile.getNode(lineNumber);
        if (node == null) {
            return;
        }

        noteFile.removeNode(lineNumber);
        noteFile.setNode(newLineNumber, node);
        saveNote(noteFile);
    }

    @Override
    public void updateVersion(String path, Object... params) throws Exception {
        byte[] previousBytes = extractPreviousBytes(params);
        if (previousBytes != null) {
            updateVersionWithPreviousContent(path, previousBytes);
            return;
        }

        Config config = configService.get();
        File file = new File(path);
        String canonicalPath = file.getCanonicalPath();

        String beforeVersion = versionCache.get(canonicalPath);
        if (beforeVersion == null) {
            beforeVersion = generateVersionByCache(file);
        }
        if (StrUtil.isBlank(beforeVersion)) {
            return;
        }

        File oldStorage = resolveStorageFile(config, file, beforeVersion, getAbsolutePath(config, file, beforeVersion).toFile());
        if (!oldStorage.exists()) {
            return;
        }

        String version = generateVersion(true, file);
        if (StrUtil.isBlank(version)) {
            return;
        }
        versionCache.put(canonicalPath, version);

        NoteFile noteFile = readNoteFile(oldStorage);
        if (noteFile == null) {
            return;
        }
        populateIdentity(noteFile, file, version);
        saveNote(noteFile);

        noteFileCache.remove(oldStorage.getAbsolutePath());
        File newStorage = getAbsolutePath(config, noteFile).toFile();
        if (!Objects.equals(oldStorage.getAbsolutePath(), newStorage.getAbsolutePath())) {
            FileUtil.del(oldStorage);
        }
    }

    @Override
    public void updateFileName(String nowPath, String oldFileName, Object... params) throws Exception {
        Config config = configService.get();
        File file = new File(nowPath);
        File oldFile = new File(file.getParentFile(), oldFileName);
        String version = generateVersionByCache(file);
        if (StrUtil.isBlank(version)) {
            return;
        }
        File oldStorage = resolveStorageFile(config, oldFile, version, getAbsolutePath(config, oldFile, version).toFile());
        if (!oldStorage.exists()) {
            return;
        }

        NoteFile noteFile = readNoteFile(oldStorage);
        if (noteFile == null) {
            return;
        }
        populateIdentity(noteFile, file, version);
        saveNote(noteFile);

        noteFileCache.remove(oldStorage.getAbsolutePath());
        File newStorage = getAbsolutePath(config, noteFile).toFile();
        if (!Objects.equals(oldStorage.getAbsolutePath(), newStorage.getAbsolutePath())) {
            FileUtil.del(oldStorage);
        }
    }

    @Override
    public void removeCache(long lastTime) {
        noteFileCache.keySet().stream()
                .filter(noteFilePath -> {
                    File noteFile = FileUtil.file(noteFilePath);
                    return noteFile.exists() && noteFile.lastModified() > lastTime;
                })
                .collect(Collectors.toList())
                .forEach(noteFileCache::remove);
    }

    @Override
    public String migratePathPrefix(String oldPrefix, String newPrefix) throws Exception {
        String normalizedOldPrefix = normalizeMigrationPrefix(oldPrefix);
        String normalizedNewPrefix = normalizeMigrationPrefix(newPrefix);
        if (StrUtil.isBlank(normalizedOldPrefix) || StrUtil.isBlank(normalizedNewPrefix)) {
            throw new IllegalArgumentException("旧路径前缀和新路径前缀不能为空");
        }

        Config config = configService.get();
        File storageRoot = new File(config.getUserSavePath());
        if (!storageRoot.exists()) {
            return "未找到私人注释目录，无需迁移";
        }

        List<File> storageFiles = new ArrayList<>();
        collectStorageFiles(storageRoot, storageFiles);
        Map<String, List<File>> targetFilesByName = indexFilesByName(new File(normalizedNewPrefix));
        StringBuilder migratedDetails = new StringBuilder();
        StringBuilder skippedDetails = new StringBuilder();

        int migrated = 0;
        int merged = 0;
        int skipped = 0;

        for (File storageFile : storageFiles) {
            NoteFile noteFile;
            try {
                noteFile = readNoteFile(storageFile);
            } catch (Exception e) {
                skipped++;
                appendMigrationLine(skippedDetails, "跳过", storageFile.getAbsolutePath(), "注释文件无法解析: " + e.getMessage());
                continue;
            }
            if (noteFile == null) {
                skipped++;
                appendMigrationLine(skippedDetails, "跳过", storageFile.getAbsolutePath(), "注释文件为空");
                continue;
            }

            MigrationTargetResolution resolution = resolveMigrationTargetFile(noteFile, normalizedOldPrefix, normalizedNewPrefix, targetFilesByName);
            if (resolution.targetFile == null) {
                skipped++;
                appendMigrationLine(skippedDetails, "跳过", describeNoteFile(storageFile, noteFile), resolution.reason);
                continue;
            }

            File targetFile = resolution.targetFile;
            String targetVersion = targetFile.exists() ? generateVersionByCache(targetFile) : noteFile.getVersion();
            populateIdentity(noteFile, targetFile, targetVersion);

            File targetStorage = getAbsolutePath(config, noteFile).toFile();
            int mergedInThisItem = 0;
            if (targetStorage.exists() && !Objects.equals(targetStorage.getAbsolutePath(), storageFile.getAbsolutePath())) {
                NoteFile existingNoteFile = readNoteFile(targetStorage);
                if (existingNoteFile != null) {
                    populateIdentity(existingNoteFile, targetFile, targetVersion);
                    mergedInThisItem = mergeNotes(existingNoteFile, noteFile);
                    merged += mergedInThisItem;
                    noteFile = existingNoteFile;
                }
            }

            saveNote(noteFile);
            noteFileCache.remove(storageFile.getAbsolutePath());
            noteFileCache.put(targetStorage.getAbsolutePath(), noteFile);
            if (!Objects.equals(targetStorage.getAbsolutePath(), storageFile.getAbsolutePath())) {
                FileUtil.del(storageFile);
            }
            migrated++;
            String detail = targetFile.getCanonicalPath();
            if (mergedInThisItem > 0) {
                detail += String.format("，合并 %d 条同目标注释", mergedInThisItem);
            }
            appendMigrationLine(migratedDetails, "迁移", describeNoteFile(storageFile, noteFile), detail);
        }
        StringBuilder report = new StringBuilder();
        report.append(String.format("路径迁移完成：迁移 %d 条，合并 %d 条，跳过 %d 条", migrated, merged, skipped)).append("\n");
        report.append("旧前缀: ").append(normalizedOldPrefix).append("\n");
        report.append("新前缀: ").append(normalizedNewPrefix).append("\n");
        if (migratedDetails.length() > 0) {
            report.append("\n成功明细\n").append(migratedDetails);
        }
        if (skippedDetails.length() > 0) {
            report.append("\n跳过明细\n").append(skippedDetails);
        }
        return report.toString().trim();
    }

    public Path getAbsolutePath(Config config, NoteFile noteFile) {
        if (StrUtil.isBlank(noteFile.getFileId())) {
            return getLegacyAbsolutePath(config, noteFile.getFileName(), noteFile.getVersion());
        }
        return Paths.get(config.getUserSavePath(), fileNamingRules(noteFile) + ".txt");
    }

    public Path getAbsolutePath(Config config, File file, String version) throws Exception {
        FileNameParts fileNameParts = splitFileName(file.getName());
        return Paths.get(config.getUserSavePath(), fileNamingRules(fileNameParts.simpleName, fileNameParts.type, generateFileId(file), version) + ".txt");
    }

    public String fileNamingRules(NoteFile noteFile) {
        return fileNamingRules(noteFile.getFileSimpleName(), noteFile.getFileType(), noteFile.getFileId(), noteFile.getVersion());
    }

    public String fileNamingRules(String fileName, String type, String fileId, String version) {
        return fileName + type + "_" + fileId + "_" + version;
    }

    private NoteFile migrateIfNeeded(File file,
                                     String version,
                                     File storageFile,
                                     File expectedStorage,
                                     NoteFile noteFile) throws Exception {
        boolean metadataChanged = populateIdentity(noteFile, file, version);
        if (!Objects.equals(storageFile.getAbsolutePath(), expectedStorage.getAbsolutePath()) || metadataChanged) {
            saveNote(noteFile);
            noteFileCache.remove(storageFile.getAbsolutePath());
            noteFileCache.put(expectedStorage.getAbsolutePath(), noteFile);
            if (!Objects.equals(storageFile.getAbsolutePath(), expectedStorage.getAbsolutePath())) {
                FileUtil.del(storageFile);
            }
        }
        return noteFile;
    }

    private void updateVersionWithPreviousContent(String path, byte[] previousBytes) throws Exception {
        Config config = configService.get();
        File file = new File(path);
        String canonicalPath = file.getCanonicalPath();
        String previousVersion = generateVersion(previousBytes);
        String currentVersion = generateVersion(true, file);
        if (StrUtil.isBlank(currentVersion)) {
            return;
        }
        versionCache.put(canonicalPath, currentVersion);

        File oldStorage = resolveStorageFile(config, file, previousVersion, getAbsolutePath(config, file, previousVersion).toFile());
        if (!oldStorage.exists()) {
            return;
        }

        NoteFile noteFile = readNoteFile(oldStorage);
        if (noteFile == null) {
            return;
        }

        populateIdentity(noteFile, file, previousVersion);
        relocateNotes(noteFile, previousBytes, readCurrentBytes(file));
        populateIdentity(noteFile, file, currentVersion);
        saveNote(noteFile);

        noteFileCache.remove(oldStorage.getAbsolutePath());
        File newStorage = getAbsolutePath(config, noteFile).toFile();
        if (!Objects.equals(oldStorage.getAbsolutePath(), newStorage.getAbsolutePath())) {
            FileUtil.del(oldStorage);
        }
    }

    private void relocateNotes(NoteFile noteFile, byte[] previousBytes, byte[] currentBytes) {
        Map<Integer, String> notes = noteFile.getNotes();
        if (notes == null || notes.isEmpty()) {
            return;
        }

        List<String> oldLines = splitLines(previousBytes);
        List<String> newLines = splitLines(currentBytes);
        int commonPrefix = countCommonPrefixLines(oldLines, newLines);
        int commonSuffix = countCommonSuffixLines(oldLines, newLines, commonPrefix);
        TreeMap<Integer, String> sortedNotes = new TreeMap<>(notes);
        TreeMap<Integer, String> relocatedNotes = new TreeMap<>();
        Set<Integer> usedLines = new HashSet<>();
        int maxLine = Math.max(newLines.size() - 1, 0);

        for (Map.Entry<Integer, String> entry : sortedNotes.entrySet()) {
            int candidate = findRelocatedLine(entry.getKey(), oldLines, newLines, commonPrefix, commonSuffix);
            int assignedLine = reserveLine(candidate, maxLine, usedLines);
            relocatedNotes.put(assignedLine, entry.getValue());
        }
        noteFile.setNotes(relocatedNotes);
    }

    private int findRelocatedLine(int oldLine,
                                  List<String> oldLines,
                                  List<String> newLines,
                                  int commonPrefix,
                                  int commonSuffix) {
        int newMaxLine = Math.max(newLines.size() - 1, 0);
        if (oldLines.isEmpty()) {
            return clampLine(oldLine, newMaxLine);
        }

        int normalizedOldLine = clampLine(oldLine, oldLines.size() - 1);
        if (normalizedOldLine < commonPrefix) {
            return clampLine(normalizedOldLine, newMaxLine);
        }
        if (commonSuffix > 0 && normalizedOldLine >= oldLines.size() - commonSuffix) {
            int suffixDistance = oldLines.size() - normalizedOldLine;
            return clampLine(newLines.size() - suffixDistance, newMaxLine);
        }

        String currentLineText = oldLines.get(normalizedOldLine);
        if (normalizedOldLine < newLines.size() && currentLineText.equals(newLines.get(normalizedOldLine))) {
            return normalizedOldLine;
        }

        String previousLineText = normalizedOldLine > 0 ? oldLines.get(normalizedOldLine - 1) : null;
        String nextLineText = normalizedOldLine + 1 < oldLines.size() ? oldLines.get(normalizedOldLine + 1) : null;

        Integer uniqueContextLine = findUniqueContextLine(previousLineText, currentLineText, nextLineText, newLines);
        if (uniqueContextLine != null) {
            return uniqueContextLine;
        }

        List<Integer> exactLineMatches = findLineMatches(currentLineText, newLines);
        if (!exactLineMatches.isEmpty()) {
            return nearestLine(exactLineMatches, normalizedOldLine);
        }

        return clampLine(normalizedOldLine + (newLines.size() - oldLines.size()), newMaxLine);
    }

    private Integer findUniqueContextLine(String previousLineText,
                                          String currentLineText,
                                          String nextLineText,
                                          List<String> newLines) {
        List<Integer> tripleMatches = findContextMatches(previousLineText, currentLineText, nextLineText, newLines);
        if (tripleMatches.size() == 1) {
            return tripleMatches.get(0);
        }

        if (previousLineText != null) {
            List<Integer> previousAndCurrentMatches = findContextMatches(previousLineText, currentLineText, null, newLines);
            if (previousAndCurrentMatches.size() == 1) {
                return previousAndCurrentMatches.get(0);
            }
        }

        if (nextLineText != null) {
            List<Integer> currentAndNextMatches = findContextMatches(null, currentLineText, nextLineText, newLines);
            if (currentAndNextMatches.size() == 1) {
                return currentAndNextMatches.get(0);
            }
        }
        return null;
    }

    private List<Integer> findContextMatches(String previousLineText,
                                             String currentLineText,
                                             String nextLineText,
                                             List<String> newLines) {
        return java.util.stream.IntStream.range(0, newLines.size())
                .filter(index -> currentLineText.equals(newLines.get(index)))
                .filter(index -> previousLineText == null || (index > 0 && previousLineText.equals(newLines.get(index - 1))))
                .filter(index -> nextLineText == null || (index + 1 < newLines.size() && nextLineText.equals(newLines.get(index + 1))))
                .boxed()
                .collect(Collectors.toList());
    }

    private List<Integer> findLineMatches(String targetLine, List<String> lines) {
        return java.util.stream.IntStream.range(0, lines.size())
                .filter(index -> targetLine.equals(lines.get(index)))
                .boxed()
                .collect(Collectors.toList());
    }

    private int nearestLine(List<Integer> candidates, int targetLine) {
        return candidates.stream()
                .min((left, right) -> {
                    int leftDistance = Math.abs(left - targetLine);
                    int rightDistance = Math.abs(right - targetLine);
                    if (leftDistance != rightDistance) {
                        return Integer.compare(leftDistance, rightDistance);
                    }
                    return Integer.compare(left, right);
                })
                .orElse(targetLine);
    }

    private int reserveLine(int candidate, int maxLine, Set<Integer> usedLines) {
        int normalizedCandidate = clampLine(candidate, maxLine);
        for (int line = normalizedCandidate; line <= maxLine; line++) {
            if (usedLines.add(line)) {
                return line;
            }
        }
        for (int line = normalizedCandidate - 1; line >= 0; line--) {
            if (usedLines.add(line)) {
                return line;
            }
        }
        return normalizedCandidate;
    }

    private int countCommonPrefixLines(List<String> oldLines, List<String> newLines) {
        int limit = Math.min(oldLines.size(), newLines.size());
        int index = 0;
        while (index < limit && Objects.equals(oldLines.get(index), newLines.get(index))) {
            index++;
        }
        return index;
    }

    private int countCommonSuffixLines(List<String> oldLines, List<String> newLines, int prefixLength) {
        int oldIndex = oldLines.size() - 1;
        int newIndex = newLines.size() - 1;
        int suffix = 0;
        while (oldIndex >= prefixLength
                && newIndex >= prefixLength
                && Objects.equals(oldLines.get(oldIndex), newLines.get(newIndex))) {
            suffix++;
            oldIndex--;
            newIndex--;
        }
        return suffix;
    }

    private List<String> splitLines(byte[] content) {
        String normalizedContent = new String(content, StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        return Arrays.asList(normalizedContent.split("\n", -1));
    }

    private int clampLine(int lineNumber, int maxLine) {
        if (maxLine <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(lineNumber, maxLine));
    }

    private byte[] extractPreviousBytes(Object... params) {
        if (params.length > 0 && params[0] instanceof byte[] previousBytes) {
            return previousBytes;
        }
        return null;
    }

    private byte[] findContentBytes(Object... params) {
        if (params == null) {
            return null;
        }
        for (Object param : params) {
            if (param instanceof byte[] contentBytes) {
                return contentBytes;
            }
        }
        return null;
    }

    private String generateVersion(byte[] content) {
        return md5Hex16(content);
    }

    private String generateFileId(File file) throws Exception {
        return "f" + md5Hex16(file.getCanonicalPath().getBytes(StandardCharsets.UTF_8));
    }

    private String generateLegacyGitFileId(File file) throws Exception {
        File gitRoot = findGitRoot(file);
        if (gitRoot == null) {
            return null;
        }
        String gitRootHash = md5Hex16(gitRoot.getCanonicalPath().getBytes(StandardCharsets.UTF_8));
        String relativePath = normalizePath(gitRoot.toPath().relativize(file.getCanonicalFile().toPath()).toString());
        String relativePathHash = md5Hex16(relativePath.getBytes(StandardCharsets.UTF_8));
        return "g" + gitRootHash + relativePathHash;
    }

    private NoteFile readNoteFile(File storageFile) {
        FileReader fileReader = new FileReader(storageFile);
        return JSONUtil.toBean(fileReader.readString(), NoteFile.class);
    }

    private File resolveStorageFile(Config config, File file, String version, File expectedStorage) throws Exception {
        if (expectedStorage.exists()) {
            return expectedStorage;
        }
        File legacyGitStorage = tryResolveLegacyGitStorage(config, file, version);
        if (legacyGitStorage != null && legacyGitStorage.exists()) {
            return legacyGitStorage;
        }
        File legacyStorage = getLegacyAbsolutePath(config, file.getName(), version).toFile();
        if (legacyStorage.exists()) {
            return legacyStorage;
        }
        return expectedStorage;
    }

    private File tryResolveLegacyGitStorage(Config config, File file, String version) {
        try {
            return getLegacyGitAbsolutePath(config, file, version).toFile();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean populateIdentity(NoteFile noteFile, File file, String version) throws Exception {
        FileNameParts fileNameParts = splitFileName(file.getName());
        String fileId = generateFileId(file);
        String filePath = file.getCanonicalPath();
        boolean changed = !Objects.equals(noteFile.getFileName(), file.getName())
                || !Objects.equals(noteFile.getFileSimpleName(), fileNameParts.simpleName)
                || !Objects.equals(noteFile.getFileType(), fileNameParts.type)
                || !Objects.equals(noteFile.getFilePath(), filePath)
                || !Objects.equals(noteFile.getFileId(), fileId)
                || !Objects.equals(noteFile.getVersion(), version);

        noteFile.setFileName(file.getName());
        noteFile.setFileSimpleName(fileNameParts.simpleName);
        noteFile.setFileType(fileNameParts.type);
        noteFile.setFilePath(filePath);
        noteFile.setFileId(fileId);
        noteFile.setVersion(version);
        return changed;
    }

    private Path getLegacyAbsolutePath(Config config, String fileName, String version) {
        FileNameParts fileNameParts = splitFileName(fileName);
        return Paths.get(config.getUserSavePath(), legacyFileNamingRules(fileNameParts.simpleName, fileNameParts.type, version) + ".txt");
    }

    private String fileNamingPrefix(File file) throws Exception {
        FileNameParts fileNameParts = splitFileName(file.getName());
        return fileNamingRules(fileNameParts.simpleName, fileNameParts.type, generateFileId(file), "");
    }

    private String legacyGitFileNamingPrefix(File file) throws Exception {
        FileNameParts fileNameParts = splitFileName(file.getName());
        String legacyGitFileId = generateLegacyGitFileId(file);
        if (legacyGitFileId == null) {
            return "";
        }
        return fileNamingRules(fileNameParts.simpleName, fileNameParts.type, legacyGitFileId, "");
    }

    private String legacyFileNamingPrefix(String fileName) {
        FileNameParts fileNameParts = splitFileName(fileName);
        return legacyFileNamingRules(fileNameParts.simpleName, fileNameParts.type, "");
    }

    private Path getLegacyGitAbsolutePath(Config config, File file, String version) throws Exception {
        FileNameParts fileNameParts = splitFileName(file.getName());
        String legacyGitFileId = generateLegacyGitFileId(file);
        if (legacyGitFileId == null) {
            return Paths.get(config.getUserSavePath(), "__not_exists__.txt");
        }
        return Paths.get(config.getUserSavePath(), fileNamingRules(fileNameParts.simpleName, fileNameParts.type, legacyGitFileId, version) + ".txt");
    }

    private File findGitRoot(File file) throws Exception {
        File current = file.getCanonicalFile();
        if (current.isFile()) {
            current = current.getParentFile();
        }
        while (current != null) {
            File gitMarker = new File(current, ".git");
            if (gitMarker.exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        return null;
    }

    private String normalizePath(String path) {
        return path.replace(File.separatorChar, '/');
    }

    private String md5Hex16(File file) throws Exception {
        MessageDigest digest = createMd5Digest();
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toMd5Hex16(digest.digest());
    }

    private String md5Hex16(byte[] content) {
        MessageDigest digest = createMd5Digest();
        digest.update(content);
        return toMd5Hex16(digest.digest());
    }

    private MessageDigest createMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5", "SUN");
        } catch (GeneralSecurityException ignored) {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("MD5 digest is not available", e);
            }
        }
    }

    private String toMd5Hex16(byte[] digestBytes) {
        String fullMd5 = HexFormat.of().formatHex(digestBytes);
        return fullMd5.substring(8, 24);
    }

    private boolean canDigestFromFile(File file) {
        try {
            Path path = file.toPath();
            return Files.isRegularFile(path) && Files.isReadable(path);
        } catch (Exception ignored) {
            return false;
        }
    }

    private byte[] readCurrentBytes(File file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return inputStream.readAllBytes();
        }
    }

    private String normalizeMigrationPrefix(String prefix) {
        if (StrUtil.isBlank(prefix)) {
            return "";
        }
        String normalized = normalizePath(new File(prefix).getAbsoluteFile().toPath().normalize().toString());
        return trimTrailingSlash(normalized);
    }

    private String trimTrailingSlash(String path) {
        if (StrUtil.isBlank(path)) {
            return path;
        }
        String result = path;
        while (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private MigrationTargetResolution resolveMigrationTargetFile(NoteFile noteFile,
                                                                 String oldPrefix,
                                                                 String newPrefix,
                                                                 Map<String, List<File>> targetFilesByName) throws Exception {
        if (StrUtil.isNotBlank(noteFile.getFilePath())) {
            String normalizedStoredPath = trimTrailingSlash(normalizePath(noteFile.getFilePath()));
            String targetPath = replacePrefix(normalizedStoredPath, oldPrefix, newPrefix);
            if (targetPath == null) {
                return MigrationTargetResolution.skipped("原路径不在旧前缀下: " + normalizedStoredPath);
            }
            File targetFile = new File(targetPath);
            if (targetFile.exists() && targetFile.isFile()) {
                return MigrationTargetResolution.resolved(targetFile.getCanonicalFile());
            }
            return MigrationTargetResolution.skipped("新路径不存在: " + targetFile.getAbsolutePath());
        }
        return guessTargetFile(noteFile, targetFilesByName);
    }

    private String replacePrefix(String path, String oldPrefix, String newPrefix) {
        if (Objects.equals(path, oldPrefix)) {
            return newPrefix;
        }
        String oldPrefixWithSlash = oldPrefix + "/";
        if (!path.startsWith(oldPrefixWithSlash)) {
            return null;
        }
        return newPrefix + path.substring(oldPrefix.length());
    }

    private MigrationTargetResolution guessTargetFile(NoteFile noteFile, Map<String, List<File>> targetFilesByName) throws Exception {
        if (StrUtil.isBlank(noteFile.getFileName()) || StrUtil.isBlank(noteFile.getVersion())) {
            return MigrationTargetResolution.skipped("旧注释缺少 filePath 且无法根据文件名/版本推断目标文件");
        }
        List<File> candidates = targetFilesByName.get(noteFile.getFileName());
        if (candidates == null || candidates.isEmpty()) {
            return MigrationTargetResolution.skipped("新前缀下没有找到同名文件: " + noteFile.getFileName());
        }

        List<File> matchedCandidates = new ArrayList<>();
        for (File candidate : candidates) {
            if (!candidate.isFile()) {
                continue;
            }
            if (Objects.equals(generateVersionByCache(candidate), noteFile.getVersion())) {
                matchedCandidates.add(candidate.getCanonicalFile());
            }
        }
        if (matchedCandidates.size() == 1) {
            return MigrationTargetResolution.resolved(matchedCandidates.get(0));
        }
        if (matchedCandidates.isEmpty()) {
            return MigrationTargetResolution.skipped("找到同名文件但内容版本不匹配: " + noteFile.getFileName());
        }
        return MigrationTargetResolution.skipped("找到多个候选文件，无法唯一确定目标: " + noteFile.getFileName());
    }

    private Map<String, List<File>> indexFilesByName(File root) {
        Map<String, List<File>> filesByName = new ConcurrentHashMap<>();
        if (!root.exists()) {
            return filesByName;
        }
        List<File> targetFiles = new ArrayList<>();
        collectSourceFiles(root, targetFiles);
        for (File targetFile : targetFiles) {
            filesByName.computeIfAbsent(targetFile.getName(), key -> new ArrayList<>()).add(targetFile);
        }
        return filesByName;
    }

    private void collectSourceFiles(File root, List<File> files) {
        File[] children = root.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectSourceFiles(child, files);
            } else {
                files.add(child);
            }
        }
    }

    private void collectStorageFiles(File root, List<File> files) {
        File[] children = root.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectStorageFiles(child, files);
            } else if (child.getName().endsWith(".txt")) {
                files.add(child);
            }
        }
    }

    private int mergeNotes(NoteFile target, NoteFile source) {
        if (source.getNotes() == null || source.getNotes().isEmpty()) {
            return 0;
        }
        if (target.getNotes() == null) {
            target.setNotes(new TreeMap<>());
        }
        int merged = 0;
        for (Map.Entry<Integer, String> entry : source.getNotes().entrySet()) {
            if (!target.getNotes().containsKey(entry.getKey())) {
                target.getNotes().put(entry.getKey(), entry.getValue());
                merged++;
            }
        }
        return merged;
    }

    private void appendMigrationLine(StringBuilder builder, String type, String source, String detail) {
        builder.append("[").append(type).append("] ")
                .append(source)
                .append(" -> ")
                .append(detail)
                .append("\n");
    }

    private String describeNoteFile(File storageFile, NoteFile noteFile) {
        if (noteFile != null && StrUtil.isNotBlank(noteFile.getFilePath())) {
            return noteFile.getFilePath();
        }
        if (noteFile != null && StrUtil.isNotBlank(noteFile.getFileName())) {
            return noteFile.getFileName() + " (" + storageFile.getName() + ")";
        }
        return storageFile.getAbsolutePath();
    }

    private String legacyFileNamingRules(String fileName, String type, String version) {
        return fileName + type + version;
    }

    private FileNameParts splitFileName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return new FileNameParts(fileName, " ");
        }
        return new FileNameParts(fileName.substring(0, dotIndex), fileName.substring(dotIndex + 1));
    }

    private static class FileNameParts {
        final String simpleName;
        final String type;

        private FileNameParts(String simpleName, String type) {
            this.simpleName = simpleName;
            this.type = type;
        }
    }

    private static class MigrationTargetResolution {
        final File targetFile;
        final String reason;

        private MigrationTargetResolution(File targetFile, String reason) {
            this.targetFile = targetFile;
            this.reason = reason;
        }

        private static MigrationTargetResolution resolved(File targetFile) {
            return new MigrationTargetResolution(targetFile, null);
        }

        private static MigrationTargetResolution skipped(String reason) {
            return new MigrationTargetResolution(null, reason);
        }
    }
}
