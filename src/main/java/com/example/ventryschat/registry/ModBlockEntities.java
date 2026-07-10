package com.example.ventryschat.registry;

import com.example.ventryschat.world.NarrationTextBlockEntity;
import com.example.ventryschat.world.WarpPortalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, "ventryschat");

    public static final RegistryObject<BlockEntityType<NarrationTextBlockEntity>> NARRATION_TEXT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("narration_text_block_entity",
                    () -> BlockEntityType.Builder.of(
                                    NarrationTextBlockEntity::new,
                                    ModBlocks.NARRATION_TEXT_BLOCK.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<WarpPortalBlockEntity>> WARP_PORTAL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("warp_portal_block_entity",
                    () -> BlockEntityType.Builder.of(
                                    WarpPortalBlockEntity::new,
                                    ModBlocks.WARP_PORTAL_BLOCK.get())
                            .build(null));

    private ModBlockEntities() {
    }
}
