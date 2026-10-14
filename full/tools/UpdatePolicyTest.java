package com.wm.usbmanager;
public final class UpdatePolicyTest {
    private static int count;
    private static void yes(boolean value) { count++; if (!value) throw new AssertionError("Update check " + count); }
    public static void main(String[] args) {
        yes(UpdatePolicy.repo("com.wmhome.launcher").equals("wm-launcher"));
        yes(UpdatePolicy.pkg("diplay-wm").equals("com.shihab.diplay.wmex5"));
        try { UpdatePolicy.repo("com.yftech.carservice"); throw new AssertionError(); } catch (SecurityException expected) { count++; }
        yes(UpdatePolicy.assetRank("wm-launcher", "WM-Launcher-0.1.0-wm19-android6.apk") > 0);
        for (String s : new String[]{"WM-Launcher-0.1.0-wm19-android9.apk", "学习强国.apk", "WM-Launcher-source.zip", "../WM-Launcher-android6.apk"}) yes(UpdatePolicy.assetRank("wm-launcher", s) < 0);
        yes(UpdatePolicy.assetRank("diplay-wm", "DiPlay-WM-EX5-perf19-audio-armv7.apk") > 0);
        for (String s : new String[]{"DiPlay-WM-EX5-perf19-arm64.apk", "DiPlay-generic-armv7.apk", "WM-Launcher-android6.apk"}) yes(UpdatePolicy.assetRank("diplay-wm", s) < 0);
        yes(UpdatePolicy.assetUrl("wm-launcher", "https://github.com/naojiren/wm-launcher/releases/download/v19/WM-Launcher-android6.apk"));
        yes(!UpdatePolicy.assetUrl("wm-launcher", "https://github.com/other/wm-launcher/releases/download/v19/a.apk"));
        yes(!UpdatePolicy.assetUrl("wm-launcher", "http://github.com/naojiren/wm-launcher/releases/download/a.apk"));
        yes(UpdatePolicy.transport("https://release-assets.githubusercontent.com/asset?signature=x"));
        for (String s : new String[]{"https://github.com.evil.test/x", "https://evilgithubusercontent.com/x", "https://api.github.com:444/x", "https://user@github.com/x", "file:///data/a.apk"}) yes(!UpdatePolicy.transport(s));
        yes(UpdatePolicy.sha("b57892aff87279a952c5e545e62ed07b8b53dc2e57a7d8ea1f9685dc0d02cc5a"));
        yes(!UpdatePolicy.sha("bad")); yes(UpdatePolicy.size(12746240)); yes(!UpdatePolicy.size(0)); yes(!UpdatePolicy.size(UpdatePolicy.MAX_APK + 1));
        System.out.println("Update policy assertions passed: " + count);
    }
}
