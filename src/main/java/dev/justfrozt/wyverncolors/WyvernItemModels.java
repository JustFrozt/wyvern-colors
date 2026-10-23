package dev.justfrozt.wyverncolors;

import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemDisplayContext;

/** Recognize model resources while baking; Minecraft evaluates the pack's selectors. */
public final class WyvernItemModels {
    public enum Role { NONE, REINS, FLUTE }
    public static final Identifier REINS = Identifier.of("minecraft", "item/wynn/mount/wyvern_saddle");
    public static final Identifier FLUTE = Identifier.of("minecraft", "item/wynn/mount/wyvern_whistle");
    private WyvernItemModels() {}

    public static boolean supportsContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.GUI
            || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    public static Role roleOf(Identifier model) {
        if (REINS.equals(model)) return Role.REINS;
        if (FLUTE.equals(model)) return Role.FLUTE;
        return Role.NONE;
    }

    /** Fresh wrappers on reload avoid any global model-number registry. */
    public static ItemModel discover(Identifier source, ItemModel original) {
        Role role = roleOf(source);
        if (role == Role.NONE) return original;
        return (state, stack, manager, context, world, held, seed) ->
            GuiColorRenderer.renderSelected(role, state, stack, original,
                (model, rendered) -> model.update(state, rendered, manager, context, world, held, seed));
    }
}
