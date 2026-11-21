package dev.justfrozt.wyverncolors.mixin.optional;

import com.wynntils.features.inventory.ItemTextOverlayFeature;
import com.wynntils.models.mount.type.ConfigMountStat;
import dev.justfrozt.wyverncolors.LoaderPlatform;
import dev.justfrozt.wyverncolors.ServerScope;
import dev.justfrozt.wyverncolors.config.PotentialSettings.Source;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.wynntils.features.inventory.ItemTextOverlayFeature$MountItemOverlay", remap = false)
public abstract class WynntilsPotentialVisibilityMixin {
    @Shadow @Final private ItemTextOverlayFeature this$0;

    @Inject(method = "isTextOverlayEnabled", at = @At("RETURN"), cancellable = true)
    private void wyverncolors$deferPotential(CallbackInfoReturnable<Boolean> result) {
        if (!result.getReturnValue() || !ServerScope.isWynncraft()) return;
        var stat = ((WynntilsPotentialSettingAccessor) (Object) this$0).wyverncolors$getMountItemStat().get();
        if (stat == ConfigMountStat.POTENTIAL && LoaderPlatform.potentialSource() != Source.WYNNTILS) {
            result.setReturnValue(false);
        }
    }
}
