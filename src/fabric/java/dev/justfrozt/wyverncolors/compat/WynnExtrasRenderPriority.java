package dev.justfrozt.wyverncolors.compat;

import dev.justfrozt.wyverncolors.MountAppearanceResolver;
import dev.justfrozt.wyverncolors.ServerScope;
import dev.justfrozt.wyverncolors.WyvernItemModels;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;

/** Tracks the original stack during a Wyvern Colors draw. */
public final class WynnExtrasRenderPriority {
    private static final ThreadLocal<ItemStack> PROTECTED_STACK = new ThreadLocal<>();

    private WynnExtrasRenderPriority() {}

    public static ItemStack begin(ItemStack stack, ItemDisplayContext context) {
        ItemStack previous = PROTECTED_STACK.get();
        if (ownsModelSelectors(stack, context)) {
            PROTECTED_STACK.set(stack);
        } else if (previous != null) {
            PROTECTED_STACK.remove();
        }
        return previous;
    }

    public static void restore(ItemStack previous) {
        if (previous == null) PROTECTED_STACK.remove();
        else PROTECTED_STACK.set(previous);
    }

    public static boolean protects(ItemStack stack) {
        return stack != null && stack == PROTECTED_STACK.get();
    }

    private static boolean ownsModelSelectors(ItemStack stack, ItemDisplayContext context) {
        if (!WyvernItemModels.supportsContext(context) || stack.isEmpty()) return false;
        var data = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (data == null || data.strings().isEmpty() || data.strings().getFirst().isEmpty()) return false;
        if (!ServerScope.isWynncraft()) return false;

        // Before model selection, this status identifies a valid wyvern appearance with mount lore.
        return MountAppearanceResolver.inspect(stack, WyvernItemModels.Role.NONE).status()
            == MountAppearanceResolver.Status.UNSUPPORTED_MODEL;
    }
}
