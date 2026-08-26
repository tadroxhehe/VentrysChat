package com.example.ventryschat.music;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

// Musique dynamique désactivée (systeme juge non fiable) : annotation retiree, plus aucun
// listener de cette classe (ni de la classe Client imbriquee) n'est enregistre par Forge.
public final class MusicEvents {
    private MusicEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MusicCatalog.reload();
        MusicServerManager.clear();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MusicServerManager.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            MusicServerManager.onServerTick(server);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp) {
            MusicServerManager.onPlayerJoin(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp) {
            MusicServerManager.onPlayerJoin(sp);
        }
    }

    public static final class Client {
        private Client() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (Minecraft.getInstance().player == null) {
                return;
            }
            MusicClientManager.clientTick();
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
            MusicClientManager.clear();
        }
    }
}
