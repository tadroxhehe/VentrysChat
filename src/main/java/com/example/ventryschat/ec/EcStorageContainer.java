package com.example.ventryschat.ec;

import com.example.ventryschat.compat.VentrysPermsBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class EcStorageContainer extends SimpleContainer {

    private static final String PERM = "ventryspermissions.staff.ec";

    private final EcSavedData data;
    private final String panelKey;
    private final int ecIndex;

    public EcStorageContainer(EcSavedData data, String panelKey, int ecIndex) {
        super(EcSavedData.SLOTS_PER_EC);
        this.data = data;
        this.panelKey = panelKey;
        this.ecIndex = ecIndex;
        EcSavedData.Panel panel = data.getPanel(panelKey).orElse(null);
        if (panel != null) {
            for (int slot = 0; slot < EcSavedData.SLOTS_PER_EC; slot++) {
                super.setItem(slot, panel.storages[ecIndex][slot].copy());
            }
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack == null ? ItemStack.EMPTY : stack);
        data.setStorageItem(panelKey, ecIndex, slot, getItem(slot));
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level.isClientSide) {
            return true;
        }
        return player instanceof ServerPlayer sp && VentrysPermsBridge.player(sp, PERM);
    }
}
