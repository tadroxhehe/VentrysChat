package com.example.ventryschat.mixin;

import com.example.ventryschat.staff.VanishManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Empeche le tracker d'entites serveur de (re)spawner un joueur vanish chez un client non
 * autorise. Complement du paquet de suppression immediat envoye par VanishManager.
 * Couvre aussi les passengers (arme/monture/cosmetique attache en tant qu'entite montee sur le
 * joueur, ex. mods d'armes visibles type YDM) : sans ca, l'entite du joueur est bien cachee mais
 * ce qui le "chevauche" reste tracke et visible independamment.
 */
@Mixin(Entity.class)
public abstract class EntityBroadcastVanishMixin {

    @Inject(method = "broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), cancellable = true)
    private void ventryschat_hideVanishedFromUnauthorized(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        for (Entity entity = self; entity != null; entity = entity.getVehicle()) {
            if (entity instanceof ServerPlayer target && !VanishManager.canSee(player, target)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
