package com.example.ventryschat.commands;

import com.example.ventryschat.util.ChatSpeakerNames;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * {@code /roll} — tirage aléatoire 0–100 visible dans un rayon de ~32 blocs.
 */
public final class RollCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollCommands.class);
    private static final double ROLL_RANGE_BLOCKS = 32.0D;

    private RollCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("roll")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                if (!(source.getEntity() instanceof ServerPlayer player)) {
                    source.sendFailure(new TextComponent("§cCette commande est réservée aux joueurs."));
                    return 0;
                }
                return executeRoll(player);
            }));
        LOGGER.debug("Commande /roll enregistrée");
    }

    private static int executeRoll(ServerPlayer player) {
        int value = ThreadLocalRandom.current().nextInt(0, 101); // inclus 0 et 100
        String name = ChatSpeakerNames.forChat(player);
        String message = "§d* §f" + name + " §dfait un roll : §e" + value + "§d *";

        double rangeSq = ROLL_RANGE_BLOCKS * ROLL_RANGE_BLOCKS;
        for (Player nearby : player.level.players()) {
            if (nearby instanceof ServerPlayer target
                    && target.level == player.level
                    && player.distanceToSqr(nearby) <= rangeSq) {
                target.sendMessage(new TextComponent(message), target.getUUID());
            }
        }
        return 1;
    }
}
