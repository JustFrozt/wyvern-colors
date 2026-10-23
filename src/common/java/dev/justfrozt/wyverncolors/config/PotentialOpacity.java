package dev.justfrozt.wyverncolors.config;

/** Opacity is independent of the optional size/position override. */
public final class PotentialOpacity {
    public static final int DEFAULT = 100;
    private PotentialOpacity() {}
    public static int clamp(int percent) { return Math.max(0, Math.min(100, percent)); }
    public static int apply(int argb, int percent) {
        int alpha = Math.round((argb >>> 24) * clamp(percent) / 100f);
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
