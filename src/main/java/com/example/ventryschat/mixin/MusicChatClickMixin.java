package com.example.ventryschat.mixin;

import com.example.ventryschat.music.MusicClientManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Oui/Non musique 100 % client — n'envoie plus /musiclisten au serveur.
 */
public final class MusicChatClickMixin {
    private MusicChatClickMixin() {
    }

    @Mixin(Screen.class)
    public abstract static class ScreenClicks {
        @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
        private void ventryschat$musicConsentClick(Style style, CallbackInfoReturnable<Boolean> cir) {
            if (style == null) {
                return;
            }
            ClickEvent click = style.getClickEvent();
            if (click == null || click.getAction() != ClickEvent.Action.RUN_COMMAND) {
                return;
            }
            if (MusicClientManager.tryHandleListenCommand(click.getValue())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Mixin(LocalPlayer.class)
    public abstract static class LocalPlayerChat {
        @Inject(method = "chat", at = @At("HEAD"), cancellable = true)
        private void ventryschat$blockMusicListenSend(String message, CallbackInfo ci) {
            if (MusicClientManager.tryHandleListenCommand(message)) {
                ci.cancel();
            }
        }
    }
}
