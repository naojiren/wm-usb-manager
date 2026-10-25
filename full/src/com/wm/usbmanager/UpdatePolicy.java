package com.wm.usbmanager;

import java.net.URI;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Pattern PERF = Pattern.compile("(?:^|[-_.])perf([0-9]+)(?=[-_.]|$)", Pattern.CASE_INSENSITIVE);
    public static BigInteger perf(String name) {
        if (name == null) return null;
        Matcher m = PERF.matcher(name);
        if (!m.find()) return null;
        BigInteger value = new BigInteger(m.group(1));
        return m.find() ? null : value;
    }
    /** Null means that a file has no unambiguous perf number. */
    public static Integer comparePerf(String candidate, String current) {
        BigInteger a = perf(candidate), b = perf(current);
        return a == null || b == null ? null : a.compareTo(b);
    }
    public static boolean betterAsset(String repo, String candidate, String current) {
        int rank = assetRank(repo, candidate);
        if (rank < 0 || (repo.equals("diplay-wm") && perf(candidate) == null)) return false;
        if (current == null) return true;
        if (repo.equals("diplay-wm")) {
            Integer order = comparePerf(candidate, current);
            if (order != null && order != 0) return order > 0;
        }
        return rank > assetRank(repo, current);
    }
    public static boolean preferLocal(String repo, String candidate, int code, String current, int currentCode) {
        if (current == null) return true;
        if (repo.equals("diplay-wm")) {
            BigInteger a = perf(candidate), b = perf(current);
            if (a != null && b == null) return true;
            if (a == null && b != null) return false;
            if (a != null) return a.compareTo(b) > 0;
        }
        return code > currentCode;
    }
    public static boolean canUpdate(String repo, String remote, String local, boolean sameDigest) {
        if (sameDigest) return false;
        if (repo.equals("diplay-wm")) {
            if (perf(remote) == null) return false;
            Integer order = comparePerf(remote, local);
            return order == null || order > 0;
        }
        return true;
    }
    public static String comparison(String repo, String remote, String local, boolean sameDigest) {
        if (repo.equals("diplay-wm")) {
            Integer order = comparePerf(remote, local);
            if (order != null) {
                if (order < 0) return "本地 perf" + perf(local) + " 高于线上 perf" + perf(remote) + "，无需更新";
                if (order == 0) return "本地与线上均为 perf" + perf(remote) + "，无需更新";
                return "发现新版 perf" + perf(remote) + "（本地 perf" + perf(local) + "）";
            }
            if (local != null && !local.isEmpty()) return "本地文件名未标注 perf 数字，无法比较；线上 perf" + perf(remote);
            return "可下载 perf" + perf(remote);
        }
        return sameDigest ? "本地安装包已与发布包一致，无需更新" : "可下载最新正式发布的安装包";
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
