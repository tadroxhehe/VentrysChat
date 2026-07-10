package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.example.ventryschat.staff.StaffChatService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StaffChatCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaffChatCommands.class);

    private StaffChatCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerStaffChat(dispatcher, "sc");
        registerStaffChat(dispatcher, "staffchat");

        dispatcher.register(Commands.literal("ticket")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String message = StringArgumentType.getString(ctx, "message");
                            return StaffChatService.sendTicket(player, message);
                        })));

        LOGGER.debug("Commandes staff chat et ticket enregistrées");
    }

    private static void registerStaffChat(CommandDispatcher<CommandSourceStack> dispatcher, String literal) {
        dispatcher.register(Commands.literal(literal)
                .requires(source -> VentrysPermsBridge.staff(source, StaffChatService.PERM_STAFF_CHAT))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String message = StringArgumentType.getString(ctx, "message");
                            return StaffChatService.sendStaffMessage(player, message);
                        }))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(new TextComponent("§cUsage : /" + literal + " <message>"));
                    return 0;
                }));
    }
}
