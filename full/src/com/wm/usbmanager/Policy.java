package com.wm.usbmanager;

import java.io.File;
import java.io.IOException;

/** Shared, closed allowlist. Unknown packages cannot be removed. */
public final class Policy {
    public static final String PACKAGE = "com.wm.usbmanager";
    public static final String IME = PACKAGE + "/.BasicIme";
    public static final String SOCKET = "wm.usbmanager.v1";
    public static final String[][] OPTIONAL = {
        {"com.ivi.map", "原厂百度地图", "移除原厂导航及其仪表导航画面；请使用已安装的替代导航。", "1"},
        {"com.sinovoice.hcicloudinput", "原厂语音输入法", "移除语音键盘；执行前自动切换到可用键盘，备用键盘仅支持英文和数字。", "1"},
        {"com.ivi.audiobook", "原厂有声读物", "移除在线听书入口；不删除底层音频服务。", "0"},
        {"com.ivi.audiobookwidget", "听书桌面卡片", "移除原厂桌面的听书卡片。", "0"},
        {"com.ivi.netvideo", "原厂在线视频", "移除在线视频应用。", "0"},
        {"com.yftech.video", "原厂本地视频", "移除本地视频播放器，USB 视频播放入口可能不可用。", "0"},
        {"com.ivi.wechat", "原厂微信", "移除原厂微信应用及其入口。", "0"},
        {"com.ivi.wechatwidget", "微信桌面卡片", "移除原厂桌面的微信卡片。", "0"},
        {"com.ivi.mijia", "原厂米家", "移除米家智能家居入口。", "0"},
        {"com.ivi.musicplayer", "原厂音乐", "移除原厂音乐播放器；请先安装替代播放器。", "0"},
        {"com.ivi.musicwidget", "音乐桌面卡片", "移除原厂桌面的音乐卡片。", "0"}
    };
    public static String[] entry(String pkg) {
        for (String[] e : OPTIONAL) if (e[0].equals(pkg)) return e;
        return null;
    }
    public static void requireRemovable(String pkg) {
        if (entry(pkg) == null) throw new SecurityException("此程序受保护，禁止删除：" + pkg);
    }
    public static boolean protectedInstall(String pkg, boolean system) {
        return system || pkg.equals(PACKAGE) || pkg.equals("android") ||
            pkg.startsWith("com.android.") || pkg.startsWith("com.wm.") ||
            pkg.startsWith("com.yftech.") || pkg.startsWith("com.ivi.") ||
            pkg.startsWith("com.living.") || pkg.startsWith("com.nforetek.") ||
            pkg.startsWith("com.iflytek.") || pkg.startsWith("com.sinovoice.");
    }
    /** Exact identities keep the phone-side ADB helper out of the default set. */
    public static int installRank(String pkg) {
        if (pkg.equals("com.car.adbswitch")) return 0;
        if (pkg.equals("com.shihab.diplay.wmex5")) return 1;
        if (pkg.equals("com.wmhome.launcher")) return 2;
        if (pkg.equals("com.estrongs.android.pop")) return 3;
        if (pkg.equals("info.kfsoft.force.rotation")) return 4;
        return 100;
    }
    public static boolean defaultInstall(String pkg) { return installRank(pkg) < 5; }
    public static boolean removableThirdParty(String pkg, boolean system, int uid, boolean persistent, boolean role) {
        return !system && uid >= 10000 && !persistent && !role && !protectedInstall(pkg, false);
    }
    public static String installLabel(String pkg, String fallback) {
        if (pkg.equals("com.car.adbswitch")) return "ADB 工具";
        if (pkg.equals("com.shihab.diplay.wmex5")) return "DiPlay";
        if (pkg.equals("com.wmhome.launcher")) return "威马桌面";
        if (pkg.equals("com.estrongs.android.pop")) return "ES 文件管理器";
        if (pkg.equals("info.kfsoft.force.rotation")) return "强制旋转";
        if (pkg.equals("com.autonavi.amapauto")) return "高德地图";
        if (pkg.equals("com.bilibili.bilithings")) return "哔哩哔哩车机版";
        if (pkg.equals("com.netease.cloudmusic.iot")) return "网易云音乐车机版";
        if (pkg.equals("com.netease.cloudmusic")) return "网易云音乐经典版";
        if (pkg.equals("com.fgsqw.lanshare")) return "LANShare 局域网快传";
        if (pkg.equals("net.easyconn")) return "亿联车机版";
        if (pkg.equals("com.syu.hicar")) return "华为 HiCar";
        if (pkg.equals("com.ss.android.ugc.aweme")) return "抖音定制版";
        if (pkg.equals("com.didjdk.adbhelper")) return "甲壳虫 ADB（手机用）";
        return fallback;
    }
    public static String installDescription(String pkg) {
        if (pkg.equals("com.car.adbswitch")) return "车机 ADB 开关，便于连接调试和管理软件。";
        if (pkg.equals("com.shihab.diplay.wmex5")) return "CarPlay 连接与投屏，适用于此威马车机版本。";
        if (pkg.equals("com.wmhome.launcher")) return "威马车机桌面；安装后可自行选择默认桌面。";
        if (pkg.equals("com.estrongs.android.pop")) return "浏览 U 盘和车机文件，支持复制、移动与文件管理。";
        if (pkg.equals("info.kfsoft.force.rotation")) return "控制应用横竖屏；首次使用可能需开启相关系统权限。";
        if (pkg.equals("com.autonavi.amapauto")) return "高德导航及仪表投屏版本；投屏效果取决于车机适配。";
        if (pkg.equals("com.bilibili.bilithings")) return "哔哩哔哩视频车机版，需要联网；请停车后观看。";
        if (pkg.equals("com.netease.cloudmusic.iot")) return "网易云音乐车机界面，提供在线音乐播放。";
        if (pkg.equals("com.netease.cloudmusic")) return "网易云音乐旧版手机界面，可与车机版按需选择。";
        if (pkg.equals("com.fgsqw.lanshare")) return "同一局域网内传送文件，方便手机与车机互传。";
        if (pkg.equals("net.easyconn")) return "手机与车机互联、投屏，需配合对应手机端使用。";
        if (pkg.equals("com.syu.hicar")) return "华为手机互联；连接效果取决于手机与车机支持。";
        if (pkg.equals("com.ss.android.ugc.aweme")) return "抖音定制版本，需要联网；请停车后观看。";
        if (pkg.equals("com.didjdk.adbhelper")) return "通常装在手机上，用于通过 ADB 管理车机，无需默认装入车机。";
        if (pkg.equals("org.videolan.vlc")) return "本地音视频播放器。";
        if (pkg.contains("gonemad")) return "本地音乐播放器。";
        if (pkg.contains("kuwo")) return "车载音乐播放器。";
        if (pkg.contains("carwebguru")) return "车载桌面；安装后可自行选择默认桌面。";
        return "按软件声明的权限安装；不自动设为默认应用。";
    }
    public static File apk(File root, String name) throws IOException {
        if (name.length() == 0 || name.contains("/") || name.contains("\\") ||
                name.contains("\n") || name.contains("\r") || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".apk"))
            throw new SecurityException("安装文件名无效");
        File dir = new File(root, "apk").getCanonicalFile();
        if (!dir.getParentFile().equals(root.getCanonicalFile()))
            throw new SecurityException("apk 文件夹不能指向 U 盘以外的位置");
        File file = new File(dir, name);
        if (!file.getCanonicalFile().getParentFile().equals(dir) || !file.isFile())
            throw new SecurityException("只允许安装 U 盘根目录 apk 文件夹中的文件");
        return file.getCanonicalFile();
    }
    public static String quote(String s) { return "'" + s.replace("'", "'\\''") + "'"; }
}
