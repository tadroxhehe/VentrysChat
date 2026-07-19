package com.example.ventryschat.registry;

import com.example.ventryschat.ec.EcMenuFactories;
import com.example.ventryschat.ec.EcHubMenu;
import com.example.ventryschat.ec.EcPanelMenu;
import com.example.ventryschat.staff.InvSeeMenuFactory;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registry.MENU_REGISTRY, "ventryschat");

    public static final RegistryObject<MenuType<com.example.ventryschat.staff.InvSeeMenu>> INVSEE =
            MENU_TYPES.register("invsee", () -> IForgeMenuType.create(InvSeeMenuFactory::create));

    public static final RegistryObject<MenuType<EcHubMenu>> EC_HUB =
            MENU_TYPES.register("ec_hub", () -> IForgeMenuType.create(EcMenuFactories::createHub));

    public static final RegistryObject<MenuType<EcPanelMenu>> EC_PANEL =
            MENU_TYPES.register("ec_panel", () -> IForgeMenuType.create(EcMenuFactories::createPanel));

    public static final RegistryObject<MenuType<net.minecraft.world.inventory.ChestMenu>> EC_STORAGE =
            MENU_TYPES.register("ec_storage", () -> IForgeMenuType.create(EcMenuFactories::createStorage));

    private ModMenuTypes() {
    }
}
