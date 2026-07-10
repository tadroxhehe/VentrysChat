package com.example.ventryschat.util;

import com.example.ventryschat.RPDataManager;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Résout le nom affiché dans le chat : nom RP si défini, sinon pseudo Minecraft (jamais vide).
 */
public final class ChatSpeakerNames {

    private ChatSpeakerNames() {
    }

    public static String forChat(Player player) {
        if (player == null) {
            return "Joueur";
        }

        UUID playerUUID = player.getUUID();
        if (playerUUID != null) {
            String rpName = RPDataManager.getFullName(playerUUID);
            if (rpName != null && !rpName.isBlank()) {
                return rpName.trim();
            }
        }

        return minecraftUsername(player);
    }

    public static String minecraftUsername(Player player) {
        if (player == null) {
            return "Joueur";
        }
        String profileName = player.getGameProfile().getName();
        if (profileName != null && !profileName.isBlank()) {
            return profileName;
        }
        String componentName = player.getName().getString();
        if (componentName != null && !componentName.isBlank()) {
            return componentName;
        }
        return "Joueur";
    }
}
