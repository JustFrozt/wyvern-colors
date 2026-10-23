package dev.justfrozt.wyverncolors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** One local chat notification and at most 32 distinct details per game session. */
public final class ColorFallbackWarnings {
    private static final Logger LOGGER = LoggerFactory.getLogger("wyverncolors");
    private static final WarningLimiter LIMITER = new WarningLimiter();
    private static String pendingReason;
    private static boolean notified;

    private ColorFallbackWarnings() {}

    public static void report(String code, String identity, String details, RuntimeException error) {
        var decision = LIMITER.record(code + "|" + identity);
        if (decision == WarningLimiter.Decision.DETAIL) {
            String message = "[WyvernColors] Color fallback (" + code + "): " + details
                + ". Keeping the original Wynncraft icon for affected items; supported appearances remain enabled.";
            if (error == null) LOGGER.warn(message); else LOGGER.warn(message, error);
        } else if (decision == WarningLimiter.Decision.LIMIT) {
            LOGGER.warn("[WyvernColors] Further distinct color-fallback details suppressed for this game session. "
                + "Original Wynncraft icons remain available; supported appearances remain enabled.");
        }
        if (!notified && pendingReason == null) pendingReason = code;
        deliverPending();
    }

    public static void deliverPending() {
        if (notified || pendingReason == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        Text reason = Text.translatable("message.wyverncolors.reason." + pendingReason);
        client.inGameHud.getChatHud().addMessage(
            Text.translatable("message.wyverncolors.colorFallback", reason).formatted(Formatting.YELLOW));
        notified = true;
        pendingReason = null;
    }
}
