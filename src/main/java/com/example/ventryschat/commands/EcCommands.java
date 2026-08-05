package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.example.ventryschat.ec.EcAccess;
import com.example.ventryschat.ec.EcMenuOpener;
import com.example.ventryschat.ec.EcSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.regex.Pattern;

public final class EcCommands {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");

    private EcCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ec")
                .requires(source -> VentrysPermsBridge.staff(source, EcAccess.PERM_OWN))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    EcSavedData data = EcSavedData.get(player.getLevel());
                    EcMenuOpener.openHub(player);
                    if (data.visiblePanelsFor(player).isEmpty()) {
                        player.sendMessage(new TextComponent(
                                "§eAucun panneau EC à vous. Créez-en un avec §f/ec create <nom>§e."), player.getUUID());
                    }
                    return 1;
                })
                .then(Commands.literal("create")
                        .then(Commands.argument("nom", StringArgumentType.word())
                                .executes(ctx -> createFor(
                                        ctx.getSource().getPlayerOrException(),
                                        ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "nom")
                                )))
                        .then(Commands.argument("joueur", EntityArgument.player())
                                .requires(source -> VentrysPermsBridge.staff(source, EcAccess.PERM_OTHER))
                                .then(Commands.argument("nom", StringArgumentType.word())
                                        .executes(ctx -> createFor(
                                                ctx.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(ctx, "joueur"),
                                                StringArgumentType.getString(ctx, "nom")
                                        )))))
                .then(Commands.literal("transfer")
                        .requires(source -> VentrysPermsBridge.staff(source, EcAccess.PERM_OTHER))
                        .then(Commands.argument("nom", StringArgumentType.word())
                                .then(Commands.argument("joueur", EntityArgument.player())
                                        .executes(ctx -> transfer(
                                                ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "nom"),
                                                EntityArgument.getPlayer(ctx, "joueur")
                                        ))))));
    }

    private static int createFor(ServerPlayer actor, ServerPlayer owner, String name) throws CommandSyntaxException {
        if (!VALID_NAME.matcher(name).matches()) {
            actor.sendMessage(new TextComponent(
                    "§cNom invalide (1-32 caractères : lettres, chiffres, _ ou -)."),
                    actor.getUUID());
            return 0;
        }
        EcSavedData data = EcSavedData.get(actor.getLevel());
        if (data.panelCount() >= EcSavedData.MAX_PANELS) {
            actor.sendMessage(new TextComponent(
                    "§cLimite globale de panneaux EC atteinte ("
                            + EcSavedData.MAX_PANELS + ")."),
                    actor.getUUID());
            return 0;
        }
        if (data.countOwnedBy(owner.getUUID()) >= EcSavedData.MAX_PANELS_PER_OWNER) {
            String who = owner.getUUID().equals(actor.getUUID()) ? "Vous avez" : owner.getGameProfile().getName() + " a";
            actor.sendMessage(new TextComponent(
                    "§c" + who + " déjà "
                            + EcSavedData.MAX_PANELS_PER_OWNER
                            + " panneaux EC."),
                    actor.getUUID());
            return 0;
        }
        if (!data.createPanel(name, owner.getUUID())) {
            actor.sendMessage(new TextComponent(
                    "§cUn panneau EC nommé §f" + name + " §cexiste déjà."),
                    actor.getUUID());
            return 0;
        }
        boolean forOther = !owner.getUUID().equals(actor.getUUID());
        if (forOther) {
            actor.sendMessage(new TextComponent(
                    "§aPanneau EC « §f" + name.trim()
                            + "§a » créé pour §f" + owner.getGameProfile().getName()
                            + "§a (privé, 8 coffres)."),
                    actor.getUUID());
            owner.sendMessage(new TextComponent(
                    "§aUn panneau EC « §f" + name.trim()
                            + "§a » vous a été créé. Ouvrez-le avec §f/ec§a."),
                    owner.getUUID());
        } else {
            actor.sendMessage(new TextComponent(
                    "§aPanneau EC « §f" + name.trim()
                            + "§a » créé (privé, 8 coffres)."),
                    actor.getUUID());
        }
        return 1;
    }

    private static int transfer(ServerPlayer actor, String name, ServerPlayer newOwner) {
        EcSavedData data = EcSavedData.get(actor.getLevel());
        if (data.getPanel(name).isEmpty()) {
            actor.sendMessage(new TextComponent(
                    "§cAucun panneau EC nommé §f" + name + "§c."),
                    actor.getUUID());
            return 0;
        }
        if (!data.transferPanel(name, newOwner.getUUID())) {
            actor.sendMessage(new TextComponent(
                    "§cImpossible de transférer (plafond de "
                            + EcSavedData.MAX_PANELS_PER_OWNER
                            + " panneaux pour §f" + newOwner.getGameProfile().getName() + "§c)."),
                    actor.getUUID());
            return 0;
        }
        actor.sendMessage(new TextComponent(
                "§aPanneau EC « §f" + name.trim()
                        + "§a » transféré à §f" + newOwner.getGameProfile().getName() + "§a."),
                actor.getUUID());
        newOwner.sendMessage(new TextComponent(
                "§aLe panneau EC « §f" + name.trim()
                        + "§a » vous a été attribué. Ouvrez-le avec §f/ec§a."),
                newOwner.getUUID());
        return 1;
    }
}
