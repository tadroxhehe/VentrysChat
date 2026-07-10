package com.example.ventryschat.events;

import com.mojang.brigadier.ParseResults;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ventryschat")
public final class VanillaPrivateMessageTracker {
    private VanillaPrivateMessageTracker() {
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer from)) {
            return;
        }

        String raw = event.getParseResults().getReader().getString();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String line = raw.trim();
        if (line.startsWith("/")) {
            line = line.substring(1);
        }

        String lower = line.toLowerCase();
        if (!(lower.startsWith("msg ") || lower.startsWith("tell ") || lower.startsWith("w "))) {
            return;
        }
        if (lower.startsWith("ventrysmsg_internal ")) {
            return;
        }

        int firstSpace = line.indexOf(' ');
        if (firstSpace < 0 || firstSpace + 1 >= line.length()) {
            return;
        }

        String payload = line.substring(firstSpace + 1).trim();
        if (payload.isEmpty()) {
            return;
        }
        if (from.getServer() == null || from.getServer().getCommands() == null) {
            return;
        }

        String rewritten = "ventrysmsg_internal " + payload;
        ParseResults<net.minecraft.commands.CommandSourceStack> replaced =
                from.getServer().getCommands().getDispatcher().parse(rewritten, event.getParseResults().getContext().getSource());
        event.setParseResults(replaced);
    }
}
