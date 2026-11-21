package dev.justfrozt.wyverncolors;

/** Potential tiers with separate Crimson and Chroma animations. */
public final class PotentialColors {
    private static final int[] UPPER_BOUNDS = {350, 650, 850, 1100, 1500, 1900, 2400, 2900};
    private static final int[] COLORS = {
        0xFFFF626B, 0xFFFFA348, 0xFFFFE45C, 0xFF63E680,
        0xFF57E6E2, 0xFF79A9FF, 0xFFC493FF, 0xFFFFFFFF
    };
    // The Chroma cycle uses red through purple.
    private static final int SPECTRUM_LENGTH = COLORS.length - 1;
    private static final int MILLIS_PER_STEP = 850;
    private static final int CRIMSON_LIMIT = 3400;
    private static final int CRIMSON_CYCLE_MILLIS = 3200;
    private static final int DARK_CRIMSON = 0xFFAA0000;
    private static final int BRIGHT_CRIMSON = 0xFFE63838;

    private PotentialColors() {}

    public static int color(int potential) {
        return color(potential, System.nanoTime() / 1_000_000L);
    }

    static int color(int potential, long millis) {
        for (int index = 0; index < UPPER_BOUNDS.length; index++) {
            if (potential <= UPPER_BOUNDS[index]) return COLORS[index];
        }
        if (potential <= CRIMSON_LIMIT) {
            double phase = (double) Math.floorMod(millis, CRIMSON_CYCLE_MILLIS) / CRIMSON_CYCLE_MILLIS;
            float brightness = (float) ((1 - Math.cos(phase * 2 * Math.PI)) / 2);
            return interpolate(DARK_CRIMSON, BRIGHT_CRIMSON, brightness);
        }

        int elapsed = Math.floorMod(millis, SPECTRUM_LENGTH * MILLIS_PER_STEP);
        int index = elapsed / MILLIS_PER_STEP;
        float fraction = (elapsed % MILLIS_PER_STEP) / (float) MILLIS_PER_STEP;
        int from = COLORS[index];
        int to = COLORS[(index + 1) % SPECTRUM_LENGTH];
        return interpolate(from, to, fraction);
    }

    private static int interpolate(int from, int to, float fraction) {
        return 0xFF000000 | blend(from, to, 16, fraction)
            | blend(from, to, 8, fraction) | blend(from, to, 0, fraction);
    }

    private static int blend(int from, int to, int shift, float fraction) {
        int start = (from >>> shift) & 0xFF;
        int end = (to >>> shift) & 0xFF;
        return Math.round(start + (end - start) * fraction) << shift;
    }
}
