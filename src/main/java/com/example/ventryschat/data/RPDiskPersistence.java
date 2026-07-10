package com.example.ventryschat.data;

import com.example.ventryschat.RPDataManager;
import com.example.ventryschat.RPMenuDisplay;
import com.example.ventryschat.util.ChatLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Lecture / écriture disque des données RP (aucune règle métier).
 */
public final class RPDiskPersistence {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson GSON_SAVE = new GsonBuilder().create();

    private RPDiskPersistence() {
    }

    public static void loadInto(Map<UUID, RPDataManager.PlayerRPData> playerData, Logger logger) {
        Path dataPath = RPDataPaths.resolveDataPath(logger);
        ChatLog.diagnose(logger, "Chargement données RP : {}", dataPath);

        if (Files.exists(dataPath)) {
            ChatLog.diagnose(logger, "Fichier RP trouvé, lecture…");

            try (Reader reader = Files.newBufferedReader(dataPath)) {
                TypeToken<Map<UUID, RPDataManager.PlayerRPData>> typeToken =
                    new TypeToken<Map<UUID, RPDataManager.PlayerRPData>>() {};
                Map<UUID, RPDataManager.PlayerRPData> loadedData = GSON.fromJson(reader, typeToken.getType());

                if (loadedData != null && !loadedData.isEmpty()) {
                    int validEntries = 0;
                    int invalidEntries = 0;

                    for (Map.Entry<UUID, RPDataManager.PlayerRPData> entry : loadedData.entrySet()) {
                        RPDataManager.PlayerRPData data = entry.getValue();
                        if (data != null && (data.firstName != null || data.lastName != null)) {
                            validEntries++;
                        } else {
                            invalidEntries++;
                            logger.warn("⚠️ Entrée invalide détectée pour le joueur : {}", entry.getKey());
                        }
                    }

                    if (invalidEntries > 0) {
                        logger.warn("⚠️ {} entrées invalides détectées sur {} total", invalidEntries, loadedData.size());
                    }

                    if (validEntries > 0) {
                        playerData.clear();
                        playerData.putAll(loadedData);

                        // Pré-remplir l'affichage client uniquement sur le client physique.
                        // RPMenuDisplay est @OnlyIn(CLIENT) : on isole l'accès dans un double-lambda
                        // pour que la classe ne soit jamais chargée sur serveur dédié (RuntimeDistCleaner).
                        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                            net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> {
                                for (Map.Entry<UUID, RPDataManager.PlayerRPData> e : playerData.entrySet()) {
                                    RPDataManager.PlayerRPData d = e.getValue();
                                    if (d != null) {
                                        RPMenuDisplay.setPlayerNames(e.getKey(), d.firstName, d.lastName);
                                    }
                                }
                            });

                        ChatLog.diagnose(logger, "Données RP chargées : {} joueur(s) valide(s)", validEntries);
                    } else {
                        logger.warn("⚠️ Aucune donnée valide trouvée dans le fichier");
                    }
                } else {
                    logger.warn("⚠️ Fichier de données RP vide ou corrompu");
                }
            } catch (Exception e) {
                logger.error("❌ Erreur lors du chargement des données RP : {}", e.getMessage());

                try {
                    if (!playerData.isEmpty()) {
                        ChatLog.diagnose(logger, "Sauvegarde de secours des données en mémoire…");
                        saveFrom(playerData, logger, 3);
                    }
                } catch (Exception backupError) {
                    logger.error("❌ Échec de la sauvegarde de secours : {}", backupError.getMessage());
                }
            }
        } else {
            ChatLog.diagnose(logger, "Aucun fichier RP — démarrage vide ({})", dataPath);

            try {
                Path parentDir = dataPath.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                    ChatLog.diagnose(logger, "Dossier créé : {}", parentDir);
                }
            } catch (Exception e) {
                logger.warn("⚠️ Impossible de créer le dossier parent : {}", e.getMessage());
            }
        }
    }

    public static boolean saveFrom(Map<UUID, RPDataManager.PlayerRPData> playerData, Logger logger, int maxRetries) {
        Path dataPath = RPDataPaths.resolveDataPath(logger);
        boolean gameThread = isPrimaryServerThread();
        int attempts = gameThread ? 1 : maxRetries;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                logger.debug("Tentative de sauvegarde {} sur {} dans : {}", attempt, attempts, dataPath);

                Path parentDir = dataPath.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                    logger.debug("Dossier créé : {}", parentDir);
                }

                Path tempFile = dataPath.resolveSibling(dataPath.getFileName() + ".tmp");
                try (Writer writer = Files.newBufferedWriter(tempFile)) {
                    GSON_SAVE.toJson(playerData, writer);
                }

                boolean tempOk = verifyTempFileLightweight(tempFile);
                if (!tempOk) {
                    logger.error("Fichier temporaire invalide, tentative {} échouée", attempt);
                    Files.deleteIfExists(tempFile);
                    continue;
                }

                Files.move(tempFile, dataPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                ChatLog.detail(logger, "Données RP sauvegardées : {} ({} joueur(s))", dataPath, playerData.size());
                return true;

            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde (tentative {}/{}): {}", attempt, attempts, e.getMessage());

                if (attempt == attempts) {
                    logger.error("❌ Toutes les tentatives de sauvegarde ont échoué");
                    return false;
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    public static boolean verifyIntegrity(Logger logger) {
        try {
            Path dataPath = RPDataPaths.resolveDataPath(logger);
            if (!Files.exists(dataPath)) {
                logger.warn("Fichier de données RP introuvable : {}", dataPath);
                return false;
            }

            try (Reader reader = Files.newBufferedReader(dataPath)) {
                TypeToken<Map<UUID, RPDataManager.PlayerRPData>> typeToken =
                    new TypeToken<Map<UUID, RPDataManager.PlayerRPData>>() {};
                Map<UUID, RPDataManager.PlayerRPData> testData = GSON.fromJson(reader, typeToken.getType());

                if (testData == null) {
                    logger.warn("Fichier de données RP corrompu : données null");
                    return false;
                }

                ChatLog.diagnose(logger, "Intégrité fichier RP : {} joueur(s)", testData.size());
                return true;
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification d'intégrité : {}", e.getMessage());
            return false;
        }
    }

    private static boolean isPrimaryServerThread() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }

    private static boolean verifyTempFileLightweight(Path tempFile) {
        try {
            return Files.exists(tempFile) && Files.size(tempFile) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
