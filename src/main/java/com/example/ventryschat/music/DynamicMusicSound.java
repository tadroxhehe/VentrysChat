package com.example.ventryschat.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Instance de lecture d'une zone : volume = atténuation distance × volume personnel.
 * Catégorie RECORDS pour ne pas dépendre du slider « Musique » vanilla.
 */
@OnlyIn(Dist.CLIENT)
public final class DynamicMusicSound extends AbstractTickableSoundInstance {
    private final UUID zoneId;
    private final MusicZone zone;
    private boolean done;

    public DynamicMusicSound(MusicZone zone) {
        super(resolve(zone), SoundSource.RECORDS);
        this.zoneId = zone.zoneId;
        this.zone = zone;
        this.looping = false;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.x = zone.x;
        this.y = zone.y;
        this.z = zone.z;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = false;
    }

    private static SoundEvent resolve(MusicZone zone) {
        SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(zone.soundId);
        if (ev != null) {
            return ev;
        }
        return new SoundEvent(zone.soundId);
    }

    public UUID zoneId() {
        return zoneId;
    }

    public MusicZone zone() {
        return zone;
    }

    public void markDone() {
        this.done = true;
        this.stop();
    }

    @Override
    public void tick() {
        if (done || System.currentTimeMillis() >= zone.endsAtEpochMs()) {
            markDone();
            return;
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.level.dimension().equals(zone.dimension)) {
            this.volume = 0.0F;
            return;
        }
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        float distFactor = zone.distanceAttenuation(px, py, pz);
        float personal = MusicClientConfig.getVolumeMultiplier();
        this.volume = distFactor * personal;
        this.x = zone.x;
        this.y = zone.y;
        this.z = zone.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
