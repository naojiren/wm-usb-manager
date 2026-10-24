package com.wm.usbmanager;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.io.File;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(12, 104, 124), INK = Color.rgb(25, 40, 61);
    private static final int WORKING = Color.rgb(167, 76, 0), DONE = Color.rgb(0, 112, 69), ERROR = Color.rgb(184, 40, 40);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private JSONArray apps = new JSONArray(), apks = new JSONArray();
    private final Map<String, Boolean> removal = new HashMap<>(), installation = new HashMap<>();
    private LinearLayout root, list, bar, footer;
    private ScrollView scroll;
    private final Map<String, JSONObject> releases = new HashMap<>();
    private final Map<String, String> updateStatus = new HashMap<>();
    private TextView state, title, hint, results, current, storageText;
    private JSONObject storageData = new JSONObject(), cleanupData = new JSONObject();
    private boolean cleanupSelected;
    private ProgressBar progressBar;
    private Switch grants, systemSwitch;
    private boolean resident, systemLauncher = true;
    private boolean scanning, ready, busy, grant = true, validationMode;
    private int page;
    private String summary = "", stateText = "向导已打开 · 即将扫描车机软件和 U 盘安装包…";
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); resident = !getIntent().getBooleanExtra("validation", false) && !getIntent().getBooleanExtra("usb_mode", false); BridgeClient.resident = resident; validationMode = getIntent().getBooleanExtra("validation", false); layout();
        android.util.Log.i("WMUsbManager", "UI_CREATED " + android.os.SystemClock.elapsedRealtime());
        root.postDelayed(() -> refresh(), 250);
    }
    @Override protected void onNewIntent(android.content.Intent intent) { super.onNewIntent(intent); if (busy) return; setIntent(intent); recreate(); }
    private int d(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size + 6); t.setTextColor(INK);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private Button button(String s, View.OnClickListener listener) {
        Button b = new Button(this); b.setText(s); b.setTextSize(26); b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(222, 237, 240))); b.setTextColor(INK); b.setOnClickListener(listener); b.setMinHeight(d(80)); return b;
    }
    private void layout() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(d(24), d(16), d(24), d(12));
        root.setBackgroundColor(Color.rgb(234, 241, 244)); setContentView(root);
        LinearLayout heading = new LinearLayout(this); heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
        heading.addView(text("威马车机软件向导", 30, true), new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(button("刷新", v -> refresh()));
        heading.addView(button("退出", v -> { if (!busy || scanning) finish(); })); root.addView(heading);
        state = text(stateText, 17, false); state.setPadding(0, d(8), 0, d(8)); root.addView(state);
        bar = new LinearLayout(this);
        String[] steps = resident ? new String[]{"卸载软件", "联网更新安装"} : new String[]{"① 删除原厂软件", "② 安装新软件", "恢复已移除软件"};
        for (int i = 0; i < steps.length; i++) {
            final int target = i; Button b = button(steps[i], v -> { if (!busy || scanning) { page = target; render(); if (target == 1 && !busy) worker.submit(() -> updateStorage()); } });
            bar.addView(b, new LinearLayout.LayoutParams(0, d(88), 1));
        }
        root.addView(bar);
        storageText = text("数据分区剩余空间：正在读取…", 20, true);
        storageText.setPadding(d(12), d(8), d(12), d(8)); root.addView(storageText);
        storageText.setVisibility(View.GONE);
        title = text("", 24, true); title.setPadding(0, d(12), 0, d(4)); 
        hint = text("", 17, false); 
        grants = new Switch(this); grants.setText("自动授予软件声明的运行时权限"); grants.setTextSize(23); grants.setMinHeight(d(80)); grants.setChecked(true);
        systemSwitch = new Switch(this); systemSwitch.setText("威马桌面安装到系统分区（重启生效）"); systemSwitch.setTextSize(23); systemSwitch.setMinHeight(d(72)); systemSwitch.setChecked(true);
        systemSwitch.setOnCheckedChangeListener((b, checked) -> systemLauncher = checked);
        grants.setOnCheckedChangeListener((b, checked) -> grant = checked); 
        scroll = new ScrollView(this); scroll.setFillViewport(false);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setVisibility(View.GONE); root.addView(progressBar, new LinearLayout.LayoutParams(-1, d(8)));
        current = text("准备扫描车机软件…", 19, true); current.setTextColor(WORKING);
        current.setPadding(0, d(8), 0, d(4)); root.addView(current);
        results = text("", 19, true); results.setMaxLines(2); results.setTextColor(DONE);
        results.setPadding(0, d(4), 0, d(8)); root.addView(results);
        footer = new LinearLayout(this); root.addView(footer); render();
    }
    private void status(String message) { stateText = message; state.setText(message); }
    private void refresh() {
        if (busy || isFinishing()) return; scanning = true; setBusy(true); render();
        status("正在扫描软件，请稍候…");
        current.setText("正在扫描软件…"); current.setTextColor(WORKING);
        progressBar.setIndeterminate(true); progressBar.setVisibility(View.VISIBLE);
        android.util.Log.i("WMUsbManager", "SCAN_BEGIN " + android.os.SystemClock.elapsedRealtime());
        worker.submit(() -> {
            boolean bridgeConnected = false;
            try {
                JSONObject data;
                try { if (resident) startResident(); awaitBridge(); }
                catch (Exception e) {
                    runOnUiThread(() -> { status("正在通过原厂 U 盘入口恢复权限…"); current.setText("正在恢复操作权限，随后继续扫描…"); });
                    if (validationMode || resident) throw e;
                    RootBridge.run("/system/xbin/su -c 'sh /mnt/udisk/wm_manager/start.sh --resume'", 30);
                    awaitBridge();
                }
                bridgeConnected = true;
                data = BridgeClient.call(new JSONObject().put("action", "inventory"), validationMode);
                if (!data.optBoolean("ok")) throw new Exception(data.optString("message"));
                final JSONObject result = data;
                runOnUiThread(() -> {
                    apps = result.optJSONArray("apps"); apks = result.optJSONArray("apks");
                    ready = result.optBoolean("usb");
                    storageData = result.optJSONObject("storage"); cleanupData = result.optJSONObject("cleanup");
                    if (cleanupData == null) cleanupData = new JSONObject();
                    if (cleanupData.optInt("count") == 0) cleanupSelected = false;
                    for (int i = 0; i < apps.length(); i++) {
                        JSONObject a = apps.optJSONObject(i); String p = a.optString("package");
                        if (!removal.containsKey(p)) removal.put(p, a.optBoolean("selected"));
                        if (!a.optBoolean("installed") || !a.optBoolean("removable", true)) removal.put(p, false);
                    }
                    for (int i = 0; i < apks.length(); i++) {
                        JSONObject a = apks.optJSONObject(i); String n = a.optString("name");
                        if (!installation.containsKey(n)) installation.put(n, a.optBoolean("selected"));
                        if (!a.optBoolean("eligible")) installation.put(n, false);
                    }
                    status(resident ? "车机版 · 卸载与联网更新 · 请停车后操作" : validationMode ? "验证模式 · 原厂软件禁止改动" : ready ? "U 盘已连接 · 权限已就绪 · 请停车后操作" : "权限已就绪；请插入准备好的 U 盘后点刷新");
                    scanning = false; setBusy(false); progressBar.setVisibility(View.GONE);
                    current.setText("扫描完成 · " + result.optInt("installedCount") + " 个已装软件 · " + String.format(Locale.ROOT, "%.1f", result.optLong("scanMillis") / 1000.0) + " 秒"); current.setTextColor(DONE);
                    android.util.Log.i("WMUsbManager", "SCAN_DONE " + android.os.SystemClock.elapsedRealtime()); render();
                });
            } catch (Exception e) {
                final boolean connected = bridgeConnected;
                android.util.Log.e("WMUsbManager", connected ? "INVENTORY_FAILED" : "BRIDGE_FAILED", e);
                runOnUiThread(() -> {
                    ready = false;
                    status(connected ? "软件扫描未完成，请点刷新重试。" : resident ? "权限通道未就绪，请检查已有 su 授权。" : "权限通道未就绪，请通过 U 盘入口重新启动。");
                    summary = (connected ? "扫描失败：" : "权限通道未就绪：") + RootBridge.cleanError(e); scanning = false; setBusy(false);
                    progressBar.setVisibility(View.GONE); current.setText(connected ? "扫描未完成，请查看具体错误后刷新" : "权限通道连接失败，请重新启动向导"); current.setTextColor(ERROR); render();
                });
            }
        });
    }
    private void startResident() throws Exception {
        try { if (BridgeClient.call(new JSONObject().put("action", "status"), false, 2000).optBoolean("ok")) return; } catch (Exception ignored) { }
        String command = "CLASSPATH=" + Policy.quote(getApplicationInfo().sourceDir) + " toybox nohup /system/bin/app_process /system/bin com.wm.usbmanager.RootBridge --resident >/data/local/tmp/wm-wizard-resident.log 2>&1 </dev/null &";
        RootBridge.run("su -c " + Policy.quote(command), 15);
    }
    private void installGuide() {
        if (busy || !ready) return; setBusy(true); current.setText("正在安装向导到车机…");
        worker.submit(() -> { String message;
            try { JSONObject reply = BridgeClient.call(new JSONObject().put("action", "install-guide")); message = reply.optString("message"); }
            catch (Exception e) { message = RootBridge.cleanError(e); }
            final String result = message; runOnUiThread(() -> { setBusy(false); current.setText(result); largeDialog(new android.app.AlertDialog.Builder(this).setTitle("安装向导到车机").setMessage(result).setPositiveButton("完成", null).show()); });
        });
    }
    private void awaitBridge() throws Exception {
        long deadline = android.os.SystemClock.elapsedRealtime() + 45000;
        Exception last = null;
        do {
            try {
                JSONObject r = BridgeClient.call(new JSONObject().put("action", "status"), validationMode, 3000);
                if (r.optBoolean("ok") && r.optInt("uid", -1) == 0) return;
                last = new Exception("操作权限尚未就绪");
            } catch (Exception e) { last = e; }
            Thread.sleep(250);
        } while (android.os.SystemClock.elapsedRealtime() < deadline && !isFinishing());
        throw last == null ? new Exception("操作入口未连接") : last;
    }
    private void completed(String message) {
        if (message.trim().isEmpty()) { results.setText(""); return; }
        String[] lines = message.trim().split("\n"); StringBuilder recent = new StringBuilder();
        for (int i = Math.max(0, lines.length - 2); i < lines.length; i++) {
            if (recent.length() > 0) recent.append('\n'); recent.append(lines[i]);
        }
        SpannableString colored = new SpannableString(recent.toString()); int offset = 0;
        for (String line : recent.toString().split("\n")) {
            colored.setSpan(new ForegroundColorSpan(line.startsWith("✗") ? ERROR : DONE), offset, offset + line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            offset += line.length() + 1;
        }
        results.setText(colored);
    }
    private void setBusy(boolean value) {
        busy = value; enable(root, !value);
        if (value && scanning) { enable(bar, true); ((android.view.ViewGroup)root.getChildAt(0)).getChildAt(2).setEnabled(true); }
        if (!value && !ready) enable(footer, false);
    }
    private void enable(View view, boolean value) {
        view.setEnabled(value);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) enable(group.getChildAt(i), value);
        }
    }
    private void render() {
        list.removeAllViews(); footer.removeAllViews(); completed(summary);
        renderStorage(); storageText.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        list.addView(title); list.addView(hint); list.addView(grants); list.addView(systemSwitch); systemSwitch.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        grants.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < bar.getChildCount(); i++) {
            Button b = (Button)bar.getChildAt(i); b.setTextColor(i == page ? Color.WHITE : INK); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(i == page ? BLUE : Color.rgb(222,237,240)));
        }
        if (page == 0) {
            title.setText("选择要移除的软件"); title.setTextColor(ERROR);
            hint.setText("原厂百度地图和语音输入法默认勾选，其它项目按需选择。第三方软件单独列出、默认不选。保留数据；原厂软件可从 U 盘恢复，第三方软件用安装包重装。");
            if (resident) hint.setText("原厂软件从当前用户移除，保留系统 APK。第三方软件默认不选；系统、当前桌面和正在使用的基础服务受保护。此车机版不附恢复包。");
            cleanupCard();
            boolean thirdPartyHeading = false, protectedHeading = false;
            for (int i = 0; i < apps.length(); i++) {
                JSONObject a = apps.optJSONObject(i);
                if (i == 0) list.addView(text("原厂可选软件", 20, true));
                if (!thirdPartyHeading && a.optString("kind").equals("thirdParty")) {
                    list.addView(text("已安装的第三方软件 · 默认不选", 20, true)); thirdPartyHeading = true;
                }
                if (!protectedHeading && a.optString("kind").equals("protected")) {
                    list.addView(text("系统及受保护软件 · 已扫描，不可删除", 20, true)); protectedHeading = true;
                }
                removalRow(a, false);
            }
            if (apps.length() == 0) empty(scanning ? "正在扫描已安装的软件，请稍候…" : "连接 U 盘后读取原厂软件清单。");
            addFooter(button("删除已选软件，进入安装", v -> batch(false)));
            addFooter(button("跳过删除 → 安装", v -> { page = 1; render(); worker.submit(() -> updateStorage()); }));
        } else if (page == 1) {
            title.setText("选择要安装的软件"); title.setTextColor(BLUE);
            hint.setText("默认仅选 ADB 工具、DiPlay、威马桌面、ES 文件管理器和强制旋转。其它软件按需勾选；不兼容或较旧版本不可选。特殊权限由软件首次使用时提示。");
            if (!resident) { LinearLayout keep = card(); keep.addView(text("把向导留在车机", 22, true)); keep.addView(text("无需携带 U 盘即可卸载软件、查询并安装桌面与 WePlay 更新。不复制内置软件。", 17, false)); keep.addView(button("安装向导到车机", v -> installGuide())); }
            updateCard("wm-launcher", "威马桌面"); updateCard("diplay-wm", "WePlay / DiPlay"); if (resident) hint.setText("查询 GitHub 最新发布，点击下载安装。桌面默认写入系统分区，下载缓存仅包含你选择的两款软件。");
            if (!resident) {
            LinearLayout select = new LinearLayout(this);
            select.addView(button("全选", v -> selectApks(true))); select.addView(button("取消全选", v -> selectApks(false))); list.addView(select);
            select.addView(button("恢复默认", v -> defaultApks()));
            boolean optionalHeading = false;
            for (int i = 0; i < apks.length(); i++) {
                JSONObject a = apks.optJSONObject(i);
                if (i == 0 && a.optInt("priority", 100) < 5) list.addView(text("优先软件 · 兼容时默认勾选", 20, true));
                if (!optionalHeading && a.optInt("priority", 100) >= 5) {
                    list.addView(text("其它软件 · 按需勾选", 20, true)); optionalHeading = true;
                }
                apkRow(a);
            }
            if (apks.length() == 0) empty(scanning ? "正在扫描 U 盘 apk 文件夹，请稍候…" : "apk 文件夹中没有安装包。把 .apk 文件放到 U 盘根目录的 apk 文件夹，再点刷新。");
            addFooter(button("一键安装已选软件", v -> batch(true)));
            }
            addFooter(button("返回删除页", v -> { page = 0; render(); }));
        } else {
            title.setText("恢复原厂软件"); hint.setText("优先恢复车机保留的原厂程序到当前用户；必要时使用 U 盘本地恢复包。不提取新备份，不自动切换默认应用。");
            boolean any = false;
            for (int i = 0; i < apps.length(); i++) {
                JSONObject a = apps.optJSONObject(i); if (a.optBoolean("restorable")) { removalRow(a, true); any = true; }
            }
            if (!any) empty("没有检测到可恢复的原厂软件。");
            addFooter(button("进入安装页", v -> { page = 1; render(); worker.submit(() -> updateStorage()); }));
        }
        if (busy) { enable(root, false); if (scanning) { enable(bar, true); ((android.view.ViewGroup)root.getChildAt(0)).getChildAt(2).setEnabled(true); } }
        else if (!ready) enable(footer, false);
    }
    private static String space(long bytes) { return String.format(Locale.ROOT, "%.1f MiB", bytes / 1048576.0); }
    private void renderStorage() {
        if (storageData == null || !storageData.has("available")) { storageText.setText("数据分区剩余空间：暂不可用，请刷新"); return; }
        long available = storageData.optLong("available"), reserve = storageData.optLong("reserve");
        boolean reserveKnown = storageData.optBoolean("reserveKnown");
        storageText.setText("车机数据分区 /data：剩余 " + space(available) + " / 总计 " + space(storageData.optLong("total"))
            + (reserveKnown ? "\n扣除系统预留后约 " + space(storageData.optLong("aboveReserve")) : "\n系统预留暂不可读")
            + "；安装还需中转、解压空间");
        storageText.setTextColor(available <= reserve + 150L * 1048576 ? ERROR : BLUE);
    }
    private void updateStorage() {
        try {
            JSONObject reply = BridgeClient.call(new JSONObject().put("action", "storage"), validationMode);
            if (!reply.optBoolean("ok") || !reply.has("storage")) throw new Exception("空间读取失败");
            final JSONObject data = reply.getJSONObject("storage");
            runOnUiThread(() -> { storageData = data; renderStorage(); });
        } catch (Exception e) { runOnUiThread(() -> { storageData = null; renderStorage(); }); }
    }
    private void cleanupCard() {
        LinearLayout c = card();
        CheckBox check = new CheckBox(this); check.setTextSize(27); check.setMinHeight(d(80)); check.setTextColor(INK);
        check.setText("清理历史临时调试文件 · 可选"); check.setChecked(cleanupSelected);
        check.setEnabled(ready && !busy && cleanupData.optInt("count") > 0);
        check.setOnCheckedChangeListener((v, checked) -> cleanupSelected = checked); c.addView(check);
        String cleanupStatus = cleanupData.optString("status");
        String estimate = cleanupStatus.equals("ready") ? "识别到可选临时文件 " + space(cleanupData.optLong("bytes")) + "，共 " + cleanupData.optInt("count") + " 个。"
            : cleanupStatus.equals("unavailable") ? "清理统计读取失败，请查看原因后刷新。" : "临时文件尚未统计完成，请刷新。";
        c.addView(text(estimate, 19, true));
        c.addView(text("识别旧测试 APK、日志、截图和向导遗留安装中转包，不再限制24小时。不清空整个目录；保留备份、应用数据和正在使用的文件，实际释放量以结果为准。", 17, false));
        if (cleanupStatus.equals("ready")) c.addView(text("顶层备份文件保留 " + space(cleanupData.optLong("backupBytes")) + "；未分类文件保留 " + space(cleanupData.optLong("unknownBytes")) + "。目录及应用数据不计入本项。", 17, false));
        if (cleanupStatus.equals("ready") && cleanupData.optInt("count") == 0) c.addView(text("没有识别到可清理临时文件。空间也可能由已安装软件、应用数据或备份占用。", 17, false));
        if (cleanupData.has("error")) c.addView(text(cleanupData.optString("error"), 17, false));
        Button details = button("查看待清理文件", v -> {
            StringBuilder lines = new StringBuilder(); JSONArray files = cleanupData.optJSONArray("files");
            if (files != null) for (int i = 0; i < files.length(); i++) lines.append(files.optString(i)).append('\n');
            largeDialog(new android.app.AlertDialog.Builder(this).setTitle("待清理文件 · 默认不选").setMessage(lines.length() == 0 ? "没有符合条件的历史文件" : lines.toString()).setPositiveButton("返回", null).show());
        }); c.addView(details);
    }
    private void addFooter(Button b) {
        if (b.getText().toString().startsWith("删除") || b.getText().toString().startsWith("一键安装")) {
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(page == 0 ? ERROR : BLUE));
            b.setTextColor(Color.WHITE); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        footer.addView(b, new LinearLayout.LayoutParams(0, d(88), 1));
    }
    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(d(20), d(16), d(20), d(16));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(d(18)); bg.setStroke(d(1), Color.rgb(215, 227, 232)); c.setBackground(bg); c.setElevation(d(2)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = d(8); list.addView(c, p); return c;
    }
    private void empty(String s) { LinearLayout c = card(); c.addView(text(s, 20, false)); }
    private void removalRow(JSONObject a, boolean restore) {
        String pkg = a.optString("package"); LinearLayout c = card();
        if (restore) {
            c.addView(text(a.optString("label"), 21, true));
            c.addView(button("恢复", v -> restore(pkg)));
        } else {
            CheckBox check = new CheckBox(this); check.setTextSize(27); check.setMinHeight(d(80)); check.setPadding(d(8), d(8), d(8), d(8)); check.setTextColor(INK);
            check.setText(a.optString("label") + (!a.optBoolean("removable", true) ? " · 受保护" : a.optBoolean("installed") ? "" : " · 已移除"));
            check.setChecked(Boolean.TRUE.equals(removal.get(pkg))); check.setEnabled(a.optBoolean("installed") && a.optBoolean("removable", true) && ready && !busy);
            check.setOnCheckedChangeListener((v, checked) -> removal.put(pkg, checked)); c.addView(check);
        }
        c.addView(text(pkg, 13, false));
        TextView desc = text(a.optString("description"), 17, false); desc.setPadding(d(8), 0, 0, 0); c.addView(desc);
    }
    private void apkRow(JSONObject a) {
        LinearLayout c = card(); String name = a.optString("name"); boolean allowed = a.optBoolean("eligible");
        CheckBox check = new CheckBox(this); check.setTextSize(27); check.setMinHeight(d(80)); check.setPadding(d(8), d(8), d(8), d(8)); check.setTextColor(INK);
        check.setText(a.optString("label", name));
        check.setChecked(Boolean.TRUE.equals(installation.get(name))); check.setEnabled(allowed && ready && !busy);
        check.setOnCheckedChangeListener((v, checked) -> installation.put(name, checked)); c.addView(check);
        c.addView(text("版本 " + a.optString("version", ""), 18, false));
        String desc = a.optString("description");
        if (!allowed) desc += "\n不可安装：" + a.optString("reason");
        if (!a.optString("existing").isEmpty()) desc += " · 已安装 " + a.optString("existing");
        c.addView(text(desc, 17, false));
        c.addView(text(name + " · " + String.format(java.util.Locale.ROOT, "%.1f MB", a.optLong("size") / 1048576.0), 14, false));
    }
    private void selectApks(boolean checked) {
        for (int i = 0; i < apks.length(); i++) {
            JSONObject a = apks.optJSONObject(i); installation.put(a.optString("name"), checked && a.optBoolean("eligible"));
        }
        render();
    }
    private void defaultApks() {
        for (int i = 0; i < apks.length(); i++) {
            JSONObject a = apks.optJSONObject(i);
            installation.put(a.optString("name"), a.optBoolean("selected") && a.optBoolean("eligible"));
        }
        render();
    }
    private void batch(boolean install) {
        if (!ready || busy) return;
        List<JSONObject> jobs = new ArrayList<>(); Set<String> packages = new HashSet<>();
        JSONArray from = install ? apks : apps;
        for (int i = 0; i < from.length(); i++) {
            JSONObject a = from.optJSONObject(i); String key = a.optString(install ? "name" : "package");
            if (Boolean.TRUE.equals((install ? installation : removal).get(key)) && (install || a.optBoolean("removable", true))) {
                if (install && !packages.add(a.optString("package"))) {
                    summary = "同一软件选中了多个 APK，请只保留一个版本后安装。"; completed(summary); return;
                }
                jobs.add(a);
            }
        }
        if (!install && cleanupSelected && cleanupData.optInt("count") > 0) {
            try { jobs.add(0, new JSONObject().put("kind", "cleanup").put("label", "历史临时调试文件").put("token", cleanupData.getString("token"))); }
            catch (JSONException e) { summary = "请刷新清理清单后重试。"; completed(summary); return; }
        }
        if (jobs.isEmpty()) {
            if (!install) { page = 1; render(); worker.submit(() -> updateStorage()); }
            else { summary = "没有选中安装包。"; completed(summary); } return;
        }
        final boolean grantNow = grant; setBusy(true); summary = ""; completed("");
        progressBar.setIndeterminate(false); progressBar.setMax(jobs.size()); progressBar.setProgress(0); progressBar.setVisibility(View.VISIBLE);
        worker.submit(() -> {
            int success = 0; StringBuilder log = new StringBuilder(), completedLog = new StringBuilder();
            for (int i = 0; i < jobs.size(); i++) {
                JSONObject a = jobs.get(i); final String progress = (install ? "正在安装 " : "正在删除 ") + (i + 1) + "/" + jobs.size() + " · " + a.optString("label");
                runOnUiThread(() -> { status(progress); current.setText(progress); current.setTextColor(WORKING); });
                boolean itemSuccess = false;
                try {
                    JSONObject q = new JSONObject().put("action", install ? "install" : "remove");
                    if (install) q.put("name", a.getString("name")).put("token", a.getString("token")).put("grant", grantNow).put("systemLauncher", systemLauncher);
                    else if (a.optString("kind").equals("cleanup")) q.put("action", "cleanup").put("token", a.getString("token"));
                    else q.put("package", a.getString("package"));
                    JSONObject r = BridgeClient.call(q, validationMode);
                    itemSuccess = r.optBoolean("ok"); if (itemSuccess) success++;
                    log.append(r.optBoolean("ok") ? "✓ " : "✗ ").append(a.optString("label")).append("：").append(r.optString("message")).append('\n');
                } catch (Exception e) { log.append("✗ ").append(a.optString("label")).append("：").append(RootBridge.cleanError(e)).append('\n'); }
                completedLog.append(itemSuccess ? "✓ 已" + (install ? "安装 " : "删除 ") : "✗ 处理失败 ").append(a.optString("label")).append('\n');
                updateStorage();
                if (a.optString("kind").equals("cleanup")) runOnUiThread(() -> { cleanupSelected = false; cleanupData = new JSONObject(); });
                final String done = completedLog.toString(); final int finished = i + 1;
                runOnUiThread(() -> { summary = done; completed(summary); progressBar.setProgress(finished); });
            }
            final int count = success;
            final String done = (install ? "安装" : "删除") + "完成：成功 " + count + "，失败 " + (jobs.size() - count) + "。\n" + log;
            runOnUiThread(() -> {
                summary = completedLog.toString(); setBusy(false); if (!install) page = 1;
                status((install ? "安装" : "删除") + "完成：成功 " + count + "，失败 " + (jobs.size() - count));
                current.setText(stateText); current.setTextColor(count == jobs.size() ? DONE : ERROR); progressBar.setVisibility(View.GONE);
                if (install) {
                    largeDialog(new android.app.AlertDialog.Builder(this).setTitle("安装结果")
                        .setMessage(done).setPositiveButton("完成", (dialog, which) -> refresh()).show());
                } else {
                    largeDialog(new android.app.AlertDialog.Builder(this).setTitle("删除结果 · 下一步安装")
                        .setMessage(done).setPositiveButton("进入安装页", (dialog, which) -> refresh()).show());
                }
                render();
            });
        });
    }
    private void restore(String pkg) {
        if (!ready || busy) return; setBusy(true); status("正在恢复原厂软件…");
        worker.submit(() -> {
            try {
                JSONObject r = BridgeClient.call(new JSONObject().put("action", "restore").put("package", pkg), validationMode);
                if (!r.optBoolean("ok")) throw new Exception(r.optString("message"));
                runOnUiThread(() -> { summary = r.optString("message"); setBusy(false); refresh(); });
            } catch (Exception e) {
                runOnUiThread(() -> { summary = RootBridge.cleanError(e); setBusy(false); render(); });
            }
        });
    }

    private JSONObject localApk(String repo) {
        String pkg = UpdatePolicy.pkg(repo); JSONObject best = null;
        for (int i = 0; i < apks.length(); i++) {
            JSONObject a = apks.optJSONObject(i);
            if (pkg.equals(a.optString("package")) && (best == null || a.optInt("code") > best.optInt("code"))) best = a;
        }
        return best;
    }
    private void updateCard(String repo, String label) {
        LinearLayout c = card(); c.addView(text(label + " · 在线更新安装包", 22, true));
        JSONObject local = localApk(repo), release = releases.get(repo);
        c.addView(text(local == null ? (resident ? "尚未下载，安装时核对签名与车机版本" : "U 盘中尚无此软件安装包") : "本地版本 " + local.optString("version"), 18, false));
        String message = updateStatus.containsKey(repo) ? updateStatus.get(repo) : "点击查询 GitHub 最新正式版";
        if (release != null && local != null && release.optString("sha256").equals(local.optString("sha256"))) message = "本地已是最新版 · " + release.optString("tag");
        TextView details = text(message, 18, false); details.setTextColor(BLUE); c.addView(details);
        LinearLayout buttons = new LinearLayout(this);
        buttons.addView(button("查询新版", v -> checkUpdate(repo, label)), new LinearLayout.LayoutParams(0, d(88), 1));
        Button download = button(resident ? "下载并安装" : "更新到 U 盘", v -> updateFile(repo, label));
        download.setEnabled(release != null && ready && !busy);
        buttons.addView(download, new LinearLayout.LayoutParams(0, d(88), 1)); c.addView(buttons);
        c.addView(text(resident ? "联网下载后校验签名和版本，再安装到车机。" : "需要车机联网。更新 U 盘文件后勾选安装。", 17, false));
    }
    private void checkUpdate(String repo, String label) {
        if (busy) return; setBusy(true);
        status("正在查询 " + label + " 最新发布版本…"); current.setText(stateText); current.setTextColor(WORKING);
        progressBar.setIndeterminate(true); progressBar.setVisibility(View.VISIBLE);
        worker.submit(() -> {
            try {
                JSONObject release = GitHubRelease.latest(repo);
                runOnUiThread(() -> {
                    releases.put(repo, release); updateStatus.put(repo, "最新发布 " + release.optString("tag"));
                    setBusy(false); progressBar.setVisibility(View.GONE); current.setText("查询完成 · " + label + " " + release.optString("tag")); current.setTextColor(DONE); render();
                    JSONObject local = localApk(repo); boolean same = local != null && release.optString("sha256").equals(local.optString("sha256"));
                    String notes = release.optString("notes"); if (notes.length() > 1200) notes = notes.substring(0, 1200) + "…";
                    String message = (same ? "本地安装包已是最新版，可重新下载校验。" : "可下载最新正式发布的安装包。")
                        + "\n\n发布版本：" + release.optString("tag") + "\n文件：" + release.optString("file")
                        + "\n大小：" + String.format(Locale.ROOT, "%.1f MB", release.optLong("size") / 1048576.0)
                        + (resident ? "\n\n确认后下载、校验并安装到车机。\n\n" : "\n\n下载完成后替换 U 盘文件，勾选后再安装到车机。\n\n") + notes;
                    android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(this).setTitle(label + " · 查询结果").setMessage(message).setNegativeButton("关闭", null);
                    if (ready) dialog.setPositiveButton(resident ? "下载并安装" : "更新到 U 盘", (a, b) -> updateFile(repo, label));
                    largeDialog(dialog.show());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    releases.remove(repo); updateStatus.put(repo, "查询失败，请检查网络后重试"); setBusy(false); progressBar.setVisibility(View.GONE);
                    current.setText("查询失败 · " + label); current.setTextColor(ERROR); render();
                    largeDialog(new android.app.AlertDialog.Builder(this).setTitle("查询未完成").setMessage("请确认车机可访问 GitHub，再点查询新版。\n\n" + RootBridge.cleanError(e)).setPositiveButton("知道了", null).show());
                });
            }
        });
    }
    private void updateFile(String repo, String label) {
        JSONObject release = releases.get(repo); if (!ready || busy || release == null) return;
        JSONObject old = localApk(repo); final String oldName = old == null ? "" : old.optString("name"), oldToken = old == null ? "" : old.optString("token");
        final boolean selected = old == null || Boolean.TRUE.equals(installation.get(oldName));
        File directory = new File(getCacheDir(), "updates");
        if (!directory.isDirectory() && !directory.mkdirs()) { current.setText("无法创建下载缓存，请检查车机存储空间"); current.setTextColor(ERROR); return; }
        File target = new File(directory, "download-" + UUID.randomUUID() + ".apk");
        setBusy(true); summary = ""; completed(""); status("正在下载 " + label + "，完成后更新 U 盘文件…");
        current.setText("正在下载 " + label + " · 0%"); current.setTextColor(WORKING);
        progressBar.setIndeterminate(false); progressBar.setMax(100); progressBar.setProgress(0); progressBar.setVisibility(View.VISIBLE);
        worker.submit(() -> {
            try {
                GitHubRelease.download(release, target, (received, total) -> runOnUiThread(() -> {
                    current.setText("正在下载 " + label + " · " + received * 100 / total + "% · " + String.format(Locale.ROOT, "%.1f / %.1f MB", received / 1048576.0, total / 1048576.0)); progressBar.setProgress((int)(received * 100 / total));
                }));
                runOnUiThread(() -> { current.setText("正在校验 " + label + (resident ? " 并安装到车机…" : " 并写入 U 盘…")); progressBar.setIndeterminate(true); });
                JSONObject q = new JSONObject().put("action", "update-apk").put("repo", repo).put("source", target.getAbsolutePath())
                    .put("name", release.getString("file")).put("size", release.getLong("size")).put("sha256", release.getString("sha256"))
                    .put("oldName", oldName).put("oldToken", oldToken).put("systemLauncher", systemLauncher).put("grant", grant);
                JSONObject reply = BridgeClient.call(q, validationMode);
                if (!reply.optBoolean("ok")) throw new Exception(reply.optString("message"));
                JSONObject refreshed;
                try { refreshed = BridgeClient.call(new JSONObject().put("action", "inventory"), validationMode); }
                catch (Exception ignored) { refreshed = new JSONObject(); }
                updateStorage();
                final JSONObject inventory = refreshed;
                runOnUiThread(() -> {
                    installation.remove(oldName); installation.put(release.optString("file"), selected);
                    if (!inventory.optBoolean("ok")) ready = false;
                    if (inventory.optBoolean("ok")) {
                        apks = inventory.optJSONArray("apks");
                        for (int i = 0; i < apks.length(); i++) {
                            JSONObject a = apks.optJSONObject(i); String n = a.optString("name");
                            if (!installation.containsKey(n)) installation.put(n, a.optBoolean("selected"));
                            if (!a.optBoolean("eligible")) installation.put(n, false);
                        }
                    }
                    setBusy(false); progressBar.setVisibility(View.GONE); status(resident ? "下载和安装处理完成" : "U 盘安装包更新完成 · 勾选后可一键安装");
                    current.setText("✓ " + label + " · " + reply.optString("message")); current.setTextColor(DONE); summary = "✓ 已完成 " + label + " · " + reply.optString("version"); render();
                    largeDialog(new android.app.AlertDialog.Builder(this).setTitle(label + " · 文件更新完成").setMessage(reply.optString("message") + (inventory.optBoolean("ok") ? "" : "。列表读取失败，请点刷新。" )).setPositiveButton("完成", null).show());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false); progressBar.setVisibility(View.GONE); current.setText("更新未完成 · " + label); current.setTextColor(ERROR); render();
                    largeDialog(new android.app.AlertDialog.Builder(this).setTitle("更新未完成").setMessage("已停止更新，请检查网络和 U 盘后重试。\n\n" + RootBridge.cleanError(e)).setPositiveButton("知道了", null).show());
                });
            } finally { target.delete(); }
        });
    }
    private void largeDialog(android.app.AlertDialog dialog) {
        TextView message = (TextView)dialog.findViewById(android.R.id.message); if (message != null) message.setTextSize(24);
        int titleId = getResources().getIdentifier("alertTitle", "id", "android"); TextView heading = (TextView)dialog.findViewById(titleId); if (heading != null) heading.setTextSize(28);
        for (int which : new int[]{android.app.AlertDialog.BUTTON_POSITIVE, android.app.AlertDialog.BUTTON_NEGATIVE}) {
            Button b = dialog.getButton(which); if (b != null) { b.setTextSize(26); b.setMinHeight(d(80)); b.setAllCaps(false); }
        }
    }

    @Override public void onBackPressed() { if (!busy || scanning) super.onBackPressed(); }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
