package dev.justfrozt.wyverncolors.mixin;

import dev.justfrozt.wyverncolors.WyvernItemModels;
import net.minecraft.client.render.item.model.BasicItemModel;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasicItemModel.Unbaked.class)
public abstract class BasicItemModelUnbakedMixin {
    @Shadow public abstract Identifier model();

    @Inject(method = "bake", at = @At("RETURN"), cancellable = true)
    private void wyverncolors$discoverModel(ItemModel.BakeContext context,
            CallbackInfoReturnable<ItemModel> cir) {
        ItemModel original = cir.getReturnValue();
        ItemModel discovered = WyvernItemModels.discover(model(), original);
        if (discovered != original) cir.setReturnValue(discovered);
    }
}
