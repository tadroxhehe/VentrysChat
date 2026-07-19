package com.example.ventryschat.client;

import com.example.ventryschat.registry.ModBlockEntities;
import com.example.ventryschat.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "ventryschat", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.INVSEE.get(), EcScreens::invsee);
            MenuScreens.register(ModMenuTypes.EC_HUB.get(), EcScreens::hub);
            MenuScreens.register(ModMenuTypes.EC_PANEL.get(), EcScreens::panel);
            MenuScreens.register(ModMenuTypes.EC_STORAGE.get(), EcScreens::storage);
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.NARRATION_TEXT_BLOCK_ENTITY.get(), NarrationTextBlockRenderer::new);
    }
}
