package com.example.ventryschat.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Façade publique des commandes RP (enregistrement délégué).
 */
public class RPCommands {

    private RPCommands() {
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RPCommandRegistration.register(dispatcher);
    }
}
