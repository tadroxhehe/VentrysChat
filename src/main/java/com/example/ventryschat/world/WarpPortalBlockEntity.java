package com.example.ventryschat.world;

import com.example.ventryschat.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WarpPortalBlockEntity extends BlockEntity {
    private String warpName = "";

    public WarpPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARP_PORTAL_BLOCK_ENTITY.get(), pos, state);
    }

    public String getWarpName() {
        return warpName;
    }

    public boolean hasWarp() {
        return warpName != null && !warpName.isBlank();
    }

    public void setWarpName(String name) {
        this.warpName = name == null ? "" : name.trim().toLowerCase();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("WarpName", warpName);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        warpName = tag.getString("WarpName");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
