package com.example.ventryschat.music;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL11;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client : zones actives, lecture, sync (seek OpenAL), distance × volume perso.
 */
@OnlyIn(Dist.CLIENT)
public final class MusicClientManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, MusicZone> ZONES = new HashMap<>();
    private static final Map<UUID, DynamicMusicSound> PLAYING = new HashMap<>();
    private static Field soundEngineField;
    private static Field instanceToChannelField;
    private static Field channelSourceField;
    private static boolean reflectFailed;

    private MusicClientManager() {
    }

    public static void upsert(MusicZone zone) {
        ZONES.put(zone.zoneId, zone);
        ensurePlaying(zone);
    }

    public static void remove(UUID zoneId) {
        ZONES.remove(zoneId);
        stopPlaying(zoneId);
    }

    public static void applySnapshot(List<MusicZone> zones) {
        ZONES.clear();
        for (UUID id : List.copyOf(PLAYING.keySet())) {
            stopPlaying(id);
        }
        for (MusicZone z : zones) {
            ZONES.put(z.zoneId, z);
        }
        for (MusicZone z : ZONES.values()) {
            ensurePlaying(z);
        }
    }

    public static void clear() {
        ZONES.clear();
        for (UUID id : List.copyOf(PLAYING.keySet())) {
            stopPlaying(id);
        }
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, MusicZone>> it = ZONES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, MusicZone> e = it.next();
            MusicZone z = e.getValue();
            if (z.isExpired(now) || !z.dimension.equals(mc.level.dimension())) {
                stopPlaying(e.getKey());
                if (z.isExpired(now)) {
                    it.remove();
                }
                continue;
            }
            ensurePlaying(z);
            DynamicMusicSound sound = PLAYING.get(z.zoneId);
            if (sound != null && sound.isStopped()) {
                PLAYING.remove(z.zoneId);
            }
        }
    }

    private static void ensurePlaying(MusicZone zone) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!zone.dimension.equals(mc.level.dimension())) {
            stopPlaying(zone.zoneId);
            return;
        }
        if (zone.isExpired(System.currentTimeMillis())) {
            stopPlaying(zone.zoneId);
            return;
        }
        DynamicMusicSound existing = PLAYING.get(zone.zoneId);
        if (existing != null && !existing.isStopped()) {
            return;
        }
        if (MusicClientConfig.getVolumePercent() <= 0
                && zone.distanceAttenuation(mc.player.getX(), mc.player.getY(), mc.player.getZ()) <= 0) {
            // Toujours démarrer pour rester sync ; volume 0 possible via canStartSilent
        }
        DynamicMusicSound sound = new DynamicMusicSound(zone);
        PLAYING.put(zone.zoneId, sound);
        mc.getSoundManager().play(sound);
        float elapsedSec = (System.currentTimeMillis() - zone.startEpochMs) / 1000.0F;
        if (elapsedSec > 0.35F) {
            mc.execute(() -> seekAfterStart(sound, elapsedSec));
        }
    }

    private static void stopPlaying(UUID zoneId) {
        DynamicMusicSound sound = PLAYING.remove(zoneId);
        if (sound != null) {
            sound.markDone();
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
    }

    /** Seek OpenAL pour synchroniser les entrants tardifs. */
    private static void seekAfterStart(SoundInstance instance, float elapsedSec) {
        if (reflectFailed) {
            return;
        }
        try {
            if (soundEngineField == null) {
                soundEngineField = SoundManager.class.getDeclaredField("soundEngine");
                soundEngineField.setAccessible(true);
            }
            SoundManager manager = Minecraft.getInstance().getSoundManager();
            Object engine = soundEngineField.get(manager);
            if (instanceToChannelField == null) {
                instanceToChannelField = SoundEngine.class.getDeclaredField("instanceToChannel");
                instanceToChannelField.setAccessible(true);
            }
            @SuppressWarnings("unchecked")
            Map<SoundInstance, ?> map = (Map<SoundInstance, ?>) instanceToChannelField.get(engine);
            Object channelHandle = map.get(instance);
            if (channelHandle == null) {
                // Canal pas encore prêt : retenter une fois
                Minecraft.getInstance().execute(() -> seekAfterStart(instance, elapsedSec));
                return;
            }
            // ChannelAccess.ChannelHandle has channel field
            Field channelField = channelHandle.getClass().getDeclaredField("channel");
            channelField.setAccessible(true);
            Object channel = channelField.get(channelHandle);
            if (channelSourceField == null) {
                channelSourceField = channel.getClass().getDeclaredField("source");
                channelSourceField.setAccessible(true);
            }
            int source = channelSourceField.getInt(channel);
            float max = Math.max(0.0F, (float) (((DynamicMusicSound) instance).zone().durationMs / 1000.0) - 0.05F);
            float offset = Math.min(elapsedSec, max);
            AL11.alSourcef(source, AL11.AL_SEC_OFFSET, offset);
        } catch (Throwable t) {
            reflectFailed = true;
            LOGGER.debug("Seek musique dynamique indisponible: {}", t.toString());
        }
    }
}
