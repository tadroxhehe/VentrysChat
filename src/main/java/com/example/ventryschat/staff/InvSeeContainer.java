package com.example.ventryschat.staff;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class InvSeeContainer extends SimpleContainer {
    private final ServerPlayer target;

    public InvSeeContainer(ServerPlayer target) {
        super(45);
        this.target = target;
        refreshFromTarget();
    }

    public void refreshFromTarget() {
        Inventory inv = target.getInventory();
        for (int i = 0; i < 36; i++) {
            super.setItem(i, inv.getItem(i).copy());
        }
        for (int i = 0; i < 4; i++) {
            super.setItem(36 + i, inv.getItem(36 + i).copy());
        }
        super.setItem(40, inv.getItem(40).copy());
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack == null ? ItemStack.EMPTY : stack);
        ItemStack copy = getItem(slot).copy();
        Inventory inv = target.getInventory();
        if (slot >= 0 && slot < 36) {
            inv.setItem(slot, copy);
        } else if (slot >= 36 && slot < 40) {
            inv.setItem(slot, copy);
        } else if (slot == 40) {
            inv.setItem(40, copy);
        }
        inv.setChanged();
    }
}
