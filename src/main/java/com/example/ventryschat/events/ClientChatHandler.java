package com.example.ventryschat.events;

import com.mojang.logging.LogUtils;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.example.ventryschat.RPMessageHandler;

/**
 * Gestionnaire des événements de chat côté client
 * IMPORTANT: Ce handler ne fait que laisser passer les messages RP
 * Le traitement réel se fait côté serveur pour éviter les conflits
 */
@Mod.EventBusSubscriber
public class ClientChatHandler {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String message = event.getMessage();
        
        // Ne traiter que les messages qui ne sont pas des commandes
        if (message.startsWith("/")) {
            // Laisser passer toutes les commandes normalement
            return;
        }
        
        // Pour les messages RP avec préfixes, laisser passer normalement
        // Le serveur s'occupera du formatage et de la diffusion
        if (RPMessageHandler.isRPMessage(message)) {
            LOGGER.debug("Message RP détecté côté client: '{}' - laissé passer", message);
            return;
        }
        
        // Pour les messages normaux (sans préfixe), laisser passer
        // Le serveur les formatera automatiquement avec [RP]
        LOGGER.debug("Message normal détecté côté client: '{}' - laissé passer", message);
    }
}
