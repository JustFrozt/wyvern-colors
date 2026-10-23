package dev.justfrozt.wyverncolors.config;

/** Exact slot-local anchor of the visible glyphs, recomputed after scaling to fit. */
public record MountLabelLayout(float x, float y, float scale) {
    /** Stable base sizes: an upstream font-scale setting cannot multiply ours. */
    public static MountLabelLayout potential(TextInkBounds bounds, String text,
                                            int sizePercent, PotentialPosition position) {
        return calculate(bounds, text.length() >= 4 ? 0.75f : 0.9f, sizePercent, position);
    }

    public static MountLabelLayout calculate(TextInkBounds bounds, float originalScale,
            int percent, PotentialPosition position) {
        if (bounds == null || position == null || !Float.isFinite(originalScale) || originalScale <= 0) {
            throw new IllegalArgumentException("Invalid font measurements");
        }
        float scale = Math.min(originalScale * PotentialSettings.clampSize(percent) / 100f,
            Math.min(16f / bounds.width(), 16f / bounds.height()));
        float x = position.horizontalAnchor() * (16 - bounds.width() * scale) - bounds.minX() * scale;
        float y = position.bottom() ? 16 - bounds.maxY() * scale : -bounds.minY() * scale;
        return new MountLabelLayout(x, y, scale);
    }
}
