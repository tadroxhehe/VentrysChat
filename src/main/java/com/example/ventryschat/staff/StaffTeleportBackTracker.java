package com.example.ventryschat.staff;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "ventryschat")
public final class StaffTeleportBackTracker {

    public record BackPos(ResourceKey<Level> dimension, BlockPos pos, float yRot, float xRot) {
    }

    private static final Map<UUID, BackPos> LAST = new ConcurrentHashMap<>();

    private StaffTeleportBackTracker() {
    }

    public static void setBack(ServerPlayer player, BackPos pos) {
        LAST.put(player.getUUID(), pos);
    }

    public static BackPos getBack(ServerPlayer player) {
        return LAST.get(player.getUUID());
    }

    public static void clearBack(ServerPlayer player) {
        LAST.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        Level fromLevel = player.level;
        if (fromLevel.isClientSide) {
            return;
        }
        BlockPos from = player.blockPosition();
        ResourceKey<Level> dim = fromLevel.dimension();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        LAST.put(player.getUUID(), new BackPos(dim, from, yaw, pitch));
    }

    public static boolean teleportToBack(ServerPlayer player) {
        BackPos back = LAST.get(player.getUUID());
        if (back == null) {
            return false;
        }
        ServerLevel targetLevel = player.getServer().getLevel(back.dimension());
        if (targetLevel == null) {
            return false;
        }
        float x = back.pos.getX() + 0.5F;
        float y = back.pos.getY();
        float z = back.pos.getZ() + 0.5F;
        player.teleportTo(targetLevel, x, y, z, back.yRot, back.xRot);
        return true;
    }
}
