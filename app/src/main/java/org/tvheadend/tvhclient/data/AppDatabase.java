package org.tvheadend.tvhclient.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.dao.ChannelDao;
import org.tvheadend.tvhclient.data.dao.ChannelTagDao;
import org.tvheadend.tvhclient.data.dao.ConnectionDao;
import org.tvheadend.tvhclient.data.dao.ProgramDao;
import org.tvheadend.tvhclient.data.dao.RecordingDao;
import org.tvheadend.tvhclient.data.dao.SeriesRecordingDao;
import org.tvheadend.tvhclient.data.dao.ServerProfileDao;
import org.tvheadend.tvhclient.data.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.dao.TagAndChannelDao;
import org.tvheadend.tvhclient.data.dao.TimerRecordingDao;
import org.tvheadend.tvhclient.data.dao.TranscodingProfileDao;
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
        version = 2)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                instance = Room.databaseBuilder(context, AppDatabase.class, "tvhclient")
                        .addMigrations(MIGRATION_1_2)
                        .build();
            }
        }
        return instance;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE programs ADD COLUMN channel_name TEXT NULL;");
            database.execSQL("ALTER TABLE programs ADD COLUMN channel_icon TEXT NULL;");
            database.execSQL("ALTER TABLE programs ADD COLUMN recording_title TEXT NULL;");
            database.execSQL("ALTER TABLE programs ADD COLUMN recording_state TEXT NULL;");
            database.execSQL("ALTER TABLE programs ADD COLUMN recording_error TEXT NULL;");
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
