package dev.justfrozt.wyverncolors.mixin.wynntils;

import com.wynntils.core.persisted.config.ConfigManager;
import dev.justfrozt.wyverncolors.config.PotentialSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ConfigManager.class, remap = false)
public abstract class PotentialPositionMigrationMixin {
    @Inject(method = "loadConfigOptions", at = @At("RETURN"))
    private void wyverncolors$migratePosition(boolean resetIfNotFound, boolean initOverlayGroups, CallbackInfo ci) {
        var settings = PotentialSettings.backend();
        if (settings != null) settings.migrateLegacyPosition();
    }
}
