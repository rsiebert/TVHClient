package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ProgramData implements DataSourceInterface<Program> {

    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 3;
    private AppRoomDatabase db;

    @Inject
    public ProgramData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(Program item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    @Override
    public void addItems(List<Program> items) {
        for (Program program : items) {
            addItem(program);
        }
    }

    @Override
    public void updateItem(Program item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void updateItems(List<Program> items) {
        for (Program program : items) {
            updateItem(program);
        }
    }

    @Override
    public void removeItem(Program item) {
        new ItemHandlerTask(db, item, DELETE).execute();
    }

    @Override
    public void removeItems(List<Program> items) {
        for (Program program : items) {
            removeItem(program);
        }
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
            return new ItemLoaderTask(db, (int) id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static class ItemLoaderTask extends AsyncTask<Void, Void, Program> {
        private final AppRoomDatabase db;
        private final int id;

        ItemLoaderTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected Program doInBackground(Void... voids) {
            return db.getProgramDao().loadProgramByIdSync(id);
        }
    }

    protected static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final Program program;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, Program program, int type) {
            this.db = db;
            this.program = program;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
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
}
