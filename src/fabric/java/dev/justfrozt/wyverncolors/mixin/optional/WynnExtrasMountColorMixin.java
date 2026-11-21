package dev.justfrozt.wyverncolors.mixin.optional;

import dev.justfrozt.wyverncolors.RenderDiagnostics;
import dev.justfrozt.wyverncolors.compat.WynnExtrasRenderPriority;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "julianh06.wynnextras.features.inventory.MountColorBackground", remap = false)
public abstract class WynnExtrasMountColorMixin {
    @Unique private static boolean wyverncolors$reported;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private static void wyverncolors$preserveWyvernModelData(ItemStack stack,
            CallbackInfoReturnable<ItemStack> result) {
        if (!WynnExtrasRenderPriority.protects(stack)) return;
        if (!wyverncolors$reported && RenderDiagnostics.enabled()) {
            wyverncolors$reported = true;
            RenderDiagnostics.debug("WynnExtras compatibility preserved a wyvern's original model data.");
        }
        result.setReturnValue(stack);
    }
}
