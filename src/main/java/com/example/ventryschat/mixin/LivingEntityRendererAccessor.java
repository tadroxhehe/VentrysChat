package com.example.ventryschat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Expose setupRotations() (protected) pour CarriedPlayerBodyRotMixin. */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {

    @Invoker("setupRotations")
    void ventryschat_setupRotations(LivingEntity entity, PoseStack poseStack, float ageInTicks, float bodyRot, float partialTick);
}
