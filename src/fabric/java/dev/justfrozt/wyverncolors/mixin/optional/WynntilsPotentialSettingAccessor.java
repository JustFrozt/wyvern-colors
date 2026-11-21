package dev.justfrozt.wyverncolors.mixin.optional;

import com.wynntils.core.persisted.config.Config;
import com.wynntils.models.mount.type.ConfigMountStat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.wynntils.features.inventory.ItemTextOverlayFeature", remap = false)
public interface WynntilsPotentialSettingAccessor {
    @Accessor("mountItemStat")
    Config<ConfigMountStat> wyverncolors$getMountItemStat();
}
