package com.example.ventryschat.staff;

import com.example.ventryschat.RPDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class RPNameHelper {
    private RPNameHelper() {
    }

    public static String displayName(ServerPlayer player) {
        String full = RPDataManager.getFullName(player.getUUID());
        if (full != null && !full.isBlank()) {
            return full;
        }
        return player.getName().getString();
    }

    public static ServerPlayer findOnlinePlayer(MinecraftServer server, String hint) {
        if (hint == null || hint.isBlank()) {
            return null;
        }
        String trimmed = hint.trim();
        ServerPlayer byName = server.getPlayerList().getPlayerByName(trimmed);
        if (byName != null) {
            return byName;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String rp = RPDataManager.getFullName(p.getUUID());
            if (rp != null && rp.equalsIgnoreCase(trimmed)) {
                return p;
            }
        }
        String lower = trimmed.toLowerCase();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String rp = RPDataManager.getFullName(p.getUUID());
            if (rp != null && rp.toLowerCase().startsWith(lower)) {
                return p;
            }
        }
        return null;
    }
}
