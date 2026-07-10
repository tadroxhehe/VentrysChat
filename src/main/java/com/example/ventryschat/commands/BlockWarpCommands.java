package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.example.ventryschat.registry.ModBlocks;
import com.example.ventryschat.staff.WarpSavedData;
import com.example.ventryschat.staff.WarpTeleportHelper;
import com.example.ventryschat.world.WarpPortalBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class BlockWarpCommands {
    private static final int SCAN_RADIUS = 5;
    private static final String PERM_SET = "ventryspermissions.warp.block.set";
    private static final String PERM_GIVE = "ventryspermissions.warp.block.give";

    private BlockWarpCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("blockwarp")
                .then(Commands.literal("set")
                        .requires(source -> VentrysPermsBridge.staff(source, PERM_SET))
                        .then(Commands.argument("nom", StringArgumentType.string())
                                .suggests((ctx, builder) -> suggestWarpNames(ctx.getSource(), builder))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String warpName = StringArgumentType.getString(ctx, "nom");
                                    WarpSavedData data = WarpSavedData.get(player.getLevel());
                                    if (data.getWarp(warpName) == null) {
                                        player.sendMessage(new TextComponent("§cWarp inconnu : §e" + warpName), player.getUUID());
                                        return 0;
                                    }
                                    BlockPos portalPos = WarpTeleportHelper.findNearestWarpPortal(
                                            player.getLevel(), player.blockPosition(), SCAN_RADIUS);
                                    if (portalPos == null) {
                                        player.sendMessage(new TextComponent(
                                                "§cAucun bloc portail warp dans un rayon de " + SCAN_RADIUS + " blocs."), player.getUUID());
                                        return 0;
                                    }
                                    WarpPortalBlockEntity portal = (WarpPortalBlockEntity) player.getLevel().getBlockEntity(portalPos);
                                    if (portal == null) {
                                        player.sendMessage(new TextComponent("§cBloc portail introuvable."), player.getUUID());
                                        return 0;
                                    }
                                    portal.setWarpName(warpName);
                                    player.sendMessage(new TextComponent(
                                            "§aPortail §7(" + portalPos.getX() + ", " + portalPos.getY() + ", " + portalPos.getZ()
                                                    + ") §alié au warp §e" + warpName.trim().toLowerCase() + "§a."), player.getUUID());
                                    return 1;
                                })))
                .then(Commands.literal("give")
                        .requires(source -> VentrysPermsBridge.staff(source, PERM_GIVE))
                        .executes(ctx -> giveBlocks(ctx.getSource(), 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> giveBlocks(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"))))));
    }

    private static int giveBlocks(CommandSourceStack source, int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = new ItemStack(ModBlocks.WARP_PORTAL_BLOCK.get(), count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.sendMessage(new TextComponent("§a" + count + " bloc(s) portail warp reçu(s)."), player.getUUID());
        return 1;
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
}
