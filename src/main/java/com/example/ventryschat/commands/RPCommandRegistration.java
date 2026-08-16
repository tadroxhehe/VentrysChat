package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RPCommandRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RPCommandRegistration.class);

    private RPCommandRegistration() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /setname, /setsurname : desactives, migration vers le plugin (identite RP).
        // RPCommandHandlers/RPDataManager restent intacts pour l'acces Skript existant.

        dispatcher.register(Commands.literal("narration")
            .requires(source -> source.hasPermission(0))
            .then(Commands.argument("distance", IntegerArgumentType.integer(1, 5000))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> {
                        int distance = IntegerArgumentType.getInteger(context, "distance");
                        String message = StringArgumentType.getString(context, "message");
                        CommandSourceStack source = context.getSource();

                        if (source.getEntity() instanceof ServerPlayer player) {
                            return RPCommandHandlers.executeNarration(player, distance, message);
                        } else {
                            source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                            return 0;
                        }
                    })))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    String message = StringArgumentType.getString(context, "message");
                    CommandSourceStack source = context.getSource();

                    if (source.getEntity() instanceof ServerPlayer player) {
                        return RPCommandHandlers.executeNarration(player, message);
                    } else {
                        source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                        return 0;
                    }
                }))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /narration [distance] <message>"));
                return 0;
            }));

        dispatcher.register(Commands.literal("nrp")
            .requires(source -> source.hasPermission(0))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> RPCommandSuggestions.suggestOnlinePlayers(context.getSource(), builder))
                .then(Commands.argument("couleur", StringArgumentType.word())
                    .suggests((context, builder) -> RPCommandSuggestions.suggestNarrationColors(builder))
                    .then(Commands.argument("texte", StringArgumentType.greedyString())
                        .executes(context -> {
                            String targetPlayerName = StringArgumentType.getString(context, "joueur");
                            String color = StringArgumentType.getString(context, "couleur");
                            String text = StringArgumentType.getString(context, "texte");
                            CommandSourceStack source = context.getSource();

                            if (source.getEntity() instanceof ServerPlayer player) {
                                return RPCommandHandlers.executeNRP(player, targetPlayerName, text, color);
                            } else {
                                source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                                return 0;
                            }
                        })))
                .executes(context -> {
                    context.getSource().sendFailure(new TextComponent("Usage: /nrp <joueur> <couleur> <texte>"));
                    context.getSource().sendFailure(new TextComponent("Couleurs disponibles: white, yellow, green, blue, purple, red, orange, gray"));
                    return 0;
                }))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /nrp <joueur> <couleur> <texte>"));
                context.getSource().sendFailure(new TextComponent("Couleurs disponibles: white, yellow, green, blue, purple, red, orange, gray"));
                return 0;
            }));

        // /rpstatus, /setnameother, /setsurnameother, /setbirthdate, /lorejob, /giveprestige,
        // /rpprofile, /resetrpdata : desactives, migration vers le plugin (identite RP).
        // RPCommandHandlers/RPDataManager restent intacts pour l'acces Skript existant.

        dispatcher.register(Commands.literal("chathelp")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                ServerPlayer player = null;
                if (source.getEntity() instanceof ServerPlayer) {
                    player = (ServerPlayer) source.getEntity();
                }
                return RPCommandHandlers.executeHelp(player, source, VentrysPermsBridge.staff(source, "ventryspermissions.meta.manage"));
            }));

        LOGGER.debug("Commandes RP enregistrées");
    }
}
