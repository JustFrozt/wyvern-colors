package dev.justfrozt.wyverncolors;

import dev.justfrozt.wyverncolors.config.PotentialSettings;
import dev.justfrozt.wyverncolors.config.PotentialPosition;
import dev.justfrozt.wyverncolors.config.MountLabelLayout;
import dev.justfrozt.wyverncolors.config.WyvernSettings;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in, bounded mount diagnostics. Never changes a stack. */
public final class RenderDiagnostics {
    private static final int MAX_SNAPSHOTS = 64;
    private static final Logger LOGGER = LoggerFactory.getLogger("wyverncolors");
    private static final Set<String> CANDIDATES = new HashSet<>();
    private static final Set<String> DECISIONS = new HashSet<>();
    private static final Map<ItemStack, Observed> OBSERVED = new WeakHashMap<>();
    private record Observed(LoreComponent lore, CustomModelDataComponent data,
                            Identifier itemModel, String address) {}
    private static boolean managerSeen;
    private static boolean wrappedCallSeen;
    private static boolean limitReported;
    private static boolean potentialLayoutSeen;

    private RenderDiagnostics() {}

    public static boolean enabled() { return WyvernSettings.current().debugLogging(); }

    public static void debug(String message, Object... arguments) {
        if (enabled()) LOGGER.info("[WyvernColors] " + message, arguments);
    }

    /** Starting a new capture also observes stacks that were already visible. */
    public static void settingsChanged() {
        CANDIDATES.clear();
        DECISIONS.clear();
        OBSERVED.clear();
        managerSeen = false;
        wrappedCallSeen = false;
        limitReported = false;
        potentialLayoutSeen = false;
        if (enabled()) {
            LOGGER.info("[WyvernColors] Debug mode enabled; up to {} mount snapshots. Open an inventory to capture appearances.", MAX_SNAPSHOTS);
            debug("Runtime: Java={}, OS={}, architecture={}", Runtime.version(),
                System.getProperty("os.name"), System.getProperty("os.arch"));
            for (String id : List.of("minecraft", "fabricloader", "wynntils", "modmenu")) {
                String version = LoaderPlatform.version(id);
                debug("{}={}", id, version);
            }
            debug("Shared Wynntils potential-text settings available={}", PotentialSettings.available());
        }
    }

    public static void startup() {
        String version = LoaderPlatform.version("wyverncolors");
        LOGGER.info("[WyvernColors] {} initialized; debug mode {}", version, enabled() ? "on" : "off");
        settingsChanged();
    }

    public static void managerEntered(ItemStack stack, ItemDisplayContext context) {
        if (!enabled() || context != ItemDisplayContext.GUI) return;
        if (!managerSeen) {
            managerSeen = true;
            LOGGER.info("[WyvernColors] GUI manager entry hook reached; item={}", Registries.ITEM.getId(stack.getItem()));
        }
        if (CANDIDATES.size() >= MAX_SNAPSHOTS) {
            if (!limitReported) {
                limitReported = true;
                LOGGER.info("[WyvernColors] Candidate snapshot limit reached; further snapshots suppressed.");
            }
            return;
        }
        CustomModelDataComponent data = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        Float model = data == null ? null : data.getFloat(0);
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        Identifier itemModel = stack.get(DataComponentTypes.ITEM_MODEL);
        String address = serverAddress();
        Observed previous = OBSERVED.get(stack);
        if (previous != null && previous.lore() == lore && previous.data() == data
                && Objects.equals(previous.itemModel(), itemModel)
                && Objects.equals(previous.address(), address)) return;
        OBSERVED.put(stack, new Observed(lore, data, itemModel, address));
        List<String> lines = lore == null ? List.of() : lore.lines().stream().map(Text::getString).toList();
        Optional<Appearance> appearance = AppearanceParser.parse(lines);
        boolean mountLore = lines.stream().skip(1).anyMatch(line -> line.contains("Potential"));
        if (appearance.isEmpty() && !mountLore) return;
        String signature = Registries.ITEM.getId(stack.getItem()) + "|" + itemModel + "|" + data + "|" + address + "|" + lines.stream().skip(1).toList();
        if (!CANDIDATES.add(signature)) return;
        boolean serverAllowed = ServerScope.isWynncraftAddress(address);
        String reason;
        if (!serverAllowed) reason = "server-scope-rejected";
        else if (lore == null) reason = "no-lore";
        else if (appearance.isEmpty()) reason = "appearance-not-found-or-conflicting";
        else reason = "appearance-ready-awaiting-native-model-selection";
        int snapshot = CANDIDATES.size();
        LOGGER.info("[WyvernColors] candidate={} item={} itemModel={} modelData={} server={} serverAllowed={} resolver={} reason={}",
            snapshot, Registries.ITEM.getId(stack.getItem()), stack.get(DataComponentTypes.ITEM_MODEL),
            data, address == null ? "<none>" : address, serverAllowed,
            appearance.map(Appearance::key).orElse("<none>"), reason);
        // The editable title and custom name are deliberately omitted even from diagnostics.
        for (int i = 1; i < Math.min(lines.size(), 64); i++) {
            String line = lines.get(i);
            if (line.length() > 2048) line = line.substring(0, 2048) + "[truncated]";
            LOGGER.info("[WyvernColors] candidate={} lore[{}] raw={} normalized={}",
                snapshot, i, quote(line), quote(AppearanceParser.normalize(line)));
        }
    }

    public static void wrappedCallEntered(ItemDisplayContext context) {
        if (!enabled()) return;
        if (context == ItemDisplayContext.GUI && !wrappedCallSeen) {
            wrappedCallSeen = true;
            LOGGER.info("[WyvernColors] GUI wrapped ItemModel.update call reached.");
        }
    }

    public static void modelSelected(Appearance appearance, Identifier model,
            Object vanillaModel, Object coloredModel) {
        if (!enabled()) return;
        String signature = appearance.key() + "|" + model + "|" + modelClass(vanillaModel) + "|" + modelClass(coloredModel);
        if (DECISIONS.size() < MAX_SNAPSHOTS && DECISIONS.add(signature)) {
            LOGGER.info("[WyvernColors] selected={} model={} vanillaModelClass={} coloredModelClass={} willUseColoredModel={}",
                appearance.key(), model, modelClass(vanillaModel), modelClass(coloredModel), coloredModel != null);
        }
    }

    public static void potentialLayoutSelected(int percent, PotentialPosition position, MountLabelLayout layout) {
        if (!enabled() || potentialLayoutSeen) return;
        potentialLayoutSeen = true;
        LOGGER.info("[WyvernColors] Potential-text renderer active: size={}%, position={}, offset=({},{}), scale={}",
            percent, position, layout.x(), layout.y(), layout.scale());
    }

    private static String serverAddress() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo server = client == null ? null : client.getCurrentServerEntry();
        return server == null ? null : server.address;
    }

    private static String modelClass(Object model) {
        return model == null ? "<null>" : model.getClass().getName();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}
