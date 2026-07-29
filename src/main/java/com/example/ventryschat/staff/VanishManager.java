package com.example.ventryschat.staff;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanish réel : retire l'entité + tab des autres clients.
 * {@code setInvisible} seul laisse les spectateurs / staff voir un fantôme transparent.
 */
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
            applyInvisibleState(player);
            hideFromEveryone(server, player);
        } else {
            VANISHED.remove(player.getUUID());
            player.removeEffect(MobEffects.INVISIBILITY);
            player.setInvisible(false);
            showToEveryone(server, player);
        }
    }

    private static void applyInvisibleState(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        player.setInvisible(true);
    }

    /** Retire tab + modèle pour tous les autres joueurs (y compris staff / spectateur). */
    private static void hideFromEveryone(MinecraftServer server, ServerPlayer subject) {
        ClientboundPlayerInfoPacket removeInfo =
                new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, List.of(subject));
        ClientboundRemoveEntitiesPacket removeEntity = new ClientboundRemoveEntitiesPacket(subject.getId());
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == subject) {
                continue;
            }
            try {
                other.connection.send(removeInfo);
                other.connection.send(removeEntity);
            } catch (Exception e) {
                LOGGER.debug("Erreur hide vanish: {}", e.getMessage());
            }
        }
    }

    private static void showToEveryone(MinecraftServer server, ServerPlayer subject) {
        ClientboundPlayerInfoPacket addInfo =
                new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, List.of(subject));
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == subject) {
                continue;
            }
            try {
                other.connection.send(addInfo);
                if (other.getLevel() == subject.getLevel()
                        && other.distanceToSqr(subject) < trackingRangeSq(other)) {
                    spawnForViewer(other, subject);
                }
            } catch (Exception e) {
                LOGGER.debug("Erreur show vanish: {}", e.getMessage());
            }
        }
    }

    private static double trackingRangeSq(ServerPlayer viewer) {
        // Portée de tracking joueur vanilla ~128 blocs (souvent 64–128 selon view distance).
        int range = Math.max(64, viewer.getLevel().getServer().getPlayerList().getViewDistance() * 16);
        return (double) range * (double) range;
    }

    private static void spawnForViewer(ServerPlayer viewer, ServerPlayer subject) {
        viewer.connection.send(new ClientboundAddPlayerPacket(subject));
        viewer.connection.send(new ClientboundRotateHeadPacket(subject, headRotationByte(subject.getYHeadRot())));
        viewer.connection.send(new ClientboundSetEntityDataPacket(subject.getId(), subject.getEntityData(), true));

        List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>(6);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = subject.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                equipment.add(Pair.of(slot, stack.copy()));
            }
        }
        if (!equipment.isEmpty()) {
            viewer.connection.send(new ClientboundSetEquipmentPacket(subject.getId(), equipment));
        }
    }

    private static byte headRotationByte(float yHeadRot) {
        return (byte) net.minecraft.util.Mth.floor(yHeadRot * 256.0F / 360.0F);
    }

    /**
     * Dès qu'un client commence à tracker un vanished, on détruit l'entité chez lui
     * (sinon le fantôme transparent réapparaît à l'entrée en chunk).
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof ServerPlayer target) || !isVanished(target.getUUID())) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer viewer) || viewer == target) {
            return;
        }
        MinecraftServer server = viewer.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            if (!isVanished(target.getUUID()) || viewer.connection == null) {
                return;
            }
            try {
                viewer.connection.send(new ClientboundRemoveEntitiesPacket(target.getId()));
            } catch (Exception e) {
                LOGGER.debug("Erreur StartTracking vanish: {}", e.getMessage());
            }
        });
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
                    joiner.connection.send(new ClientboundPlayerInfoPacket(
                            ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, List.of(vanished)));
                    joiner.connection.send(new ClientboundRemoveEntitiesPacket(vanished.getId()));
                }
            }
            if (VANISHED.contains(joiner.getUUID())) {
                applyInvisibleState(joiner);
                hideFromEveryone(server, joiner);
            }
        });
    }
}
