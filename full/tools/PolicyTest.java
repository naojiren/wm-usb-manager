package com.wm.usbmanager;
import java.io.*;
import java.nio.file.*;
public final class PolicyTest {
    private static int checked;
    private static void assertThat(boolean v) { checked++; if (!v) throw new AssertionError(); }
    private static void rejected(Runnable r) {
        checked++; try { r.run(); throw new AssertionError("Expected rejection"); } catch (SecurityException expected) {}
    }
    public static void main(String[] args) throws Exception {
        Policy.requireRemovable("com.ivi.map");
        Policy.requireRemovable("com.sinovoice.hcicloudinput");
        for (String pkg : new String[]{"android", "com.wm.launcher", "com.wm.multidisplay", "com.yftech.avmservice", "com.yftech.carservice", "com.android.settings", "com.yftech.btservice", "com.yftech.tboxprovider", "x;reboot", Policy.PACKAGE}) rejected(() -> Policy.requireRemovable(pkg));
        assertThat(Policy.protectedInstall("com.yftech.avmservice", false));
        assertThat(Policy.protectedInstall("com.wm.multidisplay", false));
        assertThat(Policy.protectedInstall("org.videolan.vlc", true));
        assertThat(!Policy.protectedInstall("org.videolan.vlc", false));
        assertThat(!Policy.protectedInstall("com.carwebguru", false));
        String[] defaults = {"com.car.adbswitch", "com.shihab.diplay.wmex5", "com.wmhome.launcher", "com.estrongs.android.pop", "info.kfsoft.force.rotation"};
        for (int i = 0; i < defaults.length; i++) {
            assertThat(Policy.installRank(defaults[i]) == i);
            assertThat(Policy.defaultInstall(defaults[i]));
            assertThat(!Policy.installDescription(defaults[i]).startsWith("按软件声明"));
        }
        for (String pkg : new String[]{"com.didjdk.adbhelper", "com.autonavi.amapauto", "com.bilibili.bilithings", "com.netease.cloudmusic.iot", "com.netease.cloudmusic", "com.fgsqw.lanshare", "net.easyconn", "com.syu.hicar", "com.ss.android.ugc.aweme"}) {
            assertThat(!Policy.defaultInstall(pkg));
            assertThat(!Policy.installDescription(pkg).startsWith("按软件声明"));
        }
        assertThat(!Policy.defaultInstall("unknown.diplay.launcher.adbswitch"));
        assertThat(!Policy.defaultInstall("org.videolan.vlc"));
        assertThat(Policy.removableThirdParty("org.videolan.vlc", false, 10123, false, false));
        assertThat(!Policy.removableThirdParty("org.videolan.vlc", true, 10123, false, false));
        assertThat(!Policy.removableThirdParty("org.videolan.vlc", false, 1000, false, false));
        assertThat(!Policy.removableThirdParty("org.videolan.vlc", false, 10123, true, false));
        assertThat(!Policy.removableThirdParty("org.videolan.vlc", false, 10123, false, true));
        for (String pkg : new String[]{Policy.PACKAGE, "com.wm.launcher", "com.yftech.carservice", "com.android.settings"})
            assertThat(!Policy.removableThirdParty(pkg, false, 10123, false, false));
        assertThat(Policy.quote("a'b;$(id)").equals("'a'\\''b;$(id)'"));
        assertThat(!Policy.defaultInstall("com.tiger.wm.amapcompanion"));
        assertThat(!Policy.defaultInstall("com.autonavi.amapauto"));
        assertThat(Policy.installDescription("com.autonavi.amapauto", "9.1.0.600087").contains("功能以所选安装包为准"));
        assertThat(!Policy.installDescription("com.autonavi.amapauto", "7.1.0").contains("130M"));
        assertThat(Policy.installDescription("com.tiger.wm.amapcompanion").equals("可选，安装后与高德V9.1配合使用。"));
        assertThat(Policy.installLabel("com.autonavi.amapauto", "高德", "9.1.0.600087").contains("V9.1"));
        assertThat(Policy.packageNames("package:android\r\npackage:com.ivi.radio\r\n").contains("com.ivi.radio"));
        assertThat(Policy.packageNames("package:android\npackage:com.ivi.radio\npackage:com.ivi.radio").size() == 2);
        rejected(() -> Policy.packageNames("Error: package service unavailable"));
        rejected(() -> Policy.packageNames(""));
        Policy.requireRemovable("com.ivi.radio"); Policy.requireRemovable("com.ivi.radiowidget");
        assertThat(Policy.entry("com.yftech.radioservice") == null);
        assertThat(Policy.entry("com.ivi.radio")[3].equals("0"));
        assertThat(Policy.installLabel("com.autonavi.amapauto", "高德", "9.1.0.600087", "高德_V9.1_仪表指引后台投屏_单APP测试9.apk").contains("单APP测试9"));
        assertThat(Policy.installDescription("com.autonavi.amapauto", "9.1.0.600087", "高德_V9.1_仪表指引后台投屏_单APP测试9.apk").contains("无需另装仪表伴侣"));
        assertThat(!Policy.installDescription("com.autonavi.amapauto", "9.1.0.600087", "other.apk").contains("单APP测试9"));
        assertThat(!Policy.installLabel("other.package", "Other", "9.1.0.600087", "高德_V9.1_仪表指引后台投屏_单APP测试9.apk").contains("单APP测试9"));
        assertThat(!Policy.defaultInstall("com.tencent.qqmusiccar"));
        assertThat(Policy.installLabel("com.tencent.qqmusiccar", "unknown").equals("QQ音乐车机版"));
        File root = Files.createTempDirectory("wm-policy").toFile();
        File dir = new File(root, "apk"); assertThat(dir.mkdir());
        File apk = new File(dir, "测试 ' 引号.apk"); assertThat(apk.createNewFile());
        assertThat(Policy.apk(root, apk.getName()).equals(apk.getCanonicalFile()));
        for (String name : new String[]{"../bad.apk", "sub/bad.apk", "sub\\bad.apk", "bad.apk\n", "bad.zip"}) {
            rejected(() -> { try { Policy.apk(root, name); } catch (IOException e) { throw new RuntimeException(e); } });
        }
        File outside = new File(root, "outside.apk"); assertThat(outside.createNewFile());
        Path link = new File(dir, "link.apk").toPath(); Files.createSymbolicLink(link, outside.toPath());
        rejected(() -> { try { Policy.apk(root, "link.apk"); } catch (IOException e) { throw new RuntimeException(e); } });
        File second = Files.createTempDirectory("wm-policy-dir").toFile();
        Files.createSymbolicLink(new File(second, "apk").toPath(), dir.toPath());
        rejected(() -> { try { Policy.apk(second, apk.getName()); } catch (IOException e) { throw new RuntimeException(e); } });
        System.out.println("Policy assertions passed: " + checked);
    }
}
