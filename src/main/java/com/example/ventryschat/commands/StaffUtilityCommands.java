package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.example.ventryschat.staff.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class StaffUtilityCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaffUtilityCommands.class);

    private StaffUtilityCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warp")
                .then(Commands.literal("set")
                        .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.warp.set"))
                        .then(Commands.argument("nom", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestWarpNames(ctx.getSource(), builder))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "nom");
                                    ServerLevel level = p.getLevel();
                                    WarpSavedData data = WarpSavedData.get(level);
                                    data.setWarp(name, level.dimension(), p.blockPosition(), p.getYRot(), p.getXRot());
                                    p.sendMessage(new TextComponent("§aWarp §e" + name + " §aenregistré."), p.getUUID());
                                    return 1;
                                })))
                .then(Commands.literal("del")
                        .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.warp.delete"))
                        .then(Commands.argument("nom", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestWarpNames(ctx.getSource(), builder))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "nom");
                                    WarpSavedData data = WarpSavedData.get(p.getLevel());
                                    if (data.removeWarp(name)) {
                                        p.sendMessage(new TextComponent("§aWarp §e" + name + " §asupprimé."), p.getUUID());
                                    } else {
                                        p.sendMessage(new TextComponent("§cWarp introuvable."), p.getUUID());
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("list")
                        .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.warp.list"))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            WarpSavedData data = WarpSavedData.get(p.getLevel());
                            if (data.getWarpNames().isEmpty()) {
                                p.sendMessage(new TextComponent("§7Aucun warp."), p.getUUID());
                            } else {
                                p.sendMessage(new TextComponent("§6Warps: §f" + String.join(", ", data.getWarpNames())), p.getUUID());
                            }
                            return 1;
                        }))
                .then(Commands.argument("nom", StringArgumentType.word())
                        .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.warp.use"))
                        .suggests((ctx, builder) -> suggestWarpNames(ctx.getSource(), builder))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "nom");
                            WarpSavedData data = WarpSavedData.get(p.getLevel());
                            WarpSavedData.WarpEntry e = data.getWarp(name);
                            if (e == null) {
                                p.sendMessage(new TextComponent("§cWarp inconnu."), p.getUUID());
                                return 0;
                            }
                            ServerLevel dest = p.getServer().getLevel(e.dimension());
                            if (dest == null) {
                                p.sendMessage(new TextComponent("§cDimension introuvable."), p.getUUID());
                                return 0;
                            }
                            rememberBack(p);
                            p.teleportTo(dest, e.pos().getX() + 0.5, e.pos().getY(), e.pos().getZ() + 0.5, e.yRot(), e.xRot());
                            p.sendMessage(new TextComponent("§aTéléportation vers §e" + name + "§a."), p.getUUID());
                            return 1;
                        })));

        dispatcher.register(Commands.literal("back")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.teleport.back"))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (StaffTeleportBackTracker.teleportToBack(p)) {
                        p.sendMessage(new TextComponent("§aRetour à la position précédente."), p.getUUID());
                        return 1;
                    }
                    p.sendMessage(new TextComponent("§cAucune position précédente."), p.getUUID());
                    return 0;
                })
                .then(Commands.argument("joueur", StringArgumentType.word())
                        .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.teleport.back.other"))
                        .suggests((ctx, builder) -> suggestOnlinePlayerNames(ctx.getSource(), builder))
                        .executes(ctx -> {
                            ServerPlayer exec = ctx.getSource().getPlayerOrException();
                            String targetName = StringArgumentType.getString(ctx, "joueur");
                            ServerPlayer target = exec.getServer().getPlayerList().getPlayerByName(targetName);
                            if (target == null) {
                                exec.sendMessage(new TextComponent("§cJoueur introuvable."), exec.getUUID());
                                return 0;
                            }
                            if (StaffTeleportBackTracker.teleportToBack(target)) {
                                exec.sendMessage(new TextComponent("§aRetour forcé de §e" + target.getGameProfile().getName() + "§a."), exec.getUUID());
                                if (target != exec) {
                                    target.sendMessage(new TextComponent("§eTu as été renvoyé à ta position précédente."), target.getUUID());
                                }
                                return 1;
                            }
                            exec.sendMessage(new TextComponent("§cAucune position précédente pour §e" + target.getGameProfile().getName() + "§c."), exec.getUUID());
                            return 0;
                        })));

        dispatcher.register(Commands.literal("vanish")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.moderation.vanish"))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    VanishManager.setVanished(p, true);
                    p.sendMessage(new TextComponent("§aVanish activé."), p.getUUID());
                    return 1;
                }));

        dispatcher.register(Commands.literal("devanish")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.moderation.vanish"))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    VanishManager.setVanished(p, false);
                    p.sendMessage(new TextComponent("§aVanish désactivé."), p.getUUID());
                    return 1;
                }));

        dispatcher.register(Commands.literal("tpzone")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.teleport.zone"))
                .then(Commands.argument("joueur", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestOnlinePlayerNames(ctx.getSource(), builder))
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.5, 512))
                                .executes(ctx -> {
                                    ServerPlayer exec = ctx.getSource().getPlayerOrException();
                                    String targetName = StringArgumentType.getString(ctx, "joueur");
                                    ServerPlayer target = exec.getServer().getPlayerList().getPlayerByName(targetName);
                                    if (target == null) {
                                        exec.sendMessage(new TextComponent("§cJoueur cible introuvable."), exec.getUUID());
                                        return 0;
                                    }
                                    double r = DoubleArgumentType.getDouble(ctx, "radius");
                                    double r2 = r * r;
                                    Level world = target.level;
                                    BlockPos center = target.blockPosition();
                                    int count = 0;
                                    for (ServerPlayer other : exec.getServer().getPlayerList().getPlayers()) {
                                        if (other == target) continue;
                                        if (other.level != world) continue;
                                        if (other.distanceToSqr(target) <= r2) {
                                            rememberBack(other);
                                            other.teleportTo((ServerLevel) world, center.getX() + 0.5, center.getY(), center.getZ() + 0.5, target.getYRot(), target.getXRot());
                                            other.sendMessage(new TextComponent("§eTéléportation par zone vers §f" + target.getGameProfile().getName() + "§e."), other.getUUID());
                                            count++;
                                        }
                                    }
                                    exec.sendMessage(new TextComponent("§a" + count + " joueur(s) téléporté(s) vers §e" + target.getGameProfile().getName() + "§a."), exec.getUUID());
                                    return 1;
                                }))));

        dispatcher.register(Commands.literal("warpzone")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.teleport.zone"))
                .then(Commands.argument("nom", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestWarpNames(ctx.getSource(), builder))
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.5, 512))
                                .executes(ctx -> {
                                    ServerPlayer exec = ctx.getSource().getPlayerOrException();
                                    String warpName = StringArgumentType.getString(ctx, "nom");
                                    double r = DoubleArgumentType.getDouble(ctx, "radius");
                                    WarpSavedData data = WarpSavedData.get(exec.getLevel());
                                    WarpSavedData.WarpEntry e = data.getWarp(warpName);
                                    if (e == null) {
                                        exec.sendMessage(new TextComponent("§cWarp inconnu."), exec.getUUID());
                                        return 0;
                                    }
                                    ServerLevel dest = exec.getServer().getLevel(e.dimension());
                                    if (dest == null) {
                                        exec.sendMessage(new TextComponent("§cDimension introuvable."), exec.getUUID());
                                        return 0;
                                    }
                                    double r2 = r * r;
                                    Level world = exec.level;
                                    int count = 0;
                                    for (ServerPlayer other : exec.getServer().getPlayerList().getPlayers()) {
                                        if (other == exec) continue;
                                        if (other.level != world) continue;
                                        if (other.distanceToSqr(exec) <= r2) {
                                            rememberBack(other);
                                            other.teleportTo(dest, e.pos().getX() + 0.5, e.pos().getY(), e.pos().getZ() + 0.5, e.yRot(), e.xRot());
                                            other.sendMessage(new TextComponent("§eTéléportation vers le warp §f" + warpName + "§e."), other.getUUID());
                                            count++;
                                        }
                                    }
                                    exec.sendMessage(new TextComponent("§a" + count + " joueur(s) envoyé(s) au warp §e" + warpName + "§a."), exec.getUUID());
                                    return 1;
                                }))));

        dispatcher.register(Commands.literal("invsee")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.moderation.invsee"))
                .then(Commands.argument("joueur", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestOnlinePlayerNames(ctx.getSource(), builder))
                        .executes(ctx -> {
                            ServerPlayer viewer = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "joueur");
                            ServerPlayer target = viewer.getServer().getPlayerList().getPlayerByName(name);
                            if (target == null) {
                                viewer.sendMessage(new TextComponent("§cJoueur hors ligne ou inconnu."), viewer.getUUID());
                                return 0;
                            }
                            NetworkHooks.openGui(viewer, new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return new TextComponent("InvSee: " + target.getGameProfile().getName());
                                }

                                @Override
                                public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                                    return new InvSeeMenu(syncId, inv, new InvSeeContainer(target));
                                }
                            }, buf -> buf.writeUUID(target.getUUID()));
                            return 1;
                        })));

        dispatcher.register(Commands.literal("ventrysmsg_internal")
                .then(Commands.argument("tout", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestPrivateMessageTargets(ctx.getSource(), builder))
                        .executes(ctx -> executePrivateMessage(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "tout")))));

        dispatcher.register(Commands.literal("msg")
                .then(Commands.argument("tout", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestPrivateMessageTargets(ctx.getSource(), builder))
                        .executes(ctx -> executePrivateMessage(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "tout")))));

        dispatcher.register(Commands.literal("tell")
                .then(Commands.argument("tout", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestPrivateMessageTargets(ctx.getSource(), builder))
                        .executes(ctx -> executePrivateMessage(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "tout")))));

        dispatcher.register(Commands.literal("w")
                .then(Commands.argument("tout", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestPrivateMessageTargets(ctx.getSource(), builder))
                        .executes(ctx -> executePrivateMessage(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "tout")))));

        dispatcher.register(Commands.literal("r")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer from = ctx.getSource().getPlayerOrException();
                            String message = StringArgumentType.getString(ctx, "message");
                            ServerPlayer to = PrivateMessageHandler.getLastPartner(from, from.getServer());
                            if (to == null) {
                                from.sendMessage(new TextComponent("§cPersonne à qui répondre."), from.getUUID());
                                return 0;
                            }
                            PrivateMessageHandler.sendMsg(from, to, message);
                            return 1;
                        })));

        LOGGER.debug("Commandes staff utilitaires enregistrées");
    }

    private static String[] splitTargetAndMessage(net.minecraft.server.MinecraftServer server, String greedy) {
        String[] words = greedy.trim().split("\\s+");
        if (words.length < 2) {
            return null;
        }
        int maxTake = Math.min(6, words.length - 1);
        for (int take = maxTake; take >= 1; take--) {
            StringBuilder candidate = new StringBuilder();
            for (int i = 0; i < take; i++) {
                if (i > 0) candidate.append(' ');
                candidate.append(words[i]);
            }
            String c = candidate.toString();
            if (RPNameHelper.findOnlinePlayer(server, c) != null) {
                StringBuilder rest = new StringBuilder();
                for (int i = take; i < words.length; i++) {
                    if (i > take) rest.append(' ');
                    rest.append(words[i]);
                }
                return new String[]{c, rest.toString()};
            }
        }
        return null;
    }

    private static CompletableFuture<Suggestions> suggestPrivateMessageTargets(CommandSourceStack source, SuggestionsBuilder builder) {
        if (source.getServer() == null) {
            return builder.buildFuture();
        }
        String input = builder.getRemaining();
        if (splitTargetAndMessage(source.getServer(), input) != null) {
            return builder.buildFuture();
        }
        Set<String> candidates = new LinkedHashSet<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            candidates.add(player.getGameProfile().getName() + " ");
            String rpName = RPNameHelper.displayName(player);
            if (!rpName.isBlank()) {
                candidates.add(rpName + " ");
            }
        }
        return SharedSuggestionProvider.suggest(candidates, builder);
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayerNames(CommandSourceStack source, SuggestionsBuilder builder) {
        if (source.getServer() == null) {
            return builder.buildFuture();
        }
        Set<String> candidates = new LinkedHashSet<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            candidates.add(player.getGameProfile().getName());
        }
        return SharedSuggestionProvider.suggest(candidates, builder);
    }

    private static CompletableFuture<Suggestions> suggestWarpNames(CommandSourceStack source, SuggestionsBuilder builder) {
        if (source.getServer() == null) {
            return builder.buildFuture();
        }
        Set<String> candidates = new LinkedHashSet<>();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            candidates.addAll(WarpSavedData.get(level).getWarpNames());
        }
        return SharedSuggestionProvider.suggest(candidates, builder);
    }

    private static int executePrivateMessage(ServerPlayer from, String greedy) {
        String[] parsed = splitTargetAndMessage(from.getServer(), greedy);
        if (parsed == null) {
            from.sendMessage(new TextComponent("§cUsage: /msg <pseudo ou nom RP> <message>"), from.getUUID());
            return 0;
        }
        ServerPlayer to = RPNameHelper.findOnlinePlayer(from.getServer(), parsed[0]);
        if (to == null) {
            from.sendMessage(new TextComponent("§cJoueur introuvable (pseudo ou nom RP)."), from.getUUID());
            return 0;
        }
        String cleanMessage = parsed[1] == null ? "" : parsed[1].trim();
        if (cleanMessage.isEmpty()) {
            from.sendMessage(new TextComponent("§cMessage vide."), from.getUUID());
            return 0;
        }
        if (from.getUUID().equals(to.getUUID())) {
            from.sendMessage(new TextComponent("§cTu ne peux pas t'envoyer un MP à toi-même."), from.getUUID());
            return 0;
        }
        PrivateMessageHandler.sendMsg(from, to, cleanMessage);
        return 1;
    }

    private static void rememberBack(ServerPlayer player) {
        StaffTeleportBackTracker.setBack(
                player,
                new StaffTeleportBackTracker.BackPos(
                        player.level.dimension(),
                        player.blockPosition(),
                        player.getYRot(),
                        player.getXRot()
                )
        );
    }
}
