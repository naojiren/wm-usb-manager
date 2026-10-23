package com.wm.usbmanager;

import android.content.pm.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;

/** Writes only the explicitly trusted WM launcher; never touches the factory launcher. */
final class SystemInstaller {
    static void install(File source, PackageManager pm) throws Exception {
        PackageInfo p = pm.getPackageArchiveInfo(source.getPath(), PackageManager.GET_SIGNATURES);
        if (p == null || !"com.wmhome.launcher".equals(p.packageName) || p.signatures == null || p.signatures.length != 1)
            throw new SecurityException("系统分区仅允许威马桌面");
        StringBuilder hash = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(p.signatures[0].toByteArray())) hash.append(String.format(Locale.ROOT, "%02x", b & 255));
        if (!hash.toString().equals("e2db4c74775d527181c7ca92959fbffaffd273ee2dbff7a143fc8c28687fa1ba")) throw new SecurityException("桌面签名不匹配");
        String location = "/system/app/WMUserLauncher";
        PackageInfo old = pm.getPackageInfo(p.packageName, 0);
        if (old.applicationInfo.sourceDir.startsWith("/system/")) {
            File existing = new File(old.applicationInfo.sourceDir);
            if (!existing.getParent().startsWith("/system/app/") && !existing.getParent().startsWith("/system/priv-app/")) throw new IOException("不支持此系统桌面布局");
            location = existing.getParent();
        } else {
            // An updated system app points to /data; locate its original system APK by package identity.
            for (String parent : new String[]{"/system/app", "/system/priv-app"}) {
                File[] dirs = new File(parent).listFiles(); if (dirs == null) continue;
                for (File dir : dirs) { File[] files = dir.listFiles(); if (files == null) continue;
                    for (File candidate : files) if (candidate.getName().endsWith(".apk")) {
                        PackageInfo pi = pm.getPackageArchiveInfo(candidate.getPath(), 0);
                        if (pi != null && p.packageName.equals(pi.packageName)) { location = dir.getPath(); }
                    }
                }
            }
        }
        File dir = new File(location), target = new File(dir, "WMUserLauncher.apk");
        if (!dir.getCanonicalPath().equals(dir.getAbsolutePath())) throw new SecurityException("系统目标目录不能为链接");
        int apkCount = 0; File[] prior = dir.listFiles();
        if (prior != null) for (File file : prior) if (file.getName().endsWith(".apk")) {
            PackageInfo pi = pm.getPackageArchiveInfo(file.getPath(), PackageManager.GET_SIGNATURES);
            if (pi == null || !p.packageName.equals(pi.packageName) || !java.util.Arrays.equals(pi.signatures, p.signatures)) throw new SecurityException("目标目录包含其它软件");
            if (++apkCount > 1) throw new IOException("系统目录存在多个 APK，需人工处理");
            target = file;
        }
        File temp = new File(dir, ".wm-new"), backup = new File("/data/local/tmp/wm-launcher-system-backup.apk");
        if (!target.getCanonicalPath().equals(target.getAbsolutePath())) throw new SecurityException("系统 APK 不能为链接");
        boolean existed = target.isFile(), committed = false;
        String mounts = RootBridge.run("cat /proc/mounts", 10); boolean wasReadOnly = false, found = false;
        for (String line : mounts.split("\n")) { String[] parts = line.split(" "); if (parts.length > 3 && parts[1].equals("/system")) { found = true; wasReadOnly = java.util.Arrays.asList(parts[3].split(",")).contains("ro"); } }
        if (!found) throw new IOException("无法确认系统分区挂载状态");
        try {
            RootBridge.run("mount -o remount,rw /system", 15);
            if (existed) copy(target, backup);
            if (!dir.isDirectory() && !dir.mkdir()) throw new IOException("系统分区不可写");
            if (dir.getUsableSpace() < source.length() + 4*1024*1024) throw new IOException("系统分区空间不足");
            copy(source, temp);
            if (!RootBridge.sha(source).equals(RootBridge.sha(temp))) throw new IOException("系统副本校验失败");
            RootBridge.run("chown 0:0 " + Policy.quote(dir.getPath()) + " " + Policy.quote(temp.getPath()) + " && chmod 755 " + Policy.quote(dir.getPath()) + " && chmod 644 " + Policy.quote(temp.getPath()), 15);
            android.system.Os.rename(temp.getPath(), target.getPath()); committed = true;
            RootBridge.run("restorecon " + Policy.quote(dir.getPath()) + " " + Policy.quote(target.getPath()) + "; sync", 15);
            android.system.StructStat stat = android.system.Os.stat(target.getPath());
            if (stat.st_uid != 0 || stat.st_gid != 0 || (stat.st_mode & 0777) != 0644) throw new IOException("系统 APK 权限校验失败");
            if (RootBridge.run("getenforce", 10).equals("Enforcing") && !RootBridge.run("ls -Z " + Policy.quote(target.getPath()), 10).contains(":system_file:")) throw new IOException("系统 APK 安全标签校验失败");
            if (!RootBridge.sha(source).equals(RootBridge.sha(target))) throw new IOException("系统桌面校验失败");
        } catch (Exception e) {
            if (committed) { if (existed) copy(backup, target); else target.delete(); }
            throw e;
        } finally {
            temp.delete();
            if (wasReadOnly) {
                RootBridge.run("mount -o remount,ro /system", 15);
                String after = RootBridge.run("cat /proc/mounts", 10);
                boolean restored = false;
                for (String line : after.split("\n")) { String[] a = line.split(" "); if (a.length > 3 && a[1].equals("/system")) restored = java.util.Arrays.asList(a[3].split(",")).contains("ro"); }
                if (!restored) throw new IOException("系统分区只读恢复失败，请重启车机");
            }
        }
    }
    private static void copy(File from, File to) throws Exception {
        try (InputStream in = new FileInputStream(from); FileOutputStream out = new FileOutputStream(to)) {
            byte[] b = new byte[65536]; for (int n; (n=in.read(b))>0;) out.write(b,0,n); out.getFD().sync();
        }
    }
}
