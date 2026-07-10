package com.example.ventryschat.events;

import com.example.ventryschat.staff.StaffTeleportBackTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ventryschat")
public final class VanillaTeleportBackTracker {
    private VanillaTeleportBackTracker() {
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer sourcePlayer)) {
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
        if (lower.startsWith("tp ") || lower.startsWith("teleport ")) {
            StaffTeleportBackTracker.setBack(
                    sourcePlayer,
                    new StaffTeleportBackTracker.BackPos(
                            sourcePlayer.level.dimension(),
                            sourcePlayer.blockPosition(),
                            sourcePlayer.getYRot(),
                            sourcePlayer.getXRot()
                    )
            );
        }
    }
}
