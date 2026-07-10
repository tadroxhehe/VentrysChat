package com.example.ventryschat.client;

import com.example.ventryschat.network.RPNetworkHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;

public class NarrationTextEditScreen extends Screen {
    private final BlockPos targetPos;
    private final String initialText;
    private EditBox textBox;

    public NarrationTextEditScreen(BlockPos targetPos, String initialText) {
        super(new TextComponent("Edition du texte narratif"));
        this.targetPos = targetPos;
        this.initialText = initialText == null ? "" : initialText;
    }

    public static void open(BlockPos pos, String currentText) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(new NarrationTextEditScreen(pos, currentText));
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.textBox = new EditBox(this.font, centerX - 120, centerY - 10, 240, 20, new TextComponent("Texte"));
        this.textBox.setMaxLength(32767);
        this.textBox.setValue(initialText);
        this.textBox.setFocus(true);
        this.addRenderableWidget(this.textBox);

        this.addRenderableWidget(new Button(centerX - 120, centerY + 20, 115, 20, new TextComponent("Done"), button -> saveAndClose()));
        this.addRenderableWidget(new Button(centerX + 5, centerY + 20, 115, 20, new TextComponent("Cancel"), button -> onClose()));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, this.height / 2 - 36, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, "Shift + clic droit en jeu pour changer la couleur", this.width / 2, this.height / 2 - 24, 0xAAAAAA);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter
            if (Screen.hasControlDown()) {
                saveAndClose();
            } else {
                insertLineBreakAtCursor();
            }
            return true;
        }
        if (keyCode == 256) { // Escape
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void insertLineBreakAtCursor() {
        String value = textBox.getValue();
        int cursor = textBox.getCursorPosition();
        if (cursor < 0) {
            cursor = 0;
        }
        if (cursor > value.length()) {
            cursor = value.length();
        }

        String updated = value.substring(0, cursor) + "\n" + value.substring(cursor);
        textBox.setValue(updated);
        textBox.setCursorPosition(cursor + 1);
    }

    private void saveAndClose() {
        RPNetworkHandler.sendNarrationTextUpdate(targetPos, textBox.getValue());
        onClose();
    }
}
