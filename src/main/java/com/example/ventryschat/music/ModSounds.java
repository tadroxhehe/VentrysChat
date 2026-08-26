package com.example.ventryschat.music;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "ventryschat");

    public static final RegistryObject<SoundEvent> MUSIC_BATAILLE = register("music.bataille");
    public static final RegistryObject<SoundEvent> MUSIC_TAVERNE = register("music.taverne");
    public static final RegistryObject<SoundEvent> MUSIC_ROYAL = register("music.royal");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation("ventryschat", name);
        return SOUND_EVENTS.register(name, () -> new SoundEvent(id));
    }
}
