package com.example.ventryschat.util;

import com.example.ventryschat.config.VentrysChatConfig;
import org.apache.logging.log4j.Logger;

/**
 * Logs VentrysChat : INFO minimal en prod, détail si {@code verbose_logging}.
 */
public final class ChatLog {

    private ChatLog() {
    }

    public static boolean verboseLogging() {
        try {
            return VentrysChatConfig.VERBOSE_LOGGING.get();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean dumpPlayersOnStartup() {
        try {
            return VentrysChatConfig.DUMP_PLAYERS_ON_STARTUP.get();
        } catch (Exception e) {
            return false;
        }
    }

    /** Une ligne INFO utile en prod (démarrage, sauvegarde OK). */
    public static void startup(Logger log, String message, Object... args) {
        log.info(message, args);
    }

    public static void startup(org.slf4j.Logger log, String message, Object... args) {
        log.info(message, args);
    }

    /** Diagnostic technique — toujours DEBUG. */
    public static void diagnose(Logger log, String message, Object... args) {
        log.debug(message, args);
    }

    /** Détail opérationnel : INFO si verbose, sinon DEBUG. */
    public static void detail(Logger log, String message, Object... args) {
        if (verboseLogging()) {
            log.info(message, args);
        } else {
            log.debug(message, args);
        }
    }

    public static void detail(org.slf4j.Logger log, String message, Object... args) {
        if (verboseLogging()) {
            log.info(message, args);
        } else {
            log.debug(message, args);
        }
    }
}
