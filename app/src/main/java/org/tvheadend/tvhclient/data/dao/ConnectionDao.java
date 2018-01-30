package org.tvheadend.tvhclient.data.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.Connection;

import java.util.List;

@Dao
public interface ConnectionDao {

    @Query("SELECT * FROM connections")
    List<Connection> loadAllConnectionsSync();

    @Query("SELECT * FROM connections WHERE active = 'true'")
    Connection loadActiveConnectionSync();

    @Query("SELECT * FROM connections WHERE id = :id")
    Connection loadConnectionSync(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Connection... connection);

    @Update
    void update(Connection... connection);

    @Delete
    void delete(Connection connection);

    @Query("DELETE FROM connections WHERE id = :id")
    void deleteById(int id);

    @Query("UPDATE connections SET active = '0' WHERE active = '1'")
    void disableActiveConnectionSync();
}
