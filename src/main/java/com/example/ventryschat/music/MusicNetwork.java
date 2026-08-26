package com.example.ventryschat.music;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Canal dédié musique dynamique (séparé du RP). */
public final class MusicNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("ventryschat", "music"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    private static int id;

    private MusicNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(id++, UpsertPacket.class, UpsertPacket::encode, UpsertPacket::decode, UpsertPacket::handle);
        CHANNEL.registerMessage(id++, RemovePacket.class, RemovePacket::encode, RemovePacket::decode, RemovePacket::handle);
        CHANNEL.registerMessage(id++, SnapshotPacket.class, SnapshotPacket::encode, SnapshotPacket::decode, SnapshotPacket::handle);
    }

    public static void broadcastUpsert(MinecraftServer server, MusicZone zone) {
        if (server == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.ALL.noArg(), new UpsertPacket(zone));
    }

    public static void broadcastRemove(MinecraftServer server, UUID zoneId) {
        if (server == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.ALL.noArg(), new RemovePacket(zoneId));
    }

    public static void sendSnapshot(ServerPlayer player, List<MusicZone> zones) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SnapshotPacket(zones));
    }

    public static final class UpsertPacket {
        private final MusicZone zone;

        public UpsertPacket(MusicZone zone) {
            this.zone = zone;
        }

        public void encode(FriendlyByteBuf buf) {
            zone.encode(buf);
        }

        public static UpsertPacket decode(FriendlyByteBuf buf) {
            return new UpsertPacket(MusicZone.decode(buf));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            MusicZone z = zone;
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MusicClientManager.upsert(z)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class RemovePacket {
        private final UUID zoneId;

        public RemovePacket(UUID zoneId) {
            this.zoneId = zoneId;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(zoneId);
        }

        public static RemovePacket decode(FriendlyByteBuf buf) {
            return new RemovePacket(buf.readUUID());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            UUID id = zoneId;
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MusicClientManager.remove(id)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class SnapshotPacket {
        private final List<MusicZone> zones;

        public SnapshotPacket(List<MusicZone> zones) {
            this.zones = zones;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(zones.size());
            for (MusicZone z : zones) {
                z.encode(buf);
            }
        }

        public static SnapshotPacket decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<MusicZone> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(MusicZone.decode(buf));
            }
            return new SnapshotPacket(list);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            List<MusicZone> list = zones;
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MusicClientManager.applySnapshot(list)));
            ctx.get().setPacketHandled(true);
        }
    }
}
