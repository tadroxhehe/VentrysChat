package com.example.ventryschat;

import com.example.ventryschat.aptitudes.MartialiteBonuses;
import com.example.ventryschat.aptitudes.MartialiteWeaponChecks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applique les bonus passifs de martialité (cadence via attribut, sans potions visibles).
 * Dégâts et rési mêlée : {@link com.example.ventryschat.aptitudes.MartialiteCombatHandler}.
 */
@Mod.EventBusSubscriber
public class AptitudesEffectsManager {

    private static final UUID MARTIALITE_SPEED_UUID = UUID.fromString("8b4e2c91-6f3a-4d1e-9c0b-2a7f5e8d1c3b");
    private static final String MARTIALITE_SPEED_NAME = "ventryschat.martialite_speed";

    private static final int EFFECT_REAPPLY_INTERVAL_TICKS = 100;
    private static final ConcurrentHashMap<UUID, Integer> lastReapplyTick = new ConcurrentHashMap<>();

    public static void applyMartialiteEffects(ServerPlayer player) {
        if (player == null) {
            return;
        }

        removeLegacyPotionEffects(player);

        RPDataManager.PlayerRPData data = RPDataManager.getPlayerData(player.getUUID());
        int martialite = data != null ? data.martialite : 0;
        updateAttackSpeedModifier(player, martialite);
    }

    private static void updateAttackSpeedModifier(ServerPlayer player, int martialite) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }

        attackSpeed.removeModifier(MARTIALITE_SPEED_UUID);

        MartialiteBonuses bonuses = MartialiteBonuses.forLevel(martialite);
        if (bonuses.attackSpeedPercent() <= 0f) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!MartialiteWeaponChecks.isMartialiteAttackSpeedEligible(mainHand)) {
            return;
        }

        attackSpeed.addPermanentModifier(new AttributeModifier(
            MARTIALITE_SPEED_UUID,
            MARTIALITE_SPEED_NAME,
            bonuses.attackSpeedPercent() / 100.0D,
            AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    /** Retire les anciens effets potion du système legacy. */
    private static void removeLegacyPotionEffects(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var server = serverPlayer.getServer();
        if (server == null) {
            return;
        }

        server.execute(() -> {
            applyMartialiteEffects(serverPlayer);
            lastReapplyTick.put(serverPlayer.getUUID(), server.getTickCount());
        });
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        int currentTick = server.getTickCount();
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        for (ServerPlayer player : players) {
            if (player == null) {
                continue;
            }

            UUID playerUUID = player.getUUID();
            Integer lastTick = lastReapplyTick.get(playerUUID);

            if (lastTick == null || (currentTick - lastTick) >= EFFECT_REAPPLY_INTERVAL_TICKS) {
                applyMartialiteEffects(player);
                lastReapplyTick.put(playerUUID, currentTick);
            }
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (event.getSlot() != EquipmentSlot.MAINHAND) {
            return;
        }

        RPDataManager.PlayerRPData data = RPDataManager.getPlayerData(serverPlayer.getUUID());
        int martialite = data != null ? data.martialite : 0;
        removeLegacyPotionEffects(serverPlayer);
        updateAttackSpeedModifier(serverPlayer, martialite);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            lastReapplyTick.remove(serverPlayer.getUUID());
            AttributeInstance attackSpeed = serverPlayer.getAttribute(Attributes.ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.removeModifier(MARTIALITE_SPEED_UUID);
            }
            removeLegacyPotionEffects(serverPlayer);
        }
    }
}
