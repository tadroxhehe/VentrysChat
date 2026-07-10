package com.example.ventryschat.staff;

import com.example.ventryschat.world.WarpPortalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WarpTeleportHelper {

    private WarpTeleportHelper() {
    }

    public static boolean teleportToWarp(ServerPlayer player, String warpName) {
        if (player == null || warpName == null || warpName.isBlank()) {
            return false;
        }
        WarpSavedData data = WarpSavedData.get(player.getLevel());
        WarpSavedData.WarpEntry entry = data.getWarp(warpName);
        if (entry == null) {
            player.sendMessage(new TextComponent("§cWarp inconnu."), player.getUUID());
            return false;
        }
        ServerLevel dest = player.getServer().getLevel(entry.dimension());
        if (dest == null) {
            player.sendMessage(new TextComponent("§cDimension introuvable."), player.getUUID());
            return false;
        }
        rememberBack(player);
        player.teleportTo(
                dest,
                entry.pos().getX() + 0.5,
                entry.pos().getY(),
                entry.pos().getZ() + 0.5,
                entry.yRot(),
                entry.xRot()
        );
        player.sendMessage(new TextComponent("§aTéléportation vers §e" + warpName.trim() + "§a."), player.getUUID());
        return true;
    }

    public static void rememberBack(ServerPlayer player) {
        StaffTeleportBackTracker.setBack(
                player,
                new StaffTeleportBackTracker.BackPos(
                        player.level.dimension(),
                        player.blockPosition(),
                        player.getYRot(),
                        player.getXRot()
                )
        );
    }

    public static BlockPos findNearestWarpPortal(ServerLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        int r2 = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockEntity(cursor) instanceof WarpPortalBlockEntity) {
                        double dist = center.distSqr(cursor);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = cursor.immutable();
                        }
                    }
                }
            }
        }
        return nearest;
    }
}
