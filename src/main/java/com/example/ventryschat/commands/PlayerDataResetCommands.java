package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.example.ventryschat.staff.PlayerFullResetService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerDataResetCommands {

    private static final String PERM = "ventryspermissions.player.data.reset";

    private PlayerDataResetCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerAlias(dispatcher, "resetallplayerdata");
        registerAlias(dispatcher, "resetalldata");
    }

    private static void registerAlias(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(buildResetCommand(commandName));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildResetCommand(String commandName) {
        return Commands.literal(commandName)
                .requires(source -> VentrysPermsBridge.staff(source, PERM))
                .then(Commands.argument("joueur", StringArgumentType.word())
                        .suggests((ctx, builder) -> RPCommandSuggestions.suggestOnlinePlayers(ctx.getSource(), builder))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayer executor)) {
                                source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur."));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(ctx, "joueur");
                            ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(targetName);
                            if (target == null) {
                                executor.sendMessage(new TextComponent("§cJoueur introuvable ou hors ligne : §e" + targetName), executor.getUUID());
                                return 0;
                            }
                            PlayerFullResetService.ResetResult result = PlayerFullResetService.resetOnlinePlayer(target);
                            PlayerFullResetService.sendSummary(executor, target, result);
                            return result.failures().isEmpty() ? 1 : 0;
                        }))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(new TextComponent("Usage: /" + commandName + " <joueur>"));
                    return 0;
                });
    }
}
