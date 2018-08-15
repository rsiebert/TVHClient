package org.tvheadend.tvhclient.injection.modules;

import android.content.Context;

import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncHandler;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = RepositoryModule.class)
public class EpgSyncHandlerModule {

    @Singleton
    @Provides
    public EpgSyncHandler provideEpgSyncHandlerModule(Context context, AppRepository appRepository) {
        return new EpgSyncHandler(context, appRepository);
    }
}
