package dev.justfrozt.wyverncolors.mixin;

import dev.justfrozt.wyverncolors.GuiColorRenderer;
import net.minecraft.client.render.model.BakedModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedModelManager.class)
public abstract class ColorResourceReloadMixin {
    @Inject(method = "upload", at = @At("RETURN"))
    private void wyverncolors$retryColorsAfterReload(CallbackInfo ci) {
        // upload runs in the apply phase, after the new baked models are installed.
        // Warning suppression remains session-wide; a reload never spams chat.
        GuiColorRenderer.resourcesReloaded();
    }
}
