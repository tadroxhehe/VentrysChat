package com.example.ventryschat.music;

import com.mojang.logging.LogUtils;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lecture URL hors thread jeu (pas de freeze) via {@link SourceDataLine}.
 * Formats : OGG Vorbis, MP3, WAV. Volume = distance × volume perso.
 */
@OnlyIn(Dist.CLIENT)
public final class UrlMusicPlayer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int MAX_BYTES = 40 * 1024 * 1024;
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ventryschat-music");
        t.setDaemon(true);
        return t;
    });
    private static final Map<UUID, Playback> PLAYING = new ConcurrentHashMap<>();
    private static final Set<UUID> FAILED = ConcurrentHashMap.newKeySet();

    private UrlMusicPlayer() {
    }

    public static void ensurePlaying(MusicZone zone) {
        if (!zone.isUrlStream() || FAILED.contains(zone.zoneId)) {
            return;
        }
        Playback existing = PLAYING.get(zone.zoneId);
        if (existing != null && !existing.stopped) {
            return;
        }
        Playback playback = new Playback(zone);
        PLAYING.put(zone.zoneId, playback);
        WORKERS.execute(playback);
    }

    public static void stop(UUID zoneId) {
        FAILED.remove(zoneId);
        Playback p = PLAYING.remove(zoneId);
        if (p != null) {
            p.requestStop();
        }
    }

    public static void stopAll() {
        FAILED.clear();
        for (UUID id : Set.copyOf(PLAYING.keySet())) {
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
            p.refreshTargetGain(mc);
        }
    }

    private static void tell(String msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.displayClientMessage(new TextComponent(msg), false);
            }
        });
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("ventryschat-music-cache");
    }

    private static Path cacheFileFor(String url) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(url.getBytes(StandardCharsets.UTF_8));
        return cacheDir().resolve(HexFormat.of().formatHex(hash).substring(0, 32) + ".bin");
    }

    private enum Fmt { OGG, MP3, WAV, UNKNOWN }

    private static Fmt sniff(byte[] data, String url) {
        if (data != null && data.length >= 4) {
            if (data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S') {
                return Fmt.OGG;
            }
            if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') {
                return Fmt.WAV;
            }
            if (data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
                return Fmt.MP3;
            }
            int b0 = data[0] & 0xFF;
            int b1 = data[1] & 0xFF;
            if (b0 == 0xFF && (b1 & 0xE0) == 0xE0) {
                return Fmt.MP3;
            }
        }
        String lower = url.toLowerCase(Locale.ROOT);
        int q = lower.indexOf('?');
        if (q >= 0) {
            lower = lower.substring(0, q);
        }
        if (lower.endsWith(".mp3")) {
            return Fmt.MP3;
        }
        if (lower.endsWith(".wav")) {
            return Fmt.WAV;
        }
        if (lower.endsWith(".ogg") || lower.endsWith(".oga")) {
            return Fmt.OGG;
        }
        return Fmt.UNKNOWN;
    }

    private static final class Playback implements Runnable {
        private final MusicZone zone;
        private volatile boolean stopped;
        private volatile float targetGain = 1.0F;
        private SourceDataLine line;

        private Playback(MusicZone zone) {
            this.zone = zone;
        }

        private void requestStop() {
            stopped = true;
            SourceDataLine l = line;
            if (l != null) {
                try {
                    l.stop();
                    l.flush();
                    l.close();
                } catch (Throwable ignored) {
                }
            }
        }

        private void refreshTargetGain(Minecraft mc) {
            if (mc.player == null || mc.level == null || !mc.level.dimension().equals(zone.dimension)) {
                targetGain = 0.0F;
                return;
            }
            float dist = zone.distanceAttenuation(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            targetGain = dist * MusicClientConfig.getVolumeMultiplier();
            applyGain(line, targetGain);
        }

        @Override
        public void run() {
            try {
                tell("§7[Musique] Téléchargement…");
                byte[] data = loadBytes(zone.streamUrl);
                if (stopped) {
                    return;
                }
                if (data.length >= 15) {
                    String head = new String(data, 0, Math.min(64, data.length), StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
                    if (head.contains("<html") || head.contains("<!doctype")) {
                        fail("Le lien renvoie une page web, pas un fichier audio.");
                        return;
                    }
                }
                Fmt fmt = sniff(data, zone.streamUrl);
                if (fmt == Fmt.UNKNOWN) {
                    fail("Format inconnu. Utilise .mp3, .ogg (Vorbis) ou .wav.");
                    return;
                }
                float skipSec = Math.max(0.0F, (System.currentTimeMillis() - zone.startEpochMs) / 1000.0F);
                refreshTargetGain(Minecraft.getInstance());
                switch (fmt) {
                    case OGG -> playOgg(data, skipSec);
                    case MP3 -> playMp3(data, skipSec);
                    case WAV -> playWav(data, skipSec);
                    default -> fail("Format non supporté.");
                }
            } catch (Exception e) {
                LOGGER.warn("URL music fail {}: {}", zone.streamUrl, e.toString());
                fail(e.getMessage() == null ? e.toString() : e.getMessage());
            } finally {
                stopped = true;
                requestStop();
            }
        }

        private void fail(String msg) {
            stopped = true;
            FAILED.add(zone.zoneId);
            tell("§c[Musique] " + msg);
        }

        private byte[] loadBytes(String url) throws Exception {
            Files.createDirectories(cacheDir());
            Path target = cacheFileFor(url);
            if (Files.isRegularFile(target) && Files.size(target) > 64) {
                return Files.readAllBytes(target);
            }
            byte[] downloaded = download(url);
            Path tmp = target.resolveSibling(target.getFileName() + ".part");
            Files.write(tmp, downloaded);
            Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return downloaded;
        }

        private byte[] download(String url) throws Exception {
            String current = url;
            for (int hop = 0; hop < 5; hop++) {
                HttpURLConnection conn = (HttpURLConnection) URI.create(current).toURL().openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(12_000);
                conn.setReadTimeout(90_000);
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "*/*");
                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_MOVED_PERM
                        || code == HttpURLConnection.HTTP_MOVED_TEMP
                        || code == HttpURLConnection.HTTP_SEE_OTHER
                        || code == 307
                        || code == 308) {
                    String loc = conn.getHeaderField("Location");
                    if (loc == null || loc.isBlank()) {
                        throw new IllegalStateException("HTTP " + code + " sans Location");
                    }
                    current = URI.create(current).resolve(loc).toString();
                    conn.disconnect();
                    continue;
                }
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("HTTP " + code);
                }
                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[16_384];
                    int total = 0;
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        if (stopped) {
                            throw new IllegalStateException("annulé");
                        }
                        total += n;
                        if (total > MAX_BYTES) {
                            throw new IllegalStateException("fichier > " + (MAX_BYTES / (1024 * 1024)) + " Mo");
                        }
                        out.write(buf, 0, n);
                    }
                    return out.toByteArray();
                } finally {
                    conn.disconnect();
                }
            }
            throw new IllegalStateException("trop de redirections");
        }

        private void playOgg(byte[] data, float skipSec) throws Exception {
            ByteBuffer encoded = MemoryUtil.memAlloc(data.length);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                encoded.put(data).flip();
                java.nio.IntBuffer error = stack.mallocInt(1);
                long decoder = STBVorbis.stb_vorbis_open_memory(encoded, error, null);
                if (decoder == 0L) {
                    fail("OGG illisible (il faut Vorbis, pas Opus). Convertis avec: ffmpeg -i in.mp3 -c:a libvorbis out.ogg");
                    return;
                }
                try {
                    STBVorbisInfo info = STBVorbisInfo.mallocStack(stack);
                    STBVorbis.stb_vorbis_get_info(decoder, info);
                    int channels = info.channels();
                    int rate = info.sample_rate();
                    if (skipSec > 0.25F) {
                        STBVorbis.stb_vorbis_seek(decoder, (int) (skipSec * rate));
                    }
                    AudioFormat format = new AudioFormat(rate, 16, channels, true, false);
                    openLine(format);
                    tell("§a[Musique] Lecture OGG…");
                    short[] samples = new short[2048 * Math.max(1, channels)];
                    byte[] bytes = new byte[samples.length * 2];
                    while (!stopped) {
                        int n = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, samples);
                        if (n <= 0) {
                            break;
                        }
                        int sampleCount = n * channels;
                        shortsToBytes(samples, sampleCount, bytes);
                        writeFully(bytes, sampleCount * 2);
                    }
                } finally {
                    STBVorbis.stb_vorbis_close(decoder);
                }
            } finally {
                MemoryUtil.memFree(encoded);
            }
        }

        private void playMp3(byte[] data, float skipSec) throws Exception {
            Bitstream bitstream = new Bitstream(new ByteArrayInputStream(data));
            Decoder decoder = new Decoder();
            Header first = bitstream.readFrame();
            if (first == null) {
                fail("MP3 invalide.");
                return;
            }
            int rate = first.frequency();
            int channels = first.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
            bitstream.unreadFrame();
            AudioFormat format = new AudioFormat(rate, 16, channels, true, false);
            openLine(format);
            tell("§a[Musique] Lecture MP3…");

            double skipped = 0.0;
            Header header;
            while (!stopped && (header = bitstream.readFrame()) != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                short[] samples = output.getBuffer();
                int len = output.getBufferLength();
                double frameSec = header.ms_per_frame() / 1000.0;
                if (skipped + frameSec <= skipSec) {
                    skipped += frameSec;
                    bitstream.closeFrame();
                    continue;
                }
                byte[] bytes = new byte[len * 2];
                shortsToBytes(samples, len, bytes);
                writeFully(bytes, len * 2);
                bitstream.closeFrame();
            }
            bitstream.close();
        }

        private void playWav(byte[] data, float skipSec) throws Exception {
            try (var ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
                AudioFormat base = ais.getFormat();
                AudioFormat target = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(),
                    16,
                    base.getChannels(),
                    base.getChannels() * 2,
                    base.getSampleRate(),
                    false
                );
                try (var pcm = AudioSystem.getAudioInputStream(target, ais)) {
                    openLine(target);
                    tell("§a[Musique] Lecture WAV…");
                    long skipBytes = (long) (skipSec * target.getSampleRate() * target.getFrameSize());
                    while (skipBytes > 0 && !stopped) {
                        long s = pcm.skip(skipBytes);
                        if (s <= 0) {
                            break;
                        }
                        skipBytes -= s;
                    }
                    byte[] buf = new byte[8192];
                    int n;
                    while (!stopped && (n = pcm.read(buf)) > 0) {
                        writeFully(buf, n);
                    }
                }
            }
        }

        private void openLine(AudioFormat format) throws Exception {
            SourceDataLine l = AudioSystem.getSourceDataLine(format);
            l.open(format, Math.max(format.getFrameSize() * 4096, 16384));
            l.start();
            line = l;
            applyGain(l, targetGain);
            if (targetGain <= 0.001F) {
                float personal = MusicClientConfig.getVolumeMultiplier();
                if (personal <= 0.001F) {
                    tell("§e[Musique] Volume perso à 0% (Échap → Musique).");
                } else {
                    tell("§e[Musique] Hors rayon — rapproche-toi du point de lancement.");
                }
            }
        }

        private void writeFully(byte[] bytes, int len) {
            SourceDataLine l = line;
            if (l == null || stopped) {
                return;
            }
            applyGain(l, targetGain);
            int off = 0;
            while (off < len && !stopped) {
                int w = l.write(bytes, off, len - off);
                if (w < 0) {
                    break;
                }
                off += w;
            }
        }

        private static void shortsToBytes(short[] samples, int count, byte[] out) {
            for (int i = 0; i < count; i++) {
                short s = samples[i];
                out[i * 2] = (byte) (s & 0xFF);
                out[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
            }
        }

        private static void applyGain(SourceDataLine l, float linear) {
            if (l == null) {
                return;
            }
            try {
                if (!l.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    return;
                }
                FloatControl ctrl = (FloatControl) l.getControl(FloatControl.Type.MASTER_GAIN);
                float g = Math.max(0.0001F, Math.min(1.0F, linear));
                float db = (float) (20.0 * Math.log10(g));
                db = Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), db));
                ctrl.setValue(db);
            } catch (Throwable ignored) {
            }
        }
    }
}
