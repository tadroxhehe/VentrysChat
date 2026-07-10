package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import com.example.ventryschat.AptitudesManager;
import com.example.ventryschat.RPDataManager;
import com.example.ventryschat.util.ChatLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AptitudesCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(AptitudesCommands.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Commande /setfocus pour définir le focus
        dispatcher.register(Commands.literal("setfocus")
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((context, builder) -> suggestFocusTypes(builder))
                .executes(context -> {
                    String focusType = StringArgumentType.getString(context, "type");
                    CommandSourceStack source = context.getSource();
                    
                    if (source.getEntity() instanceof ServerPlayer player) {
                        return executeSetFocus(player, focusType);
                    } else {
                        source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                        return 0;
                    }
                }))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /setfocus <martialité|artisanat|savoir>"));
                return 0;
            }));
        
        // Commande /aptitudes — libre pour soi ; staff pour voir autrui (view.other)
        dispatcher.register(Commands.literal("aptitudes")
            .executes(AptitudesCommands::executeSelfAptitudes)
            .then(Commands.literal("me")
                .executes(AptitudesCommands::executeSelfAptitudes))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> suggestOnlinePlayers(context.getSource(), builder))
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.aptitudes.view.other"))
                .executes(context -> {
                    String targetPlayerName = StringArgumentType.getString(context, "joueur");
                    CommandSourceStack source = context.getSource();
                    
                    if (source.getEntity() instanceof ServerPlayer player) {
                        var server = player.getServer();
                        if (server == null) {
                            source.sendFailure(new TextComponent("§c❌ Erreur : serveur non disponible"));
                            return 0;
                        }
                        
                        ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
                        if (targetPlayer == null) {
                            source.sendFailure(new TextComponent("§c❌ Joueur non trouvé : §e" + targetPlayerName));
                            return 0;
                        }
                        
                        return executeAptitudes(player, targetPlayer.getUUID());
                    } else {
                        source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                        return 0;
                    }
                })));
        
        // Commande /giveaptitudeinitiale (staff)
        dispatcher.register(Commands.literal("giveaptitudeinitiale")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.aptitudes.give.initial"))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> suggestOnlinePlayers(context.getSource(), builder))
                .executes(context -> {
                    String targetPlayerName = StringArgumentType.getString(context, "joueur");
                    CommandSourceStack source = context.getSource();
                    
                    if (source.getEntity() instanceof ServerPlayer executor) {
                        return executeGiveAptitudeInitiale(executor, targetPlayerName);
                    } else {
                        source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                        return 0;
                    }
                }))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /giveaptitudeinitiale <joueur>"));
                return 0;
            }));
        
        // Commande /giveaptitude (staff)
        dispatcher.register(Commands.literal("giveaptitude")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.aptitudes.give"))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> suggestOnlinePlayers(context.getSource(), builder))
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> suggestFocusTypes(builder))
                    .then(Commands.argument("nombre", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            String targetPlayerName = StringArgumentType.getString(context, "joueur");
                            String type = StringArgumentType.getString(context, "type");
                            int nombre = IntegerArgumentType.getInteger(context, "nombre");
                            CommandSourceStack source = context.getSource();
                            
                            if (source.getEntity() instanceof ServerPlayer executor) {
                                return executeGiveAptitude(executor, targetPlayerName, type, nombre);
                            } else {
                                source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                                return 0;
                            }
                        }))))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /giveaptitude <joueur> <martialité|artisanat|savoir> <nombre>"));
                return 0;
            }));
        
        // Commande /giveaptitudeglobal (staff)
        dispatcher.register(Commands.literal("giveaptitudeglobal")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.aptitudes.give.global"))
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                return executeGiveAptitudeGlobal(source);
            }));
        
        // Commande /aptitudeseligibles (staff)
        dispatcher.register(Commands.literal("aptitudeseligibles")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.aptitudes.eligibles"))
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                return executeAptitudesEligibles(source);
            }));
        
        // Commande /aptitudeshistorique (staff)
        dispatcher.register(Commands.literal("aptitudeshistorique")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.aptitudes.history"))
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                return executeAptitudesHistorique(source);
            }));
        
        // Commande /resetaptitudes (staff)
        dispatcher.register(Commands.literal("resetaptitudes")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.aptitudes.reset"))
            .then(Commands.argument("joueur", StringArgumentType.word())
                .suggests((context, builder) -> suggestOnlinePlayers(context.getSource(), builder))
                .executes(context -> {
                    String targetPlayerName = StringArgumentType.getString(context, "joueur");
                    CommandSourceStack source = context.getSource();
                    
                    if (source.getEntity() instanceof ServerPlayer executor) {
                        return executeResetAptitudes(executor, targetPlayerName);
                    } else {
                        source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
                        return 0;
                    }
                }))
            .executes(context -> {
                context.getSource().sendFailure(new TextComponent("Usage: /resetaptitudes <joueur>"));
                return 0;
            }));
        
        LOGGER.debug("Commandes d'aptitudes enregistrées");
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandSourceStack source, SuggestionsBuilder builder) {
        if (source.getServer() == null) {
            return builder.buildFuture();
        }
        Set<String> names = new LinkedHashSet<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            names.add(player.getGameProfile().getName());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static CompletableFuture<Suggestions> suggestFocusTypes(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[]{
                "martialité", "martialite", "artisanat", "savoir"
        }, builder);
    }
    
    /**
     * Exécute la commande /setfocus
     */
    private static int executeSetFocus(ServerPlayer player, String focusType) {
        try {
            // Valider le type de focus
            String normalizedFocus = normalizeFocusType(focusType);
            if (normalizedFocus == null) {
                player.sendMessage(new TextComponent("§c❌ Type de focus invalide. Utilisez : martialité, artisanat ou savoir"), player.getUUID());
                return 0;
            }
            
            UUID playerUUID = player.getUUID();
            RPDataManager.PlayerRPData data = AptitudesManager.getOrCreateAptitudesData(playerUUID);
            
            // Vérifier si le focus change vraiment
            String ancienFocus = data.focus;
            if (normalizedFocus.equals(ancienFocus)) {
                player.sendMessage(new TextComponent("§e⚠️ Votre focus est déjà défini sur : §6" + normalizedFocus), player.getUUID());
                return 0;
            }
            
            // Changer le focus (active le cooldown de 14 jours)
            AptitudesManager.changerFocus(playerUUID, normalizedFocus);
            
            // Message de confirmation
            if (ancienFocus == null || ancienFocus.isEmpty()) {
                player.sendMessage(new TextComponent("§a§l✅ Focus défini : §e" + normalizedFocus), player.getUUID());
            } else {
                player.sendMessage(new TextComponent("§a§l✅ Focus changé de §e" + ancienFocus + "§a vers §e" + normalizedFocus), player.getUUID());
                player.sendMessage(new TextComponent("§c⚠️ Cooldown de 14 jours activé. Vos connexions ne compteront pas pour l'éligibilité au give global pendant cette période."), player.getUUID());
            }
            
            ChatLog.detail(LOGGER,"Focus défini pour {} : {}", player.getName().getString(), normalizedFocus);
            return 1;
        } catch (Exception e) {
            player.sendMessage(new TextComponent("§c❌ Erreur lors de la définition du focus"), player.getUUID());
            LOGGER.error("Erreur lors de la définition du focus pour {}", player.getName().getString(), e);
            return 0;
        }
    }
    
    /**
     * /aptitudes et /aptitudes me — tous les joueurs.
     */
    private static int executeSelfAptitudes(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            return executeAptitudes(player, player.getUUID());
        }
        source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur !"));
        return 0;
    }

    /**
     * Exécute la commande /aptitudes
     */
    private static int executeAptitudes(ServerPlayer viewer, UUID targetUUID) {
        try {
            RPDataManager.PlayerRPData data = AptitudesManager.getAptitudesData(targetUUID);
            if (data == null) {
                viewer.sendMessage(new TextComponent("§c❌ Aucune donnée d'aptitudes trouvée"), viewer.getUUID());
                return 0;
            }
            
            // Obtenir le nom du joueur cible
            String targetName = viewer.getServer() != null ? 
                (viewer.getServer().getPlayerList().getPlayer(targetUUID) != null ? 
                    viewer.getServer().getPlayerList().getPlayer(targetUUID).getName().getString() : 
                    targetUUID.toString()) : 
                targetUUID.toString();
            
            boolean isSelf = viewer.getUUID().equals(targetUUID);
            
            // Afficher les aptitudes
            viewer.sendMessage(new TextComponent("§6§l═══════════════════════════════════════"), viewer.getUUID());
            viewer.sendMessage(new TextComponent("§6§l    APTITUDES" + (isSelf ? "" : " - " + targetName)), viewer.getUUID());
            viewer.sendMessage(new TextComponent("§6§l═══════════════════════════════════════"), viewer.getUUID());
            viewer.sendMessage(new TextComponent(""), viewer.getUUID());
            
            viewer.sendMessage(new TextComponent("§c⚔️ Martialité : §e" + data.martialite + "§7/10"), viewer.getUUID());
            com.example.ventryschat.aptitudes.MartialiteBonuses martialiteBonuses =
                com.example.ventryschat.aptitudes.MartialiteBonuses.forLevel(data.martialite);
            viewer.sendMessage(new TextComponent("§7Titre : §f" + martialiteBonuses.title()), viewer.getUUID());
            viewer.sendMessage(new TextComponent(martialiteBonuses.formatSummary()), viewer.getUUID());
            if (data.martialite > 0) {
                viewer.sendMessage(new TextComponent("§8Cadence : poings ou arme VentrysCombat · Dégâts : poings ou fer · Rési : mêlée reçue"), viewer.getUUID());
            }
            viewer.sendMessage(new TextComponent("§a🔨 Artisanat : §e" + data.artisanat + "§7/10"), viewer.getUUID());
            viewer.sendMessage(new TextComponent("§b📚 Savoir : §e" + data.savoir + "§7/10"), viewer.getUUID());
            viewer.sendMessage(new TextComponent(""), viewer.getUUID());
            
            viewer.sendMessage(new TextComponent("§eTotal réparti : §6" + data.getTotalRepartis() + "§7/15"), viewer.getUUID());
            viewer.sendMessage(new TextComponent("§ePoints à répartir : §6" + data.pointsARepartir), viewer.getUUID());
            viewer.sendMessage(new TextComponent(""), viewer.getUUID());
            
            if (data.focus != null && !data.focus.isEmpty()) {
                viewer.sendMessage(new TextComponent("§eFocus actuel : §6" + data.focus), viewer.getUUID());
            } else {
                viewer.sendMessage(new TextComponent("§eFocus actuel : §7Non défini"), viewer.getUUID());
            }
            
            if (data.estEnCooldown()) {
                long joursRestants = data.getJoursRestantsCooldown();
                viewer.sendMessage(new TextComponent("§c⚠️ Cooldown actif : §e" + joursRestants + " jour(s) restant(s)"), viewer.getUUID());
            }
            
            if (isSelf && data.derniereConnexionEligible > 0) {
                viewer.sendMessage(new TextComponent("§7Dernière connexion éligible : §e" + DATE_FORMAT.format(new Date(data.derniereConnexionEligible))), viewer.getUUID());
            }
            
            viewer.sendMessage(new TextComponent("§6§l═══════════════════════════════════════"), viewer.getUUID());
            
            return 1;
        } catch (Exception e) {
            viewer.sendMessage(new TextComponent("§c❌ Erreur lors de l'affichage des aptitudes"), viewer.getUUID());
            LOGGER.error("Erreur lors de l'affichage des aptitudes", e);
            return 0;
        }
    }
    
    /**
     * Exécute la commande /giveaptitudeinitiale
     */
    private static int executeGiveAptitudeInitiale(ServerPlayer executor, String targetPlayerName) {
        try {
            var server = executor.getServer();
            if (server == null) {
                executor.sendMessage(new TextComponent("§c❌ Erreur : serveur non disponible"), executor.getUUID());
                return 0;
            }
            
            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
            if (targetPlayer == null) {
                executor.sendMessage(new TextComponent("§c❌ Joueur non trouvé : §e" + targetPlayerName), executor.getUUID());
                return 0;
            }
            
            UUID targetUUID = targetPlayer.getUUID();
            
            if (AptitudesManager.donnerPointsInitiaux(targetUUID)) {
                executor.sendMessage(new TextComponent("§a§l✅ 5 points à répartir donnés à §e" + targetPlayerName), executor.getUUID());
                targetPlayer.sendMessage(new TextComponent("§a§l✅ Vous avez reçu §e5 points à répartir§a ! Utilisez §e/giveaptitude§a pour les répartir."), targetPlayer.getUUID());
                ChatLog.detail(LOGGER,"Points initiaux donnés à {} par {}", targetPlayerName, executor.getName().getString());
                return 1;
            } else {
                executor.sendMessage(new TextComponent("§c❌ Ce joueur a déjà reçu ses points initiaux"), executor.getUUID());
                return 0;
            }
        } catch (Exception e) {
            executor.sendMessage(new TextComponent("§c❌ Erreur lors de l'attribution des points initiaux"), executor.getUUID());
            LOGGER.error("Erreur lors de l'attribution des points initiaux", e);
            return 0;
        }
    }
    
    /**
     * Exécute la commande /giveaptitude
     */
    private static int executeGiveAptitude(ServerPlayer executor, String targetPlayerName, String type, int nombre) {
        try {
            var server = executor.getServer();
            if (server == null) {
                executor.sendMessage(new TextComponent("§c❌ Erreur : serveur non disponible"), executor.getUUID());
                return 0;
            }
            
            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
            if (targetPlayer == null) {
                executor.sendMessage(new TextComponent("§c❌ Joueur non trouvé : §e" + targetPlayerName), executor.getUUID());
                return 0;
            }
            
            UUID targetUUID = targetPlayer.getUUID();
            String normalizedType = normalizeFocusType(type);
            
            if (normalizedType == null) {
                executor.sendMessage(new TextComponent("§c❌ Type invalide. Utilisez : martialité, artisanat ou savoir"), executor.getUUID());
                return 0;
            }
            
            RPDataManager.PlayerRPData data = AptitudesManager.getOrCreateAptitudesData(targetUUID);
            
            // Vérifier les limites
            if (!data.peutRecevoirPoints(nombre, normalizedType)) {
                int valeurActuelle = data.getValeurAptitude(normalizedType);
                int totalRepartis = data.getTotalRepartis();
                
                if (valeurActuelle + nombre > 10) {
                    executor.sendMessage(new TextComponent("§c❌ Limite atteinte : cette aptitude ne peut pas dépasser 10 points (actuellement : " + valeurActuelle + ")"), executor.getUUID());
                } else if (totalRepartis + nombre > 15) {
                    executor.sendMessage(new TextComponent("§c❌ Limite totale atteinte : le joueur ne peut pas avoir plus de 15 points répartis (actuellement : " + totalRepartis + ")"), executor.getUUID());
                }
                return 0;
            }
            
            // Donner les points
            if (AptitudesManager.donnerPoints(targetUUID, normalizedType, nombre)) {
                int pointsConsommes = Math.min(nombre, data.pointsARepartir);
                int pointsRestants = data.pointsARepartir;
                
                // Réappliquer les effets si c'est la martialité qui a changé
                if (normalizedType.equalsIgnoreCase("martialité") || normalizedType.equalsIgnoreCase("martialite")) {
                    com.example.ventryschat.AptitudesEffectsManager.applyMartialiteEffects(targetPlayer);
                }
                
                executor.sendMessage(new TextComponent("§a§l✅ " + nombre + " point(s) donné(s) à §e" + targetPlayerName + "§a dans §e" + normalizedType), executor.getUUID());
                if (pointsConsommes > 0) {
                    executor.sendMessage(new TextComponent("§7(" + pointsConsommes + " point(s) à répartir consommé(s), " + pointsRestants + " restant(s))"), executor.getUUID());
                }
                
                targetPlayer.sendMessage(new TextComponent("§a§l✅ Vous avez reçu §e" + nombre + " point(s)§a dans §e" + normalizedType), targetPlayer.getUUID());
                
                ChatLog.detail(LOGGER,"Points donnés à {} par {} : {} points dans {}", targetPlayerName, executor.getName().getString(), nombre, normalizedType);
                return 1;
            } else {
                executor.sendMessage(new TextComponent("§c❌ Erreur lors de l'attribution des points"), executor.getUUID());
                return 0;
            }
        } catch (Exception e) {
            executor.sendMessage(new TextComponent("§c❌ Erreur lors de l'attribution des points"), executor.getUUID());
            LOGGER.error("Erreur lors de l'attribution des points", e);
            return 0;
        }
    }
    
    /**
     * Exécute la commande /giveaptitudeglobal
     */
    private static int executeGiveAptitudeGlobal(CommandSourceStack source) {
        try {
            source.sendSuccess(new TextComponent("§6⚙️ Give global en cours..."), false);
            
            int nombreJoueursAyantRecu = AptitudesManager.effectuerGiveGlobal();
            
            source.sendSuccess(new TextComponent("§a§l✅ Give global terminé ! §e" + nombreJoueursAyantRecu + "§a joueur(s) ont reçu le point."), false);
            
            ChatLog.detail(LOGGER,"Give global effectué par {}", source.getTextName());
            return 1;
        } catch (Exception e) {
            source.sendFailure(new TextComponent("§c❌ Erreur lors du give global"));
            LOGGER.error("Erreur lors du give global", e);
            return 0;
        }
    }
    
    /**
     * Exécute la commande /aptitudeseligibles
     */
    private static int executeAptitudesEligibles(CommandSourceStack source) {
        try {
            var server = source.getServer();
            if (server == null) {
                source.sendFailure(new TextComponent("§c❌ Erreur : serveur non disponible"));
                return 0;
            }
            
            int nombreEligibles = 0;
            
            source.sendSuccess(new TextComponent("§6§l═══════════════════════════════════════"), false);
            source.sendSuccess(new TextComponent("§6§l    JOUEURS ÉLIGIBLES POUR GIVE GLOBAL"), false);
            source.sendSuccess(new TextComponent("§6§l═══════════════════════════════════════"), false);
            source.sendSuccess(new TextComponent(""), false);
            
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID playerUUID = player.getUUID();
                if (AptitudesManager.estEligiblePourGiveGlobal(playerUUID)) {
                    nombreEligibles++;
                    RPDataManager.PlayerRPData data = AptitudesManager.getAptitudesData(playerUUID);
                    if (data != null) {
                        String focus = data.focus != null ? data.focus : "Non défini";
                        source.sendSuccess(new TextComponent("§e- §7" + player.getName().getString() + " §7(Focus: §e" + focus + "§7)"), false);
                    }
                }
            }
            
            source.sendSuccess(new TextComponent(""), false);
            source.sendSuccess(new TextComponent("§eTotal éligibles : §6" + nombreEligibles), false);
            source.sendSuccess(new TextComponent("§6§l═══════════════════════════════════════"), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(new TextComponent("§c❌ Erreur lors de l'affichage des joueurs éligibles"));
            LOGGER.error("Erreur lors de l'affichage des joueurs éligibles", e);
            return 0;
        }
    }
    
    /**
     * Exécute la commande /aptitudeshistorique
     */
    private static int executeAptitudesHistorique(CommandSourceStack source) {
        try {
            AptitudesManager.AptitudesHistory history = AptitudesManager.getHistory();
            
            source.sendSuccess(new TextComponent("§6§l═══════════════════════════════════════"), false);
            source.sendSuccess(new TextComponent("§6§l    HISTORIQUE DES GIVE GLOBAUX"), false);
            source.sendSuccess(new TextComponent("§6§l═══════════════════════════════════════"), false);
            source.sendSuccess(new TextComponent(""), false);
            
            if (history.dernierGiveGlobal > 0) {
                source.sendSuccess(new TextComponent("§eDernier give global : §6" + DATE_FORMAT.format(new Date(history.dernierGiveGlobal))), false);
            } else {
                source.sendSuccess(new TextComponent("§eDernier give global : §7Jamais"), false);
            }
            
            source.sendSuccess(new TextComponent(""), false);
            source.sendSuccess(new TextComponent("§eNombre total de give globaux : §6" + history.nombreGiveGlobaux), false);
            source.sendSuccess(new TextComponent("§6§l═══════════════════════════════════════"), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(new TextComponent("§c❌ Erreur lors de l'affichage de l'historique"));
            LOGGER.error("Erreur lors de l'affichage de l'historique", e);
            return 0;
        }
    }
    
    /**
     * Exécute la commande /resetaptitudes
     */
    private static int executeResetAptitudes(ServerPlayer executor, String targetPlayerName) {
        try {
            var server = executor.getServer();
            if (server == null) {
                executor.sendMessage(new TextComponent("§c❌ Erreur : serveur non disponible"), executor.getUUID());
                return 0;
            }
            
            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
            if (targetPlayer == null) {
                executor.sendMessage(new TextComponent("§c❌ Joueur non trouvé : §e" + targetPlayerName), executor.getUUID());
                return 0;
            }
            
            AptitudesManager.resetAptitudes(targetPlayer.getUUID());
            
            executor.sendMessage(new TextComponent("§a§l✅ Aptitudes réinitialisées pour §e" + targetPlayerName), executor.getUUID());
            targetPlayer.sendMessage(new TextComponent("§c§l⚠️ Vos aptitudes ont été réinitialisées par §e" + executor.getName().getString()), targetPlayer.getUUID());
            
            ChatLog.detail(LOGGER,"Aptitudes réinitialisées pour {} par {}", targetPlayerName, executor.getName().getString());
            return 1;
        } catch (Exception e) {
            executor.sendMessage(new TextComponent("§c❌ Erreur lors de la réinitialisation des aptitudes"), executor.getUUID());
            LOGGER.error("Erreur lors de la réinitialisation des aptitudes", e);
            return 0;
        }
    }
    
    /**
     * Normalise le type de focus/aptitude
     */
    private static String normalizeFocusType(String type) {
        if (type == null) {
            return null;
        }
        
        String normalized = type.toLowerCase().trim();
        return switch (normalized) {
            case "martialité", "martialite", "martial" -> "martialité";
            case "artisanat", "artisan" -> "artisanat";
            case "savoir", "sav" -> "savoir";
            default -> null;
        };
    }
}

