package com.example.ventryschat.staff;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "ventryschat")
public final class VanishManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();

    private VanishManager() {
    }

    public static boolean isVanished(UUID uuid) {
        return VANISHED.contains(uuid);
    }

    public static void setVanished(ServerPlayer player, boolean vanish) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (vanish) {
            VANISHED.add(player.getUUID());
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
            player.setInvisible(true);
            broadcastInfo(server, ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, player);
        } else {
            VANISHED.remove(player.getUUID());
            player.removeEffect(MobEffects.INVISIBILITY);
            player.setInvisible(false);
            broadcastInfo(server, ClientboundPlayerInfoPacket.Action.ADD_PLAYER, player);
        }
    }

    private static void broadcastInfo(MinecraftServer server, ClientboundPlayerInfoPacket.Action action, ServerPlayer subject) {
        ClientboundPlayerInfoPacket packet = new ClientboundPlayerInfoPacket(action, List.of(subject));
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == subject) {
                continue;
            }
            try {
                other.connection.send(packet);
            } catch (Exception e) {
                LOGGER.debug("Erreur envoi PlayerInfoPacket: {}", e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer joiner)) {
            return;
        }
        MinecraftServer server = joiner.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (UUID vanishedId : VANISHED) {
                ServerPlayer vanished = server.getPlayerList().getPlayer(vanishedId);
                if (vanished != null && vanished != joiner) {
                    joiner.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, List.of(vanished)));
                }
            }
            if (VANISHED.contains(joiner.getUUID())) {
                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    if (other == joiner) continue;
                    other.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, List.of(joiner)));
                }
            }
        });
    }
}
