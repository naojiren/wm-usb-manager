package com.wm.usbmanager;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class CleanupPolicyTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("cleanup check " + checks); }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("wm-cleanup-test").toFile();
        long now = System.currentTimeMillis();
        for (String name : new String[]{"diplay-perf4.apk", "weplay-perf28.apk", "wm-home-wm4.apk", "perf27.apk", "diplay-perf4-install.log"}) {
            File f = new File(root, name); Files.write(f.toPath(), new byte[]{1}); check(f.setLastModified(now - CleanupPolicy.MIN_AGE - 10000));
        }
        for (String name : new String[]{"wm-launcher-system-backup.apk", "wm-original-backup-part.bin", "diplay-backup.apk", "weplay-perf28-rollback.apk", "adbhelper_server.apk", "scrcpy_server.apk", "unknown.apk", "wm-original.tar", "diplay-test.sh"}) {
            check(!CleanupPolicy.recognized(name));
            File f = new File(root, name); Files.write(f.toPath(), new byte[]{1}); f.setLastModified(now - 2 * CleanupPolicy.MIN_AGE);
        }
        File recent = new File(root, "weplay-perf99.apk"); Files.write(recent.toPath(), new byte[]{1});
        check(CleanupPolicy.eligible(root, recent, now));
        check(recent.setLastModified(now + 86400000)); check(CleanupPolicy.eligible(root, recent, now));
        File nested = new File(root, "wm-usb-manager"); check(nested.mkdir());
        File staged = new File(nested, "diplay-perf1.apk"); Files.write(staged.toPath(), new byte[]{1}); staged.setLastModified(now - 2 * CleanupPolicy.MIN_AGE);
        check(!CleanupPolicy.eligible(root, staged, now));
        File outside = Files.createTempFile("wm-outside", ".apk").toFile(); outside.setLastModified(now - 2 * CleanupPolicy.MIN_AGE);
        File link = new File(root, "diplay-link.apk"); Files.createSymbolicLink(link.toPath(), outside.toPath());
        check(!CleanupPolicy.eligible(root, link, now));
        check(CleanupPolicy.scan(root, now).size() == 6);
        File stage = new File(nested, "stage"); check(stage.mkdir());
        File selected = new File(stage, "selected.apk"); Files.write(selected.toPath(), new byte[]{8});
        check(CleanupPolicy.eligible(root, selected, now)); check(CleanupPolicy.scan(root, now).size() == 7);
        File rollback = new File(nested, "launcher-backup.apk"); Files.write(rollback.toPath(), new byte[]{8});
        check(!CleanupPolicy.eligible(root, rollback, now));
        check(CleanupPolicy.recognized("wm10-home-final.png")); check(CleanupPolicy.recognized("wm-rotation-lock-0.1.0.apk"));
        check(CleanupPolicy.backup("wm-original-backup-part.bin"));
        CleanupPolicy.Item item = CleanupPolicy.scan(root, now).get(0);
        check(CleanupPolicy.unchanged(root, item, now));
        Files.write(new File(root, item.name).toPath(), new byte[]{2, 3});
        check(!CleanupPolicy.unchanged(root, item, now));
        File alias = new File(root.getParentFile(), root.getName() + "-link"); Files.createSymbolicLink(alias.toPath(), root.toPath());
        boolean rejected = false; try { CleanupPolicy.scan(alias, now); } catch (IOException expected) { rejected = true; } check(rejected);
        check(outside.exists() && staged.exists() && new File(root, "wm-launcher-system-backup.apk").exists());
        System.out.println("Cleanup assertions passed: " + checks);
    }
}
