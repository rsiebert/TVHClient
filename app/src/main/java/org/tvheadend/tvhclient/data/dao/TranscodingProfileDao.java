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

    @Query("SELECT p.* FROM transcoding_profiles AS p " +
            "LEFT JOIN server_status AS s ON s.playback_transcoding_profile_id = p.id " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1")
    TranscodingProfile loadPlaybackProfileSync();

    @Query("SELECT p.* FROM transcoding_profiles AS p " +
            "LEFT JOIN server_status AS s ON s.recording_transcoding_profile_id = p.id " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1")
    TranscodingProfile loadRecordingProfileSync();

    @Insert
    long insert(TranscodingProfile transcodingProfile);

    @Update
    void update(TranscodingProfile... transcodingProfiles);

    @Delete
    void delete(TranscodingProfile transcodingProfile);

    @Query("SELECT * FROM transcoding_profiles WHERE id = :id")
    TranscodingProfile loadProfileByIdSync(int id);
}
