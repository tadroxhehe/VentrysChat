package com.example.ventryschat.ec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EcSavedData extends SavedData {

    public static final int EC_COUNT = 8;
    public static final int SLOTS_PER_EC = 27;
    /** Plafond anti-abus (création staff). */
    public static final int MAX_PANELS = 64;
    /** Max de panneaux par propriétaire (hors legacy / audit). */
    public static final int MAX_PANELS_PER_OWNER = 8;
    private static final String DATA_NAME = "ventryschat_ec_panels";

    public static final class Panel {
        public final String key;
        public final String displayName;
        /** {@code null} = panneau legacy sans propriétaire (accès audit only). */
        public final UUID ownerId;
        public final ItemStack[][] storages = new ItemStack[EC_COUNT][SLOTS_PER_EC];

        public Panel(String key, String displayName, UUID ownerId) {
            this.key = key;
            this.displayName = displayName;
            this.ownerId = ownerId;
            for (int ec = 0; ec < EC_COUNT; ec++) {
                for (int slot = 0; slot < SLOTS_PER_EC; slot++) {
                    storages[ec][slot] = ItemStack.EMPTY;
                }
            }
        }
    }

    private final Map<String, Panel> panels = new LinkedHashMap<>();

    public static EcSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(EcSavedData::load, EcSavedData::new, DATA_NAME);
    }

    public static EcSavedData load(CompoundTag tag) {
        EcSavedData data = new EcSavedData();
        ListTag panelsList = tag.getList("Panels", Tag.TAG_COMPOUND);
        for (int i = 0; i < panelsList.size(); i++) {
            CompoundTag panelTag = panelsList.getCompound(i);
            String key = panelTag.getString("Key");
            String displayName = panelTag.getString("DisplayName");
            if (displayName.isEmpty()) {
                displayName = key;
            }
            UUID ownerId = null;
            if (panelTag.hasUUID("Owner")) {
                ownerId = panelTag.getUUID("Owner");
            }
            Panel panel = new Panel(key, displayName, ownerId);
            ListTag ecList = panelTag.getList("Storages", Tag.TAG_COMPOUND);
            for (int ec = 0; ec < Math.min(EC_COUNT, ecList.size()); ec++) {
                CompoundTag ecTag = ecList.getCompound(ec);
                ListTag items = ecTag.getList("Items", Tag.TAG_COMPOUND);
                for (int j = 0; j < items.size(); j++) {
                    CompoundTag itemTag = items.getCompound(j);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot >= 0 && slot < SLOTS_PER_EC) {
                        panel.storages[ec][slot] = ItemStack.of(itemTag);
                    }
                }
            }
            data.panels.put(key, panel);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag panelsList = new ListTag();
        for (Panel panel : panels.values()) {
            CompoundTag panelTag = new CompoundTag();
            panelTag.putString("Key", panel.key);
            panelTag.putString("DisplayName", panel.displayName);
            if (panel.ownerId != null) {
                panelTag.putUUID("Owner", panel.ownerId);
            }
            ListTag ecList = new ListTag();
            for (int ec = 0; ec < EC_COUNT; ec++) {
                CompoundTag ecTag = new CompoundTag();
                ListTag items = new ListTag();
                for (int slot = 0; slot < SLOTS_PER_EC; slot++) {
                    ItemStack stack = panel.storages[ec][slot];
                    if (!stack.isEmpty()) {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putByte("Slot", (byte) slot);
                        stack.save(itemTag);
                        items.add(itemTag);
                    }
                }
                ecTag.put("Items", items);
                ecList.add(ecTag);
            }
            panelTag.put("Storages", ecList);
            panelsList.add(panelTag);
        }
        tag.put("Panels", panelsList);
        return tag;
    }

    public static String normalizeKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public boolean createPanel(String rawName, UUID ownerId) {
        String key = normalizeKey(rawName);
        if (key.isEmpty() || panels.containsKey(key) || panels.size() >= MAX_PANELS) {
            return false;
        }
        if (ownerId != null && countOwnedBy(ownerId) >= MAX_PANELS_PER_OWNER) {
            return false;
        }
        panels.put(key, new Panel(key, rawName.trim(), ownerId));
        setDirty();
        return true;
    }

    /**
     * Transfère la propriété d’un panneau (contenu conservé).
     * @return {@code false} si panneau introuvable ou plafond propriétaire atteint
     */
    public boolean transferPanel(String rawName, UUID newOwnerId) {
        if (newOwnerId == null) {
            return false;
        }
        String key = normalizeKey(rawName);
        Panel old = panels.get(key);
        if (old == null) {
            return false;
        }
        if (!newOwnerId.equals(old.ownerId) && countOwnedBy(newOwnerId) >= MAX_PANELS_PER_OWNER) {
            return false;
        }
        Panel neu = new Panel(old.key, old.displayName, newOwnerId);
        for (int ec = 0; ec < EC_COUNT; ec++) {
            for (int slot = 0; slot < SLOTS_PER_EC; slot++) {
                ItemStack stack = old.storages[ec][slot];
                neu.storages[ec][slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
        }
        panels.put(key, neu);
        setDirty();
        return true;
    }

    public int countOwnedBy(UUID ownerId) {
        if (ownerId == null) {
            return 0;
        }
        int n = 0;
        for (Panel panel : panels.values()) {
            if (ownerId.equals(panel.ownerId)) {
                n++;
            }
        }
        return n;
    }

    public Optional<Panel> getPanel(String rawName) {
        return Optional.ofNullable(panels.get(normalizeKey(rawName)));
    }

    public List<Panel> allPanels() {
        return new ArrayList<>(panels.values());
    }

    /** Panneaux visibles pour ce joueur (les siens, ou tous si audit {@code staff.ec.other}). */
    public List<Panel> visiblePanelsFor(net.minecraft.server.level.ServerPlayer player) {
        List<Panel> out = new ArrayList<>();
        for (Panel panel : panels.values()) {
            if (EcAccess.canAccess(player, panel)) {
                out.add(panel);
            }
        }
        return out;
    }

    public int panelCount() {
        return panels.size();
    }

    public void setStorageItem(String panelKey, int ecIndex, int slot, ItemStack stack) {
        Panel panel = panels.get(normalizeKey(panelKey));
        if (panel == null || ecIndex < 0 || ecIndex >= EC_COUNT || slot < 0 || slot >= SLOTS_PER_EC) {
            return;
        }
        panel.storages[ecIndex][slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        setDirty();
    }
}
