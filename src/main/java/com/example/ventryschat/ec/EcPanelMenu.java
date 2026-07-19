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

public final class EcPanelMenu extends AbstractContainerMenu {

    private static final String PERM = "ventryspermissions.staff.ec";
    private static final int ROWS = 1;

    public EcPanelMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenuTypes.EC_PANEL.get(), containerId);
        checkContainerSize(container, ROWS * 9);
        container.startOpen(playerInventory.player);
        for (int col = 0; col < 9; col++) {
            addSlot(new net.minecraft.world.inventory.Slot(container, col, 8 + col * 18, 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player instanceof ServerPlayer sp && slotId >= 0 && slotId < ROWS * 9) {
            ItemStack stack = getSlot(slotId).getItem();
            String panelKey = EcDisplay.panelKey(stack);
            int ecIndex = EcDisplay.ecIndex(stack);
            if (panelKey != null && ecIndex >= 0) {
                EcMenuOpener.openStorage(sp, panelKey, ecIndex);
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
}
