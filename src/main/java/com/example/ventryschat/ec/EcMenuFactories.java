package com.example.ventryschat.ec;

import com.example.ventryschat.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

public final class EcMenuFactories {

    private EcMenuFactories() {
    }

    public static EcHubMenu createHub(int windowId, Inventory inv, FriendlyByteBuf buf) {
        int rows = buf.readVarInt();
        rows = Math.max(1, Math.min(6, rows));
        return new EcHubMenu(windowId, inv, new SimpleContainer(rows * 9), rows);
    }

    public static EcPanelMenu createPanel(int windowId, Inventory inv, FriendlyByteBuf buf) {
        buf.readUtf();
        return new EcPanelMenu(windowId, inv, new SimpleContainer(9));
    }

    public static ChestMenu createStorage(int windowId, Inventory inv, FriendlyByteBuf buf) {
        buf.readUtf();
        buf.readVarInt();
        return new ChestMenu(ModMenuTypes.EC_STORAGE.get(), windowId, inv, new SimpleContainer(27), 3);
    }
}
