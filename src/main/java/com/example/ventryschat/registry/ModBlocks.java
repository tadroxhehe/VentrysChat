package com.example.ventryschat.registry;
import com.example.ventryschat.world.NarrationTextBlock;
import com.example.ventryschat.world.WarpPortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "ventryschat");

    public static final RegistryObject<Block> NARRATION_TEXT_BLOCK = BLOCKS.register(
            "narration_text_block",
            () -> new NarrationTextBlock(BlockBehaviour.Properties.of(Material.DECORATION)
                    .strength(2.5F)
                    .noOcclusion())
    );

    public static final RegistryObject<Block> WARP_PORTAL_BLOCK = BLOCKS.register(
            "warp_portal_block",
            () -> new WarpPortalBlock(BlockBehaviour.Properties.of(Material.BARRIER)
                    .strength(0.5F)
                    .noOcclusion()
                    .noCollission())
    );

    private ModBlocks() {
    }
}
