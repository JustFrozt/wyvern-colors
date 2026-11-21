package dev.justfrozt.wyverncolors;

import dev.justfrozt.wyverncolors.compat.GlyphInkInsets;
import dev.justfrozt.wyverncolors.compat.GlyphInkRegistry;
import dev.justfrozt.wyverncolors.config.MountLabelLayout;
import dev.justfrozt.wyverncolors.config.NativePotentialSettings;
import dev.justfrozt.wyverncolors.config.TextInkBounds;
import dev.justfrozt.wyverncolors.config.WyvernSettings;
import dev.justfrozt.wyverncolors.config.PotentialOpacity;
import dev.justfrozt.wyverncolors.mixin.wynntils.BakedGlyphRectAccessor;
import java.util.Map;
import java.util.OptionalInt;
import java.util.WeakHashMap;
import net.minecraft.client.font.TextDrawable;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Uses the same visible-ink anchors as the Fabric integration, on native loaders only. */
public final class NativePotentialRenderer {
    private record Cached(LoreComponent lore, String name, OptionalInt potential) {}
    private static final Map<ItemStack, Cached> CACHE = new WeakHashMap<>();
    private static boolean warned;
    private NativePotentialRenderer() {}

    public static void draw(DrawContext context, TextRenderer font, ItemStack stack, int x, int y) {
        var settings = NativePotentialSettings.active();
        if (settings == null || stack.isEmpty() || !ServerScope.isWynncraft()) return;
        int opacity = WyvernSettings.current().potentialOpacityPercent();
        if (opacity == 0) return;
        try {
            OptionalInt parsed = potential(stack);
            if (parsed.isEmpty()) return;
            int potential = parsed.getAsInt();
            String label = Integer.toString(potential);
            Text text = Text.literal(label);
            Collector collector = new Collector();
            font.prepare(text.asOrderedText(), 0, 0, 0xFFFFFFFF, false, false, 0).draw(collector);
            if (collector.minX >= collector.maxX || collector.minY >= collector.maxY) return;
            TextInkBounds bounds = new TextInkBounds(collector.minX, collector.minY, collector.maxX, collector.maxY).outlined();
            MountLabelLayout layout = settings.overrideEnabled()
                ? MountLabelLayout.calculate(bounds, 0.9f, settings.fontSizePercent(), settings.position())
                : new MountLabelLayout(8 - font.getWidth(text) * .65f / 2, 2, .65f);
            if (settings.overrideEnabled()) {
                RenderDiagnostics.potentialLayoutSelected(settings.fontSizePercent(), settings.position(), layout);
            }
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            try {
                matrices.translate(x + layout.x(), y + layout.y());
                matrices.scale(layout.scale(), layout.scale());
                int outlineColor = PotentialOpacity.apply(0xFF000000, opacity);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        context.drawText(font, text, dx, dy, outlineColor, false);
                    }
                }
                context.drawText(font, text, 0, 0, PotentialOpacity.apply(PotentialColors.color(potential), opacity), false);
            } finally { matrices.popMatrix(); }
        } catch (RuntimeException failure) {
            if (!warned) {
                warned = true;
                org.slf4j.LoggerFactory.getLogger("wyverncolors").warn(
                    "[WyvernColors] Could not render native potential text", failure);
            }
        }
    }

    public static OptionalInt potential(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return OptionalInt.empty();

        String name = stack.getName().getString();
        Cached cached = CACHE.get(stack);
        if (cached == null || cached.lore() != lore || !cached.name().equals(name)) {
            cached = new Cached(lore, name, MountPotentialParser.parse(
                lore.lines().stream().map(Text::getString).toList(), name));
            CACHE.put(stack, cached);
        }
        return cached.potential();
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
