package org.tvheadend.tvhclient.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.database.Cursor;
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
        version = 10)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                instance = Room.databaseBuilder(context, AppDatabase.class, "tvhclient")
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .addMigrations(MIGRATION_3_4)
                        .addMigrations(MIGRATION_4_5)
                        .addMigrations(MIGRATION_5_6)
                        .addMigrations(MIGRATION_6_7)
                        .addMigrations(MIGRATION_7_8)
                        .addMigrations(MIGRATION_8_9)
                        .addMigrations(MIGRATION_9_10)
                        .fallbackToDestructiveMigration()
                        .build();
            }
        }
        return instance;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE connections ADD COLUMN channel_tag INT DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE connections ADD COLUMN streaming_port INT DEFAULT 9981;");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add the required columns for WOL
            database.execSQL("ALTER TABLE connections ADD COLUMN wol_address TEXT NULL;");
            database.execSQL("ALTER TABLE connections ADD COLUMN wol_port INT DEFAULT 9;");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE connections ADD COLUMN wol_broadcast INT DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add the id columns for the profiles.
            database.execSQL("ALTER TABLE connections ADD COLUMN playback_profile_id INT DEFAULT 0;");
            database.execSQL("ALTER TABLE connections ADD COLUMN recording_profile_id INT DEFAULT 0;");
            // Add the new profile table
            final String query = "CREATE TABLE IF NOT EXISTS profiles ("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "profile_enabled INT DEFAULT 0, "
                    + "profile_uuid TEXT NULL, "
                    + "container TEXT NULL, "
                    + "transcode INT DEFAULT 0, "
                    + "resolution TEXT NULL, "
                    + "acode_codec TEXT NULL, "
                    + "video_codec TEXT NULL, "
                    + "subtitle_codec TEXT NULL);";
            database.execSQL(query);
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE profiles ADD COLUMN profile_name TEXT NULL;");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add the id columns for the profiles but check if they exist
            Cursor cursor = database.query("SELECT * FROM connections");
            int colIndex = cursor.getColumnIndex("playback_profile_id");
            if (colIndex < 0) {
                database.execSQL("ALTER TABLE connections ADD COLUMN playback_profile_id INT DEFAULT 0;");
            }

            cursor = database.query("SELECT * FROM connections");
            colIndex = cursor.getColumnIndex("recording_profile_id");
            if (colIndex < 0) {
                database.execSQL("ALTER TABLE connections ADD COLUMN recording_profile_id INT DEFAULT 0;");
            }

            // Add the new profile table
            final String query = "CREATE TABLE IF NOT EXISTS profiles ("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "profile_enabled INT DEFAULT 0, "
                    + "profile_uuid TEXT NULL, "
                    + "profile_name TEXT NULL, "
                    + "container TEXT NULL, "
                    + "transcode INT DEFAULT 0, "
                    + "resolution TEXT NULL, "
                    + "acode_codec TEXT NULL, "
                    + "video_codec TEXT NULL, "
                    + "subtitle_codec TEXT NULL);";
            database.execSQL(query);
            cursor.close();
        }
    };

    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE connections ADD COLUMN cast_profile_id INT DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

            // Rename the previous connection table so that we can create the new connection table.
            // The data from the renamed one is then moved to the new connection table.
            database.execSQL("ALTER TABLE connections RENAME TO connections_old");

            database.execSQL("CREATE TABLE IF NOT EXISTS connections (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "hostname TEXT, " +
                    "port INTEGER NOT NULL, " +
                    "username TEXT, " +
                    "password TEXT, " +
                    "active INTEGER NOT NULL, " +
                    "streaming_port INTEGER NOT NULL, " +
                    "wol_hostname TEXT, " +
                    "wol_port INTEGER NOT NULL, " +
                    "wol_use_broadcast INTEGER NOT NULL)");

            database.execSQL("CREATE TABLE IF NOT EXISTS timer_recordings (" +
                    "id TEXT NOT NULL, " +
                    "title TEXT, " +
                    "directory TEXT, " +
                    "enabled INTEGER NOT NULL, " +
                    "name TEXT, " +
                    "config_name TEXT, " +
                    "channel_id INTEGER NOT NULL, " +
                    "days_of_week INTEGER NOT NULL, " +
                    "priority INTEGER NOT NULL, " +
                    "start INTEGER NOT NULL, " +
                    "stop INTEGER NOT NULL, " +
                    "retention INTEGER NOT NULL, " +
                    "owner TEXT, " +
                    "creator TEXT, " +
                    "channel_name TEXT, " +
                    "channel_icon TEXT, " +
                    "PRIMARY KEY(id))");

            database.execSQL("CREATE TABLE IF NOT EXISTS series_recordings (" +
                    "id TEXT NOT NULL, " +
                    "enabled INTEGER NOT NULL, " +
                    "name TEXT, " +
                    "min_duration INTEGER NOT NULL, " +
                    "max_duration INTEGER NOT NULL, " +
                    "retention INTEGER NOT NULL, " +
                    "days_of_week INTEGER NOT NULL, " +
                    "priority INTEGER NOT NULL, " +
                    "approx_time INTEGER NOT NULL, " +
                    "start INTEGER NOT NULL, " +
                    "start_window INTEGER NOT NULL, " +
                    "start_extra INTEGER NOT NULL, " +
                    "stop_extra INTEGER NOT NULL, " +
                    "title TEXT, " +
                    "fulltext INTEGER NOT NULL, " +
                    "directory TEXT, " +
                    "channel_id INTEGER NOT NULL, " +
                    "owner TEXT, " +
                    "creator TEXT, " +
                    "dup_detect INTEGER NOT NULL, " +
                    "channel_name TEXT, " +
                    "channel_icon TEXT, " +
                    "PRIMARY KEY(id))");

            database.execSQL("CREATE TABLE IF NOT EXISTS recordings (" +
                    "id INTEGER NOT NULL, " +
                    "channel_id INTEGER NOT NULL, " +
                    "start INTEGER NOT NULL, " +
                    "stop INTEGER NOT NULL, " +
                    "start_extra INTEGER NOT NULL, " +
                    "stop_extra INTEGER NOT NULL, " +
                    "retention INTEGER NOT NULL, " +
                    "priority INTEGER NOT NULL, " +
                    "event_id INTEGER NOT NULL, " +
                    "autorec_id TEXT, " +
                    "timerec_id TEXT, " +
                    "content_type INTEGER NOT NULL, " +
                    "title TEXT, " +
                    "subtitle TEXT, " +
                    "summary TEXT, " +
                    "description TEXT, " +
                    "state TEXT, " +
                    "error TEXT, " +
                    "owner TEXT, " +
                    "creator TEXT, " +
                    "subscription_error TEXT, " +
                    "stream_errors TEXT, " +
                    "data_errors TEXT, " +
                    "path TEXT, " +
                    "data_size INTEGER NOT NULL, " +
                    "enabled INTEGER NOT NULL, " +
                    "episode TEXT, " +
                    "comment TEXT, " +
                    "channel_name TEXT, " +
                    "channel_icon TEXT, " +
                    "PRIMARY KEY(id))");

            database.execSQL("CREATE TABLE IF NOT EXISTS programs (" +
                    "id INTEGER NOT NULL, " +
                    "channel_id INTEGER NOT NULL, " +
                    "start INTEGER NOT NULL, " +
                    "stop INTEGER NOT NULL, " +
                    "title TEXT, " +
                    "subtitle TEXT, " +
                    "summary TEXT, " +
                    "description TEXT, " +
                    "series_link_id INTEGER NOT NULL, " +
                    "episode_id INTEGER NOT NULL, " +
                    "season_id INTEGER NOT NULL, " +
                    "brand_id INTEGER NOT NULL, " +
                    "content_type INTEGER NOT NULL, " +
                    "age_rating INTEGER NOT NULL, " +
                    "star_rating INTEGER NOT NULL, " +
                    "first_aired INTEGER NOT NULL, " +
                    "season_number INTEGER NOT NULL, " +
                    "season_count INTEGER NOT NULL, " +
                    "episode_number INTEGER NOT NULL, " +
                    "episode_count INTEGER NOT NULL, " +
                    "part_number INTEGER NOT NULL, " +
                    "part_count INTEGER NOT NULL, " +
                    "episode_on_screen TEXT, " +
                    "image TEXT, " +
                    "dvr_id INTEGER NOT NULL, " +
                    "next_event_id INTEGER NOT NULL, " +
                    "PRIMARY KEY(id))");

            database.execSQL("CREATE TABLE IF NOT EXISTS channels (" +
                    "id INTEGER NOT NULL, " +
                    "channel_number INTEGER NOT NULL, " +
                    "channel_number_minor INTEGER NOT NULL, " +
                    "channel_name TEXT, " +
                    "channel_icon TEXT, " +
                    "event_id INTEGER NOT NULL, " +
                    "next_event_id INTEGER NOT NULL, " +
                    "program_id INTEGER NOT NULL, " +
                    "program_start INTEGER NOT NULL, " +
                    "program_stop INTEGER NOT NULL, " +
                    "program_title TEXT, " +
                    "program_subtitle TEXT, " +
                    "program_content_type INTEGER NOT NULL, " +
                    "next_program_title TEXT, " +
                    "recording_id INTEGER NOT NULL, " +
                    "recording_title TEXT, " +
                    "recording_state TEXT, " +
                    "recording_error TEXT, " +
                    "PRIMARY KEY(id))");

            database.execSQL("CREATE TABLE IF NOT EXISTS channel_tags (" +
                    "id INTEGER NOT NULL, " +
                    "tag_name TEXT, " +
                    "tag_index INTEGER NOT NULL, " +
                    "tag_icon TEXT, " +
                    "tag_titled_icon INTEGER NOT NULL, " +
                    "active INTEGER NOT NULL, " +
                    "PRIMARY KEY(id))");

            database.execSQL("CREATE TABLE IF NOT EXISTS tags_and_channels (" +
                    "tag_id INTEGER NOT NULL, " +
                    "channel_id INTEGER NOT NULL, " +
                    "PRIMARY KEY(tag_id, channel_id))");

            database.execSQL("CREATE TABLE IF NOT EXISTS server_profiles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "connection_id INTEGER NOT NULL, " +
                    "is_enabled INTEGER NOT NULL, " +
                    "name TEXT, " +
                    "uuid TEXT, " +
                    "comment TEXT, " +
                    "type TEXT)");

            database.execSQL("CREATE TABLE IF NOT EXISTS transcoding_profiles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "connection_id INTEGER NOT NULL, " +
                    "is_enabled INTEGER NOT NULL, " +
                    "container TEXT, " +
                    "transcode INTEGER NOT NULL, " +
                    "resolution TEXT, " +
                    "audio_codec TEXT, " +
                    "video_codec TEXT, " +
                    "subtitle_codec TEXT)");

            database.execSQL("CREATE TABLE IF NOT EXISTS server_status (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "connection_id INTEGER NOT NULL, " +
                    "connection_name TEXT, " +
                    "server_name TEXT, " +
                    "server_version TEXT, " +
                    "webroot TEXT, " +
                    "time INTEGER NOT NULL, " +
                    "timezone TEXT, " +
                    "gmt_offset INTEGER NOT NULL, " +
                    "htsp_version INTEGER NOT NULL, " +
                    "free_disc_space INTEGER NOT NULL, " +
                    "total_disc_space INTEGER NOT NULL, " +
                    "playback_server_profile_id INTEGER NOT NULL, " +
                    "recording_server_profile_id INTEGER NOT NULL, " +
                    "casting_server_profile_id INTEGER NOT NULL, " +
                    "playback_transcoding_profile_id INTEGER NOT NULL, " +
                    "recording_transcoding_profile_id INTEGER NOT NULL)");

            // Clone the connection data
            database.execSQL("INSERT INTO connections (" +
                    "id, name, hostname, port, username, password, active, streaming_port, wol_hostname, wol_port, wol_use_broadcast) " +
                    "SELECT _id, name, address, port, username, password, selected, streaming_port, wol_address, wol_port, wol_broadcast " +
                    "FROM connections_old");
            // Create a server status entry for each connection
            database.execSQL("INSERT INTO server_status (" +
                    "connection_id, " +
                    "connection_name, " +
                    "time, " +
                    "timezone, " +
                    "gmt_offset, " +
                    "htsp_version, " +
                    "free_disc_space, " +
                    "total_disc_space," +
                    "playback_server_profile_id, " +
                    "recording_server_profile_id, " +
                    "casting_server_profile_id, " +
                    "playback_transcoding_profile_id, " +
                    "recording_transcoding_profile_id) " +
                    "VALUES (" +
                    "(SELECT _id FROM connections_old), " +
                    "(SELECT name FROM connections_old), " +
                    "0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0)");
            // Delete the old connections table
            database.execSQL("DROP TABLE connections_old");
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
