package com.example.ventryschat.staff;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeMenuFactory {
    private InvSeeMenuFactory() {
    }

    public static InvSeeMenu create(int windowId, Inventory inv, FriendlyByteBuf buf) {
        buf.readUUID();
        return new InvSeeMenu(windowId, inv, new SimpleContainer(InvSeeMenu.TARGET_SLOTS));
    }
}
