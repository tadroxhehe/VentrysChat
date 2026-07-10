package com.example.ventryschat;

import com.example.ventryschat.util.ChatLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Gestionnaire des aptitudes des joueurs
 * Utilise maintenant RPDataManager pour stocker toutes les données ensemble
 */
public class AptitudesManager {
    
    private static final String HISTORY_FILE = "ventryschat_aptitudes_history.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson GSON_SAVE = new GsonBuilder().create();
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Historique des give globaux (séparé car partagé entre tous les joueurs)
    private static AptitudesHistory history = new AptitudesHistory();
    
    /**
     * Classe pour l'historique des give globaux
     */
    public static class AptitudesHistory {
        public long dernierGiveGlobal = 0;  // Timestamp du dernier give global
        public int nombreGiveGlobaux = 0;    // Nombre total de give globaux effectués
        
        public AptitudesHistory() {
            // Constructeur par défaut pour Gson
        }
    }
    
    /**
     * Obtient les données d'aptitudes d'un joueur depuis RPDataManager (crée si nécessaire)
     */
    public static RPDataManager.PlayerRPData getOrCreateAptitudesData(UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        return RPDataManager.getOrCreatePlayerData(playerUUID);
    }
    
    /**
     * Obtient les données d'aptitudes d'un joueur depuis RPDataManager (retourne null si inexistant)
     */
    public static RPDataManager.PlayerRPData getAptitudesData(UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        return RPDataManager.getPlayerData(playerUUID);
    }
    
    /**
     * Vérifie si un joueur est éligible pour le give global
     */
    public static boolean estEligiblePourGiveGlobal(UUID playerUUID) {
        RPDataManager.PlayerRPData data = getAptitudesData(playerUUID);
        if (data == null) {
            return false;
        }
        
        // Vérifier que le joueur a un focus défini
        if (data.focus == null || data.focus.isEmpty()) {
            return false;
        }
        
        // Vérifier que le joueur n'est pas en cooldown
        if (data.estEnCooldown()) {
            return false;
        }
        
        // Vérifier que le joueur s'est connecté entre les 2 derniers give globaux
        if (history.dernierGiveGlobal == 0) {
            // Premier give global, tous les joueurs avec connexion éligible sont éligibles
            return data.derniereConnexionEligible > 0;
        }
        
        // Vérifier que la dernière connexion éligible est après le dernier give global
        // ou qu'il n'y a pas eu de give global depuis plus de 14 jours
        long maintenant = System.currentTimeMillis();
        long dureeEntreGiveGlobaux = 14L * 24L * 60L * 60L * 1000L; // 14 jours
        
        // Si le dernier give global était il y a plus de 14 jours, tous les joueurs connectés sont éligibles
        if (maintenant - history.dernierGiveGlobal >= dureeEntreGiveGlobaux) {
            return data.derniereConnexionEligible > 0;
        }
        
        // Sinon, vérifier que la connexion est après le dernier give global
        return data.derniereConnexionEligible > history.dernierGiveGlobal;
    }
    
    /**
     * Marque la connexion d'un joueur comme éligible (appelé à la connexion)
     */
    public static void marquerConnexionEligible(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }
        
        RPDataManager.PlayerRPData data = getOrCreateAptitudesData(playerUUID);
        
        // Si le joueur n'est pas en cooldown, marquer la connexion comme éligible
        if (!data.estEnCooldown()) {
            data.derniereConnexionEligible = System.currentTimeMillis();
            RPDataManager.markUnsavedChanges();
            LOGGER.debug("Connexion éligible marquée pour le joueur {}", playerUUID);
        } else {
            LOGGER.debug("Connexion non éligible pour le joueur {} (en cooldown)", playerUUID);
        }
    }
    
    /**
     * Change le focus d'un joueur (active le cooldown de 14 jours)
     */
    public static void changerFocus(UUID playerUUID, String nouveauFocus) {
        if (playerUUID == null) {
            return;
        }
        
        RPDataManager.PlayerRPData data = getOrCreateAptitudesData(playerUUID);
        
        // Vérifier si le focus change vraiment
        if (nouveauFocus != null && nouveauFocus.equals(data.focus)) {
            return; // Pas de changement
        }
        
        // Changer le focus et activer le cooldown
        data.focus = nouveauFocus;
        data.dateChangementFocus = System.currentTimeMillis();
        RPDataManager.markUnsavedChanges();
        
        ChatLog.detail(LOGGER,"Focus changé pour le joueur {} : {}", playerUUID, nouveauFocus);
    }
    
    /**
     * Donne les points initiaux (5 points à répartir) à un joueur
     */
    public static boolean donnerPointsInitiaux(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }
        
        RPDataManager.PlayerRPData data = getOrCreateAptitudesData(playerUUID);
        
        if (data.pointsInitiauxDonnes) {
            return false; // Déjà donnés
        }
        
        data.pointsARepartir += 5;
        data.pointsInitiauxDonnes = true;
        RPDataManager.markUnsavedChanges();
        
        ChatLog.detail(LOGGER,"Points initiaux donnés au joueur {} : 5 points à répartir", playerUUID);
        return true;
    }
    
    /**
     * Donne des points à un joueur (consomme les points à répartir si disponibles)
     */
    public static boolean donnerPoints(UUID playerUUID, String typeAptitude, int nombrePoints) {
        if (playerUUID == null || typeAptitude == null || nombrePoints <= 0) {
            return false;
        }
        
        RPDataManager.PlayerRPData data = getOrCreateAptitudesData(playerUUID);
        
        // Vérifier les limites
        if (!data.peutRecevoirPoints(nombrePoints, typeAptitude)) {
            return false;
        }
        
        // Consommer les points à répartir si disponibles
        int pointsAConsommer = Math.min(nombrePoints, data.pointsARepartir);
        if (pointsAConsommer > 0) {
            data.consommerPointsARepartir(pointsAConsommer);
        }
        
        // Ajouter les points à l'aptitude
        data.ajouterPoints(typeAptitude, nombrePoints);
        RPDataManager.markUnsavedChanges();
        
        // Réappliquer les effets si c'est la martialité qui a changé
        if (typeAptitude.equalsIgnoreCase("martialité") || typeAptitude.equalsIgnoreCase("martialite")) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                if (player != null) {
                    com.example.ventryschat.AptitudesEffectsManager.applyMartialiteEffects(player);
                }
            }
        }
        
        ChatLog.detail(LOGGER,"Points donnés au joueur {} : {} points dans {}", playerUUID, nombrePoints, typeAptitude);
        return true;
    }
    
    /**
     * Effectue un give global (donne 1 point dans le focus de tous les joueurs éligibles)
     */
    public static int effectuerGiveGlobal() {
        int nombreJoueursEligibles = 0;
        int nombreJoueursAyantRecu = 0;
        
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.error("Impossible d'effectuer le give global : serveur non disponible");
            return 0;
        }
        
        // Optimisation : parcourir uniquement les joueurs connectés d'abord pour éviter
        // de traiter tous les UUIDs de la base de données
        var connectedPlayers = server.getPlayerList().getPlayers();
        var processedUUIDs = new java.util.HashSet<UUID>(connectedPlayers.size());
        
        // Traiter d'abord les joueurs connectés (cas le plus fréquent)
        for (ServerPlayer player : connectedPlayers) {
            if (player == null) continue;
            UUID playerUUID = player.getUUID();
            processedUUIDs.add(playerUUID);
            
            RPDataManager.PlayerRPData data = RPDataManager.getPlayerData(playerUUID);
            if (data == null) continue;
            
            if (estEligiblePourGiveGlobal(playerUUID)) {
                nombreJoueursEligibles++;
                
                // Donner 1 point dans le focus du joueur
                if (donnerPoints(playerUUID, data.focus, 1)) {
                    nombreJoueursAyantRecu++;
                    
                    // Réappliquer les effets si c'est la martialité qui a reçu le point
                    if (data.focus != null && (data.focus.equalsIgnoreCase("martialité") || data.focus.equalsIgnoreCase("martialite"))) {
                        com.example.ventryschat.AptitudesEffectsManager.applyMartialiteEffects(player);
                    }
                }
            }
        }
        
        // Traiter ensuite les joueurs non connectés mais éligibles (moins fréquent)
        // Cela permet de donner des points même aux joueurs déconnectés
        for (UUID playerUUID : RPDataManager.getAllPlayerUUIDs()) {
            if (processedUUIDs.contains(playerUUID)) continue; // Déjà traité
            
            RPDataManager.PlayerRPData data = RPDataManager.getPlayerData(playerUUID);
            if (data == null) continue;
            
            if (estEligiblePourGiveGlobal(playerUUID)) {
                nombreJoueursEligibles++;
                
                // Donner 1 point dans le focus du joueur (joueur déconnecté)
                if (donnerPoints(playerUUID, data.focus, 1)) {
                    nombreJoueursAyantRecu++;
                    // Pas besoin de réappliquer les effets pour un joueur déconnecté
                }
            }
        }
        
        // Mettre à jour l'historique
        history.dernierGiveGlobal = System.currentTimeMillis();
        history.nombreGiveGlobaux++;
        saveHistory();
        
        ChatLog.detail(LOGGER,"Give global effectué : {} joueurs éligibles, {} ont reçu le point", 
            nombreJoueursEligibles, nombreJoueursAyantRecu);
        
        return nombreJoueursAyantRecu;
    }
    
    /**
     * Réinitialise les aptitudes d'un joueur
     */
    public static void resetAptitudes(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }
        
        RPDataManager.PlayerRPData data = RPDataManager.getPlayerData(playerUUID);
        if (data != null) {
            // Réinitialiser uniquement les aptitudes, garder les autres données RP
            data.martialite = 0;
            data.artisanat = 0;
            data.savoir = 0;
            data.pointsARepartir = 0;
            data.focus = null;
            data.pointsInitiauxDonnes = false;
            data.dateChangementFocus = 0;
            data.derniereConnexionEligible = 0;
            RPDataManager.markUnsavedChanges();
        }
        
        ChatLog.detail(LOGGER,"Aptitudes réinitialisées pour le joueur {}", playerUUID);
    }
    
    /**
     * Obtient l'historique des give globaux
     */
    public static AptitudesHistory getHistory() {
        return history;
    }
    
    /**
     * Charge l'historique depuis le fichier
     * Les données des aptitudes sont maintenant gérées par RPDataManager
     */
    public static void loadData() {
        Path historyPath = getHistoryPath();
        
        // Charger uniquement l'historique (les données des joueurs sont dans RPDataManager)
        if (Files.exists(historyPath)) {
            try (Reader reader = Files.newBufferedReader(historyPath)) {
                history = GSON.fromJson(reader, AptitudesHistory.class);
                if (history == null) {
                    history = new AptitudesHistory();
                }
                ChatLog.detail(LOGGER,"✅ Historique des aptitudes chargé : dernier give global = {}", 
                    history.dernierGiveGlobal > 0 ? new java.util.Date(history.dernierGiveGlobal) : "jamais");
            } catch (Exception e) {
                LOGGER.error("❌ Erreur lors du chargement de l'historique : {}", e.getMessage());
                history = new AptitudesHistory();
            }
        } else {
            history = new AptitudesHistory();
            ChatLog.detail(LOGGER,"📂 Aucun historique trouvé, création d'un nouvel historique");
        }
    }
    
    /**
     * Sauvegarde l'historique dans le fichier
     * Les données des aptitudes sont maintenant sauvegardées par RPDataManager
     */
    public static synchronized boolean saveData() {
        return saveHistory();
    }
    
    /**
     * Sauvegarde uniquement l'historique
     */
    private static boolean saveHistory() {
        Path historyPath = getHistoryPath();
        
        try {
            // Créer le dossier parent si nécessaire
            Path parentDir = historyPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Sauvegarder l'historique
            try (Writer writer = Files.newBufferedWriter(historyPath)) {
                GSON_SAVE.toJson(history, writer);
            }
            
            LOGGER.debug("Historique des aptitudes sauvegardé");
            return true;
        } catch (Exception e) {
            LOGGER.error("❌ Erreur lors de la sauvegarde de l'historique : {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtient le chemin du fichier d'historique
     */
    private static Path getHistoryPath() {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.world.level.storage.LevelResource rootResource = net.minecraft.world.level.storage.LevelResource.ROOT;
                if (rootResource != null) {
                    Path serverPath = server.getWorldPath(rootResource);
                    return serverPath.resolve(HISTORY_FILE);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Pas de serveur détecté, utilisation du chemin client");
        }
        
        return FMLPaths.GAMEDIR.get().resolve(HISTORY_FILE);
    }
    
    /**
     * Initialise le gestionnaire d'aptitudes
     * Les données des joueurs sont maintenant gérées par RPDataManager
     */
    public static void initialize() {
        loadData();
        ChatLog.startup(LOGGER, "VentrysChat aptitudes : prêt");
    }
    
    /**
     * Arrête le gestionnaire d'aptitudes (sauvegarde l'historique)
     */
    public static void stopAutoSave() {
        // Sauvegarder l'historique avant l'arrêt
        saveHistory();
        ChatLog.detail(LOGGER,"Historique des aptitudes sauvegardé");
    }
}

