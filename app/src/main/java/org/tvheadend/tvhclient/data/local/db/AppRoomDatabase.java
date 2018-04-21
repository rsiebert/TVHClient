package org.tvheadend.tvhclient.data.local.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.local.dao.ChannelDao;
import org.tvheadend.tvhclient.data.local.dao.ChannelTagDao;
import org.tvheadend.tvhclient.data.local.dao.ConnectionDao;
import org.tvheadend.tvhclient.data.local.dao.ProgramDao;
import org.tvheadend.tvhclient.data.local.dao.RecordingDao;
import org.tvheadend.tvhclient.data.local.dao.SeriesRecordingDao;
import org.tvheadend.tvhclient.data.local.dao.ServerProfileDao;
import org.tvheadend.tvhclient.data.local.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.local.dao.TagAndChannelDao;
import org.tvheadend.tvhclient.data.local.dao.TimerRecordingDao;
import org.tvheadend.tvhclient.data.local.dao.TranscodingProfileDao;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.entity.TagAndChannel;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.data.entity.TranscodingProfile;

@Database(
        entities = {
                TimerRecording.class,
                SeriesRecording.class,
                Recording.class,
                Program.class,
                Channel.class,
                ChannelTag.class,
                TagAndChannel.class,
                Connection.class,
                ServerProfile.class,
                TranscodingProfile.class,
                ServerStatus.class
        },
        version = 4)
public abstract class AppRoomDatabase extends RoomDatabase {

    private static AppRoomDatabase instance;

    public static AppRoomDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppRoomDatabase.class) {
                instance = Room.databaseBuilder(context, AppRoomDatabase.class, "tvhclient")
                        .fallbackToDestructiveMigration()
                        .build();
            }
        }
        return instance;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // NOP
        }
    };

    public abstract TimerRecordingDao timerRecordingDao();

    public abstract SeriesRecordingDao seriesRecordingDao();

    public abstract RecordingDao recordingDao();

    public abstract ChannelDao channelDao();

    public abstract ChannelTagDao channelTagDao();

    public abstract TagAndChannelDao tagAndChannelDao();

    public abstract ProgramDao programDao();

    public abstract ConnectionDao connectionDao();

    public abstract ServerProfileDao serverProfileDao();

    public abstract TranscodingProfileDao transcodingProfileDao();

    public abstract ServerStatusDao serverStatusDao();

}
