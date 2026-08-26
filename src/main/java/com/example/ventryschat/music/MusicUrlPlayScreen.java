package com.example.ventryschat.music;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Colle une URL longue (Discord CDN, etc.) — contourne la limite ~256 du chat.
 */
@OnlyIn(Dist.CLIENT)
public final class MusicUrlPlayScreen extends Screen {
    private EditBox urlBox;
    private EditBox radiusBox;
    private EditBox durationBox;
    private String status = "§7Colle le lien MP3/OGG/WAV puis Lancer.";

    public MusicUrlPlayScreen() {
        super(new TextComponent("Lancer une musique (URL)"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.urlBox = new EditBox(this.font, cx - 160, 60, 320, 20, new TextComponent("URL"));
        this.urlBox.setMaxLength(2048);
        this.urlBox.setBordered(true);
        this.urlBox.setValue("");
        this.addWidget(this.urlBox);

        this.radiusBox = new EditBox(this.font, cx - 160, 100, 100, 20, new TextComponent("Rayon"));
        this.radiusBox.setMaxLength(6);
        this.radiusBox.setValue("50");
        this.addWidget(this.radiusBox);

        this.durationBox = new EditBox(this.font, cx - 40, 100, 100, 20, new TextComponent("Durée"));
        this.durationBox.setMaxLength(6);
        this.durationBox.setValue("300");
        this.addWidget(this.durationBox);

        this.addRenderableWidget(new Button(cx - 160, 140, 150, 20, new TextComponent("Lancer"), b -> submit()));
        this.addRenderableWidget(new Button(cx + 10, 140, 150, 20, new TextComponent("Annuler"), b -> onClose()));
        this.setInitialFocus(this.urlBox);
    }

    private void submit() {
        String url = this.urlBox.getValue() == null ? "" : this.urlBox.getValue().trim();
        if (url.isEmpty()) {
            status = "§cURL vide.";
            return;
        }
        float radius;
        try {
            radius = Float.parseFloat(this.radiusBox.getValue().trim());
        } catch (Exception e) {
            status = "§cRayon invalide.";
            return;
        }
        Long durationMs = null;
        String durRaw = this.durationBox.getValue() == null ? "" : this.durationBox.getValue().trim();
        if (!durRaw.isEmpty()) {
            try {
                durationMs = Long.parseLong(durRaw) * 1000L;
            } catch (Exception e) {
                status = "§cDurée invalide (secondes).";
                return;
            }
        }
        MusicNetwork.sendPlayUrlRequest(url, radius, durationMs);
        status = "§aDemande envoyée…";
        this.onClose();
    }

    @Override
    public void tick() {
        this.urlBox.tick();
        this.radiusBox.tick();
        this.durationBox.tick();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256) {
            this.onClose();
            return true;
        }
        return this.urlBox.keyPressed(key, scan, modifiers)
            || this.radiusBox.keyPressed(key, scan, modifiers)
            || this.durationBox.keyPressed(key, scan, modifiers)
            || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {
        this.renderBackground(pose);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        drawString(pose, this.font, "URL (Discord / CDN / …)", this.width / 2 - 160, 48, 0xA0A0A0);
        drawString(pose, this.font, "Rayon", this.width / 2 - 160, 88, 0xA0A0A0);
        drawString(pose, this.font, "Durée (s)", this.width / 2 - 40, 88, 0xA0A0A0);
        this.urlBox.render(pose, mouseX, mouseY, partial);
        this.radiusBox.render(pose, mouseX, mouseY, partial);
        this.durationBox.render(pose, mouseX, mouseY, partial);
        drawCenteredString(pose, this.font, new TextComponent(status), this.width / 2, 175, 0xFFFFFF);
        drawCenteredString(
            pose,
            this.font,
            new TextComponent("§8Ctrl+V pour coller — pas de limite chat 256."),
            this.width / 2,
            195,
            0x808080
        );
        super.render(pose, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
