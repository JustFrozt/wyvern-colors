package dev.justfrozt.wyverncolors;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class ServerScope {
    private ServerScope() {}

    public static boolean isWynncraft() {
        ServerInfo server = MinecraftClient.getInstance().getCurrentServerEntry();
        return server != null && isWynncraftAddress(server.address);
    }

    static boolean isWynncraftAddress(String address) {
        if (address == null) return false;
        String host = address.strip().toLowerCase(Locale.ROOT);
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        return host.equals("wynncraft.com") || host.endsWith(".wynncraft.com");
    }
}
