package com.latec.syncthinglegacy;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.latec.syncthinglegacy.service.NotificationHandler;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class SyncthingModule {

    private final SyncthingApp mApp;

    public SyncthingModule(SyncthingApp app) {
        mApp = app;
    }

    @Provides
    @Singleton
    public SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mApp);
    }

    @Provides
    @Singleton
    public NotificationHandler getNotificationHandler() {
        return new NotificationHandler(mApp);
    }
}
