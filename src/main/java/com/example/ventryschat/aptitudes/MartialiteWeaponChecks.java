package com.example.ventryschat.aptitudes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.ForgeRegistries;

public final class MartialiteWeaponChecks {

    private static final String VENTRYS_COMBAT_MOD = "ventryscombat";

    private MartialiteWeaponChecks() {
    }

    public static boolean isUnarmed(ItemStack stack) {
        return stack.isEmpty();
    }

    public static boolean isVentrysCombatMelee(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && VENTRYS_COMBAT_MOD.equals(id.getNamespace());
    }

    /** Armes de combat en fer (pas le bois d'entraînement). */
    public static boolean isVentrysCombatIronWeapon(ItemStack stack) {
        if (!isVentrysCombatMelee(stack)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getPath().contains("_en_fer");
    }

    /** Cadence martialité : poings ou arme VentrysCombat en main principale. */
    public static boolean isMartialiteAttackSpeedEligible(ItemStack stack) {
        return isUnarmed(stack) || isVentrysCombatMelee(stack);
    }

    /** Dégâts plats martialité : poings ou arme VentrysCombat en fer. */
    public static boolean isMartialiteDamageEligible(ItemStack stack) {
        return isUnarmed(stack) || isVentrysCombatIronWeapon(stack);
    }
}
