package org.chiterok.grandDuels.utils;

import java.util.Locale;

public final class TimeUtil {

    private TimeUtil() {}

    /** Formats seconds as mm:ss. */
    public static String mmss(long totalSeconds) {
        long s = Math.max(0, totalSeconds);
        return String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
    }

    /** Formats milliseconds as seconds with one decimal, e.g. 12.3. */
    public static String seconds(long millis) {
        return String.format(Locale.ROOT, "%.1f", Math.max(0, millis) / 1000.0);
    }
}
