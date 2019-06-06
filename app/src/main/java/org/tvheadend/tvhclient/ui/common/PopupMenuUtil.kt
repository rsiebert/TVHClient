package org.tvheadend.tvhclient.ui.common

import android.content.Context
import androidx.preference.PreferenceManager
import android.view.Menu
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ProgramInterface
import org.tvheadend.tvhclient.domain.entity.Recording
import timber.log.Timber


fun prepareMenu(context: Context,
                menu: Menu, program: ProgramInterface?, recording: Recording?,
                isNetworkAvailable: Boolean, htspVersion: Int, isUnlocked: Boolean) {

    // Hide the menus because the ones in the toolbar are not hidden when set in the xml
    for (i in 0 until menu.size()) {
        menu.getItem(i)?.isVisible = false
    }

    if (isNetworkAvailable) {
        if (recording == null || (!recording.isRecording
                        && !recording.isScheduled
                        && !recording.isCompleted
                        && !recording.isFailed
                        && !recording.isFileMissing
                        && !recording.isMissed
                        && !recording.isAborted)) {
            Timber.d("Recording is not recording or scheduled")
            menu.findItem(R.id.menu_record_once)?.isVisible = true
            menu.findItem(R.id.menu_record_once_and_edit)?.isVisible = isUnlocked
            menu.findItem(R.id.menu_record_once_custom_profile)?.isVisible = isUnlocked
            menu.findItem(R.id.menu_record_series)?.isVisible = htspVersion >= 13

        } else if (recording.isCompleted) {
            Timber.d("Recording is completed ")
            menu.findItem(R.id.menu_play)?.isVisible = true
            menu.findItem(R.id.menu_cast)?.isVisible = getCastSession(context) != null
            menu.findItem(R.id.menu_record_remove)?.isVisible = true
            menu.findItem(R.id.menu_download)?.isVisible = isUnlocked

        } else if (recording.isScheduled && !recording.isRecording) {
            Timber.d("Recording is scheduled")
            menu.findItem(R.id.menu_record_cancel)?.isVisible = true
            menu.findItem(R.id.menu_edit)?.isVisible = isUnlocked

        } else if (recording.isRecording) {
            Timber.d("Recording is being recorded")
            menu.findItem(R.id.menu_play)?.isVisible = true
            menu.findItem(R.id.menu_cast)?.isVisible = getCastSession(context) != null
            menu.findItem(R.id.menu_record_stop)?.isVisible = true
            menu.findItem(R.id.menu_edit)?.isVisible = isUnlocked

        } else if (recording.isFailed || recording.isFileMissing || recording.isMissed || recording.isAborted) {
            Timber.d("Recording is failed, file is missing, has been missed or was aborted")
            menu.findItem(R.id.menu_record_remove)?.isVisible = true
            // Allow playing a failed recording which size is not zero
            if (recording.dataSize > 0) {
                menu.findItem(R.id.menu_play)?.isVisible = true
                menu.findItem(R.id.menu_cast)?.isVisible = getCastSession(context) != null
            }
        }

        // Show the play menu item and the cast menu item (if available)
        // when the current time is between the program start and end time
        val currentTime = System.currentTimeMillis()
        if (program != null
                && program.start > 0
                && program.stop > 0
                && currentTime > program.start
                && currentTime < program.stop) {
            menu.findItem(R.id.menu_play)?.isVisible = true
            menu.findItem(R.id.menu_cast)?.isVisible = getCastSession(context) != null
        }
    }
    // Show the add reminder menu only for programs and
    // recordings where the start time is in the future.
    if (isUnlocked && PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notifications_enabled", context.resources.getBoolean(R.bool.pref_default_notifications_enabled))) {
        val currentTime = System.currentTimeMillis()
        var startTime = currentTime
        if (program != null && program.start > 0) {
            startTime = program.start
        }
        menu.findItem(R.id.menu_add_notification)?.isVisible = startTime > currentTime
    }
}

fun prepareSearchMenu(menu: Menu, title: String?, isNetworkAvailable: Boolean) {
    val visible = isNetworkAvailable && !title.isNullOrEmpty()
    menu.findItem(R.id.menu_search)?.isVisible = visible
    menu.findItem(R.id.menu_search_imdb)?.isVisible = visible
    menu.findItem(R.id.menu_search_fileaffinity)?.isVisible = visible
    menu.findItem(R.id.menu_search_youtube)?.isVisible = visible
    menu.findItem(R.id.menu_search_google)?.isVisible = visible
    menu.findItem(R.id.menu_search_epg)?.isVisible = visible
}
