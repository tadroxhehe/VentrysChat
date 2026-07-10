package com.example.ventryschat.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class VentrysPermsBridge {

    private static final String PERMS_MOD = "ventryspermissions";
    private static final Method CAN;
    private static final boolean LOADED;

    static {
        Method method = null;
        boolean modPresent = ModList.get().isLoaded(PERMS_MOD);
        if (modPresent) {
            try {
                method = Class.forName("com.ventrys.permissions.api.VentrysPerms")
                        .getMethod("can", CommandSourceStack.class, String.class);
            } catch (ReflectiveOperationException ignored) {
                modPresent = false;
            }
        }
        CAN = method;
        LOADED = modPresent && method != null;
    }

    private VentrysPermsBridge() {
    }

    public static boolean staff(CommandSourceStack source, String permissionId) {
        if (LOADED) {
            try {
                return (boolean) CAN.invoke(null, source, permissionId);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return source.hasPermission(2);
    }

    public static boolean player(ServerPlayer player, String permissionId) {
        if (player == null) {
            return false;
        }
        return staff(player.createCommandSourceStack(), permissionId);
    }
}
