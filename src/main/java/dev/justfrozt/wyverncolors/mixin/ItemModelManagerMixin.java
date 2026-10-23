package dev.justfrozt.wyverncolors.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.justfrozt.wyverncolors.ColorFallbackWarnings;
import dev.justfrozt.wyverncolors.GuiColorRenderer;
import dev.justfrozt.wyverncolors.RenderDiagnostics;
import dev.justfrozt.wyverncolors.ServerScope;
import java.util.function.Function;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerMixin {
    @Shadow @Final private Function<Identifier, ItemModel> modelGetter;

    @Inject(method = "update", at = @At("HEAD"))
    private void wyverncolors$observeGuiInput(ItemRenderState state, ItemStack stack,
            ItemDisplayContext displayContext, World world, HeldItemContext heldItemContext,
            int seed, CallbackInfo ci) {
        RenderDiagnostics.managerEntered(stack, displayContext);
        ColorFallbackWarnings.deliverPending();
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/render/item/model/ItemModel;update("
        + "Lnet/minecraft/client/render/item/ItemRenderState;"
        + "Lnet/minecraft/item/ItemStack;"
        + "Lnet/minecraft/client/item/ItemModelManager;"
        + "Lnet/minecraft/item/ItemDisplayContext;"
        + "Lnet/minecraft/client/world/ClientWorld;"
        + "Lnet/minecraft/util/HeldItemContext;I)V"))
    private void wyverncolors$recolorGuiModel(ItemModel vanillaModel, ItemRenderState state,
            ItemStack stack, ItemModelManager resolver, ItemDisplayContext displayContext,
            ClientWorld world, HeldItemContext heldItemContext, int seed,
            Operation<Void> original) {
        RenderDiagnostics.wrappedCallEntered(displayContext);
        if (dev.justfrozt.wyverncolors.WyvernItemModels.supportsContext(displayContext) && ServerScope.isWynncraft()
                && !GuiColorRenderer.recovering()) {
            GuiColorRenderer.renderRoot(state, stack, modelGetter,
                resource -> MinecraftClient.getInstance().getResourceManager().getResource(resource).isPresent(),
                () -> original.call(vanillaModel, state, stack, resolver,
                    displayContext, world, heldItemContext, seed),
                () -> resolver.clearAndUpdate(state, stack, displayContext, world, heldItemContext, seed),
                ColorFallbackWarnings::report, displayContext == ItemDisplayContext.GUI);
            return;
        }
        original.call(vanillaModel, state, stack, resolver, displayContext,
                      world, heldItemContext, seed);
    }
}
