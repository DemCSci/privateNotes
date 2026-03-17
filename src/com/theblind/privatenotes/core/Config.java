package com.theblind.privatenotes.core;


import cn.hutool.core.util.StrUtil;
import com.theblind.privatenotes.core.util.IdeaApiUtil;

import java.awt.*;
import java.io.File;

public class Config {
    public static final int DEFAULT_HOVER_POPUP_CLOSE_DELAY_MS = 3000;

    final String rootPath = System.getProperty("user.home") + File.separator + ".privateNotes";
    final String defaultUserPath = rootPath + File.separator + "default";

    /**
     * 是否开启 同步
     */
    boolean syncEnabled = false;

    /**
     *  用户
     */
    String user;

    /**
     * 注释颜色
     */
    String noteColor="128,128,128,255";

    /**
     * 注释字体
     */
    String noteFont;

    /**
     * 私人注释标记
     */
    String mark = "//";

    /**
     *标记颜色
     */
    String markColor = "153,153,255,255";

    /**
     * 最大字符数
     */
    Integer maxCharNum=30;

    /**
     * 悬浮弹层关闭延迟，单位毫秒
     */
    Integer hoverPopupCloseDelayMs = DEFAULT_HOVER_POPUP_CLOSE_DELAY_MS;


    public String getUserSavePath() {
        return StrUtil.isEmpty(user) ? defaultUserPath : (rootPath + File.separator + user);
    }

    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getNoteColor() {
        return noteColor;
    }

    public void setNoteColor(String noteColor) {
        this.noteColor = noteColor;
    }

    public String getNoteFont() {
        return noteFont;
    }

    public void setNoteFont(String noteFont) {
        this.noteFont = noteFont;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getMarkColor() {
        return markColor;
    }

    public void setMarkColor(String markColor) {
        this.markColor = markColor;
    }

    public Integer getMaxCharNum() {
        return maxCharNum;
    }

    public void setMaxCharNum(Integer maxCharNum) {
        this.maxCharNum = maxCharNum;
    }

    public Integer getHoverPopupCloseDelayMs() {
        return hoverPopupCloseDelayMs;
    }

    public void setHoverPopupCloseDelayMs(Integer hoverPopupCloseDelayMs) {
        this.hoverPopupCloseDelayMs = hoverPopupCloseDelayMs;
    }

    public static Color asColor(final String color) {
        final String[] rgb = color.split(",");
        return new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]), Integer.parseInt(rgb[3]));
    }

    public static String byColor(final Color color) {
        return String.format("%d,%d,%d,%d", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }


}
