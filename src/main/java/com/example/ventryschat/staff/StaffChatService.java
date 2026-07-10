package com.example.ventryschat.staff;

import com.example.ventryschat.compat.VentrysPermsBridge;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat staff interne et tickets d'assistance joueur → staff.
 */
public final class StaffChatService {

    public static final String PERM_STAFF_CHAT = "ventryspermissions.chat.staff";
    public static final String PERM_TICKET_SEND = "ventryspermissions.player.ticket";
    public static final String PERM_TICKET_RECEIVE = "ventryspermissions.chat.ticket.receive";

    public static final String STAFF_PREFIX = "@";

    private static final long TICKET_COOLDOWN_MS = 60_000L;
    private static final Map<UUID, Long> LAST_TICKET_AT = new ConcurrentHashMap<>();

    private StaffChatService() {
    }

    public static boolean canUseStaffChat(ServerPlayer player) {
        return VentrysPermsBridge.player(player, PERM_STAFF_CHAT);
    }

    public static boolean canReceiveTickets(ServerPlayer player) {
        return VentrysPermsBridge.player(player, PERM_TICKET_RECEIVE);
    }

    public static int sendStaffMessage(ServerPlayer sender, String message) {
        if (!canUseStaffChat(sender)) {
            sender.sendMessage(new TextComponent("§cTu n'as pas accès au chat staff."), sender.getUUID());
            return 0;
        }
        String content = message == null ? "" : message.trim();
        if (content.isEmpty()) {
            sender.sendMessage(new TextComponent("§cMessage vide."), sender.getUUID());
            return 0;
        }

        TextComponent formatted = formatStaffLine(sender, content);
        int recipients = broadcastToStaff(sender.getServer(), formatted, sender.getUUID());
        if (recipients == 0) {
            sender.sendMessage(new TextComponent("§7Aucun autre staff en ligne — message envoyé."), sender.getUUID());
        }
        return 1;
    }

    public static int sendTicket(ServerPlayer sender, String message) {
        if (!VentrysPermsBridge.player(sender, PERM_TICKET_SEND)) {
            sender.sendMessage(new TextComponent("§cTu ne peux pas envoyer de ticket."), sender.getUUID());
            return 0;
        }
        String content = message == null ? "" : message.trim();
        if (content.isEmpty()) {
            sender.sendMessage(new TextComponent("§cUsage : /ticket <message>"), sender.getUUID());
            return 0;
        }

        long now = System.currentTimeMillis();
        Long last = LAST_TICKET_AT.get(sender.getUUID());
        if (last != null && (now - last) < TICKET_COOLDOWN_MS) {
            long waitSec = (TICKET_COOLDOWN_MS - (now - last) + 999) / 1000;
            sender.sendMessage(new TextComponent("§cPatiente encore §e" + waitSec + "s §cavant un nouveau ticket."), sender.getUUID());
            return 0;
        }
        LAST_TICKET_AT.put(sender.getUUID(), now);

        TextComponent staffLine = formatTicketLine(sender, content);
        int recipients = broadcastToTicketStaff(sender.getServer(), staffLine, sender.getUUID());
        if (recipients == 0) {
            sender.sendMessage(new TextComponent("§eTicket enregistré, mais aucun staff n'est en ligne pour le moment."), sender.getUUID());
        } else {
            sender.sendMessage(new TextComponent("§aTicket envoyé au staff (§e" + recipients + "§a en ligne)."), sender.getUUID());
        }
        return 1;
    }

    private static TextComponent formatStaffLine(ServerPlayer sender, String content) {
        String role = resolveRoleLabel(sender);
        String name = sender.getGameProfile().getName();
        return new TextComponent("§8[§cStaff§8] §7" + role + " §f" + name + "§8: §f" + content);
    }

    private static TextComponent formatTicketLine(ServerPlayer sender, String content) {
        String name = sender.getGameProfile().getName();
        return new TextComponent("§8[§eTicket§8] §7de §f" + name + "§8: §f" + content);
    }

    private static int broadcastToStaff(MinecraftServer server, TextComponent message, UUID senderUuid) {
        if (server == null) {
            return 0;
        }
        int count = 0;
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (canUseStaffChat(online)) {
                online.sendMessage(message, senderUuid);
                count++;
            }
        }
        return count;
    }

    private static int broadcastToTicketStaff(MinecraftServer server, TextComponent message, UUID senderUuid) {
        if (server == null) {
            return 0;
        }
        int count = 0;
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (canReceiveTickets(online)) {
                online.sendMessage(message, senderUuid);
                count++;
            }
        }
        return count;
    }

    private static String resolveRoleLabel(ServerPlayer player) {
        try {
            Class<?> resolver = Class.forName("com.ventrys.permissions.role.RoleResolver");
            Object role = resolver.getMethod("resolve", ServerPlayer.class).invoke(null, player);
            if (role != null) {
                Object display = role.getClass().getMethod("getDisplayName").invoke(role);
                if (display instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return "Staff";
    }
}
