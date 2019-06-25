package org.tvheadend.tvhclient.util

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteException
import android.os.Environment
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.db.DatabaseHelperForMigration
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import java.util.*

class MigrateUtils(val context: Context, val appRepository: AppRepository, val sharedPreferences: SharedPreferences) {

    fun doMigrate() {
        // Lookup the current version and the last migrated version
        val currentApplicationVersion = BuildConfig.BUILD_VERSION
        val lastInstalledApplicationVersion = sharedPreferences.getInt("build_version_for_migration", 0)

        if (currentApplicationVersion != lastInstalledApplicationVersion) {
            Timber.i("Migrating from $lastInstalledApplicationVersion to $currentApplicationVersion")

            if (lastInstalledApplicationVersion < VERSION_101) {
                migrateConvertStartScreenPreference()
                migrateConnectionsFromDatabase()
                migratePreferences()
            }
            if (lastInstalledApplicationVersion < VERSION_109) {
                checkServerStatus()
            }
            if (lastInstalledApplicationVersion < VERSION_116) {
                // Convert the previous boolean channel icon action to the corresponding list entry
                val editor = sharedPreferences.edit()
                val enabled = sharedPreferences.getBoolean("channel_icon_starts_playback_enabled", true)
                editor.putString("channel_icon_action", if (enabled) "2" else "0")
                editor.apply()
            }
            if (lastInstalledApplicationVersion < VERSION_120) {
                migratePlaybackProfiles()
            }
            if (lastInstalledApplicationVersion < VERSION_143) {
                migrateInterPlayerAndChannelOrderSettings()
            }
            if (lastInstalledApplicationVersion < VERSION_144) {
                // Set the sync required flag for every connection because in this version
                // the server defined channel order was introduced. This information needs
                // to be saved during the initial connection with the server where
                // channel and tags would not be saved.
                for (connection in appRepository.connectionData.getItems()) {
                    Timber.d("Setting sync required for connection ${connection.name} to save server defined channel order")
                    connection.isSyncRequired = true
                    appRepository.connectionData.updateItem(connection)
                }

                // Two new channel sorting options were introduced in this version.
                // They are now the first to options so all other values need to be moved up by two.
                val editor = sharedPreferences.edit()
                var channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", context.resources.getString(R.string.pref_default_channel_sort_order))!!)
                if (channelSortOrder >= 2) {
                    channelSortOrder += 2
                }
                editor.putString("channel_sort_order", channelSortOrder.toString())
                editor.apply()
            }
        }

        // Store the current version as the last installed version
        val editor = sharedPreferences.edit()
        editor.putInt("build_version_for_migration", currentApplicationVersion)
        editor.apply()
    }

    private fun migrateInterPlayerAndChannelOrderSettings() {
        // Convert the previous internal player settings to the
        // new one that differentiates between channels and recordings
        val editor = sharedPreferences.edit()
        val enabled = sharedPreferences.getBoolean("internal_player_enabled", context.resources.getBoolean(R.bool.pref_default_internal_player_enabled))
        editor.putBoolean("internal_player_for_channels_enabled", enabled)
        editor.putBoolean("internal_player_for_recordings_enabled", enabled)

        // The previous three channel sort options defined only an ascending order of
        // the channel id, name and number. Now convert the value because a descending
        // order has been added after the ascending one.

        var channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", context.resources.getString(R.string.pref_default_channel_sort_order))!!)
        channelSortOrder = channelSortOrder * 2 + 1
        editor.putString("channel_sort_order", channelSortOrder.toString())
        editor.apply()
    }

    private fun migratePlaybackProfiles() {
        for (connection in appRepository.connectionData.getItems()) {
            val serverStatus = appRepository.serverStatusData.getItemById(connection.id)
            if (serverStatus != null) {
                Timber.d("Clearing playback profile for connection ${connection.name}")
                // Clear the currently selected htsp playback profile
                serverStatus.htspPlaybackServerProfileId = 0
                serverStatus.httpPlaybackServerProfileId = 0
                serverStatus.castingServerProfileId = 0
                serverStatus.recordingServerProfileId = 0
                appRepository.serverStatusData.updateItem(serverStatus)
            }
        }
        // Remove all playback profiles so we can save them again separately as htsp or http profiles
        appRepository.serverProfileData.removeAll()
    }

    private fun checkServerStatus() {
        Timber.d("Checking if a server status entry exists for each connection")
        for ((id, name) in appRepository.connectionData.getItems()) {
            Timber.d("Checking connection $name with id $id")
            var serverStatus = appRepository.serverStatusData.getItemById(id)
            if (serverStatus != null) {
                Timber.d("Server status exists for connection $id, assigned connection id is ${serverStatus.connectionId}")
            } else {
                Timber.d("No server status exists for connection $id, creating new one")
                serverStatus = ServerStatus()
                serverStatus.connectionId = id
                appRepository.serverStatusData.addItem(serverStatus)
            }
        }
    }

    private fun migrateConnectionsFromDatabase() {
        Timber.d("Migrating existing connections to the new room database")
        val db = DatabaseHelperForMigration.getInstance(context)?.readableDatabase

        // Save the previous connection details in a list, then delete the old database and insert the connection details into the new one
        val connectionList = ArrayList<Connection>()
        try {
            Timber.d("Database is readable ${db?.isOpen}")
            val cursor = db?.rawQuery("SELECT * FROM connections", null)
            cursor?.let {
                if (it.count > 0) {
                    it.moveToFirst()
                    do {
                        val connection = Connection()
                        connection.name = it.getString(it.getColumnIndex("name"))
                        connection.hostname = it.getString(it.getColumnIndex("address"))
                        connection.port = it.getInt(it.getColumnIndex("port"))
                        connection.username = it.getString(it.getColumnIndex("username"))
                        connection.password = it.getString(it.getColumnIndex("password"))
                        connection.isActive = it.getInt(it.getColumnIndex("selected")) > 0
                        connection.streamingPort = it.getInt(it.getColumnIndex("streaming_port"))
                        connection.wolMacAddress = it.getString(it.getColumnIndex("wol_address"))
                        connection.wolPort = it.getInt(it.getColumnIndex("wol_port"))
                        connection.isWolUseBroadcast = it.getInt(it.getColumnIndex("wol_broadcast")) > 0
                        connection.isWolEnabled = !connection.wolMacAddress.isNullOrEmpty()
                        connection.lastUpdate = 0
                        connection.isSyncRequired = true

                        Timber.d("Saving existing connection ${connection.name} into temporary list")
                        connectionList.add(connection)
                    } while (it.moveToNext())
                }
            }
            cursor?.close()
        } catch (e: SQLiteException) {
            Timber.e(e, "Error getting connection information from cursor")
        }

        db?.close()

        // delete the entire database so we can restart with version one in room
        Timber.d("Deleting old database")
        context.deleteDatabase("tvhclient")

        Timber.d("Adding existing connections to the new room database")
        for (connection in connectionList) {
            appRepository.connectionData.addItem(connection)
        }
    }

    private fun migrateConvertStartScreenPreference() {
        // migrate preferences from old names to new
        // names to have a consistent naming scheme afterwards
        try {
            var value = Integer.valueOf(sharedPreferences.getString("defaultMenuPositionPref", "0")!!)
            when {
                // If the value is anything above the status screen
                // menu entry, default to the channel screen
                value > 8 -> value = 0
                // The program guide screen moved from position 7 to 1
                value == 7 -> value = 1
                // If any screens except the channel and program guide
                // were set increase the value by one because the
                // program guide moved to position 2
                value != 0 -> value++
            }

            val editor = sharedPreferences.edit()
            editor.putString("start_screen", value.toString())
            editor.remove("defaultMenuPositionPref")
            editor.apply()
        } catch (e: NumberFormatException) {
            // NOP
        }

    }

    /**
     * Renames the names of the old preferences to the new naming scheme
     */
    private fun migratePreferences() {
        val editor = sharedPreferences.edit()

        // Advanced settings preferences
        editor.putString("connection_timeout", sharedPreferences.getString("connectionTimeout", context.resources.getString(R.string.pref_default_connection_timeout)))
        editor.putBoolean("debug_mode_enabled", sharedPreferences.getBoolean("pref_debug_mode", context.resources.getBoolean(R.bool.pref_default_debug_mode_enabled)))

        // UI preferences
        editor.putBoolean("light_theme_enabled", sharedPreferences.getBoolean("lightThemePref", context.resources.getBoolean(R.bool.pref_default_light_theme_enabled)))
        editor.putBoolean("localized_date_time_format_enabled", sharedPreferences.getBoolean("useLocalizedDateTimeFormatPref", context.resources.getBoolean(R.bool.pref_default_localized_date_time_format_enabled)))
        editor.putString("channel_sort_order", sharedPreferences.getString("sortChannelsPref", context.resources.getString(R.string.pref_default_channel_sort_order)))
        editor.putBoolean("channel_name_enabled", sharedPreferences.getBoolean("showChannelNamePref", context.resources.getBoolean(R.bool.pref_default_channel_name_enabled)))
        editor.putBoolean("program_progressbar_enabled", sharedPreferences.getBoolean("showProgramProgressbarPref", context.resources.getBoolean(R.bool.pref_default_program_progressbar_enabled)))
        editor.putBoolean("program_subtitle_enabled", sharedPreferences.getBoolean("showProgramSubtitlePref", context.resources.getBoolean(R.bool.pref_default_program_subtitle_enabled)))
        editor.putBoolean("next_program_title_enabled", sharedPreferences.getBoolean("showNextProgramPref", context.resources.getBoolean(R.bool.pref_default_next_program_title_enabled)))
        editor.putBoolean("genre_colors_for_channels_enabled", sharedPreferences.getBoolean("showGenreColorsChannelsPref", context.resources.getBoolean(R.bool.pref_default_genre_colors_for_channels_enabled)))
        editor.putBoolean("genre_colors_for_programs_enabled", sharedPreferences.getBoolean("showGenreColorsProgramsPref", context.resources.getBoolean(R.bool.pref_default_genre_colors_for_programs_enabled)))
        editor.putBoolean("genre_colors_for_program_guide_enabled", sharedPreferences.getBoolean("showGenreColorsGuidePref", context.resources.getBoolean(R.bool.pref_default_genre_colors_for_program_guide_enabled)))
        editor.putInt("genre_color_transparency", sharedPreferences.getInt("showGenreColorsVisibility", Integer.valueOf(context.resources.getString(R.string.pref_default_genre_color_transparency))))
        editor.putString("hours_of_epg_data_per_screen", sharedPreferences.getString("epgHoursVisible", context.resources.getString(R.string.pref_default_channel_icon_action)))
        editor.putString("days_of_epg_data", sharedPreferences.getString("epgMaxDays", context.resources.getString(R.string.pref_default_days_of_epg_data)))
        editor.putBoolean("delete_all_recordings_menu_enabled", sharedPreferences.getBoolean("hideMenuDeleteAllRecordingsPref", context.resources.getBoolean(R.bool.pref_default_delete_all_recordings_menu_enabled)))
        editor.putBoolean("channel_tag_menu_enabled", sharedPreferences.getBoolean("visibleMenuIconTagsPref", context.resources.getBoolean(R.bool.pref_default_channel_tag_menu_enabled)))

        // Casting preferences
        editor.putBoolean("casting_minicontroller_enabled", sharedPreferences.getBoolean("pref_show_cast_minicontroller", context.resources.getBoolean(R.bool.pref_default_casting_minicontroller_enabled)))

        // Notification preferences
        editor.putBoolean("notifications_enabled", sharedPreferences.getBoolean("pref_show_notifications", context.resources.getBoolean(R.bool.pref_default_notifications_enabled)))
        editor.putString("notification_lead_time", sharedPreferences.getString("pref_show_notification_offset", context.resources.getString(R.string.pref_default_notification_lead_time)))

        // Main preferences
        editor.putString("download_directory", sharedPreferences.getString("pref_download_directory", Environment.DIRECTORY_DOWNLOADS))

        editor.apply()
    }

    companion object {
        private const val VERSION_101 = 101
        private const val VERSION_109 = 109
        private const val VERSION_116 = 116
        private const val VERSION_120 = 120
        private const val VERSION_143 = 143
        private const val VERSION_144 = 144
    }
}
