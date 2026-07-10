package com.example.ventryschat.staff;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrivateMessageHandler {
    private static final Map<UUID, UUID> LAST_MSG_FROM = new ConcurrentHashMap<>();

    private PrivateMessageHandler() {
    }

    public static void recordConversation(ServerPlayer a, ServerPlayer b) {
        LAST_MSG_FROM.put(a.getUUID(), b.getUUID());
        LAST_MSG_FROM.put(b.getUUID(), a.getUUID());
    }

    public static void sendMsg(ServerPlayer from, ServerPlayer to, String message) {
        recordConversation(from, to);
        String fromName = RPNameHelper.displayName(from);
        String toName = RPNameHelper.displayName(to);
        to.sendMessage(new TextComponent("§8[§dMP§8] §f" + fromName + " §8» §dtoi§8: §f" + message), to.getUUID());
        from.sendMessage(new TextComponent("§8[§dMP§8] §7à §f" + toName + "§8: §f" + message), from.getUUID());
    }

    public static ServerPlayer getLastPartner(ServerPlayer viewer, MinecraftServer server) {
        UUID other = LAST_MSG_FROM.get(viewer.getUUID());
        if (other == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(other);
    }
}
