package com.example.ventryschat.ec;

import com.example.ventryschat.registry.ModMenuTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public final class EcMenuOpener {

    private EcMenuOpener() {
    }

    public static void openHub(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        EcSavedData data = EcSavedData.get(level);
        List<EcSavedData.Panel> visible = data.visiblePanelsFor(player);
        int count = visible.size();
        int rows = Math.max(1, Math.min(6, (count + 8) / 9));
        SimpleContainer display = new SimpleContainer(rows * 9);
        int slot = 0;
        boolean audit = EcAccess.canAccessOthers(player);
        for (EcSavedData.Panel panel : visible) {
            if (slot >= display.getContainerSize()) {
                break;
            }
            String label = panel.displayName;
            if (audit && panel.ownerId != null && !panel.ownerId.equals(player.getUUID())) {
                label = panel.displayName + " §8(autre)";
            } else if (audit && panel.ownerId == null) {
                label = panel.displayName + " §8(legacy)";
            }
            display.setItem(slot++, EcDisplay.panelIcon(label, panel.key));
        }
        final int finalRows = rows;
        NetworkHooks.openGui(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return new TextComponent(audit ? "Panneaux EC (audit)" : "Mes panneaux EC");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new EcHubMenu(syncId, inv, display, finalRows);
            }
        }, buf -> buf.writeVarInt(finalRows));
    }

    public static void openPanel(ServerPlayer player, String panelKey) {
        ServerLevel level = player.getLevel();
        EcSavedData data = EcSavedData.get(level);
        EcSavedData.Panel panel = data.getPanel(panelKey).orElse(null);
        if (panel == null) {
            player.sendMessage(new TextComponent("§cPanneau EC introuvable."), player.getUUID());
            return;
        }
        if (!EcAccess.canAccess(player, panel)) {
            player.sendMessage(new TextComponent("§cCe panneau EC ne vous appartient pas."), player.getUUID());
            return;
        }
        SimpleContainer display = new SimpleContainer(9);
        for (int i = 0; i < EcSavedData.EC_COUNT; i++) {
            display.setItem(i, EcDisplay.ecSlotIcon(panel.displayName, panel.key, i));
        }
        NetworkHooks.openGui(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return new TextComponent(panel.displayName);
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new EcPanelMenu(syncId, inv, display, panel.key);
            }
        }, buf -> buf.writeUtf(panel.key));
    }

    public static void openStorage(ServerPlayer player, String panelKey, int ecIndex) {
        ServerLevel level = player.getLevel();
        EcSavedData data = EcSavedData.get(level);
        EcSavedData.Panel panel = data.getPanel(panelKey).orElse(null);
        if (panel == null || ecIndex < 0 || ecIndex >= EcSavedData.EC_COUNT) {
            player.sendMessage(new TextComponent("§cCoffre EC introuvable."), player.getUUID());
            return;
        }
        if (!EcAccess.canAccess(player, panel)) {
            player.sendMessage(new TextComponent("§cCe panneau EC ne vous appartient pas."), player.getUUID());
            return;
        }
        EcStorageContainer storage = new EcStorageContainer(data, panel.key, ecIndex);
        final String title = panel.displayName + " — EC " + (ecIndex + 1);
        NetworkHooks.openGui(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return new TextComponent(title);
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new ChestMenu(ModMenuTypes.EC_STORAGE.get(), syncId, inv, storage, 3);
            }
        }, buf -> {
            buf.writeUtf(panel.key);
            buf.writeVarInt(ecIndex);
        });
    }
}
