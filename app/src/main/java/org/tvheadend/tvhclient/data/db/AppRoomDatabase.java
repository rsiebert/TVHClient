package org.tvheadend.tvhclient.data.db;

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
                ServerStatus.class
        },
        exportSchema = false,
        version = 8)
public abstract class AppRoomDatabase extends RoomDatabase {

    private static AppRoomDatabase instance;

    public static AppRoomDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppRoomDatabase.class) {
                instance = Room.databaseBuilder(context, AppRoomDatabase.class, "tvhclient")
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .addMigrations(MIGRATION_3_4)
                        .addMigrations(MIGRATION_4_5)
                        .addMigrations(MIGRATION_5_6)
                        .addMigrations(MIGRATION_6_7)
                        .addMigrations(MIGRATION_7_8)
                        .build();
            }
        }
        return instance;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE connections ADD COLUMN last_update INTEGER NOT NULL DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE connections ADD COLUMN sync_required INTEGER NOT NULL DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE channels ADD COLUMN display_number INTEGER NOT NULL DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE recordings ADD COLUMN duplicate INTEGER NOT NULL DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE server_status ADD COLUMN http_playback_server_profile_id INTEGER NOT NULL DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE recordings ADD COLUMN image TEXT;");
            database.execSQL("ALTER TABLE recordings ADD COLUMN fanart_image TEXT;");
            database.execSQL("ALTER TABLE recordings ADD COLUMN copyright_year INTEGER NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE recordings ADD COLUMN removal INTEGER NOT NULL DEFAULT 0;");

            database.execSQL("ALTER TABLE timer_recordings ADD COLUMN removal INTEGER NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE series_recordings ADD COLUMN removal INTEGER NOT NULL DEFAULT 0;");

            database.execSQL("ALTER TABLE series_recordings ADD COLUMN max_count INTEGER NOT NULL DEFAULT 0;");

            database.execSQL("ALTER TABLE programs ADD COLUMN credits TEXT;");
            database.execSQL("ALTER TABLE programs ADD COLUMN category TEXT;");
            database.execSQL("ALTER TABLE programs ADD COLUMN keyword TEXT;");
            database.execSQL("ALTER TABLE programs ADD COLUMN series_link_uri TEXT;");
            database.execSQL("ALTER TABLE programs ADD COLUMN episode_uri TEXT;");
            database.execSQL("ALTER TABLE programs ADD COLUMN copyright_year INTEGER NOT NULL DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE channel_tags ADD COLUMN is_selected INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public abstract TimerRecordingDao getTimerRecordingDao();

    public abstract SeriesRecordingDao getSeriesRecordingDao();

    public abstract RecordingDao getRecordingDao();

    public abstract ChannelDao getChannelDao();

    public abstract ChannelTagDao getChannelTagDao();

    public abstract TagAndChannelDao getTagAndChannelDao();

    public abstract ProgramDao getProgramDao();

    public abstract ConnectionDao getConnectionDao();

    public abstract ServerProfileDao getServerProfileDao();

    public abstract ServerStatusDao getServerStatusDao();

}
