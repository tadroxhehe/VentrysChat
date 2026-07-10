package com.example.ventryschat.staff;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WarpSavedData extends SavedData {
    private static final String DATA_NAME = "ventryschat_warps";

    private final Map<String, WarpEntry> warps = new HashMap<>();

    public static WarpSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(WarpSavedData::load, WarpSavedData::new, DATA_NAME);
    }

    public static WarpSavedData load(CompoundTag tag) {
        WarpSavedData data = new WarpSavedData();
        ListTag list = tag.getList("Warps", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag w = list.getCompound(i);
            String name = w.getString("Name");
            if (name.isEmpty()) continue;
            WarpEntry e = WarpEntry.fromTag(w);
            if (e != null) {
                data.warps.put(name.toLowerCase(), e);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, WarpEntry> e : warps.entrySet()) {
            CompoundTag w = new CompoundTag();
            w.putString("Name", e.getKey());
            e.getValue().save(w);
            list.add(w);
        }
        tag.put("Warps", list);
        return tag;
    }

    public boolean setWarp(String name, ResourceKey<Level> dimension, BlockPos pos, float yRot, float xRot) {
        if (name == null || name.isBlank()) return false;
        warps.put(name.trim().toLowerCase(), new WarpEntry(dimension, pos, yRot, xRot));
        setDirty();
        return true;
    }

    public boolean removeWarp(String name) {
        if (name == null) return false;
        WarpEntry removed = warps.remove(name.trim().toLowerCase());
        if (removed != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public WarpEntry getWarp(String name) {
        if (name == null) return null;
        return warps.get(name.trim().toLowerCase());
    }

    public Set<String> getWarpNames() {
        return warps.keySet();
    }

    public record WarpEntry(ResourceKey<Level> dimension, BlockPos pos, float yRot, float xRot) {
        void save(CompoundTag tag) {
            tag.putString("Dimension", dimension.location().toString());
            tag.putInt("X", pos.getX());
            tag.putInt("Y", pos.getY());
            tag.putInt("Z", pos.getZ());
            tag.putFloat("Yaw", yRot);
            tag.putFloat("Pitch", xRot);
        }

        static WarpEntry fromTag(CompoundTag tag) {
            try {
                String dim = tag.getString("Dimension");
                if (dim.isEmpty()) return null;
                ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dim));
                BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
                float yaw = tag.getFloat("Yaw");
                float pitch = tag.getFloat("Pitch");
                return new WarpEntry(key, pos, yaw, pitch);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
