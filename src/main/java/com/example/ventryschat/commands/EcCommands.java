package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.example.ventryschat.ec.EcAccess;
import com.example.ventryschat.ec.EcMenuOpener;
import com.example.ventryschat.ec.EcSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "nom");
                                    if (!VALID_NAME.matcher(name).matches()) {
                                        player.sendMessage(new TextComponent(
                                                "§cNom invalide (1-32 caractères : lettres, chiffres, _ ou -)."),
                                                player.getUUID());
                                        return 0;
                                    }
                                    EcSavedData data = EcSavedData.get(player.getLevel());
                                    if (data.panelCount() >= EcSavedData.MAX_PANELS) {
                                        player.sendMessage(new TextComponent(
                                                "§cLimite globale de panneaux EC atteinte ("
                                                        + EcSavedData.MAX_PANELS + ")."),
                                                player.getUUID());
                                        return 0;
                                    }
                                    if (data.countOwnedBy(player.getUUID()) >= EcSavedData.MAX_PANELS_PER_OWNER) {
                                        player.sendMessage(new TextComponent(
                                                "§cVous avez déjà "
                                                        + EcSavedData.MAX_PANELS_PER_OWNER
                                                        + " panneaux EC."),
                                                player.getUUID());
                                        return 0;
                                    }
                                    if (!data.createPanel(name, player.getUUID())) {
                                        player.sendMessage(new TextComponent(
                                                "§cUn panneau EC nommé §f" + name + " §cexiste déjà."),
                                                player.getUUID());
                                        return 0;
                                    }
                                    player.sendMessage(new TextComponent(
                                            "§aPanneau EC « §f" + name.trim()
                                                    + "§a » créé (privé, 8 coffres)."),
                                            player.getUUID());
                                    return 1;
                                }))));
    }
}
