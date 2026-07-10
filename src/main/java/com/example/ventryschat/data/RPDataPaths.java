package com.example.ventryschat.data;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * Résolution des chemins de fichiers RP (aucune logique métier).
 */
public final class RPDataPaths {

    private static final String DATA_FILE = "ventryschat_rp_data.json";

    private RPDataPaths() {
    }

    public static Path resolveDataPath(Logger logger) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Path serverPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                Path dataPath = serverPath.resolve(DATA_FILE);
                logger.debug("Serveur détecté, chemin des données : {}", dataPath);
                return dataPath;
            }
        } catch (Exception e) {
            logger.debug("Pas de serveur détecté, utilisation du chemin client : {}", e.getMessage());
        }

        Path clientPath = FMLPaths.GAMEDIR.get().resolve(DATA_FILE);
        logger.debug("Client détecté, chemin des données : {}", clientPath);
        return clientPath;
    }
}
