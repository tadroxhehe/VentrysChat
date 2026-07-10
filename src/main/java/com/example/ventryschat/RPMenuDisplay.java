package com.example.ventryschat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestionnaire de l'affichage des noms RP dans l'interface
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class RPMenuDisplay {
    
    private static final Map<UUID, String> playerFirstNames = new HashMap<>();
    private static final Map<UUID, String> playerLastNames = new HashMap<>();
    
    /**
     * Définit les noms RP d'un joueur
     */
    public static void setPlayerNames(UUID playerUUID, String firstName, String lastName) {
        if (playerUUID == null) {
            return;
        }

        if (firstName != null && !firstName.isEmpty()) {
            playerFirstNames.put(playerUUID, firstName);
        } else {
            playerFirstNames.remove(playerUUID);
        }
        if (lastName != null && !lastName.isEmpty()) {
            playerLastNames.put(playerUUID, lastName);
        } else {
            playerLastNames.remove(playerUUID);
        }
    }
    
    /**
     * Obtient le nom RP complet d'un joueur
     */
    public static String getRPDisplayName(UUID playerUUID) {
        String firstName = playerFirstNames.get(playerUUID);
        String lastName = playerLastNames.get(playerUUID);
        
        if (firstName != null && lastName != null && !firstName.isEmpty() && !lastName.isEmpty()) {
            return firstName + " " + lastName;
        } else if (firstName != null && !firstName.isEmpty()) {
            return firstName;
        } else if (lastName != null && !lastName.isEmpty()) {
            return lastName;
        }
        return "";
    }
    
    /**
     * Événement de rendu de l'interface de jeu
     */
    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Afficher le nom RP UNIQUEMENT quand on est dans un menu (screen != null)
        if (mc.screen == null) return;
        
        // Obtenir le nom RP du joueur
        String rpName = getRPDisplayName(mc.player.getUUID());
        if (rpName.isEmpty()) return;
        
        Font font = mc.font;
        
        // Position en haut à gauche avec un petit décalage
        int x = 10;
        int y = 10;
        
        // Couleur grise discrète pour le nom RP
        int color = 0xFF808080; // Gris moyen
        
        // Afficher le nom RP en gris
        font.draw(event.getMatrixStack(), rpName, x, y, color);
    }
}
