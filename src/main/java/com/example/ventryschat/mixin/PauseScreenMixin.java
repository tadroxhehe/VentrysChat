package com.example.ventryschat.mixin;

import com.example.ventryschat.music.MusicVolumeScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "createPauseMenu" , at = @At("RETURN"))
    private void ventryschat$addMusicButton(CallbackInfo ci) {
        int x = this.width / 2 - 102;
        int y = this.height / 4 + 144 + 12;
        this.addRenderableWidget(new Button(
            x,
            y,
            204,
            20,
            new TextComponent("Musiques dynamiques…"),
            b -> this.minecraft.setScreen(new MusicVolumeScreen((PauseScreen) (Object) this))
        ));
    }
}
