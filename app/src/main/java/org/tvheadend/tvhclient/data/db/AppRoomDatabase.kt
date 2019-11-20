package org.tvheadend.tvhclient.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.tvheadend.tvhclient.data.dao.*
import org.tvheadend.tvhclient.data.entity.*

@Database(
        entities = [TimerRecording::class,
            SeriesRecording::class,
            Recording::class,
            Program::class,
            Channel::class,
            ChannelTag::class,
            TagAndChannel::class,
            Connection::class,
            ServerProfile::class,
            ServerStatus::class],
        exportSchema = false,
        version = 12)
abstract class AppRoomDatabase : RoomDatabase() {

    abstract val timerRecordingDao: TimerRecordingDao

    abstract val seriesRecordingDao: SeriesRecordingDao

    abstract val recordingDao: RecordingDao

    abstract val channelDao: ChannelDao

    abstract val channelTagDao: ChannelTagDao

    abstract val tagAndChannelDao: TagAndChannelDao

    abstract val programDao: ProgramDao

    abstract val connectionDao: ConnectionDao

    abstract val serverProfileDao: ServerProfileDao

    abstract val serverStatusDao: ServerStatusDao

    companion object {

        private var instance: AppRoomDatabase? = null

        fun getInstance(context: Context): AppRoomDatabase? {
            if (instance == null) {
                synchronized(AppRoomDatabase::class.java) {
                    instance = Room.databaseBuilder(context, AppRoomDatabase::class.java, "tvhclient")
                            .addMigrations(MIGRATION_1_2)
                            .addMigrations(MIGRATION_2_3)
                            .addMigrations(MIGRATION_3_4)
                            .addMigrations(MIGRATION_4_5)
                            .addMigrations(MIGRATION_5_6)
                            .addMigrations(MIGRATION_6_7)
                            .addMigrations(MIGRATION_7_8)
                            .addMigrations(MIGRATION_8_9)
                            .addMigrations(MIGRATION_9_10)
                            .addMigrations(MIGRATION_10_11)
                            .addMigrations(MIGRATION_11_12)
                            .build()
                }
            }
            return instance
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN last_update INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN sync_required INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE channels ADD COLUMN display_number INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recordings ADD COLUMN duplicate INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE server_status ADD COLUMN http_playback_server_profile_id INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recordings ADD COLUMN image TEXT;")
                database.execSQL("ALTER TABLE recordings ADD COLUMN fanart_image TEXT;")
                database.execSQL("ALTER TABLE recordings ADD COLUMN copyright_year INTEGER NOT NULL DEFAULT 0;")
                database.execSQL("ALTER TABLE recordings ADD COLUMN removal INTEGER NOT NULL DEFAULT 0;")

                database.execSQL("ALTER TABLE timer_recordings ADD COLUMN removal INTEGER NOT NULL DEFAULT 0;")
                database.execSQL("ALTER TABLE series_recordings ADD COLUMN removal INTEGER NOT NULL DEFAULT 0;")

                database.execSQL("ALTER TABLE series_recordings ADD COLUMN max_count INTEGER NOT NULL DEFAULT 0;")

                database.execSQL("ALTER TABLE programs ADD COLUMN credits TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN category TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN keyword TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN series_link_uri TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN episode_uri TEXT;")
                database.execSQL("ALTER TABLE programs ADD COLUMN copyright_year INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE channel_tags ADD COLUMN is_selected INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX index_programs_start ON programs(start)")
                database.execSQL("CREATE INDEX index_programs_channel_id ON programs(channel_id)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE channels ADD COLUMN server_order INTEGER NOT NULL DEFAULT 0;")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN server_url TEXT;")
                database.execSQL("ALTER TABLE connections ADD COLUMN streaming_url TEXT;")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE programs ADD COLUMN modified_time INTEGER NOT NULL DEFAULT 0;")
            }
        }
    }
}
