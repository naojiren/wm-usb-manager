package com.wm.usbmanager;

import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Locale;

/** Public HTTPS requests with normal platform certificate and hostname verification. */
public final class GitHubRelease {
    public interface Progress { void changed(long received, long total); }
    private static HttpURLConnection connect(String address, boolean json) throws Exception {
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!UpdatePolicy.transport(address)) throw new SecurityException("下载地址不是 GitHub 的 HTTPS 地址");
            HttpURLConnection c = (HttpURLConnection)new URL(address).openConnection();
            ((javax.net.ssl.HttpsURLConnection)c).setSSLSocketFactory(GitHubTrust.sockets());
            c.setConnectTimeout(15000); c.setReadTimeout(25000); c.setInstanceFollowRedirects(false); c.setUseCaches(false);
            c.setRequestProperty("User-Agent", "WM-USB-Manager/1.3.0");
            c.setRequestProperty("Accept", json ? "application/vnd.github+json" : "application/octet-stream");
            c.setRequestProperty("Accept-Encoding", "identity");
            int code;
            try { code = c.getResponseCode(); } catch (IOException e) { c.disconnect(); throw new IOException("无法连接 " + new URL(address).getHost() + "：" + e.getMessage(), e); }
            if (code >= 300 && code <= 399) {
                String location = c.getHeaderField("Location");
                c.disconnect(); if (location == null) throw new IOException("GitHub 返回了无效的下载跳转");
                address = new URL(new URL(address), location).toString(); continue;
            }
            if (code != 200) {
                c.disconnect();
                throw new IOException(code == 403 || code == 429 ? "GitHub 查询次数暂时受限，请稍后重试" : "GitHub 连接失败（" + code + "）");
            }
            return c;
        }
        throw new IOException("GitHub 下载跳转过多");
    }
    private static String small(String address, boolean json) throws Exception {
        HttpURLConnection c = connect(address, json);
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0;) {
                if (bytes.size() + n > 1048576) throw new IOException("GitHub 发布说明过大");
                bytes.write(b, 0, n);
            }
            return new String(bytes.toByteArray(), "UTF-8");
        } finally { c.disconnect(); }
    }
    public static JSONObject latest(String repo) throws Exception {
        String pkg = UpdatePolicy.pkg(repo);
        JSONObject release = new JSONObject(small("https://api.github.com/repos/naojiren/" + repo + "/releases/latest", true));
        if (release.optBoolean("draft") || release.optBoolean("prerelease")) throw new IOException("尚无正式发布版本");
        JSONArray assets = release.getJSONArray("assets"); JSONObject chosen = null; int rank = -1; String sumsUrl = "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i); String name = a.optString("name");
            if (name.equals("SHA256SUMS.txt") && UpdatePolicy.assetUrl(repo, a.optString("browser_download_url"))) sumsUrl = a.getString("browser_download_url");
            int r = UpdatePolicy.assetRank(repo, name);
            if (r > rank && UpdatePolicy.size(a.optLong("size")) && "uploaded".equals(a.optString("state")) && UpdatePolicy.assetUrl(repo, a.optString("browser_download_url"))) { chosen = a; rank = r; }
        }
        if (chosen == null) throw new IOException("最新发布中没有适合本车机的安卓 6 / ARMv7 安装包");
        String checksum = chosen.optString("digest");
        checksum = checksum.startsWith("sha256:") ? checksum.substring(7).toLowerCase(Locale.ROOT) : "";
        if (!UpdatePolicy.sha(checksum) && !sumsUrl.isEmpty()) {
            for (String line : small(sumsUrl, false).split("\\r?\\n")) {
                String[] pieces = line.trim().split("\\s+", 2);
                if (pieces.length == 2 && pieces[1].replaceFirst("^\\*", "").equals(chosen.getString("name")) && UpdatePolicy.sha(pieces[0].toLowerCase(Locale.ROOT))) checksum = pieces[0].toLowerCase(Locale.ROOT);
            }
        }
        if (!UpdatePolicy.sha(checksum)) throw new IOException("发布包缺少校验值，暂不能更新本地文件");
        return new JSONObject().put("repo", repo).put("package", pkg).put("tag", release.getString("tag_name"))
            .put("file", chosen.getString("name")).put("size", chosen.getLong("size")).put("sha256", checksum)
            .put("url", chosen.getString("browser_download_url")).put("release", "https://github.com/naojiren/" + repo + "/releases")
            .put("notes", release.optString("body"));
    }
    public static void download(JSONObject release, File target, Progress callback) throws Exception {
        String repo = release.getString("repo"), name = release.getString("file"), url = release.getString("url");
        long size = release.getLong("size"); String expected = release.getString("sha256");
        if (UpdatePolicy.assetRank(repo, name) < 0 || !UpdatePolicy.assetUrl(repo, url) || !UpdatePolicy.size(size) || !UpdatePolicy.sha(expected)) throw new SecurityException("发布包信息无效，请重新查询");
        HttpURLConnection c = connect(url, false); boolean complete = false;
        try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(target)) {
            MessageDigest sha = MessageDigest.getInstance("SHA-256"); byte[] b = new byte[65536]; long count = 0, last = 0;
            for (int n; (n = in.read(b)) > 0;) {
                count += n; if (count > size || count > UpdatePolicy.MAX_APK) throw new IOException("下载大小与发布记录不符");
                out.write(b, 0, n); sha.update(b, 0, n);
                long now = System.currentTimeMillis();
                if (callback != null && (now - last >= 200 || count == size)) { callback.changed(count, size); last = now; }
            }
            out.getFD().sync(); StringBuilder digest = new StringBuilder();
            for (byte v : sha.digest()) digest.append(String.format(Locale.ROOT, "%02x", v & 255));
            if (count != size || !expected.equals(digest.toString())) throw new IOException("下载不完整或校验失败，原安装包已保留");
            complete = true;
        } finally { c.disconnect(); if (!complete) target.delete(); }
    }
    public static void main(String[] args) throws Exception { System.out.println(latest(args[0])); }
}
