package dev.justfrozt.wyverncolors.compat;

import java.util.function.IntBinaryOperator;

/** Transparent padding in a bitmap cell, measured once when a digit is baked. */
public record GlyphInkInsets(float left, float top, float right, float bottom) {
    public static GlyphInkInsets scan(int width, int height, float scale, IntBinaryOperator alpha) {
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (alpha.applyAsInt(x, y) == 0) continue;
            minX = Math.min(minX, x); minY = Math.min(minY, y);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
        if (maxX < minX) return null;
        return new GlyphInkInsets(minX * scale, minY * scale,
            (width - maxX - 1) * scale, (height - maxY - 1) * scale);
    }
}
