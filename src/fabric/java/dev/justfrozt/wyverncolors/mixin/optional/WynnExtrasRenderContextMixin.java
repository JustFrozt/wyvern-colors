package dev.justfrozt.wyverncolors.mixin.optional;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.justfrozt.wyverncolors.compat.WynnExtrasRenderPriority;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemModelManager.class)
public abstract class WynnExtrasRenderContextMixin {
    @WrapMethod(method = "update")
    private void wyverncolors$trackOriginalMount(ItemRenderState state, ItemStack stack,
            ItemDisplayContext context, World world, HeldItemContext heldItemContext,
            int seed, Operation<Void> original) {
        ItemStack previous = WynnExtrasRenderPriority.begin(stack, context);
        try {
            original.call(state, stack, context, world, heldItemContext, seed);
        } finally {
            WynnExtrasRenderPriority.restore(previous);
        }
    }
}
