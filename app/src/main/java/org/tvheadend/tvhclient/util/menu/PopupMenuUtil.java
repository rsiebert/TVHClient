package org.tvheadend.tvhclient.util.menu;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.ProgramInterface;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.util.MiscUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class PopupMenuUtil {

    public static void prepareMenu(@NonNull Context context,
                                   @NonNull Menu menu, @Nullable ProgramInterface program, @Nullable Recording recording,
                                   boolean isNetworkAvailable, int htspVersion, boolean isUnlocked) {

        // Hide the menus because the ones in the toolbar are not hidden when set in the xml
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(false);
        }

        MenuItem recordOnceMenuItem = menu.findItem(R.id.menu_record_once);
        MenuItem recordOnceAndEditMenuItem = menu.findItem(R.id.menu_record_once_and_edit);
        MenuItem recordOnceCustomProfileMenuItem = menu.findItem(R.id.menu_record_once_custom_profile);
        MenuItem recordSeriesMenuItem = menu.findItem(R.id.menu_record_series);
        MenuItem recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove);
        MenuItem recordStopMenuItem = menu.findItem(R.id.menu_record_stop);
        MenuItem recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel);
        MenuItem playMenuItem = menu.findItem(R.id.menu_play);
        MenuItem castMenuItem = menu.findItem(R.id.menu_cast);
        MenuItem addReminderMenuItem = menu.findItem(R.id.menu_add_notification);

        if (isNetworkAvailable) {
            if (recording == null || (!recording.isRecording()
                    && !recording.isScheduled()
                    && !recording.isCompleted())) {
                Timber.d("Recording is not recording or scheduled");
                recordOnceMenuItem.setVisible(true);
                recordOnceAndEditMenuItem.setVisible(isUnlocked);
                recordOnceCustomProfileMenuItem.setVisible(isUnlocked);
                recordSeriesMenuItem.setVisible(htspVersion >= 13);

            } else if (recording.isCompleted()) {
                Timber.d("Recording is completed ");
                playMenuItem.setVisible(true);
                castMenuItem.setVisible(MiscUtils.getCastSession(context) != null);
                recordRemoveMenuItem.setVisible(true);

            } else if (recording.isScheduled() && !recording.isRecording()) {
                Timber.d("Recording is scheduled");
                recordCancelMenuItem.setVisible(true);

            } else if (recording.isRecording()) {
                Timber.d("Recording is being recorded");
                playMenuItem.setVisible(true);
                castMenuItem.setVisible(MiscUtils.getCastSession(context) != null);
                recordStopMenuItem.setVisible(true);

            } else if (recording.isFailed() || recording.isFileMissing() || recording.isMissed() || recording.isAborted()) {
                Timber.d("Recording is something else");
                recordRemoveMenuItem.setVisible(true);
            }

            // Show the play menu item and the cast menu item (if available)
            // when the current time is between the program start and end time
            long currentTime = System.currentTimeMillis();
            if (program != null
                    && program.getStart() > 0
                    && program.getStop() > 0
                    && currentTime > program.getStart()
                    && currentTime < program.getStop()) {
                menu.findItem(R.id.menu_play).setVisible(true);
                menu.findItem(R.id.menu_cast).setVisible(MiscUtils.getCastSession(context) != null);
            }
        }
        // Show the add reminder menu only for programs and
        // recordings where the start time is in the future.
        if (isUnlocked && PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notifications_enabled", context.getResources().getBoolean(R.bool.pref_default_notifications_enabled))) {
            long currentTime = System.currentTimeMillis();
            long startTime = currentTime;
            if (program != null && program.getStart() > 0) {
                startTime = program.getStart();
            }
            addReminderMenuItem.setVisible(startTime > currentTime);
        }
    }

    public static void prepareSearchMenu(@NonNull Menu menu, @Nullable String title, boolean isNetworkAvailable) {
        boolean visible = isNetworkAvailable && !TextUtils.isEmpty(title);
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        if (searchMenuItem != null) {
            searchMenuItem.setVisible(visible);
            menu.findItem(R.id.menu_search_imdb).setVisible(visible);
            menu.findItem(R.id.menu_search_fileaffinity).setVisible(visible);
            menu.findItem(R.id.menu_search_youtube).setVisible(visible);
            menu.findItem(R.id.menu_search_google).setVisible(visible);
            menu.findItem(R.id.menu_search_epg).setVisible(visible);
        }
    }
}
