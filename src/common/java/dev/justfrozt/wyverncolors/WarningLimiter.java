package dev.justfrozt.wyverncolors;

import java.util.HashSet;
import java.util.Set;

/** Session-wide duplicate suppression, independent of the opt-in debug capture. */
public final class WarningLimiter {
    public enum Decision { DETAIL, LIMIT, SUPPRESS }
    private static final int MAX_DETAILS = 32;
    private final Set<String> reported = new HashSet<>();
    private boolean limitReported;

    public Decision record(String key) {
        if (reported.contains(key)) return Decision.SUPPRESS;
        if (reported.size() < MAX_DETAILS) {
            reported.add(key);
            return Decision.DETAIL;
        }
        if (limitReported) return Decision.SUPPRESS;
        limitReported = true;
        return Decision.LIMIT;
    }
}
