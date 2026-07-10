package com.example.ventryschat;

import com.example.ventryschat.config.VentrysChatConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.example.ventryschat.util.ChatSpeakerNames;

/**
 * Classe utilitaire pour gérer le traitement des messages RP
 */
public final class RPMessageHandler {
    
    private RPMessageHandler() {
        // Classe utilitaire, pas d'instanciation
    }
    
    /**
     * Vérifie si un message est un message RP
     */
    public static boolean isRPMessage(String message) {
        // Validation des entrées
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        // Vérifier tous les préfixes RP dans l'ordre de priorité
        // Utilisation de startsWith pour une détection rapide
        return message.startsWith(RPConstants.CHUCHOT_PREFIX) ||
               message.startsWith(RPConstants.NARRATION_PREFIX) ||
               message.startsWith(RPConstants.HRP_PREFIX) ||
               message.startsWith(RPConstants.WHISPER_PREFIX) ||
               message.startsWith(RPConstants.SHOUT_PREFIX) ||
               message.startsWith(RPConstants.SCREAM_PREFIX) ||
               message.startsWith(RPConstants.ACTION_PREFIX);
    }
    
    /**
     * Vérifie si un message est une commande
     */
    public static boolean isCommand(String message) {
        return message != null && message.startsWith("/");
    }
    
    /**
     * Extrait le préfixe RP d'un message
     * IMPORTANT: L'ordre des vérifications est crucial pour éviter les conflits
     */
    public static String getPrefix(String message) {
        // Validation des entrées
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        
        // Vérifier d'abord les préfixes les plus longs pour éviter les conflits
        if (message.startsWith(RPConstants.CHUCHOT_PREFIX)) return RPConstants.CHUCHOT_PREFIX;
        if (message.startsWith(RPConstants.NARRATION_PREFIX)) return RPConstants.NARRATION_PREFIX;
        if (message.startsWith(RPConstants.HRP_PREFIX)) return RPConstants.HRP_PREFIX;
        if (message.startsWith(RPConstants.WHISPER_PREFIX)) return RPConstants.WHISPER_PREFIX;
        if (message.startsWith(RPConstants.SHOUT_PREFIX)) return RPConstants.SHOUT_PREFIX;
        if (message.startsWith(RPConstants.SCREAM_PREFIX)) return RPConstants.SCREAM_PREFIX;
        if (message.startsWith(RPConstants.ACTION_PREFIX)) return RPConstants.ACTION_PREFIX;
        return "";
    }
    
    /**
     * Extrait le contenu d'un message RP (sans le préfixe)
     */
    public static String getContent(String message) {
        // Validation des entrées
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        
        String prefix = getPrefix(message);
        if (prefix.isEmpty()) return message.trim();
        
        // Enlever le préfixe et les espaces en début
        String content = message.substring(prefix.length()).trim();
        
        // Pour la narration, enlever la distance si présente
        if (prefix.equals(RPConstants.NARRATION_PREFIX) && content.contains(":")) {
            String[] parts = content.split(":", 2);
            if (parts.length == 2) {
                content = parts[1].trim();
            }
        }
        
        return content;
    }
    
    /**
     * Détermine la distance de diffusion d'un message RP
     */
    public static int getChatDistance(String prefix, String content) {
        if (prefix.equals(RPConstants.WHISPER_PREFIX)) {
            return VentrysChatConfig.whisperDistance();
        } else if (prefix.equals(RPConstants.SHOUT_PREFIX)) {
            return VentrysChatConfig.shoutDistance();
        } else if (prefix.equals(RPConstants.SCREAM_PREFIX)) {
            return VentrysChatConfig.screamDistance();
        } else if (prefix.equals(RPConstants.ACTION_PREFIX)) {
            return VentrysChatConfig.actionDistance();
        } else if (prefix.equals(RPConstants.CHUCHOT_PREFIX)) {
            return VentrysChatConfig.chuchotDistance();
        } else if (prefix.equals(RPConstants.NARRATION_PREFIX)) {
            return getNarrationDistance(content);
        } else {
            return VentrysChatConfig.normalDistance();
        }
    }
    
    /**
     * Extrait la distance personnalisée pour les messages de narration
     * Format attendu : "distance:message" (ex: "50:Le vent souffle fort")
     */
    private static int getNarrationDistance(String content) {
        if (content.contains(":")) {
            String[] parts = content.split(":", 2);
            if (parts.length == 2) {
                try {
                    String distanceStr = parts[0].trim();
                    // Parse manuel pour éviter le coût regex dans un chemin fréquent
                    StringBuilder digits = new StringBuilder(distanceStr.length());
                    for (int i = 0; i < distanceStr.length(); i++) {
                        char c = distanceStr.charAt(i);
                        if (c >= '0' && c <= '9') {
                            digits.append(c);
                        }
                    }
                    if (digits.length() > 0) {
                        int customDistance = Integer.parseInt(digits.toString());
                        return Math.max(
                            VentrysChatConfig.minNarrationDistance(),
                            Math.min(VentrysChatConfig.maxNarrationDistance(), customDistance));
                    }
                } catch (NumberFormatException e) {
                    return VentrysChatConfig.defaultNarrationDistance();
                }
            }
        }
        return VentrysChatConfig.defaultNarrationDistance();
    }
    
    /**
     * Obtient la liste des joueurs dans un rayon donné autour d'un joueur
     * Optimisé avec distanceToSqr pour éviter les calculs de racine carrée coûteux
     */
    public static List<Player> getNearbyPlayers(Player sender, int distance) {
        try {
            // Validation des paramètres
            if (sender == null || distance <= 0) {
                return new ArrayList<>();
            }
            
            var server = sender.getServer();
            if (server == null) {
                return new ArrayList<>();
            }
            
            // Utiliser la distance au carré pour éviter les calculs de racine carrée
            // distance² = distance * distance (plus rapide que Math.sqrt)
            double distanceSquared = (double) distance * distance;
            
            return server.getPlayerList().getPlayers().stream()
                .filter(player -> {
                    try {
                        // Vérification rapide de la distance
                        if (player == null || player.equals(sender)) {
                            return false;
                        }
                        
                        // Vérifier que le joueur est dans le même monde
                        if (player.level != sender.level) {
                            return false;
                        }
                        
                        // Calcul de distance optimisé avec distanceToSqr (évite Math.sqrt)
                        double actualDistanceSquared = sender.distanceToSqr(player);
                        return actualDistanceSquared <= distanceSquared;
                    } catch (Exception e) {
                        // En cas d'erreur, exclure le joueur pour éviter les problèmes
                        return false;
                    }
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            // En cas d'erreur, retourner une liste vide pour éviter les crashs
            return new ArrayList<>();
        }
    }
    
    /**
     * Formate un message RP avec les couleurs appropriées
     */
    public static Component formatRPMessage(Player player, String prefix, String content) {
        try {
            // Validation des paramètres
            if (player == null) {
                return new TextComponent("§c[ERREUR] Joueur invalide");
            }
            
            if (content == null) {
                content = "";
            }
            
            String playerName = ChatSpeakerNames.forChat(player);
            
            // Formatage selon le préfixe
            if (prefix != null) {
                if (prefix.equals(RPConstants.ACTION_PREFIX)) {
                    return new TextComponent(RPConstants.ACTION_COLOR + "§o" + playerName + " " + content);
                }
                
                if (prefix.equals(RPConstants.HRP_PREFIX)) {
                    return new TextComponent(RPConstants.HRP_COLOR + "[HRP] " + playerName + " : " + content);
                }
                
                if (prefix.equals(RPConstants.NARRATION_PREFIX)) {
                    return formatNarrationMessage(content);
                }
                
                if (prefix.equals(RPConstants.WHISPER_PREFIX)) {
                    return new TextComponent(RPConstants.WHISPER_COLOR + "[Chuchotement] " + playerName + " : " + content);
                }
                
                if (prefix.equals(RPConstants.SHOUT_PREFIX)) {
                    return new TextComponent(RPConstants.SHOUT_COLOR + "[CRI] " + playerName + " : " + content);
                }
                
                if (prefix.equals(RPConstants.SCREAM_PREFIX)) {
                    return new TextComponent(RPConstants.SCREAM_COLOR + "[HURLEMENT] " + playerName + " : " + content);
                }
                
                if (prefix.equals(RPConstants.CHUCHOT_PREFIX)) {
                    return new TextComponent(RPConstants.CHUCHOT_COLOR + "[Chuchot] " + playerName + " : " + content);
                }
            }
            
            // Message normal si aucun préfixe valide (messages sans préfixe)
            return new TextComponent(RPConstants.NORMAL_COLOR + "[RP] " + playerName + " : " + content);
            
        } catch (Exception e) {
            // En cas d'erreur, retourner un message d'erreur formaté
            return new TextComponent("§c[ERREUR] Problème de formatage du message RP");
        }
    }
    
    /**
     * Formate un message de narration avec gestion des erreurs
     */
    private static Component formatNarrationMessage(String content) {
        try {
            // Format spécial pour la narration
            String displayContent = content;
            if (content.contains(":")) {
                String[] parts = content.split(":", 2);
                if (parts.length == 2) {
                    displayContent = parts[1].trim();
                }
            }
            
            // Ajouter des retours à la ligne pour les longs textes
            String formattedContent = addLineBreaks(displayContent, 50);
            
            // Format simplifié : [narration] + texte en gris
            return new TextComponent(RPConstants.NARRATION_TITLE_COLOR + "[narration]\n" + 
                                   RPConstants.NARRATION_TEXT_COLOR + formattedContent);
        } catch (Exception e) {
            // En cas d'erreur, retourner un message simple
            return new TextComponent(RPConstants.NARRATION_TITLE_COLOR + "[narration]\n" + 
                                   RPConstants.NARRATION_TEXT_COLOR + content);
        }
    }
    
    /**
     * Ajoute des retours à la ligne pour éviter que les longs textes prennent trop d'espace
     */
    private static String addLineBreaks(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        String[] words = text.split(" ");
        int currentLineLength = 0;
        
        for (String word : words) {
            if (currentLineLength + word.length() + 1 > maxLength) {
                result.append("\n");
                currentLineLength = word.length();
            } else {
                if (currentLineLength > 0) {
                    result.append(" ");
                    currentLineLength++;
                }
                currentLineLength += word.length();
            }
            result.append(word);
        }
        
        return result.toString();
    }
}
