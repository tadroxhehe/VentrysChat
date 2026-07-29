package com.example.ventryschat;

import com.example.ventryschat.data.RpSaveHooks;
import com.example.ventryschat.data.RPAutoSaveScheduler;
import com.example.ventryschat.data.RPDiskPersistence;
import com.example.ventryschat.data.RPDataPaths;
import com.example.ventryschat.data.RPNetworkSync;
import com.example.ventryschat.util.ChatLog;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des données RP des joueurs
 */
public class RPDataManager {
    
    // Utiliser ConcurrentHashMap pour la thread-safety en multijoueur
    private static final Map<UUID, PlayerRPData> playerData = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();

    private static volatile boolean hasUnsavedChanges = false;
    private static volatile long lastSuccessfulSaveMs = 0L;

    public static boolean hasUnsavedChangesFlag() {
        return hasUnsavedChanges;
    }

    public static void clearUnsavedChangesFlag() {
        hasUnsavedChanges = false;
    }

    public static long getLastSuccessfulSaveMs() {
        return lastSuccessfulSaveMs;
    }

    public static boolean hasAnyPlayerData() {
        return !playerData.isEmpty();
    }

    private static final RPAutoSaveScheduler.SaveHooks SAVE_HOOKS = RpSaveHooks.INSTANCE;

    /**
     * Programme au plus une écriture disque (RP + historique aptitudes) sur le thread serveur.
     * Les données sont déjà en mémoire : une seule passe suffit même si plusieurs joueurs quittent.
     */
    public static void scheduleCoalescedDiskFlush(MinecraftServer server) {
        RPAutoSaveScheduler.scheduleCoalescedDiskFlush(server, SAVE_HOOKS, LOGGER);
    }

    private static void scheduleThrottledDiskSaveIfNeeded(MinecraftServer server) {
        RPAutoSaveScheduler.scheduleThrottledDiskSaveIfNeeded(server, SAVE_HOOKS, LOGGER);
    }

    /**
     * Marque qu'il y a des changements non sauvegardés (pour les aptitudes)
     */
    public static void markUnsavedChanges() {
        hasUnsavedChanges = true;
    }
    
    /**
     * Classe représentant un haut-fait/prestige
     */
    public static class Prestige {
        public String title;
        public String description;
        
        public Prestige() {
            this.title = "";
            this.description = "";
        }
        
        public Prestige(String title, String description) {
            this.title = title != null ? title : "";
            this.description = description != null ? description : "";
        }
    }
    
    /**
     * Données RP d'un joueur (incluant les aptitudes)
     */
    public static class PlayerRPData {
        // Données RP de base
        public String firstName;
        public String lastName;
        public String birthDate;
        public String lorejob;
        public List<Prestige> prestiges;
        
        // Données d'aptitudes
        public int martialite = 0;        // 0-10
        public int artisanat = 0;         // 0-10
        public int savoir = 0;            // 0-10
        public int pointsARepartir = 0;   // Points disponibles à répartir (pas de limite)
        public String focus = null;        // "martialité", "artisanat", "savoir", ou null
        public boolean pointsInitiauxDonnes = false;  // true si les 5 points initiaux ont été donnés
        public long dateChangementFocus = 0;          // Timestamp du dernier changement de focus
        public long derniereConnexionEligible = 0;    // Timestamp de la dernière connexion comptabilisée
        
        public PlayerRPData(String firstName, String lastName) {
            this.firstName = firstName != null ? firstName : "";
            this.lastName = lastName != null ? lastName : "";
            this.birthDate = "";
            this.lorejob = "";
            this.prestiges = new ArrayList<>();
            // Les aptitudes sont initialisées par défaut (valeurs à 0)
        }
        
        public PlayerRPData() {
            this.firstName = "";
            this.lastName = "";
            this.birthDate = "";
            this.lorejob = "";
            this.prestiges = new ArrayList<>();
            // Les aptitudes sont initialisées par défaut (valeurs à 0)
        }
        
        /**
         * Obtient le total des points répartis
         */
        public int getTotalRepartis() {
            return martialite + artisanat + savoir;
        }
        
        /**
         * Vérifie si le joueur peut recevoir des points dans une aptitude spécifique
         */
        public boolean peutRecevoirPoints(int nombrePoints, String typeAptitude) {
            int valeurActuelle = getValeurAptitude(typeAptitude);
            if (valeurActuelle + nombrePoints > 10) {
                return false;
            }
            if (getTotalRepartis() + nombrePoints > 15) {
                return false;
            }
            return true;
        }
        
        /**
         * Obtient la valeur d'une aptitude par son nom
         */
        public int getValeurAptitude(String typeAptitude) {
            if (typeAptitude == null) return 0;
            return switch (typeAptitude.toLowerCase()) {
                case "martialité", "martialite" -> martialite;
                case "artisanat" -> artisanat;
                case "savoir" -> savoir;
                default -> 0;
            };
        }
        
        /**
         * Ajoute des points à une aptitude
         */
        public void ajouterPoints(String typeAptitude, int nombrePoints) {
            if (typeAptitude == null) return;
            switch (typeAptitude.toLowerCase()) {
                case "martialité", "martialite" -> martialite = Math.min(10, martialite + nombrePoints);
                case "artisanat" -> artisanat = Math.min(10, artisanat + nombrePoints);
                case "savoir" -> savoir = Math.min(10, savoir + nombrePoints);
            }
        }
        
        /**
         * Consomme des points à répartir
         */
        public void consommerPointsARepartir(int nombrePoints) {
            pointsARepartir = Math.max(0, pointsARepartir - nombrePoints);
        }
        
        /**
         * Vérifie si le joueur est en cooldown (14 jours après changement de focus)
         */
        public boolean estEnCooldown() {
            if (dateChangementFocus == 0) {
                return false;
            }
            long maintenant = System.currentTimeMillis();
            long dureeCooldown = 14L * 24L * 60L * 60L * 1000L;
            return (maintenant - dateChangementFocus) < dureeCooldown;
        }
        
        /**
         * Obtient le nombre de jours restants dans le cooldown
         */
        public long getJoursRestantsCooldown() {
            if (!estEnCooldown()) {
                return 0;
            }
            long maintenant = System.currentTimeMillis();
            long dureeCooldown = 14L * 24L * 60L * 60L * 1000L;
            long tempsEcoule = maintenant - dateChangementFocus;
            long tempsRestant = dureeCooldown - tempsEcoule;
            return (tempsRestant / (24L * 60L * 60L * 1000L)) + 1;
        }
    }
    
    /**
     * Définit les noms RP d'un joueur
     * Amélioré avec validation et gestion des erreurs
     */
    public static void setPlayerNames(UUID playerUUID, String firstName, String lastName) {
        // Validation des entrées
        if (playerUUID == null) {
            LOGGER.warn("Tentative de définition de noms RP avec UUID null");
            return;
        }
        
        try {
            PlayerRPData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerRPData());
            
            boolean hasChanged = false;
            
            // Normaliser les chaînes (trim et validation)
            String normalizedFirstName = (firstName != null) ? firstName.trim() : "";
            String normalizedLastName = (lastName != null) ? lastName.trim() : "";
            
            // Limiter la longueur des noms pour éviter les problèmes
            if (normalizedFirstName.length() > 64) {
                normalizedFirstName = normalizedFirstName.substring(0, 64);
                LOGGER.warn("Prénom RP tronqué à 64 caractères pour {}", playerUUID);
            }
            if (normalizedLastName.length() > 64) {
                normalizedLastName = normalizedLastName.substring(0, 64);
                LOGGER.warn("Nom RP tronqué à 64 caractères pour {}", playerUUID);
            }
            
            if (!normalizedFirstName.isEmpty() && !normalizedFirstName.equals(data.firstName)) {
                data.firstName = normalizedFirstName;
                hasChanged = true;
                LOGGER.debug("Prénom RP défini pour {} : {}", playerUUID, normalizedFirstName);
            }
            if (!normalizedLastName.isEmpty() && !normalizedLastName.equals(data.lastName)) {
                data.lastName = normalizedLastName;
                hasChanged = true;
                LOGGER.debug("Nom RP défini pour {} : {}", playerUUID, normalizedLastName);
            }
            
            // Marquer qu'il y a des changements non sauvegardés
            if (hasChanged) {
                hasUnsavedChanges = true;
            }
            
            // Synchroniser l'affichage client (RPMenuDisplay est @OnlyIn(CLIENT) :
            // l'accès est isolé pour ne jamais charger la classe sur serveur dédié).
            syncClientDisplay(playerUUID, data.firstName, data.lastName);
            
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la définition des noms RP pour {} : {}", playerUUID, e.getMessage(), e);
        }
    }
    
    /**
     * Met à jour le cache d'affichage client ({@link RPMenuDisplay}) uniquement sur le client physique.
     *
     * <p>{@code RPMenuDisplay} est annotée {@code @OnlyIn(Dist.CLIENT)}. Toute référence directe
     * provoque le chargement de la classe par la JVM, ce que le {@code RuntimeDistCleaner} de Forge
     * refuse sur un serveur dédié (crash « invalid dist DEDICATED_SERVER »). On encapsule donc l'appel
     * dans un double-lambda passé à {@link net.minecraftforge.fml.DistExecutor} : le {@link Runnable}
     * qui référence la classe n'est jamais instancié côté serveur.</p>
     */
    private static void syncClientDisplay(UUID playerUUID, String firstName, String lastName) {
        try {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> RPMenuDisplay.setPlayerNames(playerUUID, firstName, lastName));
        } catch (Exception e) {
            LOGGER.warn("Erreur lors de la synchronisation avec RPMenuDisplay : {}", e.getMessage());
        }
    }

    /**
     * Obtient le prénom RP d'un joueur
     * Amélioré avec validation null-safe
     */
    public static String getFirstName(UUID playerUUID) {
        if (playerUUID == null) {
            return "";
        }
        PlayerRPData data = playerData.get(playerUUID);
        return (data != null && data.firstName != null) ? data.firstName : "";
    }
    
    /**
     * Obtient le nom de famille RP d'un joueur
     * Amélioré avec validation null-safe
     */
    public static String getLastName(UUID playerUUID) {
        if (playerUUID == null) {
            return "";
        }
        PlayerRPData data = playerData.get(playerUUID);
        return (data != null && data.lastName != null) ? data.lastName : "";
    }
    
    /**
     * Recherche un joueur par nom RP complet (prénom + nom), insensible à la casse / espaces.
     * Exemple : {@code "Aegon Blackfyre"}.
     */
    public static java.util.Optional<UUID> findUuidByFullName(String query) {
        if (query == null || query.isBlank()) {
            return java.util.Optional.empty();
        }
        String normalized = query.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
        for (java.util.Map.Entry<UUID, PlayerRPData> entry : playerData.entrySet()) {
            String full = getFullName(entry.getKey());
            if (full.isEmpty()) {
                continue;
            }
            if (full.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT).equals(normalized)) {
                return java.util.Optional.of(entry.getKey());
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * Obtient le nom RP complet d'un joueur
     * Amélioré avec validation null-safe
     */
    public static String getFullName(UUID playerUUID) {
        if (playerUUID == null) {
            return "";
        }
        PlayerRPData data = playerData.get(playerUUID);
        if (data == null) {
            return "";
        }
        
        String firstName = (data.firstName != null) ? data.firstName : "";
        String lastName = (data.lastName != null) ? data.lastName : "";
        
        if (!firstName.isEmpty() && !lastName.isEmpty()) {
            return firstName + " " + lastName;
        } else if (!firstName.isEmpty()) {
            return firstName;
        } else if (!lastName.isEmpty()) {
            return lastName;
        }
        return "";
    }
    
    /**
     * Définit la date de naissance RP d'un joueur
     */
    public static void setBirthDate(UUID playerUUID, String birthDate) {
        if (playerUUID == null) {
            LOGGER.warn("Tentative de définition de date de naissance avec UUID null");
            return;
        }
        
        try {
            PlayerRPData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerRPData());
            String normalizedBirthDate = (birthDate != null) ? birthDate.trim() : "";
            
            if (normalizedBirthDate.length() > 128) {
                normalizedBirthDate = normalizedBirthDate.substring(0, 128);
                LOGGER.warn("Date de naissance RP tronquée à 128 caractères pour {}", playerUUID);
            }
            
            if (!normalizedBirthDate.equals(data.birthDate)) {
                data.birthDate = normalizedBirthDate;
                hasUnsavedChanges = true;
                LOGGER.debug("Date de naissance RP définie pour {} : {}", playerUUID, normalizedBirthDate);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la définition de la date de naissance RP pour {} : {}", playerUUID, e.getMessage(), e);
        }
    }
    
    /**
     * Obtient la date de naissance RP d'un joueur
     */
    public static String getBirthDate(UUID playerUUID) {
        if (playerUUID == null) {
            return "";
        }
        PlayerRPData data = playerData.get(playerUUID);
        return (data != null && data.birthDate != null) ? data.birthDate : "";
    }
    
    /**
     * Définit le lorejob (métier RP libre) d'un joueur
     */
    public static void setLorejob(UUID playerUUID, String lorejob) {
        if (playerUUID == null) {
            LOGGER.warn("Tentative de définition de lorejob avec UUID null");
            return;
        }
        
        try {
            PlayerRPData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerRPData());
            String normalizedLorejob = (lorejob != null) ? lorejob.trim() : "";
            
            if (normalizedLorejob.length() > 256) {
                normalizedLorejob = normalizedLorejob.substring(0, 256);
                LOGGER.warn("Lorejob RP tronqué à 256 caractères pour {}", playerUUID);
            }
            
            if (!normalizedLorejob.equals(data.lorejob)) {
                data.lorejob = normalizedLorejob;
                hasUnsavedChanges = true;
                LOGGER.debug("Lorejob RP défini pour {} : {}", playerUUID, normalizedLorejob);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la définition du lorejob RP pour {} : {}", playerUUID, e.getMessage(), e);
        }
    }
    
    /**
     * Obtient le lorejob (métier RP libre) d'un joueur
     */
    public static String getLorejob(UUID playerUUID) {
        if (playerUUID == null) {
            return "";
        }
        PlayerRPData data = playerData.get(playerUUID);
        return (data != null && data.lorejob != null) ? data.lorejob : "";
    }
    
    /**
     * Ajoute un prestige/haut-fait à un joueur
     */
    public static void addPrestige(UUID playerUUID, String title, String description) {
        if (playerUUID == null) {
            LOGGER.warn("Tentative d'ajout de prestige avec UUID null");
            return;
        }
        
        try {
            PlayerRPData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerRPData());
            if (data.prestiges == null) {
                data.prestiges = new ArrayList<>();
            }
            
            String normalizedTitle = (title != null) ? title.trim() : "";
            String normalizedDescription = (description != null) ? description.trim() : "";
            
            if (normalizedTitle.length() > 128) {
                normalizedTitle = normalizedTitle.substring(0, 128);
                LOGGER.warn("Titre de prestige tronqué à 128 caractères pour {}", playerUUID);
            }
            if (normalizedDescription.length() > 512) {
                normalizedDescription = normalizedDescription.substring(0, 512);
                LOGGER.warn("Description de prestige tronquée à 512 caractères pour {}", playerUUID);
            }
            
            Prestige prestige = new Prestige(normalizedTitle, normalizedDescription);
            data.prestiges.add(prestige);
            hasUnsavedChanges = true;
            LOGGER.debug("Prestige ajouté pour {} : {}", playerUUID, normalizedTitle);
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'ajout du prestige pour {} : {}", playerUUID, e.getMessage(), e);
        }
    }
    
    /**
     * Obtient la liste des prestiges d'un joueur
     */
    public static List<Prestige> getPrestiges(UUID playerUUID) {
        if (playerUUID == null) {
            return new ArrayList<>();
        }
        PlayerRPData data = playerData.get(playerUUID);
        if (data == null || data.prestiges == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(data.prestiges);
    }
    
    /**
     * Obtient toutes les données RP d'un joueur
     */
    public static PlayerRPData getPlayerData(UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        return playerData.get(playerUUID);
    }
    
    /**
     * Obtient ou crée les données RP d'un joueur (incluant les aptitudes)
     */
    public static PlayerRPData getOrCreatePlayerData(UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        return playerData.computeIfAbsent(playerUUID, k -> new PlayerRPData());
    }
    
    /**
     * Obtient tous les UUIDs des joueurs ayant des données
     */
    public static java.util.Set<UUID> getAllPlayerUUIDs() {
        return new java.util.HashSet<>(playerData.keySet());
    }
    
    /**
     * Réinitialise toutes les données RP d'un joueur (nom, date, métier, prestiges)
     */
    public static void resetPlayerData(UUID playerUUID) {
        if (playerUUID == null) {
            LOGGER.warn("Tentative de réinitialisation avec UUID null");
            return;
        }
        
        try {
            // Supprimer toutes les données du joueur
            PlayerRPData removed = playerData.remove(playerUUID);
            
            if (removed != null) {
                hasUnsavedChanges = true;
                ChatLog.detail(LOGGER, "Données RP réinitialisées pour le joueur {}", playerUUID);
                
                // Vider l'affichage client (uniquement côté client physique).
                syncClientDisplay(playerUUID, null, null);
            } else {
                LOGGER.debug("Aucune donnée RP trouvée pour le joueur {} (déjà vide)", playerUUID);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la réinitialisation des données RP pour {} : {}", playerUUID, e.getMessage(), e);
        }
    }
    
    /**
     * Charge les données depuis le fichier avec vérification d'intégrité
     */
    public static void loadData() {
        RPDiskPersistence.loadInto(playerData, LOGGER);
    }
    
    /**
     * Sauvegarde les données dans le fichier avec retry et vérification
     * Thread-safe pour la compatibilité multijoueur
     */
    public static synchronized boolean saveData() {
        boolean saved = RPDiskPersistence.saveFrom(playerData, LOGGER, 3);
        if (saved) {
            lastSuccessfulSaveMs = System.currentTimeMillis();
        }
        return saved;
    }

    /**
     * Obtient le chemin approprié pour le fichier de données
     * Gère les cas client et serveur
     */
    private static Path getDataPath() {
        return RPDataPaths.resolveDataPath(LOGGER);
    }
    
    /**
     * Obtient le chemin du fichier de données pour le diagnostic
     * Version publique de getDataPath()
     */
    public static Path getDataPathForDiagnostic() {
        return getDataPath();
    }
    
    /**
     * Initialise le gestionnaire de données RP
     * Doit être appelé au démarrage du serveur
     */
    public static void initialize() {
        if (RPAutoSaveScheduler.isAutoSaveEnabled()) {
            ChatLog.diagnose(LOGGER, "RPDataManager déjà initialisé — arrêt de l'auto-save existante");
            stopAutoSave();
        }

        loadData();
        ChatLog.startup(LOGGER, "VentrysChat RP : {} joueur(s) chargé(s)", playerData.size());

        if (ChatLog.dumpPlayersOnStartup() && !playerData.isEmpty()) {
            ChatLog.diagnose(LOGGER, "Dump joueurs RP (dump_players_on_startup) :");
            dumpPlayerDataToLog();
        }

        RPAutoSaveScheduler.start(SAVE_HOOKS, LOGGER);
    }

    private static void dumpPlayerDataToLog() {
        for (Map.Entry<UUID, PlayerRPData> entry : playerData.entrySet()) {
            PlayerRPData data = entry.getValue();
            ChatLog.diagnose(LOGGER, "  - UUID: {} → Prénom: '{}', Nom: '{}'",
                    entry.getKey(),
                    data.firstName != null ? data.firstName : "",
                    data.lastName != null ? data.lastName : "");
        }
    }
    
    /**
     * Arrête la sauvegarde automatique
     */
    public static void stopAutoSave() {
        RPAutoSaveScheduler.stop(LOGGER);
    }
    
    /**
     * Méthode de diagnostic pour vérifier l'état des données
     */
    public static void diagnoseDataState() {
        ChatLog.diagnose(LOGGER, "=== Diagnostic RPDataManager ===");
        ChatLog.diagnose(LOGGER, "Joueurs en mémoire : {}", playerData.size());
        ChatLog.diagnose(LOGGER, "Auto-save active : {}", RPAutoSaveScheduler.isAutoSaveEnabled());
        ChatLog.diagnose(LOGGER, "Fichier données : {}", getDataPath());

        if (ChatLog.dumpPlayersOnStartup() && !playerData.isEmpty()) {
            ChatLog.diagnose(LOGGER, "Contenu mémoire :");
            dumpPlayerDataToLog();
        } else if (playerData.isEmpty()) {
            ChatLog.diagnose(LOGGER, "Aucune donnée en mémoire");
        }

        boolean integrityOK = verifyDataIntegrity();
        if (integrityOK) {
            ChatLog.diagnose(LOGGER, "Intégrité fichier : OK");
        } else {
            LOGGER.warn("Intégrité fichier RP : PROBLÈME détecté");
        }
        ChatLog.diagnose(LOGGER, "=== Fin diagnostic RPDataManager ===");
    }
    
    /**
     * Vérifie l'intégrité des données sauvegardées
     */
    public static boolean verifyDataIntegrity() {
        return RPDiskPersistence.verifyIntegrity(LOGGER);
    }
    
    /**
     * Sauvegarde les données et synchronise un joueur spécifique
     * Optimisé pour éviter les threads inutiles et les freezes
     */
    public static void saveAndSyncPlayer(UUID playerUUID) {
        LOGGER.debug("saveAndSyncPlayer appelé pour le joueur {}", playerUUID);
        
        // Synchroniser rapidement puis sauvegarder avec throttling pour limiter les spikes I/O
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Utiliser le thread du serveur pour rester dans le modèle de thread Minecraft
            server.execute(() -> {
                PlayerRPData data = playerData.get(playerUUID);
                if (data != null) {
                    LOGGER.debug("Synchronisation réseau pour le joueur {} : {} {}", playerUUID, data.firstName, data.lastName);
                    RPNetworkSync.syncPlayerToAllClients(playerUUID, data.firstName, data.lastName, LOGGER);
                } else {
                    LOGGER.warn("Aucune donnée trouvée pour le joueur {}", playerUUID);
                }
                scheduleThrottledDiskSaveIfNeeded(server);
            });
        } else {
            // Si pas de serveur, sauvegarder directement
            if (saveData()) {
                hasUnsavedChanges = false;
            }
        }
    }
    
    /**
     * Sauvegarde les données et les synchronise avec tous les clients
     * Optimisé pour éviter les blocages avec beaucoup de joueurs
     */
    public static void saveAndSync() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Utiliser le thread du serveur pour éviter les problèmes de concurrence
            server.execute(() -> {
                RPNetworkSync.syncAllPlayers(playerData, LOGGER);
                scheduleThrottledDiskSaveIfNeeded(server);
            });
        } else {
            // Si pas de serveur, sauvegarder directement
            if (saveData()) {
                hasUnsavedChanges = false;
            }
        }
    }
}
