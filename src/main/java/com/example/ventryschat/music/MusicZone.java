package com.example.ventryschat.music;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Zone musicale active côté serveur / snapshot réseau. */
public final class MusicZone {
    public final UUID zoneId;
    public final String trackId;
    public final ResourceLocation soundId;
    public final ResourceKey<Level> dimension;
    public final double x;
    public final double y;
    public final double z;
    public final float radius;
    public final long startEpochMs;
    public final long durationMs;

    public MusicZone(
            UUID zoneId,
            String trackId,
            ResourceLocation soundId,
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            float radius,
            long startEpochMs,
            long durationMs
    ) {
        this.zoneId = zoneId;
        this.trackId = trackId;
        this.soundId = soundId;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.startEpochMs = startEpochMs;
        this.durationMs = durationMs;
    }

    public Vec3 center() {
        return new Vec3(x, y, z);
    }

    public boolean contains(double px, double py, double pz) {
        double dx = px - x;
        double dy = py - y;
        double dz = pz - z;
        return dx * dx + dy * dy + dz * dz <= (double) radius * (double) radius;
    }

    public long endsAtEpochMs() {
        return startEpochMs + durationMs;
    }

    public boolean isExpired(long nowEpochMs) {
        return nowEpochMs >= endsAtEpochMs();
    }

    public float distanceAttenuation(double px, double py, double pz) {
        double dist = Math.sqrt((px - x) * (px - x) + (py - y) * (py - y) + (pz - z) * (pz - z));
        if (dist >= radius) {
            return 0.0F;
        }
        // 0–20% rayon : plein ; 20–100% : décroissance linéaire
        float inner = radius * 0.20F;
        if (dist <= inner) {
            return 1.0F;
        }
        float t = (float) ((dist - inner) / Math.max(0.001F, radius - inner));
        return Math.max(0.0F, 1.0F - t);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(zoneId);
        buf.writeUtf(trackId, 64);
        buf.writeResourceLocation(soundId);
        buf.writeResourceLocation(dimension.location());
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(radius);
        buf.writeLong(startEpochMs);
        buf.writeLong(durationMs);
    }

    public static MusicZone decode(FriendlyByteBuf buf) {
        UUID zoneId = buf.readUUID();
        String trackId = buf.readUtf(64);
        ResourceLocation soundId = buf.readResourceLocation();
        ResourceLocation dimLoc = buf.readResourceLocation();
        ResourceKey<Level> dim = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimLoc);
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float radius = buf.readFloat();
        long start = buf.readLong();
        long duration = buf.readLong();
        return new MusicZone(zoneId, trackId, soundId, dim, x, y, z, radius, start, duration);
    }
}
