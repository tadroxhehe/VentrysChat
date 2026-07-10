package com.example.ventryschat.staff;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Evite les téléportations en boucle quand le joueur reste dans le volume du portail. */
public final class WarpPortalCooldown {

    private static final long COOLDOWN_MS = 3_000L;
    private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();

    private WarpPortalCooldown() {
    }

    public static boolean isOnCooldown(ServerPlayer player) {
        Long last = LAST_USE.get(player.getUUID());
        return last != null && System.currentTimeMillis() - last < COOLDOWN_MS;
    }

    public static void mark(ServerPlayer player) {
        LAST_USE.put(player.getUUID(), System.currentTimeMillis());
    }

    public static void clear(ServerPlayer player) {
        LAST_USE.remove(player.getUUID());
    }
}
