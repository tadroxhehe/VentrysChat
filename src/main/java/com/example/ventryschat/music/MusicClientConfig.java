package com.example.ventryschat.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Volume personnel des musiques dynamiques (0–100 %), sauvegardé localement.
 * Le serveur ne lit jamais ce fichier.
 */
public final class MusicClientConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "ventryschat-dynamic-music-client.json";

    /** 0–100 */
    private static int volumePercent = 100;
    private static boolean loaded;

    private MusicClientConfig() {
    }

    public static void load() {
        loaded = true;
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj != null && obj.has("volume_percent")) {
                volumePercent = clamp(obj.get("volume_percent").getAsInt());
            }
        } catch (Exception e) {
            LOGGER.warn("Config volume musique illisible: {}", e.toString());
            volumePercent = 100;
        }
    }

    public static void save() {
        ensureLoaded();
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("volume_percent", volumePercent);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, writer);
            }
        } catch (Exception e) {
            LOGGER.warn("Impossible d'écrire {}: {}", path, e.toString());
        }
    }

    public static int getVolumePercent() {
        ensureLoaded();
        return volumePercent;
    }

    /** Multiplicateur 0.0–1.0 appliqué côté client uniquement. */
    public static float getVolumeMultiplier() {
        return getVolumePercent() / 100.0F;
    }

    public static void setVolumePercent(int percent) {
        ensureLoaded();
        volumePercent = clamp(percent);
        save();
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }
}
