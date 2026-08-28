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
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Stream HTTP d'un fichier audio distant (pas de cache disque).
 * MP3 / WAV en flux ; OGG bufferisé en mémoire le temps de la lecture.
 * Volume = distance × volume personnel.
 */
@OnlyIn(Dist.CLIENT)
public final class UrlMusicPlayer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    /** Limite de sécurité si OGG doit être bufferisé. */
    private static final int MAX_BUFFER_BYTES = 40 * 1024 * 1024;
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ventryschat-music");
        t.setDaemon(true);
        return t;
    });
    private static final Map<UUID, Playback> PLAYING = new ConcurrentHashMap<>();
    private static final Set<UUID> FINISHED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FAILED = ConcurrentHashMap.newKeySet();

    private UrlMusicPlayer() {
    }

    public static boolean isPlaying(UUID zoneId) {
        Playback p = PLAYING.get(zoneId);
        return p != null && !p.stopped;
    }

    public static boolean hasFinished(UUID zoneId) {
        return FINISHED.contains(zoneId);
    }

    public static boolean hasFailed(UUID zoneId) {
        return FAILED.contains(zoneId);
    }

    public static void clearTerminal(UUID zoneId) {
        FINISHED.remove(zoneId);
        FAILED.remove(zoneId);
    }

    /** Démarre une fois ; ne relance jamais FINISHED / FAILED. */
    public static void start(MusicZone zone) {
        if (!zone.isUrlStream()) {
            return;
        }
        if (FINISHED.contains(zone.zoneId) || FAILED.contains(zone.zoneId)) {
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

    /** Arrêt (sortie de zone) — n'enregistre pas FINISHED. */
    public static void stop(UUID zoneId) {
        Playback p = PLAYING.remove(zoneId);
        if (p != null) {
            p.requestStop();
        }
    }

    public static void stopAll() {
        for (UUID id : Set.copyOf(PLAYING.keySet())) {
            stop(id);
        }
        FINISHED.clear();
        FAILED.clear();
    }

    public static void tickAll() {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Playback> e : PLAYING.entrySet()) {
            Playback p = e.getValue();
            if (p.zone.isExpired(now)) {
                p.requestStop();
                PLAYING.remove(e.getKey(), p);
                continue;
            }
            if (p.stopped) {
                PLAYING.remove(e.getKey(), p);
                continue;
            }
            p.refreshTargetGain(mc);
        }
    }

    private static void tellOnce(String msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.displayClientMessage(new TextComponent(msg), false);
            }
        });
    }

    private enum Fmt { OGG, MP3, WAV, UNKNOWN }

    private static Fmt sniffContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return Fmt.UNKNOWN;
        }
        String t = contentType.toLowerCase(Locale.ROOT);
        if (t.contains("audio/mpeg") || t.contains("audio/mp3") || t.contains("audio/x-mpeg")) {
            return Fmt.MP3;
        }
        if (t.contains("audio/wav") || t.contains("audio/x-wav") || t.contains("audio/wave")) {
            return Fmt.WAV;
        }
        if (t.contains("audio/ogg") || t.contains("application/ogg") || t.contains("audio/vorbis")) {
            return Fmt.OGG;
        }
        return Fmt.UNKNOWN;
    }

    private static Fmt sniffUrl(String url) {
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

    private static Fmt sniffBytes(byte[] data) {
        if (data == null || data.length < 4) {
            return Fmt.UNKNOWN;
        }
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
        return Fmt.UNKNOWN;
    }

    private static final class OpenedStream implements AutoCloseable {
        final HttpURLConnection conn;
        final InputStream in;
        final String finalUrl;
        final String contentType;

        OpenedStream(HttpURLConnection conn, InputStream in, String finalUrl, String contentType) {
            this.conn = conn;
            this.in = in;
            this.finalUrl = finalUrl;
            this.contentType = contentType == null ? "" : contentType;
        }

        @Override
        public void close() {
            try {
                in.close();
            } catch (Exception ignored) {
            }
            conn.disconnect();
        }
    }

    private static final class Playback implements Runnable {
        private final MusicZone zone;
        private volatile boolean stopped;
        private volatile boolean naturalEnd;
        private volatile float targetGain = 1.0F;
        private SourceDataLine line;
        private volatile InputStream liveStream;

        private Playback(MusicZone zone) {
            this.zone = zone;
        }

        private void requestStop() {
            stopped = true;
            InputStream s = liveStream;
            if (s != null) {
                try {
                    s.close();
                } catch (Exception ignored) {
                }
            }
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
                float skipSec = Math.max(0.0F, (System.currentTimeMillis() - zone.startEpochMs) / 1000.0F);
                refreshTargetGain(Minecraft.getInstance());

                Fmt hint = sniffUrl(zone.streamUrl);
                try (OpenedStream opened = openHttp(zone.streamUrl)) {
                    BufferedInputStream bin = new BufferedInputStream(opened.in, 32_768);
                    liveStream = bin;
                    bin.mark(96);
                    byte[] head = bin.readNBytes(64);
                    bin.reset();

                    Fmt fromBytes = sniffBytes(head);
                    Fmt fromType = sniffContentType(opened.contentType);
                    Fmt fmt = fromBytes != Fmt.UNKNOWN ? fromBytes
                        : (fromType != Fmt.UNKNOWN ? fromType : hint);

                    if (fmt == Fmt.UNKNOWN) {
                        fail("Pas un fichier audio direct. Il faut un lien qui télécharge le son "
                            + "(ex. …/fichier.mp3), pas une page web. SoundHelix : "
                            + "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
                        return;
                    }
                    switch (fmt) {
                        case MP3 -> streamMp3From(bin, skipSec);
                        case WAV -> streamWavFrom(bin, skipSec);
                        case OGG -> playOggMemory(readLimited(bin), skipSec);
                        default -> fail("Format non supporté.");
                    }
                }
                if (!stopped) {
                    naturalEnd = true;
                }
            } catch (Exception e) {
                if (!stopped) {
                    LOGGER.warn("URL music fail {}: {}", zone.streamUrl, e.toString());
                    fail(e.getMessage() == null ? e.toString() : e.getMessage());
                }
            } finally {
                requestStop();
                PLAYING.remove(zone.zoneId, this);
                if (naturalEnd && !FAILED.contains(zone.zoneId)) {
                    FINISHED.add(zone.zoneId);
                }
            }
        }

        private void fail(String msg) {
            stopped = true;
            FAILED.add(zone.zoneId);
            tellOnce("§c[Musique] " + msg);
        }

        private void streamMp3From(InputStream in, float skipSec) throws Exception {
            Bitstream bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();
            Header first = bitstream.readFrame();
            if (first == null) {
                fail("MP3 invalide ou lien inaccessible.");
                return;
            }
            int rate = first.frequency();
            int channels = first.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
            bitstream.unreadFrame();
            openLine(new AudioFormat(rate, 16, channels, true, false));

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
            try {
                bitstream.close();
            } catch (Exception ignored) {
            }
        }

        private void streamWavFrom(InputStream in, float skipSec) throws Exception {
            try (var ais = AudioSystem.getAudioInputStream(in)) {
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

        private void playOggMemory(byte[] data, float skipSec) throws Exception {
            ByteBuffer encoded = MemoryUtil.memAlloc(data.length);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                encoded.put(data).flip();
                java.nio.IntBuffer error = stack.mallocInt(1);
                long decoder = STBVorbis.stb_vorbis_open_memory(encoded, error, null);
                if (decoder == 0L) {
                    fail("OGG illisible (Vorbis requis, pas Opus).");
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
                    openLine(new AudioFormat(rate, 16, channels, true, false));
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

        private byte[] readLimited(InputStream in) throws Exception {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16_384];
            int total = 0;
            int n;
            while ((n = in.read(buf)) >= 0) {
                if (stopped) {
                    throw new IllegalStateException("annulé");
                }
                total += n;
                if (total > MAX_BUFFER_BYTES) {
                    throw new IllegalStateException("fichier > " + (MAX_BUFFER_BYTES / (1024 * 1024)) + " Mo");
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }

        private OpenedStream openHttp(String url) throws Exception {
            String current = url;
            for (int hop = 0; hop < 5; hop++) {
                HttpURLConnection conn = (HttpURLConnection) URI.create(current).toURL().openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(12_000);
                conn.setReadTimeout(90_000);
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "audio/*,*/*");
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
                    String reject = MusicServerManager.urlRejectReason(current);
                    if (reject != null) {
                        conn.disconnect();
                        throw new IllegalStateException("redirection vers URL refusée");
                    }
                    conn.disconnect();
                    continue;
                }
                if (code < 200 || code >= 300) {
                    conn.disconnect();
                    throw new IllegalStateException("HTTP " + code);
                }
                return new OpenedStream(conn, conn.getInputStream(), current, conn.getContentType());
            }
            throw new IllegalStateException("trop de redirections");
        }

        private void openLine(AudioFormat format) throws Exception {
            SourceDataLine l = AudioSystem.getSourceDataLine(format);
            l.open(format, Math.max(format.getFrameSize() * 4096, 16384));
            l.start();
            line = l;
            applyGain(l, targetGain);
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
