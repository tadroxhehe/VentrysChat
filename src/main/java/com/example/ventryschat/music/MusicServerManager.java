package com.example.ventryschat.music;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    /** Durée par défaut pour une URL (5 min) si non précisée. */
    public static final long DEFAULT_URL_DURATION_MS = 300_000L;
    private static final ResourceLocation PLACEHOLDER_SOUND =
        new ResourceLocation("ventryschat", "music.bataille");
    private static int tickCounter;

    private MusicServerManager() {
    }

    public static void clear() {
        clear(null);
    }

    public static void clear(@Nullable net.minecraft.server.MinecraftServer server) {
        if (server != null && !ZONES.isEmpty()) {
            List<UUID> ids = new ArrayList<>(ZONES.keySet());
            ZONES.clear();
            for (UUID id : ids) {
                MusicNetwork.broadcastRemove(server, id);
            }
        } else {
            ZONES.clear();
        }
        tickCounter = 0;
    }

    public static Collection<MusicZone> allZones() {
        return List.copyOf(ZONES.values());
    }

    public static boolean isHttpUrl(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return v.startsWith("http://") || v.startsWith("https://");
    }

    /** Piste catalogue ou URL http(s). */
    public static Optional<MusicZone> play(
            ServerPlayer source,
            String trackOrUrl,
            float radius,
            @Nullable Long durationMsOverride
    ) {
        float r = Math.max(1.0F, Math.min(512.0F, radius));
        Vec3 pos = source.position();
        String raw = trackOrUrl == null ? "" : trackOrUrl.trim();

        if (isHttpUrl(raw)) {
            if (urlRejectReason(raw) != null) {
                return Optional.empty();
            }
            long duration = durationMsOverride != null
                ? Math.max(5_000L, Math.min(3_600_000L, durationMsOverride))
                : DEFAULT_URL_DURATION_MS;
            String label = shortUrlLabel(raw);
            MusicZone zone = new MusicZone(
                UUID.randomUUID(),
                label,
                PLACEHOLDER_SOUND,
                raw,
                source.level.dimension(),
                pos.x,
                pos.y,
                pos.z,
                r,
                System.currentTimeMillis(),
                duration
            );
            ZONES.put(zone.zoneId, zone);
            MusicNetwork.broadcastUpsert(source.getServer(), zone);
            LOGGER.info("Music URL zone {} url={} r={} by {}", zone.zoneId, raw, r, source.getGameProfile().getName());
            return Optional.of(zone);
        }

        Optional<MusicCatalog.Track> trackOpt = MusicCatalog.get(raw);
        if (trackOpt.isEmpty()) {
            return Optional.empty();
        }
        MusicCatalog.Track track = trackOpt.get();
        long duration = durationMsOverride != null
            ? Math.max(5_000L, Math.min(3_600_000L, durationMsOverride))
            : track.durationMs();
        MusicZone zone = new MusicZone(
            UUID.randomUUID(),
            track.id(),
            track.sound(),
            "",
            source.level.dimension(),
            pos.x,
            pos.y,
            pos.z,
            r,
            System.currentTimeMillis(),
            duration
        );
        ZONES.put(zone.zoneId, zone);
        MusicNetwork.broadcastUpsert(source.getServer(), zone);
        LOGGER.info("Music zone {} track={} r={} by {}", zone.zoneId, raw, r, source.getGameProfile().getName());
        return Optional.of(zone);
    }

    /** Compat ancienne signature. */
    public static Optional<MusicZone> play(ServerPlayer source, String trackId, float radius) {
        return play(source, trackId, radius, null);
    }

    /**
     * Null si l'URL est acceptable ; sinon message d'erreur joueur.
     * Pages de streaming (YouTube…) refusées — lien direct vers un fichier audio (mp3/ogg/wav…).
     */
    @Nullable
    public static String urlRejectReason(String url) {
        if (url == null || url.isBlank()) {
            return "URL vide.";
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null) {
                return "URL invalide.";
            }
            String s = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(s) && !"https".equals(s)) {
                return "URL refusée (http/https uniquement).";
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "URL invalide (hôte manquant).";
            }
            String h = host.toLowerCase(Locale.ROOT);
            if (h.equals("youtu.be")
                    || h.contains("youtube.com")
                    || h.contains("youtube-nocookie.com")
                    || h.contains("spotify.com")
                    || h.contains("soundcloud.com")
                    || h.contains("music.apple.com")
                    || h.contains("deezer.com")
                    || h.contains("bandcamp.com")
                    || h.contains("tiktok.com")
                    || h.contains("twitch.tv")) {
                return "YouTube / Spotify / etc. non supportés. "
                    + "Utilise un lien direct vers un fichier .mp3 / .ogg / .wav (ex. SoundHelix, Discord CDN).";
            }
            return null;
        } catch (Exception e) {
            return "URL invalide.";
        }
    }

    private static String shortUrlLabel(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path != null && !path.isEmpty() && !"/".equals(path)) {
                int slash = path.lastIndexOf('/');
                String name = slash >= 0 ? path.substring(slash + 1) : path;
                if (!name.isBlank()) {
                    return name.length() > 48 ? name.substring(0, 48) : name;
                }
            }
            return uri.getHost() != null ? uri.getHost() : "url";
        } catch (Exception e) {
            return "url";
        }
    }

    public static int stopAll(@Nullable net.minecraft.server.MinecraftServer server) {
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

    public static boolean stop(UUID zoneId, @Nullable net.minecraft.server.MinecraftServer server) {
        MusicZone removed = ZONES.remove(zoneId);
        if (removed == null) {
            return false;
        }
        if (server != null) {
            MusicNetwork.broadcastRemove(server, zoneId);
        }
        return true;
    }

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

    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
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
}
