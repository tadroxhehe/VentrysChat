package com.example.ventryschat.client;

import com.example.ventryschat.world.NarrationTextBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Texte HRP en billboard : toujours face à la caméra du joueur (yaw + pitch).
 */
public class NarrationTextBlockRenderer implements BlockEntityRenderer<NarrationTextBlockEntity> {
    private static final float TEXT_SCALE = 0.0115F;
    private static final int TEXT_ALPHA = 0xB3;
    private static final float LINE_HEIGHT = 9.0F;

    public NarrationTextBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NarrationTextBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        String text = blockEntity.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        String[] lines = blockEntity.getWrappedLines();
        int color = withAlpha(blockEntity.getColor(), TEXT_ALPHA);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.62D, 0.5D);
        // Oriente le plan du texte vers la caméra (360° miroir du point de vue).
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        float y = 0.0F;
        for (String line : lines) {
            float x = -font.width(line) / 2.0F;
            font.drawInBatch(
                    line,
                    x,
                    y,
                    color,
                    false,
                    poseStack.last().pose(),
                    buffer,
                    false,
                    0,
                    packedLight
            );
            y += LINE_HEIGHT;
        }

        poseStack.popPose();
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}
