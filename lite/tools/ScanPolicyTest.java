package com.wm.usbmanager;
import java.io.*;
import java.nio.file.*;
public final class ScanPolicyTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("scan check " + checks); }
    private static void denied(Runnable work) { boolean failed = false; try { work.run(); } catch (SecurityException e) { failed = true; } check(failed); }
    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("wm-scan").toFile(), f = new File(dir, "sample.apk");
        Files.write(f.toPath(), new byte[]{1, 2});
        ScanPolicy.Cache<String> cache = new ScanPolicy.Cache<>(); int[] reads = {0};
        ScanPolicy.Loader<String> loader = file -> { reads[0]++; return "package metadata"; };
        check(cache.peek(f) == null);
        check(cache.get(f, loader).equals("package metadata"));
        cache.get(f, loader); check(reads[0] == 1);
        ScanPolicy.Selection first = new ScanPolicy.Selection(f, "test.sample", 1); first.require(f, first.token); first.requireIdentity("test.sample", 1); check(true);
        denied(() -> { try { first.require(f, "forged-token"); } catch (IOException e) { throw new RuntimeException(e); } });
        denied(() -> first.requireIdentity("other.package", 1));
        denied(() -> first.requireIdentity("test.sample", 2));
        Files.write(f.toPath(), new byte[]{1, 2, 3});
        denied(() -> { try { first.require(f, first.token); } catch (IOException e) { throw new RuntimeException(e); } });
        check(cache.peek(f) == null); cache.get(f, loader); check(reads[0] == 2);
        ScanPolicy.Selection second = new ScanPolicy.Selection(f, "test.sample", 1);
        check(!first.token.equals(second.token));
        File different = new File(dir, "different.apk"); Files.copy(f.toPath(), different.toPath());
        denied(() -> { try { second.require(different, second.token); } catch (IOException e) { throw new RuntimeException(e); } });
        f.setLastModified(f.lastModified() + 5000); check(cache.peek(f) == null);
        ScanPolicy.Cache<String> unstable = new ScanPolicy.Cache<>(); boolean failed = false;
        try { unstable.get(f, file -> { Files.write(file.toPath(), new byte[]{9}); return "stale"; }); } catch (IOException expected) { failed = true; }
        check(failed && unstable.peek(f) == null);
        System.out.println("Scan assertions passed: " + checks);
    }
}
