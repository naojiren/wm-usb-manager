package com.wm.usbmanager;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Android 6 pm clear uses ActivityManager, which rejects data-only uninstalled packages. */
public final class RetainedAmapData {
    private static final String OBSERVER = "android.content.pm.IPackageDataObserver";
    public static String support() throws Exception {
        Class<?> observer = Class.forName(OBSERVER);
        Method clear = Class.forName("android.content.pm.IPackageManager")
            .getMethod("clearApplicationUserData", String.class, observer, int.class);
        Class.forName(OBSERVER + "$Stub").getMethod("asInterface", IBinder.class);
        return clear.toString();
    }
    /** Local Binder protocol check; never calls the package service or clears user data. */
    public static void checkCallbackProtocol() throws Exception {
        Class<?> type = Class.forName(OBSERVER), stub = Class.forName(OBSERVER + "$Stub");
        Method wrap = stub.getMethod("asInterface", IBinder.class);
        Method report = type.getMethod("onRemoveCompleted", String.class, boolean.class);
        Result success = new Result(); report.invoke(wrap.invoke(null, success), AmapInstall.PACKAGE, true);
        if (!success.done.await(5, TimeUnit.SECONDS) || !success.success) throw new IOException("成功回调检查失败");
        Result failure = new Result(); report.invoke(wrap.invoke(null, failure), AmapInstall.PACKAGE, false);
        if (!failure.done.await(5, TimeUnit.SECONDS) || failure.success) throw new IOException("失败回调检查失败");
        Result foreign = new Result(); report.invoke(wrap.invoke(null, foreign), "other.package", true);
        if (foreign.done.getCount() != 1 || foreign.success) throw new IOException("非高德回调检查失败");
    }
    public static boolean clearForOwner() throws Exception {
        if (android.os.Process.myUid() != 0) throw new SecurityException("清理高德残留数据需要原厂权限通道");
        Object service = Class.forName("android.app.AppGlobals").getMethod("getPackageManager").invoke(null);
        Result callback = new Result();
        Class<?> observerClass = Class.forName(OBSERVER);
        Object observer = Class.forName(OBSERVER + "$Stub").getMethod("asInterface", IBinder.class).invoke(null, callback);
        Class.forName("android.content.pm.IPackageManager")
            .getMethod("clearApplicationUserData", String.class, observerClass, int.class)
            .invoke(service, AmapInstall.PACKAGE, observer, 0);
        if (!callback.done.await(90, TimeUnit.SECONDS)) throw new IOException("清除高德残留数据等待系统确认超时，已停止安装");
        return callback.success;
    }
    private static final class Result extends Binder {
        final CountDownLatch done = new CountDownLatch(1);
        volatile boolean success;
        Result() { attachInterface(null, OBSERVER); }
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == IBinder.INTERFACE_TRANSACTION) { if (reply != null) reply.writeString(OBSERVER); return true; }
            if (code == IBinder.FIRST_CALL_TRANSACTION) {
                data.enforceInterface(OBSERVER);
                String pkg = data.readString(); boolean cleared = data.readInt() != 0;
                if (AmapInstall.PACKAGE.equals(pkg)) { success = cleared; done.countDown(); }
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }
}
