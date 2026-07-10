package com.example.ventryschat.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "ventryschat");

    public static final RegistryObject<Item> NARRATION_TEXT_BLOCK = ITEMS.register(
            "narration_text_block",
            () -> new BlockItem(ModBlocks.NARRATION_TEXT_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS))
    );

    public static final RegistryObject<Item> WARP_PORTAL_BLOCK = ITEMS.register(
            "warp_portal_block",
            () -> new BlockItem(ModBlocks.WARP_PORTAL_BLOCK.get(), new Item.Properties())
    );

    private ModItems() {
    }
}
