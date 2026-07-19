package com.example.ventryschat.ec;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class EcDisplay {

    static final String TAG_PANEL = "EcPanel";
    static final String TAG_INDEX = "EcIndex";

    private EcDisplay() {
    }

    static ItemStack panelIcon(String displayName, String panelKey) {
        ItemStack stack = new ItemStack(Items.CHEST);
        stack.setHoverName(new TextComponent(displayName));
        stack.getOrCreateTag().putString(TAG_PANEL, panelKey);
        return stack;
    }

    static ItemStack ecSlotIcon(String displayName, String panelKey, int ecIndex) {
        ItemStack stack = new ItemStack(Items.ENDER_CHEST);
        stack.setHoverName(new TextComponent(displayName + " — EC " + (ecIndex + 1)));
        stack.getOrCreateTag().putString(TAG_PANEL, panelKey);
        stack.getOrCreateTag().putInt(TAG_INDEX, ecIndex);
        return stack;
    }

    static String panelKey(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        String key = stack.getTag().getString(TAG_PANEL);
        return key.isEmpty() ? null : key;
    }

    static int ecIndex(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(TAG_INDEX)) {
            return -1;
        }
        return stack.getTag().getInt(TAG_INDEX);
    }
}
