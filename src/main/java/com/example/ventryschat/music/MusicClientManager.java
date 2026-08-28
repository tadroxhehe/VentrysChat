package com.example.ventryschat.music;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL11;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client : zones, opt-in Oui/Non, lecture, sync, distance × volume perso.
 * Ne relance jamais une piste terminée / refusée / en échec.
 */
@OnlyIn(Dist.CLIENT)
public final class MusicClientManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private enum Consent { NONE, PROMPTED, ACCEPTED, DECLINED }

    private enum PlayPhase { IDLE, PLAYING, FINISHED, FAILED }

    private static final class ZoneRuntime {
        MusicZone zone;
        Consent consent = Consent.NONE;
        PlayPhase phase = PlayPhase.IDLE;
        boolean inside;
        DynamicMusicSound packaged;

        ZoneRuntime(MusicZone zone) {
            this.zone = zone;
        }
    }

    private static final Map<UUID, ZoneRuntime> ZONES = new HashMap<>();
    private static Field soundEngineField;
    private static Field instanceToChannelField;
    private static Field channelSourceField;
    private static boolean reflectFailed;

    private MusicClientManager() {
    }

    public static void upsert(MusicZone zone) {
        ZoneRuntime rt = ZONES.get(zone.zoneId);
        if (rt == null) {
            rt = new ZoneRuntime(zone);
            ZONES.put(zone.zoneId, rt);
        } else {
            rt.zone = zone;
            // Nouvelle version de zone (re-play staff) : reset fin / échec
            if (rt.phase == PlayPhase.FINISHED || rt.phase == PlayPhase.FAILED) {
                stopAudio(rt);
                rt.phase = PlayPhase.IDLE;
                rt.consent = Consent.NONE;
                rt.inside = false; // force re-prompt au prochain tick si toujours dans la zone
                UrlMusicPlayer.clearTerminal(zone.zoneId);
            }
        }
    }

    public static void remove(UUID zoneId) {
        ZoneRuntime rt = ZONES.remove(zoneId);
        if (rt != null) {
            stopAudio(rt);
        }
        UrlMusicPlayer.stop(zoneId);
        UrlMusicPlayer.clearTerminal(zoneId);
    }

    public static void applySnapshot(List<MusicZone> zones) {
        clear();
        for (MusicZone z : zones) {
            ZONES.put(z.zoneId, new ZoneRuntime(z));
        }
    }

    public static void clear() {
        for (UUID id : List.copyOf(ZONES.keySet())) {
            remove(id);
        }
        ZONES.clear();
        UrlMusicPlayer.stopAll();
    }

    /**
     * Intercepte {@code /musiclisten accept|decline <uuid>} côté client.
     * @return true si la commande a été consommée (ne pas envoyer au serveur)
     */
    public static boolean tryHandleListenCommand(String raw) {
        if (raw == null) {
            return false;
        }
        String cmd = raw.trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (!cmd.regionMatches(true, 0, "musiclisten ", 0, "musiclisten ".length())) {
            return false;
        }
        String[] parts = cmd.split("\\s+");
        if (parts.length < 3) {
            return true;
        }
        boolean accept;
        if ("accept".equalsIgnoreCase(parts[1])) {
            accept = true;
        } else if ("decline".equalsIgnoreCase(parts[1])) {
            accept = false;
        } else {
            return true;
        }
        try {
            applyConsent(UUID.fromString(parts[2]), accept);
        } catch (IllegalArgumentException ignored) {
        }
        return true;
    }

    /** Réponse Oui/Non (écran local ou /musiclisten). */
    public static void applyConsent(UUID zoneId, boolean accept) {
        ZoneRuntime rt = ZONES.get(zoneId);
        if (rt == null) {
            tell("§7[Musique] Cette zone n'est plus active.");
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && rt.zone.dimension.equals(mc.level.dimension())) {
            rt.inside = rt.zone.distanceAttenuation(
                mc.player.getX(), mc.player.getY(), mc.player.getZ()
            ) > 0.001F;
        }
        if (accept) {
            rt.consent = Consent.ACCEPTED;
            tell("§a[Musique] Lecture acceptée.");
            // Toujours tenter : le volume gère la distance ; ne pas bloquer sur le flag inside
            if (rt.phase == PlayPhase.IDLE || rt.phase == PlayPhase.PLAYING) {
                if (rt.phase == PlayPhase.PLAYING) {
                    stopAudio(rt);
                    rt.phase = PlayPhase.IDLE;
                }
                UrlMusicPlayer.clearTerminal(zoneId);
                startAudio(rt);
            }
        } else {
            rt.consent = Consent.DECLINED;
            stopAudio(rt);
            if (rt.phase != PlayPhase.FINISHED && rt.phase != PlayPhase.FAILED) {
                rt.phase = PlayPhase.IDLE;
            }
            tell("§e[Musique] Lecture refusée.");
        }
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        Iterator<Map.Entry<UUID, ZoneRuntime>> it = ZONES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ZoneRuntime> e = it.next();
            ZoneRuntime rt = e.getValue();
            MusicZone z = rt.zone;

            if (z.isExpired(now)) {
                stopAudio(rt);
                UrlMusicPlayer.clearTerminal(e.getKey());
                it.remove();
                continue;
            }
            if (!z.dimension.equals(mc.level.dimension())) {
                if (rt.inside) {
                    onLeave(rt);
                }
                continue;
            }

            boolean nowInside = z.distanceAttenuation(px, py, pz) > 0.001F;
            if (nowInside && !rt.inside) {
                onEnter(rt);
            } else if (!nowInside && rt.inside) {
                onLeave(rt);
            }
            rt.inside = nowInside;

            if (rt.phase == PlayPhase.PLAYING) {
                if (z.isUrlStream()) {
                    if (UrlMusicPlayer.hasFailed(z.zoneId)) {
                        rt.phase = PlayPhase.FAILED;
                    } else if (UrlMusicPlayer.hasFinished(z.zoneId)) {
                        rt.phase = PlayPhase.FINISHED;
                    } else if (!UrlMusicPlayer.isPlaying(z.zoneId) && rt.consent == Consent.ACCEPTED && nowInside) {
                        // Thread mort sans finished (stop forcé) → IDLE pour pouvoir reprendre
                        rt.phase = PlayPhase.IDLE;
                    }
                } else if (rt.packaged != null && rt.packaged.isStopped()) {
                    rt.phase = PlayPhase.FINISHED;
                    rt.packaged = null;
                }
            }

            if (nowInside && rt.consent == Consent.ACCEPTED && rt.phase == PlayPhase.IDLE) {
                startAudio(rt);
            }
        }
        UrlMusicPlayer.tickAll();
    }

    private static void onEnter(ZoneRuntime rt) {
        if (rt.consent == Consent.NONE) {
            sendPrompt(rt);
            rt.consent = Consent.PROMPTED;
            return;
        }
        if (rt.consent == Consent.ACCEPTED && rt.phase == PlayPhase.IDLE) {
            startAudio(rt);
        }
    }

    private static void onLeave(ZoneRuntime rt) {
        if (rt.phase == PlayPhase.PLAYING) {
            stopAudio(rt);
            if (rt.phase != PlayPhase.FINISHED && rt.phase != PlayPhase.FAILED) {
                rt.phase = PlayPhase.IDLE;
            }
        }
    }

    private static void startAudio(ZoneRuntime rt) {
        MusicZone zone = rt.zone;
        if (rt.phase == PlayPhase.FINISHED || rt.phase == PlayPhase.FAILED) {
            return;
        }
        if (rt.consent != Consent.ACCEPTED) {
            return;
        }
        if (zone.isExpired(System.currentTimeMillis())) {
            return;
        }
        if (zone.isUrlStream()) {
            if (UrlMusicPlayer.hasFinished(zone.zoneId) || UrlMusicPlayer.hasFailed(zone.zoneId)) {
                rt.phase = UrlMusicPlayer.hasFailed(zone.zoneId) ? PlayPhase.FAILED : PlayPhase.FINISHED;
                return;
            }
            if (UrlMusicPlayer.isPlaying(zone.zoneId)) {
                rt.phase = PlayPhase.PLAYING;
                return;
            }
            UrlMusicPlayer.start(zone);
            rt.phase = PlayPhase.PLAYING;
            return;
        }
        if (rt.packaged != null && !rt.packaged.isStopped()) {
            rt.phase = PlayPhase.PLAYING;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        DynamicMusicSound sound = new DynamicMusicSound(zone);
        rt.packaged = sound;
        mc.getSoundManager().play(sound);
        float elapsedSec = (System.currentTimeMillis() - zone.startEpochMs) / 1000.0F;
        if (elapsedSec > 0.35F) {
            mc.execute(() -> seekAfterStart(sound, elapsedSec));
        }
        rt.phase = PlayPhase.PLAYING;
    }

    private static void stopAudio(ZoneRuntime rt) {
        if (rt.packaged != null) {
            rt.packaged.markDone();
            Minecraft.getInstance().getSoundManager().stop(rt.packaged);
            rt.packaged = null;
        }
        UrlMusicPlayer.stop(rt.zone.zoneId);
    }

    private static void sendPrompt(ZoneRuntime rt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String label = rt.zone.isUrlStream() ? "une musique" : ("« " + rt.zone.trackId + " »");
        String id = rt.zone.zoneId.toString();

        MutableComponent root = new TextComponent("§6[Musique] §eTu veux écouter " + label + " ? ");
        MutableComponent yes = new TextComponent("§a§l[Oui]");
        yes.setStyle(Style.EMPTY
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/musiclisten accept " + id))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Accepter la musique"))));
        MutableComponent mid = new TextComponent(" §7/ ");
        MutableComponent no = new TextComponent("§c§l[Non]");
        no.setStyle(Style.EMPTY
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/musiclisten decline " + id))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Refuser"))));
        root.append(yes).append(mid).append(no);
        mc.player.displayClientMessage(root, false);
    }

    private static void tell(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(new TextComponent(msg), false);
        }
    }

    private static void seekAfterStart(SoundInstance instance, float elapsedSec) {
        seekAfterStart(instance, elapsedSec, 0);
    }

    private static void seekAfterStart(SoundInstance instance, float elapsedSec, int attempt) {
        if (reflectFailed || attempt > 40) {
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
                int next = attempt + 1;
                Minecraft.getInstance().execute(() -> seekAfterStart(instance, elapsedSec, next));
                return;
            }
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
