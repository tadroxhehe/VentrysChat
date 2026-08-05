package com.example.ventryschat.world;

import com.example.ventryschat.staff.WarpPortalCooldown;
import com.example.ventryschat.staff.WarpTeleportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class WarpPortalBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.block();

    public WarpPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Indestructible hors créatif (GM0 / survie / aventure). */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return player != null && player.getAbilities().instabuild ? 1.0F : 0.0F;
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            net.minecraft.world.level.material.FluidState fluid
    ) {
        if (player == null || !player.getAbilities().instabuild) {
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        tryActivatePortal(level, pos, player, false);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && tryActivatePortal(level, pos, serverPlayer, true)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static boolean tryActivatePortal(Level level, BlockPos pos, ServerPlayer player, boolean fromUse) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof WarpPortalBlockEntity portal)) {
            return false;
        }
        if (!portal.hasWarp()) {
            if (fromUse) {
                player.displayClientMessage(new TextComponent("§cCe portail n'a pas de warp configuré."), true);
            }
            return false;
        }
        if (WarpPortalCooldown.isOnCooldown(player)) {
            return false;
        }
        if (WarpTeleportHelper.teleportToWarp(player, portal.getWarpName())) {
            WarpPortalCooldown.mark(player);
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarpPortalBlockEntity(pos, state);
    }
}
