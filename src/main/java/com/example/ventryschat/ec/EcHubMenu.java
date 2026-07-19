package com.example.ventryschat.ec;

import com.example.ventryschat.compat.VentrysPermsBridge;
import com.example.ventryschat.registry.ModMenuTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public final class EcHubMenu extends AbstractContainerMenu {

    private static final String PERM = "ventryspermissions.staff.ec";

    private final Container container;
    private final int containerRows;

    public EcHubMenu(int containerId, Inventory playerInventory, Container container, int rows) {
        super(ModMenuTypes.EC_HUB.get(), containerId);
        this.container = container;
        this.containerRows = rows;
        checkContainerSize(container, rows * 9);
        container.startOpen(playerInventory.player);
        addChestGrid(container, rows);
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addChestGrid(Container container, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new net.minecraft.world.inventory.Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + (rows() - 1) * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new net.minecraft.world.inventory.Slot(
                    playerInventory, col, 8 + col * 18, 161 + (rows() - 1) * 18));
        }
    }

    public int getContainerRows() {
        return containerRows;
    }

    private int rows() {
        return containerRows;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player instanceof ServerPlayer sp && slotId >= 0 && slotId < containerRows * 9) {
            ItemStack stack = getSlot(slotId).getItem();
            String panelKey = EcDisplay.panelKey(stack);
            if (panelKey != null) {
                EcMenuOpener.openPanel(sp, panelKey);
                return;
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level.isClientSide) {
            return true;
        }
        return player instanceof ServerPlayer sp && VentrysPermsBridge.player(sp, PERM);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
