package com.wm.usbmanager;
import java.io.*;
import java.util.*;

/** Lightweight listing snapshots; complete content/signature checks remain mandatory at install. */
public final class ScanPolicy {
    public interface Loader<T> { T load(File file) throws Exception; }
    private static final class Stamp {
        final String path; final long size, modified;
        Stamp(File f) throws IOException { path = f.getCanonicalPath(); size = f.length(); modified = f.lastModified(); }
        boolean matches(File f) throws IOException { return f.isFile() && path.equals(f.getCanonicalPath()) && size == f.length() && modified == f.lastModified(); }
    }
    public static final class Selection {
        public final String token = UUID.randomUUID().toString();
        private final Stamp stamp;
        private final String pkg;
        private final int version;
        public Selection(File file, String pkg, int version) throws IOException { stamp = new Stamp(file); this.pkg = pkg; this.version = version; }
        public void require(File file, String supplied) throws IOException {
            if (!token.equals(supplied) || !stamp.matches(file)) throw new SecurityException("安装包已变化，请刷新后重新选择");
        }
        public void requireIdentity(String actualPackage, int actualVersion) {
            if (!pkg.equals(actualPackage) || version != actualVersion) throw new SecurityException("安装包身份已变化，请刷新后重新选择");
        }
    }
    public static final class Cache<T> {
        private static final class Entry<T> { final Stamp stamp; final T value; Entry(File f, T v) throws IOException { stamp = new Stamp(f); value = v; } }
        private final Map<String, Entry<T>> values = new HashMap<>();
        public T peek(File file) throws IOException {
            Entry<T> entry = values.get(file.getCanonicalPath());
            return entry != null && entry.stamp.matches(file) ? entry.value : null;
        }
        public T get(File file, Loader<T> loader) throws Exception {
            T value = peek(file); if (value != null) return value;
            Stamp before = new Stamp(file); value = loader.load(file);
            if (!before.matches(file)) throw new IOException("扫描期间安装包发生变化，请刷新重试");
            put(file, value); return value;
        }
        public void put(File file, T value) throws IOException { values.put(file.getCanonicalPath(), new Entry<T>(file, value)); }
    }
}
