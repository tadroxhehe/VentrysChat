package com.example.ventryschat.compat;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Soft-dep VentrysCombat — coupe les bonus martialité legacy si le combat directionnel est actif.
 */
public final class DirectionalCombatBridge {

    private static final Method SUPPRESS;
    private static final boolean LOADED;

    static {
        Method suppress = null;
        boolean ok = ModList.get().isLoaded("ventryscombat");
        if (ok) {
            try {
                Class<?> api = Class.forName("com.ventrys.combat.api.DirectionalCombatAPI");
                suppress = api.getMethod("suppressLegacyMartialiteBonuses");
            } catch (ReflectiveOperationException e) {
                ok = false;
            }
        }
        SUPPRESS = suppress;
        LOADED = ok && suppress != null;
    }

    private DirectionalCombatBridge() {
    }

    public static boolean suppressLegacyMartialiteBonuses() {
        if (!LOADED) {
            return false;
        }
        try {
            return (boolean) SUPPRESS.invoke(null);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
