package com.example.ventryschat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Force l'angle de rotation de corps utilise au rendu d'un joueur porte
 * (passager d'un autre joueur) a suivre le porteur, au lieu du calcul
 * vanilla (base sur le regard du passager, borne a 85 degres du porteur -
 * cf. LivingEntityRenderer#render, branche shouldSit). yRot/xRot du passager
 * (regard/camera) ne sont jamais touches, seul l'angle passe au rendu du
 * modele est remplace.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class CarriedPlayerBodyRotMixin {

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"))
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void ventryschat_forceCarriedBodyRot(LivingEntityRenderer instance, LivingEntity entity, PoseStack poseStack,
                                                  float ageInTicks, float bodyRot, float partialTick) {
        float finalBodyRot = bodyRot;
        if (entity instanceof Player passenger && passenger.getVehicle() instanceof Player carrier) {
            finalBodyRot = Mth.rotLerp(partialTick, carrier.yBodyRotO, carrier.yBodyRot);
        }
        ((LivingEntityRendererAccessor) instance).ventryschat_setupRotations(entity, poseStack, ageInTicks, finalBodyRot, partialTick);
    }
}
