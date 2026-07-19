package com.example.ventryschat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class EcChestScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    private static final ResourceLocation CHEST_GUI = new ResourceLocation("textures/gui/container/generic_54.png");
    private final int inventoryRows;

    public EcChestScreen(T menu, Inventory playerInventory, Component title, int inventoryRows) {
        super(menu, playerInventory, title);
        this.inventoryRows = inventoryRows;
        this.imageHeight = 114 + inventoryRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CHEST_GUI);
        int left = this.leftPos;
        int top = this.topPos;
        this.blit(poseStack, left, top, 0, 0, this.imageWidth, inventoryRows * 18 + 17);
        this.blit(poseStack, left, top + inventoryRows * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }
}
