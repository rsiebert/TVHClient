package org.tvheadend.tvhclient.data.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.TranscodingProfile;

@Dao
public interface TranscodingProfileDao {

    @Query("DELETE FROM transcoding_profiles WHERE connection_id = :id")
    void deleteByConnectionId(int id);

    @Insert
    long insert(TranscodingProfile transcodingProfile);

    @Update
    void update(TranscodingProfile... transcodingProfiles);

    @Delete
    void delete(TranscodingProfile transcodingProfile);

    @Query("SELECT * FROM transcoding_profiles WHERE id = :id")
    TranscodingProfile loadProfileByIdSync(int id);
}
