package com.example.ventryschat.staff;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InvSeeContainer extends SimpleContainer {
    private final ServerPlayer target;

    public InvSeeContainer(ServerPlayer target) {
        super(InvSeeMenu.TARGET_SLOTS);
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
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index >= 0 && index < InvSeeMenu.TARGET_SLOTS;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= InvSeeMenu.TARGET_SLOTS) {
            return;
        }
        super.setItem(slot, stack == null ? ItemStack.EMPTY : stack);
        ItemStack copy = getItem(slot).copy();
        Inventory inv = target.getInventory();
        if (slot < 36) {
            inv.setItem(slot, copy);
        } else if (slot < 40) {
            inv.setItem(slot, copy);
        } else if (slot == 40) {
            inv.setItem(40, copy);
        }
        inv.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return target != null && !target.isRemoved() && player.isAlive();
    }
}
