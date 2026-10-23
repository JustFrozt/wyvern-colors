package dev.justfrozt.wyverncolors.mixin;

import java.util.List;
import net.minecraft.client.render.item.model.CompositeItemModel;
import net.minecraft.client.render.item.model.ItemModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompositeItemModel.class)
public interface CompositeItemModelAccessor {
    @Accessor("models") List<ItemModel> wyverncolors$getModels();
}
