package com.example.ventryschat.data;

import com.example.ventryschat.RPDataManager;
import com.example.ventryschat.network.RPNetworkHandler;
import com.example.ventryschat.network.RPNetworkHandler.SyncRPNamesPacket;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Synchronisation réseau des noms RP (aucune règle métier).
 */
public final class RPNetworkSync {

    private RPNetworkSync() {
    }

    public static void syncPlayerToAllClients(UUID playerUUID, String firstName, String lastName, Logger logger) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }

            if (playerUUID == null) {
                logger.warn("Tentative de synchronisation avec UUID null");
                return;
            }

            SyncRPNamesPacket packet = new SyncRPNamesPacket(playerUUID, firstName, lastName);

            try {
                RPNetworkHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                    packet
                );
            } catch (Exception networkError) {
                logger.warn("Erreur lors de l'envoi groupé, tentative d'envoi individuel : {}", networkError.getMessage());
                try {
                    for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                        if (player != null && player.connection != null) {
                            RPNetworkHandler.INSTANCE.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                packet
                            );
                        }
                    }
                } catch (Exception fallbackError) {
                    logger.error("Erreur lors de l'envoi individuel de fallback : {}", fallbackError.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation réseau : {}", e.getMessage(), e);
        }
    }

    public static void syncAllPlayers(Map<UUID, RPDataManager.PlayerRPData> playerData, Logger logger) {
        try {
            List<SyncRPNamesPacket> batchPackets = new ArrayList<>();
            for (Map.Entry<UUID, RPDataManager.PlayerRPData> entry : playerData.entrySet()) {
                RPDataManager.PlayerRPData data = entry.getValue();
                if (data != null) {
                    batchPackets.add(new SyncRPNamesPacket(entry.getKey(), data.firstName, data.lastName));
                }
            }

            if (!batchPackets.isEmpty()) {
                RPNetworkHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                    new RPNetworkHandler.BatchSyncRPNamesPacket(batchPackets)
                );
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation complète : {}", e.getMessage());
        }
    }
}
