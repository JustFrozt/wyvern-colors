package dev.justfrozt.wyverncolors.mixin;

import dev.justfrozt.wyverncolors.LoaderPlatform;
import dev.justfrozt.wyverncolors.NativePotentialRenderer;
import dev.justfrozt.wyverncolors.config.PotentialSettings;
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
        var source = LoaderPlatform.potentialSource();
        if (source == null || source == PotentialSettings.Source.WYVERN_COLORS) {
            NativePotentialRenderer.draw((DrawContext) (Object) this, font, stack, x, y);
        }
    }
}
