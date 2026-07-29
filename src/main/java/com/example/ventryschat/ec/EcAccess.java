package com.example.ventryschat.ec;

import com.example.ventryschat.compat.VentrysPermsBridge;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Contrôle d'accès aux panneaux EC (privé par propriétaire). */
public final class EcAccess {

    public static final String PERM_OWN = "ventryspermissions.staff.ec";
    /** Voir / ouvrir les panneaux des autres (audit staff). */
    public static final String PERM_OTHER = "ventryspermissions.staff.ec.other";

    private EcAccess() {
    }

    public static boolean canUseEc(ServerPlayer player) {
        return VentrysPermsBridge.player(player, PERM_OWN);
    }

    public static boolean canAccessOthers(ServerPlayer player) {
        return VentrysPermsBridge.player(player, PERM_OTHER);
    }

    public static boolean canAccess(ServerPlayer player, EcSavedData.Panel panel) {
        if (player == null || panel == null || !canUseEc(player)) {
            return false;
        }
        if (canAccessOthers(player)) {
            return true;
        }
        UUID owner = panel.ownerId;
        return owner != null && owner.equals(player.getUUID());
    }
}
