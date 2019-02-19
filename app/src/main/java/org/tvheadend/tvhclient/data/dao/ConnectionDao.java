package org.tvheadend.tvhclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.tvheadend.tvhclient.domain.entity.Connection;

import java.util.List;

@Dao
public interface ConnectionDao {

    @Query("SELECT * FROM connections")
    LiveData<List<Connection>> loadAllConnections();

    @Query("SELECT * FROM connections")
    List<Connection> loadAllConnectionsSync();

    @Query("SELECT * FROM connections WHERE active = 1")
    Connection loadActiveConnectionSync();

    @Query("SELECT * FROM connections WHERE id = :id")
    Connection loadConnectionByIdSync(int id);

    @Query("SELECT * FROM connections WHERE id = :id")
    LiveData<Connection> loadConnectionById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Connection connection);

    @Update
    void update(Connection... connection);

    @Delete
    void delete(Connection connection);

    @Query("UPDATE connections SET active = 0 WHERE active = 1")
    void disableActiveConnection();

    @Query("SELECT COUNT (*) FROM connections")
    LiveData<Integer> getConnectionCount();
}
