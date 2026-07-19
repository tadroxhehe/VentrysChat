package com.example.ventryschat.staff;

import com.example.ventryschat.registry.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * InvSee : 41 slots utiles (0–35 inventaire, 36–39 armure, 40 offhand) — pas de slots fantômes.
 */
public final class InvSeeMenu extends AbstractContainerMenu {

    public static final int TARGET_SLOTS = 41;
    public static final int DISPLAY_ROWS = 5;

    private final Container container;

    public InvSeeMenu(int containerId, Inventory viewerInventory, Container container) {
        super(ModMenuTypes.INVSEE.get(), containerId);
        this.container = container;
        checkContainerSize(container, TARGET_SLOTS);
        container.startOpen(viewerInventory.player);

        for (int i = 0; i < TARGET_SLOTS; i++) {
            int row = i / 9;
            int col = i % 9;
            addSlot(new Slot(container, i, 8 + col * 18, 18 + row * 18));
        }

        int playerInvY = 18 + DISPLAY_ROWS * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(viewerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(viewerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    public int getRowCount() {
        return DISPLAY_ROWS;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < TARGET_SLOTS) {
            if (!moveItemStackTo(stack, TARGET_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, TARGET_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
