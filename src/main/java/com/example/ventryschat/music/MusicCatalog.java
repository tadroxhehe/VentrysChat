package com.example.ventryschat.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Catalogue des pistes dynamiques (jar + override {@code config/ventryschat-music.json}).
 */
public final class MusicCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Track> TRACKS = new LinkedHashMap<>();

    public record Track(String id, ResourceLocation sound, long durationMs, String displayName) {
    }

    private MusicCatalog() {
    }

    public static void reload() {
        TRACKS.clear();
        loadFromStream(MusicCatalog.class.getResourceAsStream("/data/ventryschat/music_catalog.json"), "jar");
        try {
            Path override = FMLPaths.CONFIGDIR.get().resolve("ventryschat-music.json");
            if (Files.isRegularFile(override)) {
                try (InputStream in = Files.newInputStream(override)) {
                    loadFromStream(in, override.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Override musique illisible: {}", e.toString());
        }
        LOGGER.info("MusicCatalog: {} piste(s)", TRACKS.size());
    }

    private static void loadFromStream(@Nullable InputStream stream, String source) {
        if (stream == null) {
            LOGGER.warn("Catalogue musique introuvable ({})", source);
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject tracks = root.getAsJsonObject("tracks");
            if (tracks == null) {
                return;
            }
            for (Map.Entry<String, JsonElement> e : tracks.entrySet()) {
                JsonObject t = e.getValue().getAsJsonObject();
                String soundStr = t.has("sound") ? t.get("sound").getAsString() : "ventryschat:music." + e.getKey();
                long duration = t.has("duration_ms") ? Math.max(1000L, t.get("duration_ms").getAsLong()) : 30_000L;
                String display = t.has("display_name") ? t.get("display_name").getAsString() : e.getKey();
                ResourceLocation sound = ResourceLocation.tryParse(soundStr);
                if (sound == null) {
                    LOGGER.warn("Sound invalide pour {}: {}", e.getKey(), soundStr);
                    continue;
                }
                TRACKS.put(e.getKey(), new Track(e.getKey(), sound, duration, display));
            }
        } catch (Exception ex) {
            LOGGER.error("Erreur catalogue musique ({}): {}", source, ex.toString());
        }
    }

    public static Optional<Track> get(String id) {
        return Optional.ofNullable(TRACKS.get(id));
    }

    public static Set<String> ids() {
        return Collections.unmodifiableSet(TRACKS.keySet());
    }

    public static Map<String, Track> all() {
        return Collections.unmodifiableMap(TRACKS);
    }
}
