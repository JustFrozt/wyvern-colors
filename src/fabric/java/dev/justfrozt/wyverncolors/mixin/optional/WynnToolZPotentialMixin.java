package dev.justfrozt.wyverncolors.mixin.optional;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.justfrozt.wyverncolors.NativePotentialRenderer;
import dev.justfrozt.wyverncolors.PotentialColors;
import dev.justfrozt.wyverncolors.ServerScope;
import dev.justfrozt.wyverncolors.compat.WynnToolZPotential;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xyz.mashtoolz.wtz.features.mount.MountItemOverlay", remap = false)
public abstract class WynnToolZPotentialMixin {
    // WynnToolZ's final name check. Also admit legacy mounts that have a valid potential.
    @WrapOperation(method = "renderOverlayAt", at = @At(value = "INVOKE", ordinal = 5,
        target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
    private static boolean wyverncolors$includeLegacyMount(String name, CharSequence word,
            Operation<Boolean> original, @Local(argsOnly = true) ItemStack stack) {
        return original.call(name, word) || (ServerScope.isWynncraft() && WynnToolZPotential.active()
            && NativePotentialRenderer.potential(stack).isPresent());
    }

    // Both branches have joined before this field read, and the label has yet to be measured.
    @Inject(method = "renderOverlayAt", at = @At(value = "FIELD",
        target = "Lxyz/mashtoolz/wtz/config/WTZConfig;mountItemOverlayBarsEnabled:Z"))
    private static void wyverncolors$useTooltipPotential(DrawContext context, int x, int y,
            ItemStack stack, boolean inventorySlot, CallbackInfo callback,
            @Local(index = 9) LocalRef<String> label, @Local(index = 10) LocalDoubleRef value) {
        if (!ServerScope.isWynncraft() || !WynnToolZPotential.active()) return;
        var potential = NativePotentialRenderer.potential(stack);
        if (potential.isPresent()) {
            int actual = potential.getAsInt();
            label.set(Integer.toString(actual));
            value.set(actual);
        }
    }

    @WrapOperation(method = "renderOverlayAt", at = @At(value = "INVOKE",
        target = "Lxyz/mashtoolz/wtz/features/mount/MountItemOverlay;potentialColor(D)I"))
    private static int wyverncolors$colorPotential(double value, Operation<Integer> original,
            @Local(argsOnly = true) ItemStack stack) {
        if (ServerScope.isWynncraft() && WynnToolZPotential.active()) {
            var potential = NativePotentialRenderer.potential(stack);
            if (potential.isPresent()) return PotentialColors.color(potential.getAsInt());
        }
        return original.call(value);
    }
}
