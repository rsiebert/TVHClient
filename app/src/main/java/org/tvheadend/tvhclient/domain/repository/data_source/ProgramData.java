package org.tvheadend.tvhclient.domain.repository.data_source;

import androidx.lifecycle.LiveData;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.domain.entity.EpgProgram;
import org.tvheadend.tvhclient.domain.entity.Program;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public class ProgramData implements DataSourceInterface<Program> {

    private static final int LOAD_LAST_IN_CHANNEL = 1;
    private static final int LOAD_BY_ID = 2;

    private final AppRoomDatabase db;

    public ProgramData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(Program item) {
        AsyncTask.execute(() -> db.getProgramDao().insert(item));
    }

    public void addItems(@NonNull List<Program> items) {
        AsyncTask.execute(() -> db.getProgramDao().insert(new ArrayList<>(items)));
    }

    @Override
    public void updateItem(Program item) {
        AsyncTask.execute(() -> db.getProgramDao().update(item));
    }

    @Override
    public void removeItem(Program item) {
        AsyncTask.execute(() -> db.getProgramDao().delete(item));
    }

    public void removeItemsByTime(long time) {
        AsyncTask.execute(() -> db.getProgramDao().deleteProgramsByTime(time));
    }

    public void removeItemById(int id) {
        AsyncTask.execute(() -> db.getProgramDao().deleteById(id));
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return db.getProgramDao().getItemCount();
    }

    @Override
    public LiveData<List<Program>> getLiveDataItems() {
        return db.getProgramDao().loadPrograms();
    }

    @Override
    public LiveData<Program> getLiveDataItemById(@NonNull Object id) {
        return db.getProgramDao().loadProgramById((int) id);
    }

    @Override
    @Nullable
    public Program getItemById(@NonNull Object id) {
        try {
            return new ProgramByIdTask(db, (int) id, LOAD_BY_ID).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading program by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading program by id task aborted", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<Program> getItems() {
        List<Program> programs = new ArrayList<>();
        try {
            programs.addAll(new ProgramListTask(db).execute().get());
        } catch (InterruptedException e) {
            Timber.d("Loading all programs task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading all programs task aborted", e);
        }
        return programs;
    }

    public LiveData<List<Program>> getLiveDataItemsFromTime(long time) {
        return db.getProgramDao().loadProgramsFromTime(time);
    }

    public LiveData<List<Program>> getLiveDataItemByChannelIdAndTime(int channelId, long time) {
        return db.getProgramDao().loadProgramsFromChannelFromTime(channelId, time);
    }

    @NonNull
    public List<EpgProgram> getItemByChannelIdAndBetweenTime(int channelId, long startTime, long endTime) {
        List<EpgProgram> programs = new ArrayList<>();
        try {
            programs.addAll(new EpgProgramByChannelAndTimeTask(db, channelId, startTime, endTime).execute().get());
        } catch (InterruptedException e) {
            Timber.d("Loading programs by channel and time task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading programs by channel and time task aborted", e);
        }
        return programs;
    }

    @Nullable
    public Program getLastItemByChannelId(int channelId) {
        try {
            return new ProgramByIdTask(db, channelId, LOAD_LAST_IN_CHANNEL).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading last programs in channel task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading last program in channel task aborted", e);
        }
        return null;
    }

    public int getItemCount() {
        try {
            return new ProgramCountTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading program count task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading program count task aborted", e);
        }
        return 0;
    }

    private static class ProgramByIdTask extends AsyncTask<Void, Void, Program> {
        private final AppRoomDatabase db;
        private final int id;
        private final int type;

        ProgramByIdTask(AppRoomDatabase db, int id, int type) {
            this.db = db;
            this.id = id;
            this.type = type;
        }

        @Override
        protected Program doInBackground(Void... voids) {
            switch (type) {
                case LOAD_LAST_IN_CHANNEL:
                    return db.getProgramDao().loadLastProgramFromChannelSync(id);
                case LOAD_BY_ID:
                    return db.getProgramDao().loadProgramByIdSync(id);
            }
            return null;
        }
    }

    private static class EpgProgramByChannelAndTimeTask extends AsyncTask<Void, Void, List<EpgProgram>> {
        private final AppRoomDatabase db;
        private final int channelId;
        private final long startTime;
        private final long endTime;

        EpgProgramByChannelAndTimeTask(AppRoomDatabase db, int channelId, long startTime, long endTime) {
            this.db = db;
            this.channelId = channelId;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        protected List<EpgProgram> doInBackground(Void... voids) {
            return db.getProgramDao().loadProgramsFromChannelBetweenTimeSync(channelId, startTime, endTime);
        }
    }

    private static class ProgramListTask extends AsyncTask<Void, Void, List<Program>> {
        private final AppRoomDatabase db;

        ProgramListTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected List<Program> doInBackground(Void... voids) {
            return db.getProgramDao().loadProgramsSync();
        }
    }

    private static class ProgramCountTask extends AsyncTask<Void, Void, Integer> {
        private final AppRoomDatabase db;

        ProgramCountTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return db.getProgramDao().getItemCountSync();
        }
    }
}
