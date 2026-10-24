package com.wm.usbmanager;

import java.io.*;
import java.util.*;

/** Nonrecursive cleanup of recognized debug artifacts and three exact staging leftovers. */
public final class CleanupPolicy {
    public static final long MIN_AGE = 0;
    public static boolean recognized(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("backup") || n.contains("original") || n.contains("restore") || n.contains("rollback")) return false;
        return n.matches("(?:diplay-|weplay-perf|wm-home-wm|wm-xxqg-wm)[a-z0-9_.-]+\\.(?:apk|log|txt|xml|png)")
            || n.matches("wm[0-9]+-[a-z0-9_.-]+\\.(?:log|txt|xml|png)")
            || n.matches("wm-rotation-(?:diagnostics|lock)-[a-z0-9_.-]+\\.apk")
            || n.matches("perf[0-9]+\\.apk");
    }
    public static final class Item {
        public final String name;
        public final long size, modified;
        Item(File root, File file) { name = relative(root, file); size = file.length(); modified = file.lastModified(); }
    }
    private static String relative(File root, File file) {
        String prefix = root.getAbsolutePath() + File.separator, path = file.getAbsolutePath();
        return path.startsWith(prefix) ? path.substring(prefix.length()).replace(File.separatorChar, '/') : "";
    }
    public static boolean backup(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("backup") || n.contains("original") || n.contains("restore") || n.contains("rollback");
    }
    private static boolean allowed(String relative) {
        return (!relative.contains("/") && recognized(relative))
            || relative.equals("wm-usb-manager/stage/selected.apk")
            || relative.equals("wm-usb-manager/stage/restore.apk")
            || relative.equals("wm-usb-manager/bootstrap.apk");
    }
    public static boolean eligible(File root, File file, long now) throws IOException {
        return root.getAbsoluteFile().equals(root.getCanonicalFile()) && root.isDirectory()
            && file.getAbsoluteFile().equals(file.getCanonicalFile())
            && file.isFile() && allowed(relative(root, file));
    }
    public static List<Item> scan(File root, long now) throws IOException {
        List<Item> result = new ArrayList<>();
        if (!root.getAbsoluteFile().equals(root.getCanonicalFile())) throw new IOException("临时目录不能是符号链接");
        File[] files = root.listFiles();
        if (files == null) throw new IOException("无法读取临时目录");
        for (File file : files) if (eligible(root, file, now)) result.add(new Item(root, file));
        for (String name : new String[]{"wm-usb-manager/stage/selected.apk", "wm-usb-manager/stage/restore.apk", "wm-usb-manager/bootstrap.apk"}) {
            File file = new File(root, name); if (eligible(root, file, now)) result.add(new Item(root, file));
        }
        Collections.sort(result, (a, b) -> a.name.compareTo(b.name));
        return result;
    }
    public static boolean unchanged(File root, Item item, long now) throws IOException {
        File file = new File(root, item.name);
        return eligible(root, file, now) && file.length() == item.size && file.lastModified() == item.modified;
    }
}
