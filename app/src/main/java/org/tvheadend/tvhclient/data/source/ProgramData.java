package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Program;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ProgramData extends BaseData implements DataSourceInterface<Program> {

    private AppRoomDatabase db;

    @Inject
    public ProgramData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(Program item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    public void addItems(List<Program> items) {
        new ItemsHandlerTask(db, items, INSERT_ALL).execute();
    }

    @Override
    public void updateItem(Program item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void removeItem(Program item) {
        new ItemHandlerTask(db, item, DELETE).execute();
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return db.getProgramDao().getProgramCount();
    }

    @Override
    public LiveData<List<Program>> getLiveDataItems() {
        return null;
    }

    @Override
    public LiveData<Program> getLiveDataItemById(Object id) {
        return db.getProgramDao().loadProgramById((int) id);
    }

    @Override
    public Program getItemById(Object id) {
        try {
            return new ItemLoaderTask(db, (int) id, LOAD_BY_ID).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @NonNull
    public List<Program> getItems() {
        return new ArrayList<>();
    }

    public LiveData<List<Program>> getLiveDataItemByChannelIdAndTime(int channelId, long time) {
        return db.getProgramDao().loadProgramsFromChannelFromTime(channelId, time);
    }

    public LiveData<List<Program>> getLiveDataItemByChannelIdAndBetweenTime(int channelId, long startTime, long endTime) {
        return db.getProgramDao().loadProgramsFromChannelBetweenTime(channelId, startTime, endTime);
    }

    public Program getLastItemByChannelId(int channelId) {
        try {
            return new ItemLoaderTask(db, channelId, LOAD_LAST_IN_CHANNEL).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeItemsByTime(long time) {
        new ItemMiscTask(db, DELETE_BY_TIME, time).execute();
    }

    public void removeItemById(int id) {
        new ItemMiscTask(db, DELETE_BY_ID, id).execute();
    }

    protected static class ItemLoaderTask extends AsyncTask<Void, Void, Program> {
        private final AppRoomDatabase db;
        private final int id;
        private final int type;

        ItemLoaderTask(AppRoomDatabase db, int id, int type) {
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

    protected static class ItemHandlerTask extends AsyncTask<Void, Void, Program> {
        private final AppRoomDatabase db;
        private final Program program;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, Program program, int type) {
            this.db = db;
            this.program = program;
            this.type = type;
        }

        @Override
        protected Program doInBackground(Void... voids) {
            switch (type) {
                case INSERT:
                    db.getProgramDao().insert(program);
                    break;
                case UPDATE:
                    db.getProgramDao().update(program);
                    break;
                case DELETE:
                    db.getProgramDao().delete(program);
                    break;
            }
            return null;
        }
    }

    protected static class ItemsHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final List<Program> programs;
        private final int type;

        ItemsHandlerTask(AppRoomDatabase db, List<Program> programs, int type) {
            this.db = db;
            this.programs = programs;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT_ALL:
                    db.getProgramDao().insert(programs);
                    break;
            }
            return null;
        }
    }

    protected static class ItemMiscTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final int type;
        private final Object arg;

        ItemMiscTask(AppRoomDatabase db, int type, Object arg) {
            this.db = db;
            this.type = type;
            this.arg = arg;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case DELETE_BY_TIME:
                    db.getProgramDao().deleteProgramsByTime((long) arg);
                    break;
                case DELETE_BY_ID:
                    db.getProgramDao().deleteById((int) arg);
                    break;
            }
            return null;
        }
    }
}
