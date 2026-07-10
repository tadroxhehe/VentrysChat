package com.example.ventryschat.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class RPCommandSuggestions {

    private RPCommandSuggestions() {
    }

    static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandSourceStack source, SuggestionsBuilder builder) {
        if (source.getServer() == null) {
            return builder.buildFuture();
        }
        Set<String> names = new LinkedHashSet<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            names.add(player.getGameProfile().getName());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    static CompletableFuture<Suggestions> suggestNarrationColors(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[]{
            "white", "yellow", "green", "blue", "purple", "red", "orange", "gray"
        }, builder);
    }
}
