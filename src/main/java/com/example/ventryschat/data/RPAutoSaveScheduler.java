package com.example.ventryschat.data;

import com.example.ventryschat.config.VentrysChatConfig;
import com.example.ventryschat.util.ChatLog;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sauvegarde automatique périodique et flush disque coalescé / throttlé (aucune règle métier).
 */
public final class RPAutoSaveScheduler {

    private static final long MIN_SAVE_INTERVAL_MS = 15_000L;

    private static volatile boolean autoSaveEnabled = false;
    private static Thread autoSaveThread;

    private static final AtomicBoolean COALESCED_DISK_FLUSH_PENDING = new AtomicBoolean(false);
    private static final AtomicBoolean THROTTLED_SAVE_RUNNABLE_QUEUED = new AtomicBoolean(false);

    // Executor d'I/O dedie : les flush disque ne bloquent plus le thread serveur principal.
    // saveData() est synchronized et playerData est un ConcurrentHashMap (deja lu hors-thread par
    // le thread d'autosave), donc l'ecriture en arriere-plan est sure.
    // Recree apres chaque stop() : shutdown() rend l'executor inutilisable.
    private static volatile java.util.concurrent.ExecutorService diskIoExecutor = newDiskIoExecutor();

    private static java.util.concurrent.ExecutorService newDiskIoExecutor() {
        return java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "VentrysChat-RPDiskFlush");
            t.setDaemon(true);
            return t;
        });
    }

    private static java.util.concurrent.ExecutorService ensureDiskIoExecutor() {
        java.util.concurrent.ExecutorService executor = diskIoExecutor;
        if (executor == null || executor.isShutdown()) {
            synchronized (RPAutoSaveScheduler.class) {
                executor = diskIoExecutor;
                if (executor == null || executor.isShutdown()) {
                    diskIoExecutor = executor = newDiskIoExecutor();
                }
            }
        }
        return executor;
    }

    private static void executeDiskIo(Runnable task, Runnable fallbackOnRejection) {
        try {
            ensureDiskIoExecutor().execute(task);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            fallbackOnRejection.run();
        }
    }

    public interface SaveHooks {
        boolean saveData();

        void saveDataAndAptitudes();

        boolean hasUnsavedChanges();

        void clearUnsavedChanges();

        boolean hasPlayerData();

        long getLastSuccessfulSaveMs();
    }

    private RPAutoSaveScheduler() {
    }

    public static boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    public static void scheduleCoalescedDiskFlush(MinecraftServer server, SaveHooks hooks, Logger logger) {
        if (server == null) {
            return;
        }
        if (!COALESCED_DISK_FLUSH_PENDING.compareAndSet(false, true)) {
            return;
        }
        Runnable flushTask = () -> {
            try {
                hooks.saveDataAndAptitudes();
            } finally {
                COALESCED_DISK_FLUSH_PENDING.set(false);
            }
        };
        executeDiskIo(flushTask, flushTask);
    }

    public static void scheduleThrottledDiskSaveIfNeeded(MinecraftServer server, SaveHooks hooks, Logger logger) {
        if (server == null || !hooks.hasUnsavedChanges()) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - hooks.getLastSuccessfulSaveMs()) < MIN_SAVE_INTERVAL_MS) {
            return;
        }
        if (!THROTTLED_SAVE_RUNNABLE_QUEUED.compareAndSet(false, true)) {
            return;
        }
        Runnable saveTask = () -> {
            try {
                if (shouldPerformImmediateSave(hooks)) {
                    if (hooks.saveData()) {
                        hooks.clearUnsavedChanges();
                        logger.debug("Sauvegarde disque throttlée (coalescée) OK");
                    }
                }
            } finally {
                THROTTLED_SAVE_RUNNABLE_QUEUED.set(false);
            }
        };
        executeDiskIo(saveTask, saveTask);
    }

    public static void start(SaveHooks hooks, Logger logger) {
        if (autoSaveEnabled) {
            ChatLog.diagnose(logger, "Sauvegarde automatique déjà active");
            return;
        }

        ensureDiskIoExecutor();
        autoSaveEnabled = true;
        autoSaveThread = new Thread(() -> {
            ChatLog.detail(logger, "Sauvegarde automatique démarrée (intervalle: {} min)",
                VentrysChatConfig.autoSaveIntervalMinutes());

            while (autoSaveEnabled && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(VentrysChatConfig.autoSaveIntervalMinutes() * 60 * 1000L);

                    if (autoSaveEnabled && hooks.hasPlayerData() && hooks.hasUnsavedChanges()) {
                        logger.debug("Sauvegarde automatique (changements détectés)...");
                        if (hooks.saveData()) {
                            hooks.clearUnsavedChanges();
                            logger.debug("Sauvegarde automatique OK");
                        } else {
                            logger.warn("Sauvegarde automatique échouée, les changements restent non sauvegardés");
                        }
                    } else if (autoSaveEnabled && !hooks.hasUnsavedChanges()) {
                        logger.debug("Sauvegarde automatique ignorée (aucun changement depuis la dernière sauvegarde)");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Erreur lors de la sauvegarde automatique : {}", e.getMessage());
                }
            }

            ChatLog.diagnose(logger, "Sauvegarde automatique arrêtée");
        }, "RPDataManager-AutoSave");

        autoSaveThread.setDaemon(true);
        autoSaveThread.start();
    }

    public static void stop(Logger logger) {
        autoSaveEnabled = false;
        if (autoSaveThread != null && autoSaveThread.isAlive()) {
            autoSaveThread.interrupt();
        }
        java.util.concurrent.ExecutorService executor;
        synchronized (RPAutoSaveScheduler.class) {
            executor = diskIoExecutor;
            diskIoExecutor = null;
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        ChatLog.diagnose(logger, "Arrêt de la sauvegarde automatique");
    }

    private static boolean shouldPerformImmediateSave(SaveHooks hooks) {
        if (!hooks.hasUnsavedChanges()) {
            return false;
        }
        long now = System.currentTimeMillis();
        return (now - hooks.getLastSuccessfulSaveMs()) >= MIN_SAVE_INTERVAL_MS;
    }
}
