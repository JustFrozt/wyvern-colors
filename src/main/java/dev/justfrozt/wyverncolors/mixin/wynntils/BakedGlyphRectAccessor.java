package dev.justfrozt.wyverncolors.mixin.wynntils;

import net.minecraft.client.font.BakedGlyphImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.font.BakedGlyphImpl$BakedGlyphRect")
public interface BakedGlyphRectAccessor {
    @Accessor("glyph") BakedGlyphImpl wyverncolors$getGlyph();
}
