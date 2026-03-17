package com.theblind.privatenotes.core.util;


import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.intellij.openapi.project.Project;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


public class PrivateNotesUtil {

    static final TimedCache<String, Object> timedCache = CacheUtil.newTimedCache(60 * 1000);

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
            return bufferedInputStream.readAllBytes();
        }
    }


    public static void errLog(Throwable throwable, Project project) {
        String message = String.valueOf(throwable.getMessage());
        if (!timedCache.containsKey(message)) {
            timedCache.put(message, null);
            try (StringWriter stringWriter = new StringWriter();
                 PrintWriter writer = new PrintWriter(stringWriter)) {
                throwable.printStackTrace(writer);
                StringBuffer buffer = stringWriter.getBuffer();
                IdeaApiUtil.showErrNotification(buffer.toString(), project);
            } catch (IOException e) {
                // ignore
            }
        }

    }


    public static void errLog(String message, Project project) {
        try {
            IdeaApiUtil.showErrNotification(message, project);
        } catch (Exception e) {
            // ignore
        }
    }


    public static void errLog(String message) {
        errLog(message, null);
    }

    public static void infoLog(String message, Project project) {
        if (!timedCache.containsKey(message)) {
            timedCache.put(message, null);
            try {
                IdeaApiUtil.showInfoNotification(message, project);
            } catch (Exception e) {
                // ignore
            }
        }

    }

    public static void infoLog(String message) {
        infoLog(message, null);

    }

}
