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
        yes(UpdatePolicy.assetRank("diplay-wm", "WePlay-perf32-scan-test-armv7.apk") > 0);
        yes(UpdatePolicy.assetRank("diplay-wm", "WePlay-perf32-arm64.apk") < 0);
        yes(UpdatePolicy.comparePerf("WePlay-perf10-test-armv7.apk", "WePlay-perf9-armv7.apk") > 0);
        yes(UpdatePolicy.comparePerf("WePlay-perf100-armv7.apk", "DiPlay-WM-EX5-perf99-armv7.apk") > 0);
        yes(UpdatePolicy.comparePerf("WePlay-perf32-armv7.apk", "WePlay-perf22-newdate-armv7.apk") > 0);
        yes(UpdatePolicy.comparePerf("WePlay-perf032-a-armv7.apk", "WePlay-perf32-b-armv7.apk") == 0);
        yes(UpdatePolicy.comparePerf("WePlay-perf32-armv7.apk", "WePlay-perf33-armv7.apk") < 0);
        yes(UpdatePolicy.comparePerf("WePlay-perf99999999999999999999999-armv7.apk", "WePlay-perf9999999999999999999999-armv7.apk") > 0);
        for (String value : new String[]{"WePlay-performance32-armv7.apk", "WePlay-perf32x-armv7.apk", "WePlay-perf-32-armv7.apk", "WePlay-perf32-perf33-armv7.apk", "WePlay-armv7.apk"}) yes(UpdatePolicy.perf(value) == null);
        yes(UpdatePolicy.perf("WEPLAY-PERF32-SCAN-TEST-ARMV7.APK").intValue() == 32);
        yes(UpdatePolicy.perf(null) == null);
        yes(UpdatePolicy.comparePerf("WePlay-perf32-armv7.apk", "custom.apk") == null);
        String best = null;
        for (String value : new String[]{"WePlay-perf9-armv7.apk", "WePlay-perf32-scan-test-armv7.apk", "DiPlay-WM-EX5-perf22-armv7.apk", "WePlay-perf33-arm64.apk", "WePlay-perf31-newestdate-armv7.apk"})
            if (UpdatePolicy.betterAsset("diplay-wm", value, best)) best = value;
        yes(best.equals("WePlay-perf32-scan-test-armv7.apk"));
        yes(!UpdatePolicy.betterAsset("diplay-wm", "WePlay-armv7.apk", null));
        yes(UpdatePolicy.canUpdate("diplay-wm", "WePlay-perf33-armv7.apk", "WePlay-perf32-armv7.apk", false));
        yes(!UpdatePolicy.canUpdate("diplay-wm", "WePlay-perf22-armv7.apk", "WePlay-perf32-armv7.apk", false));
        yes(!UpdatePolicy.canUpdate("diplay-wm", "WePlay-perf32-new-armv7.apk", "WePlay-perf32-old-armv7.apk", false));
        yes(!UpdatePolicy.canUpdate("diplay-wm", "WePlay-perf33-armv7.apk", "WePlay-perf32-armv7.apk", true));
        yes(UpdatePolicy.canUpdate("diplay-wm", "WePlay-perf33-armv7.apk", null, false));
        yes(!UpdatePolicy.canUpdate("diplay-wm", "WePlay-armv7.apk", null, false));
        yes(UpdatePolicy.preferLocal("diplay-wm", "WePlay-perf32-armv7.apk", 1, "WePlay-perf22-armv7.apk", 999));
        yes(!UpdatePolicy.preferLocal("diplay-wm", "WePlay-perf22-armv7.apk", 999, "WePlay-perf32-armv7.apk", 1));
        yes(UpdatePolicy.preferLocal("diplay-wm", "WePlay-perf32-armv7.apk", 1, "custom.apk", 999));
        yes(!UpdatePolicy.preferLocal("diplay-wm", "custom.apk", 999, "WePlay-perf32-armv7.apk", 1));
        yes(UpdatePolicy.preferLocal("wm-launcher", "WM-Launcher-wm33-android6.apk", 1033, "WM-Launcher-wm32-android6.apk", 1032));
        yes(!UpdatePolicy.canUpdate("wm-launcher", "a.apk", "b.apk", true));
        yes(UpdatePolicy.comparison("diplay-wm", "WePlay-perf22-armv7.apk", "WePlay-perf32-armv7.apk", false).contains("高于"));
        yes(UpdatePolicy.comparison("diplay-wm", "WePlay-perf32-new-armv7.apk", "WePlay-perf32-old-armv7.apk", false).contains("无需更新"));
        yes(UpdatePolicy.comparison("diplay-wm", "WePlay-perf33-armv7.apk", "custom.apk", false).contains("无法比较"));
        System.out.println("Update policy assertions passed: " + count);
    }
}
