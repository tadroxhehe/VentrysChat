package com.example.ventryschat.client;

import com.example.ventryschat.world.NarrationTextBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class NarrationTextBlockRenderer implements BlockEntityRenderer<NarrationTextBlockEntity> {
    private static final float TEXT_SCALE = 0.018F;
    private static final int WRAP_AFTER_CHARS = 64;

    public NarrationTextBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NarrationTextBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        String text = blockEntity.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;

        float yaw = -facing.toYRot();
        var font = Minecraft.getInstance().font;

        String[] lines = wrapByChars(text, WRAP_AFTER_CHARS);

        renderFace(lines, blockEntity.getColor(), yaw, false, poseStack, buffer, packedLight, font);
        renderFace(lines, blockEntity.getColor(), yaw, true, poseStack, buffer, packedLight, font);
    }

    private static void renderFace(String[] lines, int color, float yaw, boolean backFace, PoseStack poseStack, MultiBufferSource buffer, int packedLight, net.minecraft.client.gui.Font font) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.65D, 0.5D);
        poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw + (backFace ? 180.0F : 0.0F)));
        poseStack.translate(0.0D, 0.0D, 0.281D);
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        float y = 0.0F;
        for (String line : lines) {
            int width = font.width(line);
            float x = -width / 2.0F;
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
            y += 10.0F;
        }

        poseStack.popPose();
    }

    private static String[] wrapByChars(String text, int wrapAfterChars) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String rawLine : normalized.split("\n", -1)) {
            if (rawLine.length() <= wrapAfterChars) {
                lines.add(rawLine);
                continue;
            }
            int start = 0;
            while (start < rawLine.length()) {
                int end = Math.min(start + wrapAfterChars, rawLine.length());
                lines.add(rawLine.substring(start, end));
                start = end;
            }
        }
        return lines.toArray(new String[0]);
    }
}
