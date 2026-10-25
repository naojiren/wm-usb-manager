package com.wm.usbmanager;

import java.io.IOException;

/** Invoked only after the selected APK has passed identity and compatibility checks. */
public final class AmapInstall {
    public static final String PACKAGE = "com.autonavi.amapauto";
    public static final String NOTICE = "安装前自动清除车机中高德的应用数据；收藏、账号、设置及离线地图可能丢失，请先保存所需内容。";
    public interface Command { String run(String command) throws Exception; }
    public interface RetainedData { boolean clear() throws Exception; }
    public static boolean applies(String pkg) { return PACKAGE.equals(pkg); }
    public static String beforeInstall(String pkg, boolean present, Command command) throws Exception {
        return beforeInstall(pkg, present, true, command, () -> false);
    }
    public static String beforeInstall(String pkg, boolean present, boolean active, Command command, RetainedData retained) throws Exception {
        if (!applies(pkg)) return "";
        if (!present) return "未检测到旧高德应用记录，无需清除数据";
        if (!active) {
            if (!retained.clear()) throw new IOException("高德残留应用数据清除失败，系统未确认成功，已停止安装");
            return "已通过系统包管理服务清除高德残留应用数据";
        }
        String output = command.run("pm clear --user 0 'com.autonavi.amapauto'");
        if (output == null || !output.trim().equals("Success"))
            throw new IOException("高德应用数据清除失败，已停止安装：" + output);
        return "已清除高德应用数据";
    }
    public static String failure(String pkg, String preparation, String output) {
        if (applies(pkg) && output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE"))
            return preparation + "；高德新旧包签名不一致。清除数据不能解除签名冲突，需要完整卸载旧高德后安装；本次未自动卸载旧应用。\n" + output;
        return "安装或恢复启用失败：" + (preparation.isEmpty() ? "" : preparation + "\n") + output;
    }
}
