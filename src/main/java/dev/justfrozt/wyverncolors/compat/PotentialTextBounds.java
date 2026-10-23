package dev.justfrozt.wyverncolors.compat;

import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.type.TextShadow;
import dev.justfrozt.wyverncolors.config.TextInkBounds;
import dev.justfrozt.wyverncolors.mixin.wynntils.BakedGlyphRectAccessor;
import net.minecraft.client.font.TextDrawable;
import net.minecraft.client.font.TextRenderer;

public final class PotentialTextBounds {
    private PotentialTextBounds() {}
    public static TextInkBounds measure(TextRenderer font, TextRenderTask task) {
        Collector collector = new Collector();
        TextShadow shadow = task.getSetting().shadow();
        font.prepare(task.getText().getComponent().asOrderedText(), 0, 0, 0xFFFFFFFF,
            shadow == TextShadow.NORMAL, false, 0).draw(collector);
        if (collector.minX >= collector.maxX || collector.minY >= collector.maxY) return null;
        TextInkBounds bounds = new TextInkBounds(collector.minX, collector.minY, collector.maxX, collector.maxY);
        return shadow == TextShadow.OUTLINE ? bounds.outlined() : bounds;
    }

    private static final class Collector implements TextRenderer.GlyphDrawer {
        private float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        @Override public void drawGlyph(TextDrawable.DrawnGlyphRect glyph) {
            GlyphInkInsets trim = glyph instanceof BakedGlyphRectAccessor access && !glyph.style().isItalic()
                ? GlyphInkRegistry.get(access.wyverncolors$getGlyph()) : null;
            include(glyph.getEffectiveMinX() + (trim == null ? 0 : trim.left()),
                glyph.getEffectiveMinY() + (trim == null ? 0 : trim.top()),
                glyph.getEffectiveMaxX() - (trim == null ? 0 : trim.right()),
                glyph.getEffectiveMaxY() - (trim == null ? 0 : trim.bottom()));
        }
        @Override public void drawRectangle(TextDrawable rectangle) {
            include(rectangle.getEffectiveMinX(), rectangle.getEffectiveMinY(),
                rectangle.getEffectiveMaxX(), rectangle.getEffectiveMaxY());
        }
        private void include(float left, float top, float right, float bottom) {
            minX = Math.min(minX, left); minY = Math.min(minY, top);
            maxX = Math.max(maxX, right); maxY = Math.max(maxY, bottom);
        }
    }
}
