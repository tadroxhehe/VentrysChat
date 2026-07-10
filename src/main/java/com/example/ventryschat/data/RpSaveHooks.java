package com.example.ventryschat.data;

import com.example.ventryschat.AptitudesManager;
import com.example.ventryschat.RPDataManager;

/**
 * Implémentation nommée des hooks de sauvegarde RP (évite les classes internes anonymes dans le JAR reobfusqué).
 */
public final class RpSaveHooks implements RPAutoSaveScheduler.SaveHooks {

    public static final RpSaveHooks INSTANCE = new RpSaveHooks();

    private RpSaveHooks() {
    }

    @Override
    public boolean saveData() {
        return RPDataManager.saveData();
    }

    @Override
    public void saveDataAndAptitudes() {
        RPDataManager.saveData();
        AptitudesManager.saveData();
    }

    @Override
    public boolean hasUnsavedChanges() {
        return RPDataManager.hasUnsavedChangesFlag();
    }

    @Override
    public void clearUnsavedChanges() {
        RPDataManager.clearUnsavedChangesFlag();
    }

    @Override
    public boolean hasPlayerData() {
        return RPDataManager.hasAnyPlayerData();
    }

    @Override
    public long getLastSuccessfulSaveMs() {
        return RPDataManager.getLastSuccessfulSaveMs();
    }
}
