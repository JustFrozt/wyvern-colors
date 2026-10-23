package dev.justfrozt.wyverncolors.mixin.wynntils;

import dev.justfrozt.wyverncolors.compat.BitmapDigitInk;
import dev.justfrozt.wyverncolors.compat.GlyphInkInsets;
import dev.justfrozt.wyverncolors.compat.GlyphInkRegistry;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.font.BitmapFont$BitmapFontGlyph")
public abstract class BitmapDigitInkMixin implements BitmapDigitInk {
    @Shadow @Final private float scaleFactor;
    @Shadow @Final private NativeImage image;
    @Shadow @Final private int x;
    @Shadow @Final private int y;
    @Shadow @Final private int width;
    @Shadow @Final private int height;
    @Unique private volatile boolean wyverncolors$digit;
    @Unique @Override public void wyverncolors$measureDigit() { wyverncolors$digit = true; }

    @Inject(method = "bake", at = @At("RETURN"))
    private void wyverncolors$rememberInk(Glyph.AbstractGlyphBaker baker, CallbackInfoReturnable<BakedGlyph> cir) {
        if (!wyverncolors$digit) return;
        GlyphInkRegistry.remember(cir.getReturnValue(), GlyphInkInsets.scan(width, height, scaleFactor,
            (column, row) -> image.getOpacity(x + column, y + row)));
    }
}
