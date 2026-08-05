package com.example.ventryschat.commands;

import com.example.ventryschat.registry.ModBlocks;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Donne le bloc de texte HRP — accessible à tous les joueurs.
 */
public final class NarrationBlockCommands {

    private NarrationBlockCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("narrationblock")
                .requires(source -> source.hasPermission(0))
                .executes(ctx -> giveBlock(ctx.getSource())));
        dispatcher.register(Commands.literal("nrblock")
                .requires(source -> source.hasPermission(0))
                .executes(ctx -> giveBlock(ctx.getSource())));
    }

    private static int giveBlock(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(new TextComponent("Cette commande ne peut être utilisée que par un joueur."));
            return 0;
        }
        ItemStack stack = new ItemStack(ModBlocks.NARRATION_TEXT_BLOCK.get(), 1);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.sendMessage(new TextComponent("§aBloc de narration HRP reçu. Pose-le, puis clique pour écrire."), player.getUUID());
        return 1;
    }
}
