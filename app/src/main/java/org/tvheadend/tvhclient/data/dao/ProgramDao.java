package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.model.Program;

import java.util.List;

@Dao
public interface ProgramDao {

    @Query("SELECT * FROM programs")
    LiveData<List<Program>> loadAllPrograms();

    @Query("SELECT * FROM programs WHERE id = :id")
    LiveData<Program> loadProgram(int id);

    @Query("SELECT * FROM programs WHERE id = :id")
    Program loadProgramSync(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Program> programs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Program program);

    @Update
    void update(Program... programs);

    @Delete
    void delete(Program program);

    @Query("DELETE FROM programs")
    void deleteAll();
}
