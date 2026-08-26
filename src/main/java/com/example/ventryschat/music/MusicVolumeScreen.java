package com.example.ventryschat.music;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class MusicVolumeScreen extends Screen {
    private final Screen parent;

    public MusicVolumeScreen(Screen parent) {
        super(new TextComponent("Musiques dynamiques"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 6 + 48;

        this.addRenderableWidget(new AbstractSliderButton(
            cx - 100,
            cy,
            200,
            20,
            TextComponent.EMPTY,
            MusicClientConfig.getVolumePercent() / 100.0
        ) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                int pct = (int) Math.round(this.value * 100.0);
                this.setMessage(new TextComponent("Volume musiques dynamiques : " + pct + " %"));
            }

            @Override
            protected void applyValue() {
                int pct = (int) Math.round(this.value * 100.0);
                MusicClientConfig.setVolumePercent(pct);
            }
        });

        this.addRenderableWidget(new Button(
            cx - 100,
            this.height / 6 + 168,
            200,
            20,
            new TranslatableComponent("gui.done"),
            b -> this.minecraft.setScreen(parent)
        ));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {
        this.renderBackground(pose);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        drawCenteredString(
            pose,
            this.font,
            new TextComponent("§7Courbe douce : 1 % ≈ inaudible, 100 % = max confortable."),
            this.width / 2,
            this.height / 6 + 24,
            0xA0A0A0
        );
        drawCenteredString(
            pose,
            this.font,
            new TextComponent("§8Le serveur ne force jamais ce réglage."),
            this.width / 2,
            this.height / 6 + 80,
            0x808080
        );
        super.render(pose, mouseX, mouseY, partial);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
