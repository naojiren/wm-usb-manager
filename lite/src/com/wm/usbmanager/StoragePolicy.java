package com.wm.usbmanager;

/** Settings access is optional: provider identity failures must never abort inventory. */
public final class StoragePolicy {
    public interface Reader { String read() throws Exception; }
    public static final class Snapshot {
        public final long total, available, reserve, aboveReserve;
        public final boolean reserveKnown;
        Snapshot(long total, long available, long reserve, boolean known) {
            this.total = total; this.available = available; this.reserve = reserve;
            reserveKnown = known; aboveReserve = Math.max(0, available - reserve);
        }
    }
    public static Snapshot read(long total, long available, Reader reader) {
        if (total <= 0 || available < 0 || available > total) throw new IllegalArgumentException("Invalid filesystem capacity");
        try {
            String[] lines = reader.read().replace("\r", "").trim().split("\n");
            if (lines.length != 2) throw new IllegalArgumentException("Incomplete storage thresholds");
            long percent = Long.parseLong(lines[0].trim()), cap = Long.parseLong(lines[1].trim());
            if (percent < 0 || percent > 100 || cap < 0) throw new IllegalArgumentException("Invalid storage thresholds");
            long reserve = Math.min(total / 100 * percent + total % 100 * percent / 100, cap);
            return new Snapshot(total, available, reserve, true);
        } catch (Exception unavailable) {
            return new Snapshot(total, available, 0, false);
        }
    }
}
