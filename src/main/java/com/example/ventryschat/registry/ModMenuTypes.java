package com.example.ventryschat.registry;

import com.example.ventryschat.staff.InvSeeMenuFactory;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registry.MENU_REGISTRY, "ventryschat");

    public static final RegistryObject<MenuType<net.minecraft.world.inventory.ChestMenu>> INVSEE =
            MENU_TYPES.register("invsee", () -> IForgeMenuType.create(InvSeeMenuFactory::create));

    private ModMenuTypes() {
    }
}
