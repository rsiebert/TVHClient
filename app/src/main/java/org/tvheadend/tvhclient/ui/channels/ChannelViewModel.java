package org.tvheadend.tvhclient.ui.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ChannelDao;
import org.tvheadend.tvhclient.data.entity.Channel;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ChannelViewModel extends AndroidViewModel {

    private final LiveData<List<Channel>> channels;
    private final ChannelDao dao;
    private MutableLiveData<List<Channel>> channelsByTime = new MutableLiveData<>();
    private long showProgramsFromTime;

    public ChannelViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application.getApplicationContext());
        dao = db.channelDao();
        showProgramsFromTime = new Date().getTime();

        channels = dao.loadAllChannels();
        reloadChannels();
    }

    public LiveData<List<Channel>> getChannelsByTime() {
        return channelsByTime;
    }

    public LiveData<List<Channel>> getAllChannels() {
        return channels;
    }

    public void setTime(long time) {
        this.showProgramsFromTime = time;
        reloadChannels();
    }

    public long getTime() {
        return this.showProgramsFromTime;
    }

    private void reloadChannels() {
        try {
            List<Channel> newChannels = new ChannelLoadingTask(dao, showProgramsFromTime).execute().get();
            channelsByTime.postValue(newChannels);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static class ChannelLoadingTask extends AsyncTask<Void, Void, List<Channel>> {
        private final ChannelDao dao;
        private final long time;

        ChannelLoadingTask(ChannelDao dao, long time) {
            this.dao = dao;
            this.time = time;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            return dao.loadAllChannelsByTimeSync(time);
        }
    }
}
