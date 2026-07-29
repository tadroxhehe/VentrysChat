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
        dispatcher.register(Commands.literal("setname")
            .requires(source -> source.hasPermission(0))
            .then(Commands.argument("prénom", StringArgumentType.word())
                .executes(context -> {
                    String firstName = StringArgumentType.getString(context, "prénom");
                    CommandSourceStack source = context.getSource();

                    if (source.getEntity() instanceof ServerPlayer player) {
                        return RPCommandHandlers.executeSetName(player, firstName);
                    } else {
                        source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                        return 0;
                    }
                }))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /setname <prénom>"));
                return 0;
            }));

        dispatcher.register(Commands.literal("setsurname")
            .requires(source -> source.hasPermission(0))
            .then(Commands.argument("nom", StringArgumentType.word())
                .executes(context -> {
                    String lastName = StringArgumentType.getString(context, "nom");
                    CommandSourceStack source = context.getSource();

                    if (source.getEntity() instanceof ServerPlayer player) {
                        return RPCommandHandlers.executeSetSurname(player, lastName);
                    } else {
                        source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                        return 0;
                    }
                }))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /setsurname <nom>"));
                return 0;
            }));

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

        dispatcher.register(Commands.literal("rpstatus")
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    return RPCommandHandlers.executeRPStatus(player);
                } else {
                    context.getSource().sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                    return 0;
                }
            }));

        dispatcher.register(Commands.literal("setnameother")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.rp.setname.other"))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> RPCommandSuggestions.suggestOnlinePlayers(context.getSource(), builder))
                .then(Commands.argument("prénom", StringArgumentType.word())
                    .executes(context -> {
                        String targetPlayer = StringArgumentType.getString(context, "joueur");
                        String firstName = StringArgumentType.getString(context, "prénom");
                        CommandSourceStack source = context.getSource();

                        if (source.getEntity() instanceof ServerPlayer player) {
                            return RPCommandHandlers.executeSetNameOther(player, targetPlayer, firstName);
                        } else {
                            source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                            return 0;
                        }
                    })))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /setnameother <joueur> <prénom>"));
                return 0;
            }));

        dispatcher.register(Commands.literal("setsurnameother")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.rp.setsurname.other"))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> RPCommandSuggestions.suggestOnlinePlayers(context.getSource(), builder))
                .then(Commands.argument("nom", StringArgumentType.word())
                    .executes(context -> {
                        String targetPlayer = StringArgumentType.getString(context, "joueur");
                        String surname = StringArgumentType.getString(context, "nom");
                        CommandSourceStack source = context.getSource();

                        if (source.getEntity() instanceof ServerPlayer player) {
                            return RPCommandHandlers.executeSetSurnameOther(player, targetPlayer, surname);
                        } else {
                            source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                            return 0;
                        }
                    })))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /setsurnameother <joueur> <nom>"));
                return 0;
            }));

        dispatcher.register(Commands.literal("setbirthdate")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.rp.setbirthdate"))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> RPCommandSuggestions.suggestOnlinePlayers(context.getSource(), builder))
                .then(Commands.argument("date", StringArgumentType.greedyString())
                    .executes(context -> {
                        String targetPlayer = StringArgumentType.getString(context, "joueur");
                        String birthDate = StringArgumentType.getString(context, "date");
                        CommandSourceStack source = context.getSource();

                        if (source.getEntity() instanceof ServerPlayer player) {
                            return RPCommandHandlers.executeSetBirthDateOther(player, targetPlayer, birthDate);
                        } else {
                            source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                            return 0;
                        }
                    })))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /setbirthdate <joueur> <date>"));
                return 0;
            }));

        dispatcher.register(Commands.literal("lorejob")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.rp.lorejob"))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> RPCommandSuggestions.suggestOnlinePlayers(context.getSource(), builder))
                .then(Commands.argument("métier", StringArgumentType.greedyString())
                    .executes(context -> {
                        String targetPlayer = StringArgumentType.getString(context, "joueur");
                        String job = StringArgumentType.getString(context, "métier");
                        CommandSourceStack source = context.getSource();

                        if (source.getEntity() instanceof ServerPlayer player) {
                            return RPCommandHandlers.executeSetJobOther(player, targetPlayer, job);
                        } else {
                            source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                            return 0;
                        }
                    })))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /lorejob <joueur> <métier>"));
                return 0;
            }));

        dispatcher.register(Commands.literal("giveprestige")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.rp.prestige"))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> RPCommandSuggestions.suggestOnlinePlayers(context.getSource(), builder))
                .then(Commands.argument("titre_et_texte", StringArgumentType.greedyString())
                    .executes(context -> {
                        String targetPlayer = StringArgumentType.getString(context, "joueur");
                        String titreEtTexte = StringArgumentType.getString(context, "titre_et_texte");
                        CommandSourceStack source = context.getSource();

                        if (source.getEntity() instanceof ServerPlayer player) {
                            String title = "";
                            String description = "";

                            if (titreEtTexte.startsWith("\"") && titreEtTexte.contains("\"")) {
                                int endQuote = titreEtTexte.indexOf("\"", 1);
                                if (endQuote > 0) {
                                    title = titreEtTexte.substring(1, endQuote);
                                    if (endQuote + 1 < titreEtTexte.length()) {
                                        description = titreEtTexte.substring(endQuote + 1).trim();
                                    }
                                } else {
                                    title = titreEtTexte.substring(1);
                                }
                            } else {
                                int firstSpace = titreEtTexte.indexOf(' ');
                                if (firstSpace > 0) {
                                    title = titreEtTexte.substring(0, firstSpace);
                                    description = titreEtTexte.substring(firstSpace + 1).trim();
                                } else {
                                    title = titreEtTexte;
                                }
                            }

                            return RPCommandHandlers.executeGivePrestige(player, targetPlayer, title, description);
                        } else {
                            source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                            return 0;
                        }
                    })))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /giveprestige <joueur> \"<titre>\" <texte>"));
                return 0;
            }));

        dispatcher.register(Commands.literal("rpprofile")
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    return RPCommandHandlers.executeOpenProfile(player);
                } else {
                    context.getSource().sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                    return 0;
                }
            }));

        dispatcher.register(Commands.literal("resetrpdata")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.rp.reset"))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> RPCommandSuggestions.suggestOnlinePlayers(context.getSource(), builder))
                .executes(context -> {
                    String targetPlayer = StringArgumentType.getString(context, "joueur");
                    CommandSourceStack source = context.getSource();

                    if (source.getEntity() instanceof ServerPlayer player) {
                        return RPCommandHandlers.executeResetRPData(player, targetPlayer);
                    } else {
                        source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                        return 0;
                    }
                }))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /resetrpdata <joueur>"));
                return 0;
            }));

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
