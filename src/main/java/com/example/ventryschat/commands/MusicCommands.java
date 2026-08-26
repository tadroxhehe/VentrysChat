package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.example.ventryschat.music.MusicCatalog;
import com.example.ventryschat.music.MusicServerManager;
import com.example.ventryschat.music.MusicZone;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class MusicCommands {
    public static final String PERM = "ventryspermissions.music";

    private static final SuggestionProvider<CommandSourceStack> TRACK_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(MusicCatalog.ids(), builder);

    private MusicCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("music")
                .requires(source -> VentrysPermsBridge.staff(source, PERM))
                .then(Commands.literal("play")
                    .then(Commands.argument("track", StringArgumentType.word())
                        .suggests(TRACK_SUGGESTIONS)
                        .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0F, 512.0F))
                            .executes(ctx -> play(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "track"),
                                FloatArgumentType.getFloat(ctx, "radius")
                            )))))
                .then(Commands.literal("stop")
                    .executes(ctx -> stop(ctx.getSource())))
                .then(Commands.literal("list")
                    .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("reload")
                    .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("active")
                    .executes(ctx -> active(ctx.getSource())))
        );
    }

    private static int play(CommandSourceStack source, String track, float radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<MusicZone> zone = MusicServerManager.play(player, track, radius);
        if (zone.isEmpty()) {
            source.sendFailure(new TextComponent("Piste inconnue: " + track + " — /music list"));
            return 0;
        }
        MusicZone z = zone.get();
        source.sendSuccess(new TextComponent(
            "§aMusique « " + track + " » lancée (rayon " + (int) z.radius + " blocs, "
                + (z.durationMs / 1000) + " s)."
        ), true);
        return 1;
    }

    private static int stop(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int n = MusicServerManager.stopNearOrAll(player);
        source.sendSuccess(new TextComponent("§eMusique arrêtée (" + n + " zone(s))."), true);
        return n;
    }

    private static int list(CommandSourceStack source) {
        if (MusicCatalog.ids().isEmpty()) {
            source.sendSuccess(new TextComponent("§7Aucune piste dans le catalogue."), false);
            return 0;
        }
        source.sendSuccess(new TextComponent("§6Pistes disponibles :"), false);
        for (MusicCatalog.Track t : MusicCatalog.all().values()) {
            source.sendSuccess(new TextComponent(
                " §7- §f" + t.id() + " §8(" + t.displayName() + ", " + (t.durationMs() / 1000) + " s)"
            ), false);
        }
        return MusicCatalog.ids().size();
    }

    private static int reload(CommandSourceStack source) {
        MusicCatalog.reload();
        source.sendSuccess(new TextComponent("§aCatalogue musique rechargé (" + MusicCatalog.ids().size() + " pistes)."), true);
        return 1;
    }

    private static int active(CommandSourceStack source) {
        var zones = MusicServerManager.allZones();
        if (zones.isEmpty()) {
            source.sendSuccess(new TextComponent("§7Aucune zone musicale active."), false);
            return 0;
        }
        source.sendSuccess(new TextComponent("§6Zones actives : " + zones.size()), false);
        long now = System.currentTimeMillis();
        for (MusicZone z : zones) {
            long left = Math.max(0L, (z.endsAtEpochMs() - now) / 1000L);
            source.sendSuccess(new TextComponent(
                " §7- §f" + z.trackId + " §8r=" + (int) z.radius + " reste=" + left + "s id=" + z.zoneId.toString().substring(0, 8)
            ), false);
        }
        return zones.size();
    }
}
