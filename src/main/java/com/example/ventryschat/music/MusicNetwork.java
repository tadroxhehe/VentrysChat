package com.example.ventryschat.music;

import com.example.ventryschat.commands.MusicCommands;
import com.example.ventryschat.compat.VentrysPermsBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Canal dédié musique dynamique (séparé du RP). */
public final class MusicNetwork {
    private static final String PROTOCOL = "2";
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
        CHANNEL.registerMessage(
            id++,
            OpenUrlScreenPacket.class,
            OpenUrlScreenPacket::encode,
            OpenUrlScreenPacket::decode,
            OpenUrlScreenPacket::handle
        );
        CHANNEL.registerMessage(
            id++,
            PlayUrlRequestPacket.class,
            PlayUrlRequestPacket::encode,
            PlayUrlRequestPacket::decode,
            PlayUrlRequestPacket::handle
        );
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

    public static void openUrlScreen(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenUrlScreenPacket());
    }

    /** Client → serveur : URL longue (Discord…). */
    public static void sendPlayUrlRequest(String url, float radius, @Nullable Long durationMs) {
        CHANNEL.sendToServer(new PlayUrlRequestPacket(url, radius, durationMs));
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

    public static final class OpenUrlScreenPacket {
        public void encode(FriendlyByteBuf buf) {
        }

        public static OpenUrlScreenPacket decode(FriendlyByteBuf buf) {
            return new OpenUrlScreenPacket();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                Minecraft.getInstance().setScreen(new MusicUrlPlayScreen())
            ));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class PlayUrlRequestPacket {
        private final String url;
        private final float radius;
        @Nullable
        private final Long durationMs;

        public PlayUrlRequestPacket(String url, float radius, @Nullable Long durationMs) {
            this.url = url;
            this.radius = radius;
            this.durationMs = durationMs;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(url == null ? "" : url, 2048);
            buf.writeFloat(radius);
            buf.writeBoolean(durationMs != null);
            if (durationMs != null) {
                buf.writeLong(durationMs);
            }
        }

        public static PlayUrlRequestPacket decode(FriendlyByteBuf buf) {
            String url = buf.readUtf(2048);
            float radius = buf.readFloat();
            Long dur = null;
            if (buf.readBoolean()) {
                dur = buf.readLong();
            }
            return new PlayUrlRequestPacket(url, radius, dur);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context c = ctx.get();
            c.enqueueWork(() -> {
                if (c.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
                    return;
                }
                ServerPlayer player = c.getSender();
                if (player == null) {
                    return;
                }
                if (!VentrysPermsBridge.staff(player.createCommandSourceStack(), MusicCommands.PERM)) {
                    player.sendMessage(new TextComponent("§cPermission refusée."), player.getUUID());
                    return;
                }
                String reject = MusicServerManager.urlRejectReason(url);
                if (reject != null) {
                    player.sendMessage(new TextComponent("§c" + reject), player.getUUID());
                    return;
                }
                Optional<MusicZone> zone = MusicServerManager.play(player, url, radius, durationMs);
                if (zone.isEmpty()) {
                    player.sendMessage(new TextComponent("§cImpossible de lancer cette URL."), player.getUUID());
                    return;
                }
                MusicZone z = zone.get();
                player.sendMessage(new TextComponent(
                    "§aMusique lancée §7(rayon " + (int) z.radius + ", " + (z.durationMs / 1000) + " s)."
                ), player.getUUID());
            });
            c.setPacketHandled(true);
        }
    }
}
