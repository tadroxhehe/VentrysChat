package com.example.ventryschat.client;

import com.example.ventryschat.ec.EcHubMenu;
import com.example.ventryschat.ec.EcPanelMenu;
import com.example.ventryschat.staff.InvSeeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

public final class EcScreens {

    private EcScreens() {
    }

    public static EcChestScreen<InvSeeMenu> invsee(InvSeeMenu menu, Inventory inv, Component title) {
        return new EcChestScreen<>(menu, inv, title, menu.getRowCount());
    }

    public static EcChestScreen<EcHubMenu> hub(EcHubMenu menu, Inventory inv, Component title) {
        return new EcChestScreen<>(menu, inv, title, menu.getContainerRows());
    }

    public static EcChestScreen<EcPanelMenu> panel(EcPanelMenu menu, Inventory inv, Component title) {
        return new EcChestScreen<>(menu, inv, title, 1);
    }

    public static EcChestScreen<ChestMenu> storage(ChestMenu menu, Inventory inv, Component title) {
        return new EcChestScreen<>(menu, inv, title, 3);
    }
}
