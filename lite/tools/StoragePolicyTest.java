package com.wm.usbmanager;
public final class StoragePolicyTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("storage check " + checks); }
    public static void main(String[] args) {
        long unit = 1048576L;
        StoragePolicy.Snapshot normal = StoragePolicy.read(2000 * unit, 400 * unit, () -> "10\n524288000");
        check(normal.reserveKnown && normal.reserve == 200 * unit && normal.aboveReserve == 200 * unit);
        StoragePolicy.Snapshot cap = StoragePolicy.read(10000 * unit, 400 * unit, () -> "10\r\n524288000\r\n");
        check(cap.reserveKnown && cap.reserve == 500 * unit && cap.aboveReserve == 0);
        StoragePolicy.Snapshot denied = StoragePolicy.read(2000 * unit, 400 * unit, () -> { throw new SecurityException("Unable to find app for caller when getting content provider"); });
        check(!denied.reserveKnown && denied.available == 400 * unit && denied.total == 2000 * unit);
        StoragePolicy.Snapshot timedOut = StoragePolicy.read(2000 * unit, 400 * unit, () -> { throw new java.io.IOException("timeout"); });
        check(!timedOut.reserveKnown && timedOut.available == 400 * unit);
        for (String invalid : new String[]{"", "null\nnull", "10", "denied\n10\n524288000", "101\n524288000", "-1\n524288000", "10\n-1", "not a number\n524288000", "999999999999999999999999\n1"}) {
            StoragePolicy.Snapshot s = StoragePolicy.read(2000 * unit, 400 * unit, () -> invalid);
            check(!s.reserveKnown && s.available == 400 * unit);
        }
        StoragePolicy.Snapshot zero = StoragePolicy.read(2000 * unit, 400 * unit, () -> "0\n0");
        check(zero.reserveKnown && zero.reserve == 0 && zero.aboveReserve == 400 * unit);
        System.out.println("Storage assertions passed: " + checks);
    }
}
