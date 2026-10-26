package com.wm.usbmanager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Invoked only after the selected APK has passed identity and compatibility checks. */
public final class AmapInstall {
    public static final String PACKAGE = "com.autonavi.amapauto";
    public static final String NOTICE = "安装前清除高德应用数据；普通高德签名不兼容时，自动完整卸载旧高德后重装。收藏、账号、设置及离线地图将丢失，请先保存需要的内容。";
    public interface Command { String run(String command) throws Exception; }
    public interface RetainedData { boolean clear() throws Exception; }
    public interface Presence { boolean exists() throws Exception; }
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
            return preparation + "；完整重装后系统仍拒绝高德签名，已停止重复卸载；请检查是否有系统内置高德或其他用户保留的版本。\n" + output;
        return "安装或恢复启用失败：" + (preparation.isEmpty() ? "" : preparation + "\n") + output;
    }
    public static boolean ownerOnly(String users) {
        if (users == null) return false;
        Matcher matcher = Pattern.compile("UserInfo\\{([0-9]+):").matcher(users);
        Set<String> ids = new HashSet<>();
        while (matcher.find()) ids.add(matcher.group(1));
        return ids.size() == 1 && ids.contains("0");
    }
    public static boolean signatureConflict(String pkg, String output) {
        return applies(pkg) && output != null && output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE");
    }
    public static String replaceConflict(String pkg, boolean system, String users, Command command, Presence remains) throws Exception {
        if (!applies(pkg)) throw new SecurityException("签名冲突重装只允许处理高德地图");
        if (system) throw new SecurityException("旧高德属于系统应用，不能自动完整卸载；请使用匹配签名的安装包");
        if (!ownerOnly(users)) throw new SecurityException("检测到多个用户或用户状态不可读，不能自动完整卸载高德");
        String output = command.run("pm uninstall 'com.autonavi.amapauto'");
        if (output == null || !output.trim().equals("Success")) throw new IOException("旧高德完整卸载失败，已停止安装：" + output);
        if (remains.exists()) throw new IOException("完整卸载后仍有旧高德记录，已停止安装，请检查系统保留状态");
        return "已完整卸载签名不兼容的旧高德及应用数据";
    }
}
