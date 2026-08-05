package com.example.ventryschat;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.tree.CommandNode;
import com.example.ventryschat.config.VentrysChatConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.example.ventryschat.network.RPNetworkHandler;
import com.example.ventryschat.commands.RPCommands;
import com.example.ventryschat.commands.AptitudesCommands;
import com.example.ventryschat.registry.ModBlockEntities;
import com.example.ventryschat.registry.ModBlocks;
import com.example.ventryschat.registry.ModItems;
import com.example.ventryschat.registry.ModMenuTypes;
import com.example.ventryschat.commands.BlockWarpCommands;
import com.example.ventryschat.commands.NarrationBlockCommands;
import com.example.ventryschat.commands.PlayerDataResetCommands;
import com.example.ventryschat.commands.StaffChatCommands;
import com.example.ventryschat.commands.EcCommands;
import com.example.ventryschat.commands.StaffUtilityCommands;
import com.example.ventryschat.util.ChatLog;

@Mod("ventryschat")
public class VentrysChatMod {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public VentrysChatMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, VentrysChatConfig.SPEC, "ventryschat-server.toml");

        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Enregistrer les événements du mod
        modEventBus.addListener(this::setup);

        // Registres Forge (bloc narratif, item, block entity)
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        
        // Enregistrer pour les événements du serveur
        MinecraftForge.EVENT_BUS.register(this);
        
        // Enregistrer les commandes RP
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        
        // Enregistrer l'événement d'arrêt du serveur
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        
        // Enregistrer l'événement de démarrage du serveur
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        
        LOGGER.debug("VentrysChat mod chargé");
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        // Initialiser le système de réseau
        event.enqueueWork(() -> {
            RPNetworkHandler.init();
        });
        
        LOGGER.debug("VentrysChat setup réseau OK");
        LOGGER.debug("Préfixes supportés : (aucun) [RP] (15 blocs), * (15 blocs), [ (15 blocs), - (4 blocs), + (30 blocs), ! (60 blocs), -- (2 blocs)");
        LOGGER.debug("Commandes : /setname <prénom>, /setsurname <nom>, /narration <message>, /rpstatus");
        LOGGER.debug("Commandes OP : /setnameother <joueur> <prénom>, /setsurnameother <joueur> <nom>");
    }
    
    private void registerCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        // Retire les alias MP vanilla pour laisser les commandes custom prendre la main.
        disableVanillaPrivateMessageAliases(event.getDispatcher().getRoot());
        RPCommands.register(event.getDispatcher());
        AptitudesCommands.register(event.getDispatcher());
        StaffUtilityCommands.register(event.getDispatcher());
        EcCommands.register(event.getDispatcher());
        StaffChatCommands.register(event.getDispatcher());
        BlockWarpCommands.register(event.getDispatcher());
        NarrationBlockCommands.register(event.getDispatcher());
        PlayerDataResetCommands.register(event.getDispatcher());
        LOGGER.debug("Commandes VentrysChat enregistrées");
    }

    private static void disableVanillaPrivateMessageAliases(CommandNode<?> root) {
        root.getChildren().removeIf(node -> {
            String name = node.getName();
            return "msg".equals(name) || "tell".equals(name) || "w".equals(name);
        });
    }
    
    /**
     * Gère le démarrage du serveur
     */
    private void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        try {
            com.example.ventryschat.RPDataManager.initialize();
            com.example.ventryschat.AptitudesManager.initialize();

            boolean integrityOK = com.example.ventryschat.RPDataManager.verifyDataIntegrity();
            if (!integrityOK) {
                LOGGER.warn("VentrysChat : intégrité des données RP — problème détecté au démarrage");
            }

            com.example.ventryschat.RPDataManager.diagnoseDataState();
        } catch (Exception e) {
            LOGGER.error("Erreur initialisation VentrysChat au démarrage : {}", e.getMessage());
        }
    }
    
    /**
     * Gère l'arrêt du serveur
     */
    private void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        try {
            com.example.ventryschat.RPDataManager.stopAutoSave();
            com.example.ventryschat.AptitudesManager.stopAutoSave();

            boolean saveSuccessRP = com.example.ventryschat.RPDataManager.saveData();
            boolean saveSuccessHist = com.example.ventryschat.AptitudesManager.saveData();

            if (saveSuccessRP && saveSuccessHist) {
                ChatLog.startup(LOGGER, "VentrysChat : sauvegarde finale RP + aptitudes OK");
            } else {
                LOGGER.error("VentrysChat : échec sauvegarde finale (RP: {}, aptitudes: {})", saveSuccessRP, saveSuccessHist);
                if (!saveSuccessRP) {
                    boolean integrityOK = com.example.ventryschat.RPDataManager.verifyDataIntegrity();
                    LOGGER.warn("Intégrité fichier RP après échec : {}", integrityOK ? "OK" : "problème");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur sauvegarde finale VentrysChat : {}", e.getMessage());
        }
    }
}
