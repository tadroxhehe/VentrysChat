package com.example.ventryschat.staff;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import com.example.ventryschat.registry.ModMenuTypes;

public final class InvSeeMenuFactory {
    private InvSeeMenuFactory() {
    }

    public static ChestMenu create(int windowId, Inventory inv, FriendlyByteBuf buf) {
        buf.readUUID();
        return new ChestMenu(ModMenuTypes.INVSEE.get(), windowId, inv, new SimpleContainer(45), 5);
    }
}
