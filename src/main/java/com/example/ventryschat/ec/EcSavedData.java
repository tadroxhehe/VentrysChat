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

public final class EcSavedData extends SavedData {

    public static final int EC_COUNT = 8;
    public static final int SLOTS_PER_EC = 27;
    /** Plafond anti-abus (création staff). */
    public static final int MAX_PANELS = 64;
    private static final String DATA_NAME = "ventryschat_ec_panels";

    public static final class Panel {
        public final String key;
        public final String displayName;
        public final ItemStack[][] storages = new ItemStack[EC_COUNT][SLOTS_PER_EC];

        public Panel(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
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
            Panel panel = new Panel(key, displayName);
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

    public boolean createPanel(String rawName) {
        String key = normalizeKey(rawName);
        if (key.isEmpty() || panels.containsKey(key) || panels.size() >= MAX_PANELS) {
            return false;
        }
        panels.put(key, new Panel(key, rawName.trim()));
        setDirty();
        return true;
    }

    public Optional<Panel> getPanel(String rawName) {
        return Optional.ofNullable(panels.get(normalizeKey(rawName)));
    }

    public List<Panel> allPanels() {
        return new ArrayList<>(panels.values());
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
