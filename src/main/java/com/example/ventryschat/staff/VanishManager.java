package com.example.ventryschat.staff;

import com.example.ventryschat.compat.VentrysPermsBridge;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
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
 * Vanish : effet d'invisibilite (repere visuel pour le staff autorise a voir les vanish) +
 * masquage reel de l'entite cote client pour tout le monde d'autre. Le masquage reel passe par
 * deux mecanismes complementaires : un paquet de suppression envoye immediatement au moment du
 * /vanish (voir hideFromUnauthorized), et EntityBroadcastVanishMixin qui empeche ensuite tout
 * retracking (nouvelle connexion, deplacement...) tant que le joueur reste vanish.
 * Reste visible dans le tab (choix assume).
 */
@Mod.EventBusSubscriber(modid = "ventryschat")
public final class VanishManager {
    private static final String SEE_VANISHED_PERM = "ventryspermissions.moderation.vanish";
    private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();

    private VanishManager() {
    }

    public static boolean isVanished(UUID uuid) {
        return VANISHED.contains(uuid);
    }

    /** Utilise par EntityBroadcastVanishMixin pour decider si `target` doit etre trackee chez `viewer`. */
    public static boolean canSee(ServerPlayer viewer, ServerPlayer target) {
        if (viewer == target || !isVanished(target.getUUID())) {
            return true;
        }
        return VentrysPermsBridge.player(viewer, SEE_VANISHED_PERM);
    }

    public static void setVanished(ServerPlayer player, boolean vanish) {
        if (vanish) {
            VANISHED.add(player.getUUID());
            applyInvisibleState(player);
            hideFromUnauthorized(player);
        } else {
            VANISHED.remove(player.getUUID());
            player.removeEffect(MobEffects.INVISIBILITY);
            player.setInvisible(false);
        }
    }

    /**
     * Supprime immediatement l'entite chez les joueurs non autorises, sans attendre le prochain
     * recalcul naturel du tracker (qui ne se produit qu'au changement de section de chunk).
     */
    private static void hideFromUnauthorized(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(player.getId());
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other != player && !VentrysPermsBridge.player(other, SEE_VANISHED_PERM)) {
                other.connection.send(packet);
            }
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
