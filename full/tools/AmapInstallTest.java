package com.wm.usbmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AmapInstallTest {
    private static int count;
    private static void yes(boolean value) { count++; if (!value) throw new AssertionError("Amap check " + count); }
    public static void main(String[] args) throws Exception {
        List<String> commands = new ArrayList<>();
        AmapInstall.Command success = command -> { commands.add(command); return "Success\n"; };
        yes(AmapInstall.beforeInstall(AmapInstall.PACKAGE, true, success).contains("已清除"));
        yes(commands.size() == 1 && commands.get(0).equals("pm clear --user 0 'com.autonavi.amapauto'"));
        commands.clear();
        for (String pkg : new String[]{"com.wmhome.launcher", "com.shihab.diplay.wmex5", "com.autonavi.amapauto.fake", "android", null})
            yes(AmapInstall.beforeInstall(pkg, true, success).isEmpty());
        yes(commands.isEmpty());
        yes(AmapInstall.beforeInstall(AmapInstall.PACKAGE, false, success).contains("无需"));
        yes(commands.isEmpty());
        for (String output : new String[]{"Failed", "Error: permission denied", "Success\nFailure", ""}) {
            try { AmapInstall.beforeInstall(AmapInstall.PACKAGE, true, command -> output); throw new AssertionError(); }
            catch (IOException expected) { yes(expected.getMessage().contains("已停止安装")); }
        }
        try { AmapInstall.beforeInstall(AmapInstall.PACKAGE, true, command -> { throw new IOException("timeout"); }); throw new AssertionError(); }
        catch (IOException expected) { yes(expected.getMessage().equals("timeout")); }
        yes(AmapInstall.failure(AmapInstall.PACKAGE, "已清除高德应用数据", "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]").contains("已停止重复卸载"));
        yes(!AmapInstall.failure("other", "", "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]").contains("已清除"));
        commands.clear(); final int[] retainedCalls = {0};
        AmapInstall.RetainedData retained = () -> { retainedCalls[0]++; return true; };
        yes(AmapInstall.beforeInstall(AmapInstall.PACKAGE, true, false, success, retained).contains("残留"));
        yes(commands.isEmpty() && retainedCalls[0] == 1);
        yes(AmapInstall.beforeInstall(AmapInstall.PACKAGE, false, false, success, retained).contains("无需"));
        yes(retainedCalls[0] == 1);
        yes(AmapInstall.beforeInstall("other", true, false, success, retained).isEmpty());
        yes(retainedCalls[0] == 1 && commands.isEmpty());
        try { AmapInstall.beforeInstall(AmapInstall.PACKAGE, true, false, success, () -> false); throw new AssertionError(); }
        catch (IOException expected) { yes(expected.getMessage().contains("已停止安装")); }
        try { AmapInstall.beforeInstall(AmapInstall.PACKAGE, true, false, success, () -> { throw new IOException("callback timeout"); }); throw new AssertionError(); }
        catch (IOException expected) { yes(expected.getMessage().equals("callback timeout")); }
        yes(commands.isEmpty());
        commands.clear();
        yes(AmapInstall.ownerOnly("Users:\n UserInfo{0:Owner:13} running\n"));
        yes(!AmapInstall.ownerOnly("Users:\nUserInfo{0:Owner:13}\nUserInfo{10:Guest:0}"));
        yes(!AmapInstall.ownerOnly("permission denied")); yes(!AmapInstall.ownerOnly(null));
        yes(AmapInstall.signatureConflict(AmapInstall.PACKAGE, "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]"));
        yes(!AmapInstall.signatureConflict("other", "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]"));
        yes(!AmapInstall.signatureConflict(AmapInstall.PACKAGE, "Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]"));
        yes(AmapInstall.replaceConflict(AmapInstall.PACKAGE, false, "UserInfo{0:Owner:13}", success, () -> false).contains("完整卸载"));
        yes(commands.size() == 1 && commands.get(0).equals("pm uninstall 'com.autonavi.amapauto'"));
        commands.clear();
        try { AmapInstall.replaceConflict("other", false, "UserInfo{0:Owner:13}", success, () -> false); throw new AssertionError(); }
        catch (SecurityException expected) { yes(commands.isEmpty()); }
        try { AmapInstall.replaceConflict(AmapInstall.PACKAGE, true, "UserInfo{0:Owner:13}", success, () -> false); throw new AssertionError(); }
        catch (SecurityException expected) { yes(commands.isEmpty()); }
        try { AmapInstall.replaceConflict(AmapInstall.PACKAGE, false, "UserInfo{0:Owner:13} UserInfo{10:Guest:0}", success, () -> false); throw new AssertionError(); }
        catch (SecurityException expected) { yes(commands.isEmpty()); }
        try { AmapInstall.replaceConflict(AmapInstall.PACKAGE, false, "unavailable", success, () -> false); throw new AssertionError(); }
        catch (SecurityException expected) { yes(commands.isEmpty()); }
        try { AmapInstall.replaceConflict(AmapInstall.PACKAGE, false, "UserInfo{0:Owner:13}", command -> "Failure [DELETE_FAILED_INTERNAL_ERROR]", () -> false); throw new AssertionError(); }
        catch (java.io.IOException expected) { yes(expected.getMessage().contains("卸载失败")); }
        try { AmapInstall.replaceConflict(AmapInstall.PACKAGE, false, "UserInfo{0:Owner:13}", success, () -> true); throw new AssertionError(); }
        catch (java.io.IOException expected) { yes(expected.getMessage().contains("仍有旧高德记录")); }
        System.out.println("Amap installation assertions passed: " + count);
    }
}
