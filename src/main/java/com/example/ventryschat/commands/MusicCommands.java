package com.example.ventryschat.commands;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.example.ventryschat.music.MusicCatalog;
import com.example.ventryschat.music.MusicNetwork;
import com.example.ventryschat.music.MusicServerManager;
import com.example.ventryschat.music.MusicZone;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MusicCommands {
    public static final String PERM = "ventryspermissions.music";

    private static final SuggestionProvider<CommandSourceStack> PLAY_SUGGESTIONS = (ctx, builder) -> {
        List<String> hints = new ArrayList<>(MusicCatalog.ids());
        hints.add("https://");
        return SharedSuggestionProvider.suggest(hints, builder);
    };

    private MusicCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("music")
                .requires(source -> VentrysPermsBridge.staff(source, PERM))
                .then(Commands.literal("play")
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests(PLAY_SUGGESTIONS)
                        .executes(ctx -> playFlexible(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "args")
                        ))))
                .then(Commands.literal("url")
                    .executes(ctx -> openUrlGui(ctx.getSource())))
                .then(Commands.literal("stop")
                    .executes(ctx -> stop(ctx.getSource())))
                .then(Commands.literal("list")
                    .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("reload")
                    .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("active")
                    .executes(ctx -> active(ctx.getSource())))
        );

        // Opt-in joueur (clic chat) — pas de perm staff
        dispatcher.register(
            Commands.literal("musiclisten")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("accept")
                    .then(Commands.argument("zone", StringArgumentType.string())
                        .executes(ctx -> consent(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "zone"),
                            true
                        ))))
                .then(Commands.literal("decline")
                    .then(Commands.argument("zone", StringArgumentType.string())
                        .executes(ctx -> consent(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "zone"),
                            false
                        ))))
        );
    }

    private static int consent(CommandSourceStack source, String zoneRaw, boolean accept) {
        // Le Oui/Non est géré 100% client (mixin). Ne pas renvoyer ConsentPacket
        // → évite "Received invalid message ConsentPacket" (désync client/serveur).
        return 1;
    }

    /**
     * Parse : {@code <piste|url> <rayon> [durée_secondes]}
     * Ex. {@code bataille 50} · {@code https://cdn…/theme.ogg 80} · {@code https://…/a.ogg 50 180}
     */
    private static int playFlexible(CommandSourceStack source, String rawArgs) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String trimmed = rawArgs.trim();
        if (trimmed.isEmpty()) {
            source.sendFailure(new TextComponent("Usage: /music play <piste|url> <rayon> [secondes]"));
            return 0;
        }

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length < 2) {
            source.sendFailure(new TextComponent("Indique un rayon. Ex: /music play bataille 50"));
            return 0;
        }

        // Format: <piste|url> <rayon> [durée_secondes]
        float radius;
        Long durationMs = null;
        int endExclusive;
        String last = tokens[tokens.length - 1];
        String secondLast = tokens.length >= 3 ? tokens[tokens.length - 2] : null;

        if (tokens.length >= 3 && isPlainNumber(last) && isPlainNumber(secondLast)) {
            try {
                radius = Float.parseFloat(secondLast);
                durationMs = Long.parseLong(last) * 1000L;
                endExclusive = tokens.length - 2;
            } catch (NumberFormatException e) {
                source.sendFailure(new TextComponent("Rayon / durée invalides."));
                return 0;
            }
        } else {
            try {
                radius = Float.parseFloat(last);
                endExclusive = tokens.length - 1;
            } catch (NumberFormatException e) {
                source.sendFailure(new TextComponent("Rayon invalide: " + last));
                return 0;
            }
        }

        String trackOrUrl = join(tokens, 0, endExclusive);
        if (trackOrUrl.isEmpty()) {
            source.sendFailure(new TextComponent("Piste / URL manquante."));
            return 0;
        }

        if (MusicServerManager.isHttpUrl(trackOrUrl)) {
            String reject = MusicServerManager.urlRejectReason(trackOrUrl);
            if (reject != null) {
                source.sendFailure(new TextComponent(reject));
                return 0;
            }
        }

        Optional<MusicZone> zone = MusicServerManager.play(player, trackOrUrl, radius, durationMs);
        if (zone.isEmpty()) {
            if (MusicServerManager.isHttpUrl(trackOrUrl)) {
                source.sendFailure(new TextComponent("Impossible de lancer cette URL."));
            } else {
                source.sendFailure(new TextComponent("Piste inconnue: " + trackOrUrl + " — /music list"));
            }
            return 0;
        }
        MusicZone z = zone.get();
        String what = z.isUrlStream() ? z.streamUrl : z.trackId;
        source.sendSuccess(new TextComponent(
            "§aMusique lancée §f" + shorten(what, 60)
                + " §7(rayon " + (int) z.radius + ", " + (z.durationMs / 1000) + " s)."
        ), true);
        return 1;
    }

    private static boolean isPlainNumber(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static String join(String[] tokens, int from, int toExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < toExclusive; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    private static String shorten(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }

    /** Ouvre un écran client pour coller une URL trop longue pour le chat (~256). */
    private static int openUrlGui(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MusicNetwork.openUrlScreen(player);
        source.sendSuccess(new TextComponent("§aÉcran URL ouvert — colle le lien Discord puis Lancer."), false);
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
            source.sendSuccess(new TextComponent("§7Aucune piste packagée. Tu peux aussi:"), false);
            source.sendSuccess(new TextComponent("§f/music play https://…/fichier.mp3 50"), false);
            return 0;
        }
        source.sendSuccess(new TextComponent("§6Pistes packagées (clique pour rejouer) :"), false);
        for (MusicCatalog.Track t : MusicCatalog.all().values()) {
            String cmd = "/music play " + t.id() + " 50";
            TextComponent line = new TextComponent(" §7- §a" + t.id() + " §8(" + t.displayName() + ")");
            line.setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new TextComponent("Clic → " + cmd))));
            source.sendSuccess(line, false);
        }
        source.sendSuccess(new TextComponent("§7Ou lien direct: §f/music play <url.mp3|.ogg|.wav> <rayon> [secondes]"), false);
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
            String label = z.isUrlStream() ? shorten(z.streamUrl, 40) : z.trackId;
            source.sendSuccess(new TextComponent(
                " §7- §f" + label + " §8r=" + (int) z.radius + " reste=" + left + "s"
            ), false);
        }
        return zones.size();
    }
}
