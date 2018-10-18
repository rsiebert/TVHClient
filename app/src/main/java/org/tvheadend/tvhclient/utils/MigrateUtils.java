package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.text.TextUtils;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.db.DatabaseHelperForMigration;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class MigrateUtils {
    private static final int VERSION_101 = 101;
    private static final int VERSION_109 = 109;
    private static final int VERSION_116 = 116;
    @Inject
    protected Context context;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    public void doMigrate() {
        MainApplication.getComponent().inject(this);

        // Lookup the current version and the last migrated version
        int currentApplicationVersion = BuildConfig.BUILD_VERSION;
        int lastInstalledApplicationVersion = sharedPreferences.getInt("build_version_for_migration", 0);
        Timber.i("Migrating from " + lastInstalledApplicationVersion + " to " + currentApplicationVersion);

        if (currentApplicationVersion != lastInstalledApplicationVersion) {
            if (lastInstalledApplicationVersion < VERSION_101) {
                migrateConvertStartScreenPreference();
                migrateConnectionsFromDatabase();
                migratePreferences();
            }
            if (lastInstalledApplicationVersion < VERSION_109) {
                checkServerStatus();
            }
            if (lastInstalledApplicationVersion < VERSION_116) {
                // Convert the previous boolean channel icon action to the corresponding list entry
                SharedPreferences.Editor editor = sharedPreferences.edit();
                boolean enabled = sharedPreferences.getBoolean("channel_icon_starts_playback_enabled", true);
                editor.putString("channel_icon_action", enabled ? "2" : "0");
                editor.apply();
            }
        }

        // Store the current version as the last installed version
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("build_version_for_migration", currentApplicationVersion);
        editor.apply();
    }

    private void checkServerStatus() {
        Timber.d("Checking if a server status entry exists for each connection");
        List<Connection> connections = appRepository.getConnectionData().getItems();
        for (Connection connection : connections) {
            Timber.d("Checking connection " + connection.getName() + " with id " + connection.getId());
            ServerStatus serverStatus = appRepository.getServerStatusData().getItemById(connection.getId());
            if (serverStatus != null) {
                Timber.d("Server status exists for connection " + connection.getId() + ", assigned connection id is " + serverStatus.getConnectionId());
            } else {
                Timber.d("No server status exists for connection " + connection.getId() + ", creating new one");
                serverStatus = new ServerStatus();
                serverStatus.setConnectionId(connection.getId());
            }
        }
    }

    private void migrateConnectionsFromDatabase() {
        Timber.d("Migrating existing connections to the new room database");
        SQLiteDatabase db = DatabaseHelperForMigration.getInstance(context).getReadableDatabase();

        List<Connection> connectionList = new ArrayList<>();

        // Save the connection credentials in a file and drop the database
        try {
            Timber.d("Database is readable " + db.isOpen());
            Cursor c = db.rawQuery("SELECT * FROM connections", null);
            while (c.moveToNext()) {
                Connection connection = new Connection();
                connection.setId(c.getInt(c.getColumnIndex("_id")));
                connection.setName(c.getString(c.getColumnIndex("name")));
                connection.setHostname(c.getString(c.getColumnIndex("address")));
                connection.setPort(c.getInt(c.getColumnIndex("port")));
                connection.setUsername(c.getString(c.getColumnIndex("username")));
                connection.setPassword(c.getString(c.getColumnIndex("password")));
                connection.setActive((c.getInt(c.getColumnIndex("selected")) > 0));
                connection.setStreamingPort(c.getInt(c.getColumnIndex("streaming_port")));
                connection.setWolMacAddress(c.getString(c.getColumnIndex("wol_address")));
                connection.setWolPort(c.getInt(c.getColumnIndex("wol_port")));
                connection.setWolUseBroadcast((c.getInt(c.getColumnIndex("wol_broadcast")) > 0));
                connection.setWolEnabled((!TextUtils.isEmpty(connection.getWolMacAddress())));
                connection.setLastUpdate(0);
                connection.setSyncRequired(true);

                Timber.d("Saving existing connection " + connection.getName() + " into temporary list");
                connectionList.add(connection);
            }
            c.close();
        } catch (SQLiteException ex) {
            Timber.d("Error getting connection information from cursor, " + ex.getLocalizedMessage());
        }
        db.close();

        // delete the entire database so we can restart with version one in room
        Timber.d("Deleting old database");
        context.deleteDatabase("tvhclient");

        Timber.d("Adding existing connections to the new room database");
        for (Connection connection : connectionList) {
            appRepository.getConnectionData().addItem(connection);
        }
    }

    private void migrateConvertStartScreenPreference() {
        // migrate preferences from old names to new
        // names to have a consistent naming scheme afterwards
        try {
            int value = Integer.valueOf(sharedPreferences.getString("defaultMenuPositionPref", "0"));
            if (value > 8) {
                // If the value is anything above the status screen
                // menu entry, default to the channel screen
                value = 0;
            } else if (value == 7) {
                // The program guide screen moved from position 7 to 1
                value = 1;
            } else if (value != 0) {
                // If any screens except the channel and program guide
                // were set increase the value by one because the
                // program guide moved to position 2
                value++;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("start_screen", String.valueOf(value));
            editor.remove("defaultMenuPositionPref");
            editor.apply();
        } catch (NumberFormatException e) {
            // NOP
        }
    }

    /**
     * Renames the names of the old preferences to the new naming scheme
     */
    private void migratePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Advanced settings preferences
        editor.putString("connection_timeout", sharedPreferences.getString("connectionTimeout", "5"));
        editor.putBoolean("debug_mode_enabled", sharedPreferences.getBoolean("pref_debug_mode", false));
        editor.putBoolean("send_debug_logfile_enabled", sharedPreferences.getBoolean("pref_send_logfile", false));

        // UI preferences
        editor.putBoolean("light_theme_enabled", sharedPreferences.getBoolean("lightThemePref", true));
        editor.putBoolean("localized_date_time_format_enabled", sharedPreferences.getBoolean("useLocalizedDateTimeFormatPref", false));
        editor.putString("channel_sort_order", sharedPreferences.getString("sortChannelsPref", "0"));
        editor.putBoolean("channel_name_enabled", sharedPreferences.getBoolean("showChannelNamePref", true));
        editor.putBoolean("program_progressbar_enabled", sharedPreferences.getBoolean("showProgramProgressbarPref", true));
        editor.putBoolean("program_subtitle_enabled", sharedPreferences.getBoolean("showProgramSubtitlePref", true));
        editor.putBoolean("next_program_title_enabled", sharedPreferences.getBoolean("showNextProgramPref", true));
        editor.putBoolean("genre_colors_for_channels_enabled", sharedPreferences.getBoolean("showGenreColorsChannelsPref", false));
        editor.putBoolean("genre_colors_for_programs_enabled", sharedPreferences.getBoolean("showGenreColorsProgramsPref", false));
        editor.putBoolean("genre_colors_for_program_guide_enabled", sharedPreferences.getBoolean("showGenreColorsGuidePref", false));
        editor.putInt("genre_color_transparency", sharedPreferences.getInt("showGenreColorsVisibility", 70));
        editor.putBoolean("channel_icon_starts_playback_enabled", sharedPreferences.getBoolean("playWhenChannelIconSelectedPref", true));
        editor.putString("hours_of_epg_data_per_screen", sharedPreferences.getString("epgHoursVisible", "4"));
        editor.putString("days_of_epg_data", sharedPreferences.getString("epgMaxDays", "7"));
        editor.putBoolean("delete_all_recordings_menu_enabled", sharedPreferences.getBoolean("hideMenuDeleteAllRecordingsPref", false));
        editor.putBoolean("channel_tag_menu_enabled", sharedPreferences.getBoolean("visibleMenuIconTagsPref", true));

        // Casting preferences
        editor.putBoolean("casting_minicontroller_enabled", sharedPreferences.getBoolean("pref_show_cast_minicontroller", true));

        // Notification preferences
        editor.putBoolean("notifications_enabled", sharedPreferences.getBoolean("pref_show_notifications", false));
        editor.putString("notification_lead_time", sharedPreferences.getString("pref_show_notification_offset", "5"));

        // Main preferences
        editor.putString("download_directory", sharedPreferences.getString("pref_download_directory", Environment.DIRECTORY_DOWNLOADS));

        editor.apply();
    }
}
