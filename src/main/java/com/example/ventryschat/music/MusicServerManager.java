package com.example.ventryschat.music;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestionnaire serveur : zones actives, expiration, sync réseau.
 * Ne manipule jamais le volume client.
 */
public final class MusicServerManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, MusicZone> ZONES = new LinkedHashMap<>();
    private static int tickCounter;

    private MusicServerManager() {
    }

    public static void clear() {
        ZONES.clear();
        tickCounter = 0;
    }

    public static Collection<MusicZone> allZones() {
        return List.copyOf(ZONES.values());
    }

    public static Optional<MusicZone> play(
            ServerPlayer source,
            String trackId,
            float radius
    ) {
        Optional<MusicCatalog.Track> trackOpt = MusicCatalog.get(trackId);
        if (trackOpt.isEmpty()) {
            return Optional.empty();
        }
        MusicCatalog.Track track = trackOpt.get();
        float r = Math.max(1.0F, Math.min(512.0F, radius));
        Vec3 pos = source.position();
        MusicZone zone = new MusicZone(
            UUID.randomUUID(),
            track.id(),
            track.sound(),
            source.level.dimension(),
            pos.x,
            pos.y,
            pos.z,
            r,
            System.currentTimeMillis(),
            track.durationMs()
        );
        ZONES.put(zone.zoneId, zone);
        MusicNetwork.broadcastUpsert(source.getServer(), zone);
        LOGGER.info("Music zone {} track={} r={} by {}", zone.zoneId, trackId, r, source.getGameProfile().getName());
        return Optional.of(zone);
    }

    public static int stopAll(@Nullable MinecraftServer server) {
        int n = ZONES.size();
        List<UUID> ids = new ArrayList<>(ZONES.keySet());
        ZONES.clear();
        if (server != null) {
            for (UUID id : ids) {
                MusicNetwork.broadcastRemove(server, id);
            }
        }
        return n;
    }

    public static boolean stop(UUID zoneId, @Nullable MinecraftServer server) {
        MusicZone removed = ZONES.remove(zoneId);
        if (removed == null) {
            return false;
        }
        if (server != null) {
            MusicNetwork.broadcastRemove(server, zoneId);
        }
        return true;
    }

    /** Arrête la zone la plus proche du joueur (sinon toutes). */
    public static int stopNearOrAll(ServerPlayer player) {
        MusicZone nearest = null;
        double best = Double.MAX_VALUE;
        for (MusicZone z : ZONES.values()) {
            if (!z.dimension.equals(player.level.dimension())) {
                continue;
            }
            double d = player.position().distanceToSqr(z.center());
            if (d < best) {
                best = d;
                nearest = z;
            }
        }
        if (nearest != null) {
            stop(nearest.zoneId, player.getServer());
            return 1;
        }
        return stopAll(player.getServer());
    }

    public static void onPlayerJoin(ServerPlayer player) {
        List<MusicZone> inDim = new ArrayList<>();
        for (MusicZone z : ZONES.values()) {
            if (z.dimension.equals(player.level.dimension())) {
                inDim.add(z);
            }
        }
        MusicNetwork.sendSnapshot(player, inDim);
    }

    public static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 20 != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, MusicZone>> it = ZONES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, MusicZone> e = it.next();
            if (e.getValue().isExpired(now)) {
                it.remove();
                MusicNetwork.broadcastRemove(server, e.getKey());
            }
        }
    }

    public static void notifyStaff(ServerPlayer staff, String message) {
        staff.sendMessage(new TextComponent(message), staff.getUUID());
    }
}
