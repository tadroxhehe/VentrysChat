package com.example.ventryschat.events;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.example.ventryschat.staff.StaffChatService;

/**
 * Gestionnaire des événements de chat côté serveur.
 * Le chat RP par préfixes (*, --, etc.) et la diffusion par distance sont désactivés côté mod :
 * repris par le plugin Bukkit. Ne reste que le chat staff (@).
 */
@Mod.EventBusSubscriber
public class ServerChatHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        try {
            String message = event.getMessage();
            Player player = event.getPlayer();

            if (message == null || message.trim().isEmpty() || player == null) {
                return;
            }

            if (player instanceof ServerPlayer serverPlayer && message.startsWith(StaffChatService.STAFF_PREFIX)) {
                String staffContent = message.substring(StaffChatService.STAFF_PREFIX.length()).trim();
                if (!staffContent.isEmpty()) {
                    event.setCanceled(true);
                    StaffChatService.sendStaffMessage(serverPlayer, staffContent);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors du traitement du chat staff: {}", e.getMessage());
        }
    }
}
