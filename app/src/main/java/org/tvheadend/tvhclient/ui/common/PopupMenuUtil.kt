@file:JvmName("PopupMenuUtil")

package org.tvheadend.tvhclient.ui.common

import android.content.Context
import android.preference.PreferenceManager
import android.text.TextUtils
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

    val recordOnceMenuItem = menu.findItem(R.id.menu_record_once)
    val recordOnceAndEditMenuItem = menu.findItem(R.id.menu_record_once_and_edit)
    val recordOnceCustomProfileMenuItem = menu.findItem(R.id.menu_record_once_custom_profile)
    val recordSeriesMenuItem = menu.findItem(R.id.menu_record_series)
    val recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove)
    val recordStopMenuItem = menu.findItem(R.id.menu_record_stop)
    val recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel)
    val playMenuItem = menu.findItem(R.id.menu_play)
    val castMenuItem = menu.findItem(R.id.menu_cast)
    val addReminderMenuItem = menu.findItem(R.id.menu_add_notification)

    if (isNetworkAvailable) {
        if (recording == null || (!recording.isRecording
                        && !recording.isScheduled
                        && !recording.isCompleted)) {
            Timber.d("Recording is not recording or scheduled")
            recordOnceMenuItem.isVisible = true
            recordOnceAndEditMenuItem.isVisible = isUnlocked
            recordOnceCustomProfileMenuItem.isVisible = isUnlocked
            recordSeriesMenuItem.isVisible = htspVersion >= 13

        } else if (recording.isCompleted) {
            Timber.d("Recording is completed ")
            playMenuItem.isVisible = true
            castMenuItem.isVisible = getCastSession(context) != null
            recordRemoveMenuItem.isVisible = true

        } else if (recording.isScheduled && !recording.isRecording) {
            Timber.d("Recording is scheduled")
            recordCancelMenuItem.isVisible = true

        } else if (recording.isRecording) {
            Timber.d("Recording is being recorded")
            playMenuItem.isVisible = true
            castMenuItem.isVisible = getCastSession(context) != null
            recordStopMenuItem.isVisible = true

        } else if (recording.isFailed || recording.isFileMissing || recording.isMissed || recording.isAborted) {
            Timber.d("Recording is something else")
            recordRemoveMenuItem.isVisible = true
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
        addReminderMenuItem.isVisible = startTime > currentTime
    }
}

fun prepareSearchMenu(menu: Menu, title: String?, isNetworkAvailable: Boolean) {
    val visible = isNetworkAvailable && !TextUtils.isEmpty(title)
    menu.findItem(R.id.menu_search)?.isVisible = visible
    menu.findItem(R.id.menu_search_imdb)?.isVisible = visible
    menu.findItem(R.id.menu_search_fileaffinity)?.isVisible = visible
    menu.findItem(R.id.menu_search_youtube)?.isVisible = visible
    menu.findItem(R.id.menu_search_google)?.isVisible = visible
    menu.findItem(R.id.menu_search_epg)?.isVisible = visible
}
