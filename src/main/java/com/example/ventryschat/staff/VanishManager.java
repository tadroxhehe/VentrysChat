package com.example.ventryschat.staff;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanish "doux" : rend le joueur invisible (effet d'invisibilite + drapeau entite),
 * mais le laisse visible dans le tab et ne detruit pas son entite cote client.
 * Note vanilla : l'objet tenu en main et l'armure equipee restent visibles malgre
 * l'invisibilite (comportement standard de LivingEntityRenderer, pas specifique a ce mod).
 */
@Mod.EventBusSubscriber(modid = "ventryschat")
public final class VanishManager {
    private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();

    private VanishManager() {
    }

    public static boolean isVanished(UUID uuid) {
        return VANISHED.contains(uuid);
    }

    public static void setVanished(ServerPlayer player, boolean vanish) {
        if (vanish) {
            VANISHED.add(player.getUUID());
            applyInvisibleState(player);
        } else {
            VANISHED.remove(player.getUUID());
            player.removeEffect(MobEffects.INVISIBILITY);
            player.setInvisible(false);
        }
    }

    private static void applyInvisibleState(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        player.setInvisible(true);
    }

    /** Restaure l'invisibilite si un joueur vanish s'est deconnecte puis reconnecte. */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer joiner) || !VANISHED.contains(joiner.getUUID())) {
            return;
        }
        MinecraftServer server = joiner.getServer();
        if (server != null) {
            server.execute(() -> applyInvisibleState(joiner));
        }
    }
}
