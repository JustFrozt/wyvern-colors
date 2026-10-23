package dev.justfrozt.wyverncolors.compat;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.font.BakedGlyph;

/** Font reloads naturally release old entries; no texture edits or GPU reads. */
public final class GlyphInkRegistry {
    private static final Map<BakedGlyph, GlyphInkInsets> INSETS = Collections.synchronizedMap(new WeakHashMap<>());
    private GlyphInkRegistry() {}
    public static void remember(BakedGlyph glyph, GlyphInkInsets insets) {
        if (insets != null) INSETS.put(glyph, insets);
    }
    public static GlyphInkInsets get(BakedGlyph glyph) { return INSETS.get(glyph); }
}
