package com.latec.syncthinglegacy;

import com.latec.syncthinglegacy.activities.FirstStartActivity;
import com.latec.syncthinglegacy.activities.FolderPickerActivity;
import com.latec.syncthinglegacy.activities.MainActivity;
import com.latec.syncthinglegacy.activities.SettingsActivity;
import com.latec.syncthinglegacy.activities.ShareActivity;
import com.latec.syncthinglegacy.activities.ThemedAppCompatActivity;
import com.latec.syncthinglegacy.receiver.AppConfigReceiver;
import com.latec.syncthinglegacy.service.RunConditionMonitor;
import com.latec.syncthinglegacy.service.EventProcessor;
import com.latec.syncthinglegacy.service.NotificationHandler;
import com.latec.syncthinglegacy.service.RestApi;
import com.latec.syncthinglegacy.service.SyncthingRunnable;
import com.latec.syncthinglegacy.service.SyncthingService;
import com.latec.syncthinglegacy.util.Languages;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {SyncthingModule.class})
public interface DaggerComponent {

    void inject(SyncthingApp app);
    void inject(MainActivity activity);
    void inject(FirstStartActivity activity);
    void inject(FolderPickerActivity activity);
    void inject(Languages languages);
    void inject(SyncthingService service);
    void inject(RunConditionMonitor runConditionMonitor);
    void inject(EventProcessor eventProcessor);
    void inject(SyncthingRunnable syncthingRunnable);
    void inject(NotificationHandler notificationHandler);
    void inject(AppConfigReceiver appConfigReceiver);
    void inject(RestApi restApi);
    void inject(SettingsActivity.SettingsFragment fragment);
    void inject(ShareActivity activity);
    void inject(ThemedAppCompatActivity activity);
}
