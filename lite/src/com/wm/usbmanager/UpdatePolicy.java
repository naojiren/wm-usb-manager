package com.wm.usbmanager;

import java.net.URI;
import java.util.Locale;

/** Only the two user-selected public repositories can supply managed updates. */
public final class UpdatePolicy {
    public static final long MAX_APK = 150L * 1024 * 1024;
    public static String repo(String pkg) {
        if ("com.wmhome.launcher".equals(pkg)) return "wm-launcher";
        if ("com.shihab.diplay.wmex5".equals(pkg)) return "diplay-wm";
        throw new SecurityException("此软件不支持在线更新");
    }
    public static String pkg(String repo) {
        if ("wm-launcher".equals(repo)) return "com.wmhome.launcher";
        if ("diplay-wm".equals(repo)) return "com.shihab.diplay.wmex5";
        throw new SecurityException("更新来源不受支持");
    }
    public static boolean filename(String value) {
        return value != null && value.length() <= 160 && value.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.apk") && !value.contains("..");
    }
    public static int assetRank(String repo, String name) {
        pkg(repo);
        if (!filename(name)) return -1;
        String n = name.toLowerCase(Locale.ROOT);
        if (repo.equals("wm-launcher"))
            return n.startsWith("wm-launcher-") && n.endsWith("-android6.apk") ? 10 : -1;
        if (!n.startsWith("diplay-wm-ex5-") && !n.startsWith("weplay-")) return -1;
        if (n.endsWith("-armv7.apk") || n.endsWith("-armeabi-v7a.apk")) return 10;
        return -1;
    }
    public static boolean sha(String value) { return value != null && value.matches("[a-f0-9]{64}"); }
    public static boolean size(long value) { return value > 0 && value <= MAX_APK; }
    public static boolean transport(String value) {
        try {
            URI uri = new URI(value); String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null && uri.getFragment() == null
                && (uri.getPort() == -1 || uri.getPort() == 443) && host != null
                && (host.equals("api.github.com") || host.equals("github.com") || host.endsWith(".githubusercontent.com"));
        } catch (Exception e) { return false; }
    }
    public static boolean assetUrl(String repo, String value) {
        pkg(repo);
        try {
            URI uri = new URI(value);
            return transport(value) && "github.com".equals(uri.getHost())
                && uri.getPath().startsWith("/naojiren/" + repo + "/releases/download/");
        } catch (Exception e) { return false; }
    }
}
