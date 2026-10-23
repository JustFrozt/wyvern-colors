package dev.justfrozt.wyverncolors.mixin.wynntils;

import dev.justfrozt.wyverncolors.compat.BitmapDigitInk;
import net.minecraft.client.font.BitmapFont;
import net.minecraft.client.font.Glyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BitmapFont.class)
public abstract class BitmapDigitSelectionMixin {
    @Inject(method = "getGlyph", at = @At("RETURN"))
    private void wyverncolors$markDigit(int codepoint, CallbackInfoReturnable<Glyph> cir) {
        if ((codepoint >= '0' && codepoint <= '9') || codepoint == '-') {
            if (cir.getReturnValue() instanceof BitmapDigitInk digit) digit.wyverncolors$measureDigit();
        }
    }
}
