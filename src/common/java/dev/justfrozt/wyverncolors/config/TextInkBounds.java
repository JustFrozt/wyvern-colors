package dev.justfrozt.wyverncolors.config;

/** Visible glyph bounds in unscaled font coordinates, including the selected shadow. */
public record TextInkBounds(float minX, float minY, float maxX, float maxY) {
    public TextInkBounds {
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(maxX)
                || !Float.isFinite(maxY) || maxX <= minX || maxY <= minY) {
            throw new IllegalArgumentException("Invalid visible font bounds");
        }
    }
    public float width() { return maxX - minX; }
    public float height() { return maxY - minY; }
    public TextInkBounds outlined() { return new TextInkBounds(minX - 1, minY - 1, maxX + 1, maxY + 1); }
}
