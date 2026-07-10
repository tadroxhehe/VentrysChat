package com.example.ventryschat.events;

import com.example.ventryschat.RPDataManager;
import com.example.ventryschat.AptitudesManager;
import com.example.ventryschat.AptitudesEffectsManager;
import com.example.ventryschat.network.RPNetworkHandler;
import com.example.ventryschat.network.RPNetworkHandler.SyncRPNamesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Gestionnaire des événements de connexion des joueurs
 */
@Mod.EventBusSubscriber
public class PlayerConnectionEvents {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Événement de connexion d'un joueur
     * Optimisé pour éviter les problèmes réseau
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Utiliser le thread du serveur pour éviter les problèmes de concurrence
        var server = serverPlayer.getServer();
        if (server == null) {
            return;
        }
        
        // Délayer légèrement la synchronisation pour s'assurer que le joueur est complètement connecté
        server.execute(() -> {
            UUID playerUUID = serverPlayer.getUUID();
            String firstName = RPDataManager.getFirstName(playerUUID);
            String lastName = RPDataManager.getLastName(playerUUID);
            
            // Envoyer les données RP au joueur qui se connecte
            if (!firstName.isEmpty() || !lastName.isEmpty()) {
                SyncRPNamesPacket packet = new SyncRPNamesPacket(playerUUID, firstName, lastName);
                RPNetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    packet
                );
                
                LOGGER.debug("Données RP synchronisées pour le joueur {} : {} {}", 
                    serverPlayer.getName().getString(), firstName, lastName);
            }
            
            // Synchroniser avec tous les autres joueurs connectés de manière optimisée
            syncToOtherPlayers(serverPlayer, playerUUID, firstName, lastName);
            
            // Envoyer les données de tous les autres joueurs au nouveau joueur
            syncAllPlayersToNewPlayer(serverPlayer);
            
            // Marquer la connexion comme éligible pour les aptitudes
            AptitudesManager.marquerConnexionEligible(playerUUID);
            
            // Appliquer les effets permanents basés sur les aptitudes
            AptitudesEffectsManager.applyMartialiteEffects(serverPlayer);
        });
    }
    
    /**
     * Synchronise les données de tous les joueurs existants vers le nouveau joueur
     * Optimisé avec un packet batch pour réduire le nombre de packets réseau
     */
    private static void syncAllPlayersToNewPlayer(ServerPlayer newPlayer) {
        try {
            var server = newPlayer.getServer();
            if (server == null) {
                return;
            }
            
            // Collecter toutes les données à synchroniser d'abord
            var playersToSync = new java.util.ArrayList<RPNetworkHandler.SyncRPNamesPacket>();
            
            for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
                if (otherPlayer != null && otherPlayer != newPlayer) {
                    UUID otherUUID = otherPlayer.getUUID();
                    String firstName = RPDataManager.getFirstName(otherUUID);
                    String lastName = RPDataManager.getLastName(otherUUID);
                    
                    if (!firstName.isEmpty() || !lastName.isEmpty()) {
                        playersToSync.add(new RPNetworkHandler.SyncRPNamesPacket(otherUUID, firstName, lastName));
                    }
                }
            }
            
            // Utiliser un packet batch pour envoyer toutes les données en une seule fois
            // Beaucoup plus efficace que d'envoyer N packets individuels
            if (!playersToSync.isEmpty()) {
                try {
                    RPNetworkHandler.BatchSyncRPNamesPacket batchPacket = 
                        new RPNetworkHandler.BatchSyncRPNamesPacket(playersToSync);
                    RPNetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> newPlayer),
                        batchPacket
                    );
                    LOGGER.debug("Synchronisé {} joueurs vers le nouveau joueur {} (packet batch)", 
                        playersToSync.size(), newPlayer.getName().getString());
                } catch (Exception e) {
                    LOGGER.warn("Erreur lors de l'envoi du packet batch, tentative individuelle : {}", e.getMessage());
                    // Fallback : envoyer individuellement si le batch échoue
                    for (RPNetworkHandler.SyncRPNamesPacket packet : playersToSync) {
                        try {
                            RPNetworkHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> newPlayer),
                                packet
                            );
                        } catch (Exception fallbackError) {
                            LOGGER.warn("Erreur lors de l'envoi d'un packet de synchronisation : {}", fallbackError.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la synchronisation des autres joueurs vers le nouveau joueur : {}", 
                e.getMessage(), e);
        }
    }
    
    /**
     * Événement de déconnexion d'un joueur
     * Optimisé pour éviter les blocages lors de la déconnexion
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Sauvegarde de secours lors de la déconnexion
        try {
            LOGGER.debug("Flush disque coalescé demandé (déconnexion {})", event.getPlayer().getName().getString());
            
            // Utiliser le thread du serveur si disponible
            var server = event.getPlayer().getServer();
            if (server != null) {
                RPDataManager.scheduleCoalescedDiskFlush(server);
            } else {
                RPDataManager.saveData();
                AptitudesManager.saveData();
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la sauvegarde de secours pour {} : {}", 
                event.getPlayer().getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Synchronise les données RP d'un joueur avec tous les autres joueurs connectés
     * Optimisé pour utiliser PacketDistributor.ALL au lieu d'envoyer individuellement
     */
    private static void syncToOtherPlayers(ServerPlayer sourcePlayer, UUID playerUUID, String firstName, String lastName) {
        try {
            var server = sourcePlayer.getServer();
            if (server == null) {
                return;
            }
            
            // Utiliser PacketDistributor.ALL pour envoyer à tous les joueurs en une seule opération
            // Plus efficace que d'envoyer individuellement
            SyncRPNamesPacket packet = new SyncRPNamesPacket(playerUUID, firstName, lastName);
            RPNetworkHandler.INSTANCE.send(
                PacketDistributor.ALL.noArg(),
                packet
            );
            
            LOGGER.debug("Données RP du joueur {} synchronisées avec tous les joueurs", 
                sourcePlayer.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la synchronisation avec les autres joueurs : {}", e.getMessage());
        }
    }
    
}
