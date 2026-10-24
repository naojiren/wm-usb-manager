package com.wm.usbmanager;

import java.io.IOException;
import java.lang.reflect.Method;

/** Android 6 OEM pm install can leave an uninstalled system package absent for user 0. */
public final class FactoryRestore {
    public static void installExistingForOwner(String pkg) throws Exception {
        Policy.requireRemovable(pkg);
        Object service = Class.forName("android.app.AppGlobals").getMethod("getPackageManager").invoke(null);
        Method method = Class.forName("android.content.pm.IPackageManager")
            .getMethod("installExistingPackageAsUser", String.class, int.class);
        int result = ((Number)method.invoke(service, pkg, 0)).intValue();
        if (result != 1) throw new IOException("恢复当前用户安装状态失败，系统返回 " + result);
    }
}
