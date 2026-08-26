package com.example.ventryschat.music;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lecture OpenAL d'un fichier distant (téléchargé en cache local).
 * Volume = distance × volume personnel (jamais imposé par le serveur).
 */
@OnlyIn(Dist.CLIENT)
public final class UrlMusicPlayer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService DOWNLOADS = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ventryschat-music-dl");
        t.setDaemon(true);
        return t;
    });
    private static final Map<UUID, Playback> PLAYING = new ConcurrentHashMap<>();

    private UrlMusicPlayer() {
    }

    public static void ensurePlaying(MusicZone zone) {
        if (!zone.isUrlStream()) {
            return;
        }
        Playback existing = PLAYING.get(zone.zoneId);
        if (existing != null && !existing.stopped) {
            return;
        }
        Playback playback = new Playback(zone);
        PLAYING.put(zone.zoneId, playback);
        DOWNLOADS.execute(() -> playback.startAsync());
    }

    public static void stop(UUID zoneId) {
        Playback p = PLAYING.remove(zoneId);
        if (p != null) {
            p.stop();
        }
    }

    public static void stopAll() {
        for (UUID id : PLAYING.keySet()) {
            stop(id);
        }
    }

    public static void tickAll() {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Playback> e : PLAYING.entrySet()) {
            Playback p = e.getValue();
            if (p.stopped || p.zone.isExpired(now)) {
                stop(e.getKey());
                continue;
            }
            p.updateVolume(mc);
        }
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("ventryschat-music-cache");
    }

    private static Path cacheFileFor(String url) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(url.getBytes(StandardCharsets.UTF_8));
        String hex = HexFormat.of().formatHex(hash).substring(0, 32);
        String ext = guessExtension(url);
        return cacheDir().resolve(hex + ext);
    }

    private static String guessExtension(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        int q = lower.indexOf('?');
        if (q >= 0) {
            lower = lower.substring(0, q);
        }
        if (lower.endsWith(".mp3")) {
            return ".mp3";
        }
        if (lower.endsWith(".wav")) {
            return ".wav";
        }
        return ".ogg";
    }

    private static final class Playback {
        private final MusicZone zone;
        private volatile boolean stopped;
        private volatile boolean ready;
        private int source = -1;
        private int buffer = -1;

        private Playback(MusicZone zone) {
            this.zone = zone;
        }

        private void startAsync() {
            try {
                Path file = download(zone.streamUrl);
                if (stopped) {
                    return;
                }
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".ogg")) {
                    Minecraft.getInstance().execute(() -> openOgg(file));
                } else {
                    LOGGER.warn("Format non supporté pour l'instant (ogg requis): {}", name);
                    stopped = true;
                }
            } catch (Exception e) {
                LOGGER.warn("Échec téléchargement musique {}: {}", zone.streamUrl, e.toString());
                stopped = true;
            }
        }

        private Path download(String url) throws Exception {
            Files.createDirectories(cacheDir());
            Path target = cacheFileFor(url);
            if (Files.isRegularFile(target) && Files.size(target) > 0) {
                return target;
            }
            Path tmp = target.resolveSibling(target.getFileName() + ".part");
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(60_000);
            conn.setRequestProperty("User-Agent", "VentrysChat-Music/1.0");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code);
            }
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return target;
        }

        private void openOgg(Path file) {
            if (stopped) {
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer channels = stack.mallocInt(1);
                IntBuffer sampleRate = stack.mallocInt(1);
                ShortBuffer pcm = STBVorbis.stb_vorbis_decode_filename(file.toAbsolutePath().toString(), channels, sampleRate);
                if (pcm == null) {
                    LOGGER.warn("Décodage OGG échoué: {}", file);
                    stopped = true;
                    return;
                }
                int ch = channels.get(0);
                int rate = sampleRate.get(0);
                int format = ch == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                buffer = AL10.alGenBuffers();
                AL10.alBufferData(buffer, format, pcm, rate);
                source = AL10.alGenSources();
                AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
                AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
                AL10.alSourcef(source, AL10.AL_GAIN, 0.0F);
                float elapsed = (System.currentTimeMillis() - zone.startEpochMs) / 1000.0F;
                if (elapsed > 0.35F) {
                    try {
                        org.lwjgl.openal.AL11.alSourcef(source, org.lwjgl.openal.AL11.AL_SEC_OFFSET, elapsed);
                    } catch (Throwable ignored) {
                    }
                }
                AL10.alSourcePlay(source);
                ready = true;
            } catch (Throwable t) {
                LOGGER.warn("OpenAL URL music fail: {}", t.toString());
                cleanupAl();
                stopped = true;
            }
        }

        private void updateVolume(Minecraft mc) {
            if (!ready || stopped || source < 0) {
                return;
            }
            if (mc.player == null || mc.level == null || !mc.level.dimension().equals(zone.dimension)) {
                AL10.alSourcef(source, AL10.AL_GAIN, 0.0F);
                return;
            }
            float dist = zone.distanceAttenuation(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            float personal = MusicClientConfig.getVolumeMultiplier();
            AL10.alSourcef(source, AL10.AL_GAIN, dist * personal);
            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_STOPPED) {
                stop();
            }
        }

        private void stop() {
            stopped = true;
            Minecraft.getInstance().execute(this::cleanupAl);
        }

        private void cleanupAl() {
            try {
                if (source >= 0) {
                    AL10.alSourceStop(source);
                    AL10.alDeleteSources(source);
                    source = -1;
                }
                if (buffer >= 0) {
                    AL10.alDeleteBuffers(buffer);
                    buffer = -1;
                }
            } catch (Throwable ignored) {
            }
            ready = false;
        }
    }
}
