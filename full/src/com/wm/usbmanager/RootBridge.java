package com.wm.usbmanager;

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.app.admin.DevicePolicyManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Root worker started ONLY by the factory USB entry or existing su. No TCP listener. */
public final class RootBridge {
    private static final int INCLUDE_REMOVED = 0x00002000;
    private final Context context;
    private final PackageManager pm;
    private final File root;
    private final File journal;
    private final boolean validation;
    private boolean resident;
    private final File cleanupRoot = new File("/data/local/tmp");
    private List<CleanupPolicy.Item> cleanupItems = new ArrayList<>();
    private String cleanupToken = "";
    private final Map<String, String> tokens = new HashMap<>();
    private final Map<String, ScanPolicy.Selection> selections = new HashMap<>();
    private final ScanPolicy.Cache<PackageInfo> archiveCache = new ScanPolicy.Cache<>();
    private final ScanPolicy.Cache<String> digestCache = new ScanPolicy.Cache<>();

    RootBridge(Context c, File usb, boolean test) {
        context = c; pm = c.getPackageManager(); root = usb; validation = test;
        journal = new File("/data/local/tmp/wm-usb-manager", test ? "validation.jsonl" : "operations.jsonl");
    }
    public static void main(String[] args) throws Exception {
        if (android.os.Process.myUid() != 0) throw new SecurityException("需要原厂启动入口的 root 权限");
        boolean test = args.length > 0 && args[0].equals("--validation");
        Looper.prepare();
        Class<?> at = Class.forName("android.app.ActivityThread");
        Object thread = at.getMethod("systemMain").invoke(null);
        Context c = (Context) at.getMethod("getSystemContext").invoke(thread);
        RootBridge worker = new RootBridge(c, new File(test ? "/data/local/tmp/wm-usb-validation" : "/mnt/udisk"), test);
        worker.resident = args.length > 0 && args[0].equals("--resident");
        if (worker.resident) {
            worker = new RootBridge(c, new File("/data/local/wm-software-wizard"), false);
            worker.resident = true; worker.root.mkdirs();
            run("chown 0:0 /data/local/wm-software-wizard && chmod 700 /data/local/wm-software-wizard", 10);
        }
        int allowedUid = c.getPackageManager().getApplicationInfo(Policy.PACKAGE, 0).uid;
        LocalServerSocket server = new LocalServerSocket(test ? Policy.SOCKET + ".validation" : worker.resident ? Policy.SOCKET + ".resident" : Policy.SOCKET);
        System.out.println("READY root uid=0 allowedUid=" + allowedUid + " validation=" + test);
        System.out.flush();
        while (true) {
            LocalSocket socket = server.accept();
            try {
                socket.setSoTimeout(180000);
                int uid = socket.getPeerCredentials().getUid();
                if (uid != allowedUid && uid != 0) throw new SecurityException("禁止其它应用使用提权通道");
                String line = readLine(socket.getInputStream());
                JSONObject request = new JSONObject(line);
                JSONObject reply;
                try { reply = worker.handle(request); }
                catch (Exception e) { reply = new JSONObject().put("ok", false).put("message", cleanError(e)); }
                OutputStream out = socket.getOutputStream();
                out.write((reply.toString() + "\n").getBytes("UTF-8")); out.flush();
            } catch (Exception e) { System.out.println("Request rejected: " + cleanError(e)); }
            finally { socket.close(); }
        }
    }
    static String cleanError(Exception e) {
        Throwable cause = e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException() : e;
        return cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage());
    }
    static String readLine(InputStream in) throws IOException {
        return readLine(in, 65536);
    }
    static String readLine(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for (int ch; (ch = in.read()) != -1 && ch != '\n';) {
            if (data.size() >= limit) throw new IOException("数据超过长度限制");
            data.write(ch);
        }
        return data.toString("UTF-8");
    }
    synchronized JSONObject handle(JSONObject q) throws Exception {
        String action = q.getString("action");
        if (action.equals("status")) return ok("原厂提权通道就绪").put("uid", android.os.Process.myUid()).put("usb", usbReady());
        if (action.equals("inventory")) return inventory();
        if (action.equals("storage")) return ok("空间已刷新").put("storage", storage());
        if (action.equals("install-guide") && !validation && !resident) {
            if (!usbReady() || !installed(Policy.PACKAGE)) throw new IOException("请从完整 U 盘启动管家后安装");
            File destination = new File("/data/local/wm-software-wizard"); destination.mkdirs();
            run("chown 0:0 /data/local/wm-software-wizard && chmod 700 /data/local/wm-software-wizard", 10);
            if (!destination.isDirectory() || !destination.getCanonicalFile().equals(destination)) throw new IOException("车机工作目录创建失败");
            android.system.StructStat ds = android.system.Os.stat(destination.getPath());
            if (ds.st_uid != 0 || (ds.st_mode & 0777) != 0700) throw new IOException("车机工作目录权限错误");
            return ok("管家已安装到车机。拔出 U 盘后从应用列表打开，仅保留卸载和两款软件联网更新安装；不复制内置软件。重启后需要已有 su 授权。");
        }
        if (resident && action.equals("restore")) throw new SecurityException("车机版没有原厂恢复包");
        if (action.equals("probe-keyboard") && validation) return probeKeyboard();
        if (!usbReady()) throw new IOException("U 盘已拔出或文件不完整，请重新插入");
        if (action.equals("cleanup")) return cleanup(q.getString("token"));
        if (action.equals("remove")) return remove(q.getString("package"));
        if (action.equals("restore")) return restore(q.getString("package"));
        if (action.equals("install")) return install(q.getString("name"), q.optString("token", q.optString("sha256")), q.optBoolean("grant", true), q.optBoolean("systemLauncher", true));
        if (action.equals("update-apk")) return updateApk(q);
        throw new SecurityException("操作未获允许");
    }
    private boolean usbReady() {
        return root.isDirectory() && (resident || validation || (new File(root, "factorymodel").isFile()
            && new File(root, "wm_manager/manager.apk").isFile()));
    }
    private JSONObject ok(String text) throws Exception { return new JSONObject().put("ok", true).put("message", text); }
    private PackageInfo packageInfo(String pkg, boolean include) {
        try { return pm.getPackageInfo(pkg, include ? INCLUDE_REMOVED : 0); }
        catch (Exception e) { return null; }
    }
    private boolean installed(String pkg) {
        // PackageManager flags alone can include a removed system package on OEM firmware.
        try { return Policy.packageNames(run("pm list packages --user 0", 20)).contains(pkg); }
        catch (Exception e) { return false; }
    }
    private boolean system(String pkg) {
        PackageInfo p = packageInfo(pkg, true);
        return p != null && (p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
    private JSONObject inventory() throws Exception {
        long started = android.os.SystemClock.elapsedRealtime();
        JSONArray apps = new JSONArray();
        Map<String, PackageInfo> installedPackages = new HashMap<>();
        for (PackageInfo item : pm.getInstalledPackages(0)) installedPackages.put(item.packageName, item);
        String listing = run("pm list packages --user 0", 20);
        Set<String> present = Policy.packageNames(listing);
        for (String[] e : Policy.OPTIONAL) {
            boolean active = present.contains(e[0]);
            boolean known = new File(root, "wm_manager/original_apks/" + e[0] + ".apk").isFile() || system(e[0]);
            PackageInfo info = installedPackages.get(e[0]);
            boolean enabled = info != null && info.applicationInfo != null && info.applicationInfo.enabled;
            apps.put(new JSONObject().put("package", e[0]).put("label", e[1]).put("description", e[2])
                .put("kind", "factory").put("removable", true).put("selected", e[3].equals("1") && active).put("installed", active).put("restorable", Policy.showRestore(e[0], known, active, enabled)));
        }
        Set<String> roles = protectedRoles();
        List<JSONObject> thirdParty = new ArrayList<>(), protectedApps = new ArrayList<>();
        for (String pkg : present) {
            if (Policy.entry(pkg) != null) continue;
            PackageInfo p = installedPackages.get(pkg);
            if (p == null) p = packageInfo(pkg, false);
            boolean removable = p != null && removableThirdParty(p, roles);
            String label = pkg;
            if (p != null && p.applicationInfo != null) {
                try { label = String.valueOf(p.applicationInfo.loadLabel(pm)); } catch (Exception ignored) { }
            }
            JSONObject row = new JSONObject().put("package", pkg).put("label", Policy.installLabel(pkg, label))
                .put("kind", removable ? "thirdParty" : "protected").put("selected", false).put("removable", removable)
                .put("installed", true).put("restorable", false)
                .put("description", removable ? Policy.installDescription(pkg) + " 删除后保留数据，找回需用安装包重装。"
                    : roles.contains(pkg) ? "受保护：当前桌面、输入法、设备管理或正在使用的辅助服务。"
                    : "受保护：系统、车辆服务或尚未核实用途的预装软件，不开放删除。");
            (removable ? thirdParty : protectedApps).add(row);
        }
        for (List<JSONObject> group : Arrays.asList(thirdParty, protectedApps)) {
            Collections.sort(group, (a, b) -> a.optString("label").compareTo(b.optString("label")));
            for (JSONObject row : group) apps.put(row);
        }
        JSONArray apks = new JSONArray(); tokens.clear(); selections.clear();
        List<JSONObject> rows = new ArrayList<>();
        File dir = new File(root, "apk");
        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
            for (File f : files) {
                if (!f.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) continue;
                JSONObject row = new JSONObject().put("name", f.getName()).put("size", f.length())
                    .put("priority", 100).put("selected", false)
                    .put("description", "按软件声明的权限安装；不自动设为默认应用。");
                try {
                    f = Policy.apk(root, f.getName());
                    PackageInfo p = archiveCache.get(f, candidate -> scanArchive(candidate));
                    String problem = compatible(f, p);
                    PackageInfo current = installedPackages.get(p.packageName);
                    boolean systemApp = current != null && current.applicationInfo != null && (current.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    boolean protectedApp = !p.packageName.equals("com.wmhome.launcher") && Policy.protectedInstall(p.packageName, systemApp);
                    if (protectedApp) problem = "系统或车辆程序受保护，禁止覆盖";
                    ScanPolicy.Selection selected = new ScanPolicy.Selection(f, p.packageName, p.versionCode);
                    tokens.put(f.getName(), selected.token); selections.put(f.getName(), selected);
                    String digest = digestCache.peek(f);
                    CharSequence label;
                    String known = Policy.installLabel(p.packageName, p.packageName);
                    try { label = known.equals(p.packageName) ? p.applicationInfo.loadLabel(pm) : known; } catch (Exception e) { label = p.packageName; }
                    row.put("package", p.packageName).put("label", Policy.installLabel(p.packageName, String.valueOf(label), p.versionName, f.getName())).put("version", p.versionName)
                        .put("code", p.versionCode).put("token", selected.token).put("sha256", digest == null ? "" : digest).put("eligible", problem.length() == 0)
                        .put("reason", problem).put("existing", current != null ? current.versionName : "")
                        .put("priority", Policy.installRank(p.packageName))
                        .put("description", Policy.installDescription(p.packageName, p.versionName, f.getName()));
                } catch (Exception e) { row.put("eligible", false).put("reason", "无法解析或读取：" + cleanError(e)); }
                rows.add(row);
            }
        }
        Collections.sort(rows, (a, b) -> {
            int rank = Integer.compare(a.optInt("priority", 100), b.optInt("priority", 100));
            if (rank != 0) return rank;
            int identity = a.optString("package").compareTo(b.optString("package"));
            if (identity != 0) return identity;
            if (a.optString("package").equals(b.optString("package"))) {
                int version = Integer.compare(b.optInt("code"), a.optInt("code"));
                if (version != 0) return version;
            }
            return a.optString("name").compareTo(b.optString("name"));
        });
        Set<String> defaults = new HashSet<>();
        for (JSONObject row : rows) {
            String pkg = row.optString("package");
            row.put("selected", row.optBoolean("eligible") && Policy.defaultInstall(pkg) && defaults.add(pkg));
            apks.put(row);
        }
        return ok("读取完成").put("apps", apps).put("apks", apks).put("usb", usbReady())
            .put("storage", storage()).put("cleanup", scanCleanup()).put("installedCount", present.size())
            .put("scanMillis", android.os.SystemClock.elapsedRealtime() - started).put("apkHashedBytesDuringScan", 0)
            .put("apkDirectory", dir.getAbsolutePath()).put("keyboard", run("settings get secure default_input_method", 15));
    }
    private PackageInfo scanArchive(File file) throws Exception {
        PackageInfo p = pm.getPackageArchiveInfo(file.getPath(), 0);
        if (p == null || p.applicationInfo == null) throw new IOException("APK 格式无效");
        p.applicationInfo.sourceDir = file.getPath(); p.applicationInfo.publicSourceDir = file.getPath();
        return p;
    }
    private ScanPolicy.Selection requireSelected(String name, File file, String token) throws Exception {
        if (!token.equals(tokens.get(name))) throw new SecurityException("选择凭据已失效，请刷新后重选");
        ScanPolicy.Selection selected = selections.get(name);
        if (selected != null) selected.require(file, token);
        else if (!UpdatePolicy.sha(token)) throw new SecurityException("安装选择凭据无效");
        return selected;
    }
    private JSONObject storage() throws Exception {
        JSONObject result = new JSONObject();
        try {
            android.os.StatFs stat = new android.os.StatFs("/data");
            StoragePolicy.Snapshot snapshot = StoragePolicy.read(stat.getTotalBytes(), stat.getAvailableBytes(),
                () -> run("settings get global sys_storage_threshold_percentage; settings get global sys_storage_threshold_max_bytes", 10));
            result.put("total", snapshot.total).put("available", snapshot.available).put("reserveKnown", snapshot.reserveKnown);
            if (snapshot.reserveKnown) result.put("reserve", snapshot.reserve).put("aboveReserve", snapshot.aboveReserve);
        } catch (Exception e) {
            result.put("error", "空间读取暂不可用：" + cleanError(e));
        }
        return result;
    }
    private JSONObject scanCleanup() throws Exception {
        cleanupToken = UUID.randomUUID().toString(); cleanupItems = new ArrayList<>();
        JSONObject result = new JSONObject().put("token", cleanupToken);
        try { cleanupItems = CleanupPolicy.scan(cleanupRoot, System.currentTimeMillis()); }
        catch (IOException e) { return result.put("status", "unavailable").put("error", e.getMessage()); }
        long bytes = 0; JSONArray names = new JSONArray();
        for (CleanupPolicy.Item item : cleanupItems) { bytes += item.size; names.put(item.name); }
        long backupBytes = 0, unknownBytes = 0;
        File[] top = cleanupRoot.listFiles();
        if (top != null) for (File file : top) {
            if (!file.isFile() || !file.getAbsoluteFile().equals(file.getCanonicalFile())) continue;
            if (CleanupPolicy.backup(file.getName())) backupBytes += file.length();
            else if (!CleanupPolicy.recognized(file.getName())) unknownBytes += file.length();
        }
        return result.put("status", "ready").put("count", cleanupItems.size()).put("bytes", bytes).put("files", names)
            .put("backupBytes", backupBytes).put("unknownBytes", unknownBytes);
    }
    private Set<String> inUseTemporaryFiles() throws IOException {
        Set<String> paths = new HashSet<>();
        File[] processes = new File("/proc").listFiles();
        if (processes == null) throw new IOException("无法核对正在使用的文件，已取消清理");
        for (File process : processes) {
            if (!process.getName().matches("[0-9]+")) continue;
            File[] descriptors = new File(process, "fd").listFiles();
            if (descriptors == null && process.exists()) throw new IOException("无法核对进程文件，已取消清理");
            if (descriptors != null) for (File descriptor : descriptors) {
                try { paths.add(descriptor.getCanonicalPath()); } catch (IOException ignored) { }
            }
            for (String name : new String[]{"maps", "cmdline"}) {
                try (BufferedReader in = new BufferedReader(new FileReader(new File(process, name)))) {
                    for (String line; (line = in.readLine()) != null;) {
                        for (CleanupPolicy.Item item : cleanupItems) {
                            String path = new File(cleanupRoot, item.name).getPath();
                            if (line.contains(path)) paths.add(path);
                        }
                    }
                } catch (IOException e) { if (process.exists()) throw new IOException("无法核对进程引用，已取消清理", e); }
            }
        }
        return paths;
    }
    private JSONObject cleanup(String token) throws Exception {
        if (validation) throw new SecurityException("验证模式禁止清理真实临时文件");
        if (cleanupToken.isEmpty() || !cleanupToken.equals(token)) throw new SecurityException("清理清单已变化，请刷新后重选");
        cleanupToken = "";
        Set<String> inUse = inUseTemporaryFiles();
        long bytes = 0; int deleted = 0, skipped = 0; StringBuilder detail = new StringBuilder();
        for (CleanupPolicy.Item item : cleanupItems) {
            File file = new File(cleanupRoot, item.name);
            if (!CleanupPolicy.unchanged(cleanupRoot, item, System.currentTimeMillis()) || inUse.contains(file.getPath())) { skipped++; continue; }
            android.system.StructStat st = android.system.Os.lstat(file.getPath());
            if (!android.system.OsConstants.S_ISREG(st.st_mode) || st.st_nlink != 1 || !file.delete()) { skipped++; continue; }
            deleted++; bytes += item.size; detail.append(item.name).append('\n');
        }
        String message = "已清理 " + deleted + " 个临时文件，删除文件合计约 " + String.format(Locale.ROOT, "%.1f", bytes / 1048576.0) + " MiB；跳过 " + skipped + " 个变化或使用中的文件。备份、应用数据及管家工作目录保留。";
        journal("cleanup", "debug-temp", true, message + "\n" + detail);
        cleanupItems.clear();
        return ok(message).put("storage", storage()).put("deleted", deleted).put("skipped", skipped);
    }
    private PackageInfo archive(File f) throws Exception {
        PackageInfo p = pm.getPackageArchiveInfo(f.getAbsolutePath(), PackageManager.GET_PERMISSIONS | PackageManager.GET_SIGNATURES);
        if (p == null || p.applicationInfo == null) throw new IOException("APK 格式无效");
        p.applicationInfo.sourceDir = f.getAbsolutePath(); p.applicationInfo.publicSourceDir = f.getAbsolutePath();
        return p;
    }
    private boolean protectedArchive(PackageInfo p) {
        // This user-requested desktop is an updated system app on this car.
        // Permit only a same-signer update; never relax protection for the factory desktop.
        if (p.packageName.equals("com.wmhome.launcher") && system(p.packageName)) {
            try {
                PackageInfo old = pm.getPackageInfo(p.packageName, PackageManager.GET_SIGNATURES);
                return p.signatures == null || p.signatures.length == 0 || !Arrays.equals(p.signatures, old.signatures);
            } catch (Exception e) { return true; }
        }
        return Policy.protectedInstall(p.packageName, system(p.packageName));
    }
    private Set<String> protectedRoles() throws Exception {
        Set<String> roles = new HashSet<>();
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo current = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        if (current != null && current.activityInfo != null) roles.add(current.activityInfo.packageName);
        for (ResolveInfo ime : pm.queryIntentServices(new Intent("android.view.InputMethod"), 0))
            if (ime.serviceInfo != null) roles.add(ime.serviceInfo.packageName);
        DevicePolicyManager admin = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (admin == null) throw new SecurityException("无法读取设备管理状态，已停止第三方软件扫描");
        List<ComponentName> active = admin.getActiveAdmins();
        if (active != null) for (ComponentName component : active) roles.add(component.getPackageName());
        String accessibility = run("settings get secure enabled_accessibility_services", 15).trim();
        for (String service : accessibility.split(":")) {
            int slash = service.indexOf('/'); if (slash > 0) roles.add(service.substring(0, slash));
        }
        return roles;
    }
    private boolean removableThirdParty(PackageInfo p, Set<String> roles) {
        ApplicationInfo a = p.applicationInfo;
        return a != null && Policy.removableThirdParty(p.packageName,
            (a.flags & ApplicationInfo.FLAG_SYSTEM) != 0, a.uid,
            (a.flags & ApplicationInfo.FLAG_PERSISTENT) != 0, roles.contains(p.packageName));
    }
    private String compatible(File f, PackageInfo p) throws Exception {
        AssetManager assets = AssetManager.class.getDeclaredConstructor().newInstance();
        XmlResourceParser xml = null;
        try {
            int cookie = (Integer) AssetManager.class.getMethod("addAssetPath", String.class).invoke(assets, f.getAbsolutePath());
            xml = assets.openXmlResourceParser(cookie, "AndroidManifest.xml");
            for (int type; (type = xml.next()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT;) {
                if (type == org.xmlpull.v1.XmlPullParser.START_TAG && "uses-sdk".equals(xml.getName())) {
                    int min = xml.getAttributeIntValue("http://schemas.android.com/apk/res/android", "minSdkVersion", 1);
                    if (min > Build.VERSION.SDK_INT) return "需要 Android SDK " + min + "，此车机为 " + Build.VERSION.SDK_INT;
                }
            }
        } finally { if (xml != null) xml.close(); assets.close(); }
        Set<String> abis = new HashSet<>();
        try (ZipFile zip = new ZipFile(f)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String n = entries.nextElement().getName();
                if (n.startsWith("lib/") && n.endsWith(".so")) {
                    String[] pieces = n.split("/"); if (pieces.length >= 3) abis.add(pieces[1]);
                }
            }
        }
        if (!abis.isEmpty() && Collections.disjoint(abis, Arrays.asList(Build.SUPPORTED_ABIS))) return "APK 的处理器架构与此车机不兼容";
        PackageInfo old = packageInfo(p.packageName, false);
        if (old != null && p.versionCode < old.versionCode) return "版本低于已安装软件，已阻止降级";
        return "";
    }
    private void requireRemoval(String pkg) {
        if (validation && pkg.equals("com.wm.usbfixture")) return;
        if (validation) throw new SecurityException("验证模式禁止改动车机原有软件");
        if (Policy.entry(pkg) != null) { Policy.requireRemovable(pkg); return; }
        try {
            PackageInfo p = packageInfo(pkg, false);
            if (p != null && removableThirdParty(p, protectedRoles())) return;
        } catch (Exception e) { throw new SecurityException("无法核对软件状态，已停止删除：" + pkg); }
        throw new SecurityException("车辆、系统或正在使用的基础程序受保护，禁止删除：" + pkg);
    }
    private JSONObject remove(String pkg) throws Exception {
        requireRemoval(pkg);
        if (!installed(pkg)) return ok("已经移除，无需重复操作");
        if (!resident && Policy.entry(pkg) != null && !new File(root, "wm_manager/original_apks/" + pkg + ".apk").isFile())
            throw new IOException("U 盘中缺少对应原厂备份包，已取消删除");
        if (pkg.equals("com.sinovoice.hcicloudinput")) ensureKeyboard();
        String output = run("pm uninstall -k --user 0 " + Policy.quote(pkg), 90);
        boolean success = output.contains("Success") && !installed(pkg);
        journal("remove", pkg, success, output);
        if (!success) throw new IOException("未完成删除：" + output);
        return ok(Policy.entry(pkg) != null ? "已从当前用户移除；原厂系统 APK 和应用数据保留，可恢复"
            : "已从当前用户移除，保留应用数据；找回需使用对应安装包重新安装");
    }
    private void ensureKeyboard() throws Exception {
        String current = run("settings get secure default_input_method", 15).trim();
        if (!current.startsWith("com.sinovoice.hcicloudinput/")) {
            for (String ime : run("ime list -s", 20).split("\\r?\\n")) if (ime.trim().equals(current)) return;
        }
        String target = Policy.IME;
        for (String ime : run("ime list -s", 20).split("\\r?\\n")) {
            if (ime.contains("/") && !ime.startsWith("com.sinovoice.hcicloudinput/") && !ime.startsWith(Policy.PACKAGE + "/")) {
                target = ime.trim(); break;
            }
        }
        run("ime enable " + Policy.quote(target), 20);
        run("ime set " + Policy.quote(target), 20);
        String confirmed = run("settings get secure default_input_method", 15).trim();
        if (!confirmed.equals(target)) throw new IOException("备用键盘未切换成功，已取消删除语音输入法");
    }
    private JSONObject probeKeyboard() throws Exception {
        String old = run("settings get secure default_input_method", 15).trim();
        Set<String> enabledBefore = new HashSet<>(Arrays.asList(run("ime list -s", 20).split("\\r?\\n")));
        try {
            ensureKeyboard();
            String selected = run("settings get secure default_input_method", 15).trim();
            return ok("已验证备用键盘切换；已还原原输入法").put("selectedDuringProbe", selected);
        } finally {
            run("ime set " + Policy.quote(old), 20);
            if (!enabledBefore.contains(Policy.IME)) run("ime disable " + Policy.quote(Policy.IME), 20);
        }
    }
    private boolean usable(String pkg) {
        PackageInfo p = packageInfo(pkg, false);
        return installed(pkg) && p != null && p.applicationInfo != null && p.applicationInfo.enabled;
    }
    private JSONObject restore(String pkg) throws Exception {
        requireRemoval(pkg);
        if (Policy.entry(pkg) == null && !(validation && pkg.equals("com.wm.usbfixture")))
            throw new SecurityException("此页仅恢复原厂备份软件；第三方软件请用安装包重装");
        if (installed(pkg)) {
            run("pm enable --user 0 " + Policy.quote(pkg), 20);
            if (!usable(pkg)) throw new IOException("软件存在但未能启用，请刷新后重试");
            return ok("软件已恢复并启用");
        }
        // Reattach a retained system package to user 0 instead of duplicating it in /data.
        PackageInfo retained = packageInfo(pkg, true);
        String existingError = "";
        if (retained != null && retained.applicationInfo != null && (retained.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            try {
                FactoryRestore.installExistingForOwner(pkg);
                run("pm enable --user 0 " + Policy.quote(pkg), 20);
                if (usable(pkg)) { journal("restore", pkg, true, "restored existing system package for user 0"); return ok("原厂软件已恢复到当前用户并启用；沿用车机保留的 APK，未复制备份。"); }
                existingError = "恢复后当前用户仍不可用";
            } catch (Exception e) { existingError = cleanError(e); }
        }
        File dir = new File(root, "wm_manager/original_apks").getCanonicalFile();
        File local = new File(dir, pkg + ".apk").getCanonicalFile();
        if (!local.getParentFile().equals(dir) || !local.isFile()) throw new IOException("U 盘中缺少对应原厂备份包");
        JSONObject hashes;
        try (InputStream in = new FileInputStream(new File(root, "wm_manager/originals.json"))) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0;) { if (bytes.size() > 65536) throw new IOException("恢复清单异常"); bytes.write(buf, 0, n); }
            hashes = new JSONObject(bytes.toString("UTF-8"));
        }
        String expected = hashes.getString(pkg);
        if (!sha(local).equals(expected)) throw new IOException("原厂备份包校验失败，已停止恢复");
        File stageDir = new File("/data/local/tmp/wm-usb-manager/stage"); stageDir.mkdirs();
        stageDir.getParentFile().setExecutable(true, false); stageDir.setExecutable(true, false);
        File staged = new File(stageDir, "restore.apk");
        try {
            try (InputStream in = new FileInputStream(local); OutputStream out = new FileOutputStream(staged)) {
                byte[] buf = new byte[65536]; for (int n; (n = in.read(buf)) > 0;) out.write(buf, 0, n);
            }
            staged.setReadable(true, false);
            if (!sha(staged).equals(expected)) throw new IOException("恢复包复制不完整");
            PackageInfo original = archive(staged);
            if (!pkg.equals(original.packageName)) throw new SecurityException("恢复包与选择的软件不符");
            String output = run("pm install --user 0 -r -g " + Policy.quote(staged.getAbsolutePath()), 150);
            if (output.contains("Success")) {
                if (!installed(pkg)) {
                    try { FactoryRestore.installExistingForOwner(pkg); }
                    catch (Exception e) { output += "\n恢复用户安装状态：" + cleanError(e); }
                }
                if (installed(pkg)) output += "\n" + run("pm enable --user 0 " + Policy.quote(pkg), 20);
            }
            boolean success = output.contains("Success") && usable(pkg);
            output += "\n当前用户已安装并启用=" + success + (existingError.isEmpty() ? "" : "\n系统保留包恢复：" + existingError);
            journal("restore", pkg, success, output);
            if (!success) throw new IOException("原厂备份包恢复失败：" + output);
            return ok("已使用 U 盘原厂备份包恢复；默认应用和输入法设置保持用户当前选择");
        } finally { staged.delete(); }
    }
    private String replaceAmapConflict() throws Exception {
        PackageInfo existing = pm.getPackageInfo(AmapInstall.PACKAGE, INCLUDE_REMOVED);
        if (existing.applicationInfo == null) throw new IOException("无法核对旧高德是否为系统应用，已停止重装");
        boolean originalSystem = (existing.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        try {
            String result = AmapInstall.replaceConflict(AmapInstall.PACKAGE, originalSystem, run("pm list users", 20),
                command -> run(command, 90), () -> Policy.packageNames(run("pm list packages -u --user 0", 20)).contains(AmapInstall.PACKAGE));
            journal("replace-amap-conflict", AmapInstall.PACKAGE, true, result); return result;
        } catch (Exception e) {
            journal("replace-amap-conflict", AmapInstall.PACKAGE, false, cleanError(e)); throw e;
        }
    }
    private JSONObject install(String name, String expected, boolean grant, boolean systemLauncher) throws Exception {
        File file = Policy.apk(root, name);
        ScanPolicy.Selection selected = requireSelected(name, file, expected);
        String expectedDigest = sha(file);
        if (selected != null) selected.require(file, expected);
        else if (!expected.equals(expectedDigest)) throw new SecurityException("下载文件已变化，请重新更新");
        // Copy and verify private staging file: USB replacement cannot alter the package mid-install.
        File stageDir = new File("/data/local/tmp/wm-usb-manager/stage"); stageDir.mkdirs();
        stageDir.getParentFile().setExecutable(true, false); stageDir.setExecutable(true, false);
        File staged = new File(stageDir, "selected.apk");
        try {
            try (InputStream in = new FileInputStream(file); OutputStream out = new FileOutputStream(staged)) {
                byte[] b = new byte[65536]; for (int n; (n = in.read(b)) > 0;) out.write(b, 0, n);
            }
            staged.setReadable(true, false);
            if (!expectedDigest.equals(sha(staged))) throw new IOException("U 盘文件读取不完整，已停止安装");
            PackageInfo p = archive(staged);
            if (selected != null) selected.requireIdentity(p.packageName, p.versionCode);
            if (protectedArchive(p) && !(validation && p.packageName.equals("com.wm.usbfixture")))
                throw new SecurityException("禁止覆盖系统或车辆程序");
            if (validation && !p.packageName.equals("com.wm.usbfixture")) throw new SecurityException("验证模式仅允许验证软件");
            if (resident && !p.packageName.equals("com.wmhome.launcher") && !p.packageName.equals("com.shihab.diplay.wmex5")) throw new SecurityException("车机版仅安装桌面和 WePlay");
            String problem = compatible(staged, p);
            if (!problem.isEmpty()) throw new IOException(problem);
            String preparation = ""; boolean conflictReplaced = false;
            if (AmapInstall.applies(p.packageName)) {
                boolean present;
                try { pm.getPackageInfo(AmapInstall.PACKAGE, INCLUDE_REMOVED); present = true; }
                catch (PackageManager.NameNotFoundException absent) { present = false; }
                try {
                    PackageInfo previous = present ? packageInfo(AmapInstall.PACKAGE, true) : null;
                    boolean active = previous != null && previous.applicationInfo != null
                        && previous.applicationInfo.sourceDir != null && new File(previous.applicationInfo.sourceDir).isFile()
                        && installed(AmapInstall.PACKAGE);
                    PackageInfo signer = null;
                    try { signer = pm.getPackageInfo(AmapInstall.PACKAGE, INCLUDE_REMOVED | PackageManager.GET_SIGNATURES); }
                    catch (PackageManager.NameNotFoundException absent) { }
                    boolean mismatch = signer != null && signer.signatures != null && signer.signatures.length > 0
                        && p.signatures != null && p.signatures.length > 0 && !sameSigner(p, signer);
                    if (mismatch) { preparation = replaceAmapConflict(); conflictReplaced = true; }
                    else preparation = AmapInstall.beforeInstall(p.packageName, present, active,
                        command -> run(command, 90), () -> RetainedAmapData.clearForOwner());
                    journal("clear-amap-data", p.packageName, true, preparation);
                } catch (Exception e) {
                    journal("clear-amap-data", p.packageName, false, cleanError(e));
                    throw e;
                }
            }
            String output = run("pm install -r " + (grant ? "-g " : "") + Policy.quote(staged.getAbsolutePath()), 150);
            if (!conflictReplaced && AmapInstall.signatureConflict(p.packageName, output)) {
                if (!expectedDigest.equals(sha(staged))) throw new IOException("安装包在处理签名冲突前已变化，已停止重装");
                preparation = replaceAmapConflict(); conflictReplaced = true;
                output = run("pm install -r " + (grant ? "-g " : "") + Policy.quote(staged.getAbsolutePath()), 150);
            }
            PackageInfo now = packageInfo(p.packageName, false);
            boolean success = output.contains("Success") && installed(p.packageName) && now != null && now.versionCode == p.versionCode;
            if (success) {
                output += "\n" + run("pm enable --user 0 " + Policy.quote(p.packageName), 20);
                now = packageInfo(p.packageName, false);
                success = now != null && now.applicationInfo != null && now.applicationInfo.enabled;
            }
            if (success) {
                try { if (selected != null) selected.require(file, expected); digestCache.put(file, expectedDigest); } catch (Exception ignored) { }
            }
            journal("install", p.packageName, success, preparation + "\n" + output);
            if (!success) throw new IOException(AmapInstall.failure(p.packageName, preparation, output));
            if (systemLauncher && p.packageName.equals("com.wmhome.launcher")) {
                try { SystemInstaller.install(staged, pm); }
                catch (Exception e) { throw new IOException("普通安装已完成，但系统分区写入失败：" + cleanError(e)); }
                return ok("桌面已写入系统分区并校验；请手动重启车机后生效");
            }
            return ok((preparation.isEmpty() ? "" : preparation + "；") + "安装成功，软件已启用" + (grant ? "，已授予软件声明的运行时权限" : "，权限按 Android 默认处理"));
        } finally { staged.delete(); }
    }
    private JSONObject updateApk(JSONObject q) throws Exception {
        if (validation) throw new SecurityException("验证通道禁止替换正式安装包");
        String repo = q.getString("repo"), pkg = UpdatePolicy.pkg(repo), name = q.getString("name"), expected = q.getString("sha256");
        long size = q.getLong("size");
        if (UpdatePolicy.assetRank(repo, name) < 0 || !UpdatePolicy.sha(expected) || !UpdatePolicy.size(size))
            throw new SecurityException("更新包信息无效，请重新查询");
        File cache = new File(pm.getApplicationInfo(Policy.PACKAGE, 0).dataDir, "cache/updates").getCanonicalFile();
        File source = new File(q.getString("source")).getCanonicalFile();
        if (!cache.equals(source.getParentFile()) || !source.getName().matches("download-[a-f0-9-]{36}\\.apk") || !source.isFile())
            throw new SecurityException("更新文件必须来自管家的下载缓存");
        String oldName = q.optString("oldName"), oldToken = q.optString("oldToken", q.optString("oldSha256")), oldSha = ""; File old = null;
        if (!oldName.isEmpty()) {
            old = Policy.apk(root, oldName);
            ScanPolicy.Selection previous = requireSelected(oldName, old, oldToken);
            if (!UpdatePolicy.canUpdate(repo, name, oldName, false)) throw new IOException("本地 perf 版本不低于发布版本，无需更新");
            oldSha = sha(old);
            if (previous != null) previous.require(old, oldToken);
            else if (!oldToken.equals(oldSha)) throw new IOException("本地安装包已变化，请刷新后重新更新");
        }
        File directory = new File(root, "apk");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法创建 U 盘 apk 文件夹");
        File canonicalDir = directory.getCanonicalFile();
        if (!canonicalDir.getParentFile().equals(root.getCanonicalFile())) throw new SecurityException("apk 文件夹必须在 U 盘根目录");
        File target = new File(canonicalDir, name).getCanonicalFile();
        if (!target.getParentFile().equals(canonicalDir)) throw new SecurityException("更新文件路径无效");
        File staged = new File(directory.getCanonicalFile(), ".wm-update-" + UUID.randomUUID() + ".part");
        try {
            if (source.length() != size) throw new IOException("下载大小不符，原安装包已保留");
            try (InputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(staged)) {
                byte[] b = new byte[65536]; long count = 0;
                for (int n; (n = in.read(b)) > 0;) {
                    count += n; if (count > size) throw new IOException("下载文件发生变化，已停止更新");
                    out.write(b, 0, n);
                }
                out.getFD().sync();
            }
            if (staged.length() != size || !expected.equals(sha(staged))) throw new IOException("下载校验失败，原安装包已保留");
            PackageInfo fresh = archive(staged);
            if (!pkg.equals(fresh.packageName)) throw new SecurityException("更新包与选择的软件不符");
            String problem = compatible(staged, fresh);
            if (!problem.isEmpty()) throw new IOException(problem);
            boolean trusted = trustedSigner(fresh);
            if (old != null) {
                PackageInfo local = archive(old);
                if (!pkg.equals(local.packageName) || !sameSigner(fresh, local)) throw new SecurityException("本地包与更新包的签名不一致，已保留原文件");
                if (fresh.versionCode < local.versionCode) throw new IOException("发布版本低于本地包，已阻止降级");
                trusted = true;
            }
            try {
                PackageInfo installed = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                if (!sameSigner(fresh, installed)) throw new SecurityException("车机软件与更新包的签名不一致，已停止更新");
                trusted = true;
            } catch (PackageManager.NameNotFoundException absent) { /* A matching local APK is sufficient. */ }
            if (!trusted) throw new SecurityException("缺少可核对签名的本地包，请先放入原版安装包再更新");
            if (target.exists() && (old == null || !target.equals(old)) && !expected.equals(sha(target)))
                throw new IOException("同名文件已经存在，请整理 apk 文件夹后刷新");
            if (old != null && !oldSha.equals(sha(old))) throw new IOException("本地安装包已变化，已停止更新");
            android.system.Os.rename(staged.getAbsolutePath(), target.getAbsolutePath());
            boolean retainedOld = old != null && !old.equals(target) && !old.delete();
            tokens.remove(oldName); tokens.put(name, expected);
            selections.remove(oldName); selections.remove(name); digestCache.put(target, expected);
            String message = "已更新 U 盘安装包；勾选后可安装到车机" + (retainedOld ? "。旧文件未能删除，请按需手动清理" : "");
            try { journal("update-apk", pkg, true, name + " " + expected); } catch (IOException ignored) { }
            if (resident) return install(name, expected, q.optBoolean("grant", true), q.optBoolean("systemLauncher", true)).put("version", fresh.versionName);
            return ok(message).put("name", name).put("version", fresh.versionName).put("sha256", expected);
        } finally { staged.delete(); }
    }
    private boolean trustedSigner(PackageInfo p) throws Exception {
        String expected = p.packageName.equals("com.wmhome.launcher") ? "e2db4c74775d527181c7ca92959fbffaffd273ee2dbff7a143fc8c28687fa1ba" : p.packageName.equals("com.shihab.diplay.wmex5") ? "89a94fd5d7d32fdd26d81c3827c74bb3dc215af91d1fcff0661fd8e1d2c13509" : "";
        if (p.signatures == null || p.signatures.length != 1) return false;
        StringBuilder h = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(p.signatures[0].toByteArray())) h.append(String.format(Locale.ROOT, "%02x", b & 255));
        return expected.equals(h.toString());
    }
    private boolean sameSigner(PackageInfo a, PackageInfo b) {
        return a.signatures != null && a.signatures.length > 0 && Arrays.equals(a.signatures, b.signatures);
    }
    static String sha(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[65536]; for (int n; (n = in.read(buf)) > 0;) md.update(buf, 0, n);
        }
        StringBuilder s = new StringBuilder(); for (byte b : md.digest()) s.append(String.format(Locale.ROOT, "%02x", b & 255)); return s.toString();
    }
    private void journal(String action, String pkg, boolean success, String output) throws Exception {
        JSONObject line = new JSONObject().put("time", System.currentTimeMillis()).put("action", action).put("package", pkg)
            .put("success", success).put("detail", output);
        journal.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(journal, true)) { out.write((line + "\n").getBytes("UTF-8")); }
        if (!validation && root.isDirectory()) {
            try (FileOutputStream out = new FileOutputStream(new File(root, "wm_manager/operations.jsonl"), true)) {
                out.write((line + "\n").getBytes("UTF-8"));
            } catch (IOException ignored) { /* Read-only USB does not lose the internal journal. */ }
        }
    }
    static String run(String command, int seconds) throws Exception {
        Process p = new ProcessBuilder("/system/bin/sh", "-c", command).redirectErrorStream(true).start();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<String> read = pool.submit(() -> {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            try (InputStream in = p.getInputStream()) {
                byte[] buf = new byte[4096]; for (int n; (n = in.read(buf)) > 0;) {
                    if (b.size() > 1024 * 1024) throw new IOException("命令输出过长"); b.write(buf, 0, n);
                }
            }
            return b.toString("UTF-8").replace("\r", "").trim();
        });
        try { String output = read.get(seconds, TimeUnit.SECONDS); p.waitFor(); return output; }
        catch (TimeoutException e) { p.destroy(); read.cancel(true); throw new IOException("操作超时，请检查实际安装状态后再试"); }
        finally { pool.shutdownNow(); }
    }
}
