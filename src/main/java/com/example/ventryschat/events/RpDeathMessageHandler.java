package com.example.ventryschat.events;

import com.example.ventryschat.config.VentrysChatConfig;
import com.example.ventryschat.util.ChatLog;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Désactive les annonces de mort vanilla pour le serveur RP
 * ({@code showDeathMessages} = false).
 */
@Mod.EventBusSubscriber(modid = "ventryschat", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RpDeathMessageHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private RpDeathMessageHandler() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        apply(event.getServer());
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld().isClientSide() || !(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        apply(level);
    }

    private static void apply(MinecraftServer server) {
        if (!VentrysChatConfig.hideDeathMessages()) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            apply(level);
        }
        ChatLog.startup(LOGGER, "Messages de mort vanilla désactivés (showDeathMessages=false)");
    }

    private static void apply(ServerLevel level) {
        if (!VentrysChatConfig.hideDeathMessages()) {
            return;
        }
        level.getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, level.getServer());
    }
}
