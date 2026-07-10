package com.example.ventryschat.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration serveur VentrysChat — fichier généré : {@code config/ventryschat-server.toml}
 */
public final class VentrysChatConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue MOD_ENABLED;

    public static final ForgeConfigSpec.IntValue NORMAL_DISTANCE;
    public static final ForgeConfigSpec.IntValue WHISPER_DISTANCE;
    public static final ForgeConfigSpec.IntValue SHOUT_DISTANCE;
    public static final ForgeConfigSpec.IntValue SCREAM_DISTANCE;
    public static final ForgeConfigSpec.IntValue ACTION_DISTANCE;
    public static final ForgeConfigSpec.IntValue CHUCHOT_DISTANCE;
    public static final ForgeConfigSpec.IntValue DEFAULT_NARRATION_DISTANCE;
    public static final ForgeConfigSpec.IntValue MIN_NARRATION_DISTANCE;
    public static final ForgeConfigSpec.IntValue MAX_NARRATION_DISTANCE;

    public static final ForgeConfigSpec.IntValue AUTO_SAVE_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.BooleanValue SAVE_ON_LOGOUT;
    public static final ForgeConfigSpec.BooleanValue SAVE_ON_SERVER_STOP;

    public static final ForgeConfigSpec.BooleanValue VERBOSE_LOGGING;
    public static final ForgeConfigSpec.BooleanValue DUMP_PLAYERS_ON_STARTUP;
    public static final ForgeConfigSpec.BooleanValue HIDE_DEATH_MESSAGES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("VentrysChat — configuration serveur RP");
        builder.push("general");
        MOD_ENABLED = builder
            .comment("Activer ou désactiver le mod côté serveur")
            .define("enabled", true);
        builder.pop();

        builder.push("chat");
        builder.comment("Distances de chat en blocs (valeurs par défaut = comportement code d'origine)");
        NORMAL_DISTANCE = builder.defineInRange("default_distance", 15, 1, 500);
        WHISPER_DISTANCE = builder.defineInRange("whisper_distance", 4, 1, 100);
        SHOUT_DISTANCE = builder.defineInRange("shout_distance", 30, 1, 500);
        SCREAM_DISTANCE = builder.defineInRange("scream_distance", 60, 1, 1000);
        ACTION_DISTANCE = builder.defineInRange("action_distance", 15, 1, 500);
        CHUCHOT_DISTANCE = builder.defineInRange("chuchot_distance", 2, 1, 50);
        DEFAULT_NARRATION_DISTANCE = builder.defineInRange("default_narration_distance", 100, 1, 2000);
        MIN_NARRATION_DISTANCE = builder.defineInRange("min_narration_distance", 10, 1, 500);
        MAX_NARRATION_DISTANCE = builder.defineInRange("max_narration_distance", 1000, 10, 5000);
        HIDE_DEATH_MESSAGES = builder
            .comment("Masquer les messages de mort vanilla (ex. « X was slain by … ») — chat et console serveur")
            .define("hide_death_messages", true);
        builder.pop();

        builder.push("names");
        AUTO_SAVE_INTERVAL_MINUTES = builder
            .comment("Sauvegarde automatique des données RP (minutes)")
            .defineInRange("auto_save_interval_minutes", 30, 1, 1440);
        SAVE_ON_LOGOUT = builder
            .comment("Sauvegarder à la déconnexion des joueurs")
            .define("save_on_logout", true);
        SAVE_ON_SERVER_STOP = builder
            .comment("Sauvegarder à l'arrêt du serveur")
            .define("save_on_server_stop", true);
        builder.pop();

        builder.push("logging");
        builder.comment("Contrôle du bruit dans latest.log (prod : verbose_logging = false)");
        VERBOSE_LOGGING = builder
            .comment("Logs détaillés (commandes RP, sauvegardes, init) en INFO au lieu de DEBUG")
            .define("verbose_logging", false);
        DUMP_PLAYERS_ON_STARTUP = builder
            .comment("Lister UUID + prénom/nom de chaque joueur au démarrage (niveau DEBUG)")
            .define("dump_players_on_startup", false);
        builder.pop();

        SPEC = builder.build();
    }

    private VentrysChatConfig() {
    }

    private static int safeInt(ForgeConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (Exception e) {
            return fallback;
        }
    }

    public static int normalDistance() {
        return safeInt(NORMAL_DISTANCE, 15);
    }

    public static int whisperDistance() {
        return safeInt(WHISPER_DISTANCE, 4);
    }

    public static int shoutDistance() {
        return safeInt(SHOUT_DISTANCE, 30);
    }

    public static int screamDistance() {
        return safeInt(SCREAM_DISTANCE, 60);
    }

    public static int actionDistance() {
        return safeInt(ACTION_DISTANCE, 15);
    }

    public static int chuchotDistance() {
        return safeInt(CHUCHOT_DISTANCE, 2);
    }

    public static int defaultNarrationDistance() {
        return safeInt(DEFAULT_NARRATION_DISTANCE, 100);
    }

    public static int minNarrationDistance() {
        return safeInt(MIN_NARRATION_DISTANCE, 10);
    }

    public static int maxNarrationDistance() {
        return safeInt(MAX_NARRATION_DISTANCE, 1000);
    }

    public static int autoSaveIntervalMinutes() {
        return safeInt(AUTO_SAVE_INTERVAL_MINUTES, 30);
    }

    public static boolean hideDeathMessages() {
        try {
            return HIDE_DEATH_MESSAGES.get();
        } catch (Exception e) {
            return true;
        }
    }
}
