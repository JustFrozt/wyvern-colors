package dev.justfrozt.wyverncolors.mixin;

import dev.justfrozt.wyverncolors.NativePotentialRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public abstract class NativePotentialOverlayMixin {
    @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
        at = @At("RETURN"))
    private void wyverncolors$drawNativePotential(TextRenderer font, ItemStack stack,
            int x, int y, String count, CallbackInfo ci) {
        NativePotentialRenderer.draw((DrawContext) (Object) this, font, stack, x, y);
    }
}
