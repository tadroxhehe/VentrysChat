package com.example.ventryschat.aptitudes;

import com.example.ventryschat.RPDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bonus de dégâts sortants et réduction de mêlée reçue selon la martialité.
 * Compatible poings (main vide) et VentrysCombat (armes {@code ventryscombat:*}).
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MartialiteCombatHandler {

    private MartialiteCombatHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamageHigh(LivingDamageEvent event) {
        if (event.getEntity().level.isClientSide) {
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!MartialiteWeaponChecks.isMartialiteDamageEligible(weapon)) {
            return;
        }

        RPDataManager.PlayerRPData data = RPDataManager.getPlayerData(attacker.getUUID());
        if (data == null) {
            return;
        }

        MartialiteBonuses bonuses = MartialiteBonuses.forLevel(data.martialite);
        if (bonuses.flatDamage() <= 0) {
            return;
        }

        event.setAmount(event.getAmount() + bonuses.flatDamage());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamageLow(LivingDamageEvent event) {
        if (event.getEntity().level.isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        if (!isMeleePlayerDamage(event.getSource())) {
            return;
        }

        RPDataManager.PlayerRPData data = RPDataManager.getPlayerData(victim.getUUID());
        if (data == null) {
            return;
        }

        MartialiteBonuses bonuses = MartialiteBonuses.forLevel(data.martialite);
        if (bonuses.meleeResistPercent() <= 0f) {
            return;
        }

        float factor = 1f - (bonuses.meleeResistPercent() / 100f);
        event.setAmount(event.getAmount() * factor);
    }

    private static boolean isMeleePlayerDamage(DamageSource source) {
        return !source.isProjectile() && source.getEntity() instanceof Player;
    }
}
