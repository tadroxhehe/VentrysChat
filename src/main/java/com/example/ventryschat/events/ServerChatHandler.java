package com.example.ventryschat.events;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.example.ventryschat.RPConstants;
import com.example.ventryschat.RPMessageHandler;
import com.example.ventryschat.staff.StaffChatService;
import com.example.ventryschat.util.ChatSpeakerNames;

/**
 * Gestionnaire des événements de chat côté serveur
 * Gère le formatage et la diffusion des messages RP
 */
@Mod.EventBusSubscriber
public class ServerChatHandler {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        try {
            String message = event.getMessage();
            Player player = event.getPlayer();
            
            // Validation des paramètres
            if (message == null || message.trim().isEmpty() || player == null) {
                return;
            }

            if (player instanceof ServerPlayer serverPlayer && message.startsWith(StaffChatService.STAFF_PREFIX)) {
                String staffContent = message.substring(StaffChatService.STAFF_PREFIX.length()).trim();
                if (!staffContent.isEmpty()) {
                    event.setCanceled(true);
                    StaffChatService.sendStaffMessage(serverPlayer, staffContent);
                    return;
                }
            }
            
            // Traitement des messages RP avec préfixes
            if (RPMessageHandler.isRPMessage(message) && !message.startsWith("/narration")) {
                processRPMessage(event, message, player);
            }
            // Traitement des messages normaux (sans préfixe)
            else if (!message.startsWith("/")) {
                processNormalMessage(event, message, player);
            }
            // Les commandes sont laissées passer normalement
            
        } catch (Exception e) {
            LOGGER.error("Erreur lors du traitement du message RP: {}", e.getMessage());
            // Ne pas annuler l'événement en cas d'erreur pour éviter de bloquer le chat
        }
    }
    
    /**
     * Traite un message RP avec préfixe
     */
    private static void processRPMessage(ServerChatEvent event, String message, Player player) {
        try {
            String prefix = RPMessageHandler.getPrefix(message);
            String content = RPMessageHandler.getContent(message);
            
            // Déterminer la distance de diffusion
            int distance = RPMessageHandler.getChatDistance(prefix, content);
            
            // Traiter le message (code unifié)
            processMessage(event, player, prefix, content, distance);
                
        } catch (Exception e) {
            LOGGER.error("Erreur lors du traitement du message RP: {}", e.getMessage(), e);
            // En cas d'erreur, ne pas annuler l'événement pour permettre le chat normal
            event.setCanceled(false);
        }
    }
    
    /**
     * Traite un message normal (sans préfixe)
     */
    private static void processNormalMessage(ServerChatEvent event, String message, Player player) {
        try {
            // Distance de 15 blocs pour les messages normaux
            int distance = com.example.ventryschat.config.VentrysChatConfig.normalDistance();
            
            // Traiter le message (code unifié)
            processMessage(event, player, "", message, distance);
                
        } catch (Exception e) {
            LOGGER.error("Erreur lors du traitement du message normal: {}", e.getMessage(), e);
            // En cas d'erreur, ne pas annuler l'événement pour permettre le chat normal
            event.setCanceled(false);
        }
    }
    
    /**
     * Méthode unifiée pour traiter les messages RP et normaux
     * Réduit la duplication de code et améliore la maintenabilité
     */
    private static void processMessage(ServerChatEvent event, Player player, String prefix, String content, int distance) {
        try {
            // Validation supplémentaire
            if (player == null || content == null) {
                event.setCanceled(false);
                return;
            }
            
            // Annuler l'événement original pour le remplacer par notre version
            event.setCanceled(true);
            
            // Créer le nouveau message formaté (cache le nom RP pour éviter les appels répétés)
            var formattedMessage = RPMessageHandler.formatRPMessage(player, prefix, content);
            
            // Envoyer le message formaté aux joueurs dans la distance appropriée
            var nearbyPlayers = RPMessageHandler.getNearbyPlayers(player, distance);
            
            // IMPORTANT: Envoyer le message à l'expéditeur aussi
            try {
                player.sendMessage(formattedMessage, player.getUUID());
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de l'envoi du message à l'expéditeur {}: {}", 
                    player.getName().getString(), e.getMessage());
            }
            
            // Envoyer aux autres joueurs proches de manière optimisée
            int successCount = 0;
            int failCount = 0;
            
            for (Player p : nearbyPlayers) {
                if (p != null && !p.equals(player)) { // Éviter d'envoyer deux fois au même joueur
                    try {
                        // Vérifier que le joueur est toujours connecté (pour ServerPlayer uniquement)
                        if (p.isAlive() && (p instanceof ServerPlayer serverPlayer && serverPlayer.connection != null)) {
                            p.sendMessage(formattedMessage, player.getUUID());
                            successCount++;
                        } else if (p.isAlive()) {
                            // Pour les autres types de joueurs, envoyer quand même
                            p.sendMessage(formattedMessage, player.getUUID());
                            successCount++;
                        }
                    } catch (Exception e) {
                        failCount++;
                        // Ne logger que si c'est une erreur inattendue (pas juste une déconnexion)
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Erreur lors de l'envoi du message à {}: {}", 
                                p.getName().getString(), e.getMessage());
                        }
                    }
                }
            }
            
            // Log de débogage seulement si nécessaire
            if (LOGGER.isDebugEnabled()) {
                String playerName = ChatSpeakerNames.forChat(player);
                LOGGER.debug("Message envoyé par {} avec préfixe '{}' à {} joueurs ({} réussis, {} échoués) dans un rayon de {} blocs", 
                    playerName, prefix.isEmpty() ? "[RP]" : prefix, nearbyPlayers.size() + 1, successCount + 1, failCount, distance);
            }
                
        } catch (Exception e) {
            LOGGER.error("Erreur lors du traitement du message: {}", e.getMessage(), e);
            // En cas d'erreur, ne pas annuler l'événement pour permettre le chat normal
            event.setCanceled(false);
        }
    }
}
