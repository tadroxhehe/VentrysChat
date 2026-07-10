package com.example.ventryschat.staff;

import com.example.ventryschat.AptitudesManager;
import com.example.ventryschat.RPDataManager;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Réinitialise toutes les données modpack connues pour un joueur connecté.
 */
public final class PlayerFullResetService {

    private static final Logger LOGGER = LogManager.getLogger();

    private PlayerFullResetService() {
    }

    public record ResetResult(List<String> resetModules, List<String> failures) {
    }

    public static ResetResult resetOnlinePlayer(ServerPlayer target) {
        List<String> reset = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        try {
            RPDataManager.resetPlayerData(target.getUUID());
            RPDataManager.saveAndSyncPlayer(target.getUUID());
            reset.add("VentrysChat (RP, identité, aptitudes)");
        } catch (Exception e) {
            failures.add("VentrysChat: " + e.getMessage());
            LOGGER.error("Reset RP échoué pour {}", target.getName().getString(), e);
        }

        invokeOptionalReset("ventrysjob", "com.ventrys.job.api.PlayerDataReset", target, reset, failures);
        invokeOptionalReset("ventryssurvival", "com.tadrox.ventryssurvival.api.PlayerDataReset", target, reset, failures);
        invokeOptionalReset("hdskinmod", "com.hdskinmod.api.PlayerAppearanceReset", target, reset, failures);

        return new ResetResult(reset, failures);
    }

    private static void invokeOptionalReset(
            String modId,
            String className,
            ServerPlayer target,
            List<String> reset,
            List<String> failures
    ) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod("reset", ServerPlayer.class);
            method.invoke(null, target);
            reset.add(modId);
        } catch (Exception e) {
            failures.add(modId + ": " + e.getMessage());
            LOGGER.error("Reset {} échoué pour {}", modId, target.getName().getString(), e);
        }
    }

    public static void sendSummary(ServerPlayer executor, ServerPlayer target, ResetResult result) {
        executor.sendMessage(new TextComponent(
                "§a§lReset complet pour §e" + target.getName().getString() + "§a :"), executor.getUUID());
        for (String module : result.resetModules()) {
            executor.sendMessage(new TextComponent(" §7- §a" + module), executor.getUUID());
        }
        for (String failure : result.failures()) {
            executor.sendMessage(new TextComponent(" §7- §cÉchec " + failure), executor.getUUID());
        }
        target.sendMessage(new TextComponent(
                "§c§lToutes vos données modpack ont été réinitialisées par §e" + executor.getName().getString()), target.getUUID());
    }
}
