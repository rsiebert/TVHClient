package org.tvheadend.tvhclient.utils;


import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.DownloadActivity;
import org.tvheadend.tvhclient.activities.PlayActivity;
import org.tvheadend.tvhclient.activities.SearchResultActivity;
import org.tvheadend.tvhclient.adapter.ChannelTagListAdapter;
import org.tvheadend.tvhclient.adapter.GenreColorDialogAdapter;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.GenreColorDialogItem;
import org.tvheadend.tvhclient.model.Profile;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MenuUtils {

    private final int mHtspVersion;
    private final boolean mIsUnlocked;
    private WeakReference<Activity> mActivity;

    public MenuUtils(Activity activity) {
        mActivity = new WeakReference<>(activity);
        mHtspVersion = DataStorage.getInstance().getProtocolVersion();
        mIsUnlocked = TVHClientApplication.getInstance().isUnlocked();
    }

    /**
     * Prepares a dialog that shows the available genre colors and the names. In
     * here the data for the adapter is created and the dialog prepared which
     * can be shown later.
     */
    public void handleMenuGenreColorSelection() {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        final String[] s = activity.getResources().getStringArray(R.array.pr_content_type0);

        // Fill the list for the adapter
        final List<GenreColorDialogItem> items = new ArrayList<>();
        for (int i = 0; i < s.length; ++i) {
            GenreColorDialogItem genreColor = new GenreColorDialogItem();
            genreColor.color = MiscUtils.getGenreColor(activity, ((i + 1) * 16), 0);
            genreColor.genre = s[i];
            items.add(genreColor);
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.genre_color_list)
                .adapter(new GenreColorDialogAdapter(items), null)
                .show();
    }

    public void handleMenuTimeSelection(int currentSelection, MenuTimeSelectionCallback callback) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        // TODO show the next 12h, consider overflow into the next day
        // Get the current hour of the day and create
        // a list of the next hours until midnight
        Calendar c = Calendar.getInstance();
        final int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
        String[] times = new String[12];
        times[0] = activity.getString(R.string.current_time);

        // TODO show a locale dependant time representation
        for (int i = 1; i < 12; i++) {
            int hour = (hourOfDay + i);
            times[i] = String.valueOf(hour <= 24 ? hour : hour - 24) + ":00";
        }

        new MaterialDialog.Builder(activity)
                .title(R.string.select_time)
                .items(times)
                .itemsCallbackSingleChoice(currentSelection, (dialog, itemView, which, text) -> {
                    // Convert the selected index into hours in seconds
                    // and reload the data with the given offset
                    if (callback != null) {
                        callback.menuTimeSelected(which);
                    }
                    return true;
                })
                .build()
                .show();

    }

    /**
     *
     * @param channelTagList
     * @param selectedTagId
     * @param callback
     */
    public void handleMenuTagsSelection(List<ChannelTag> channelTagList, long selectedTagId, MenuTagSelectionCallback callback) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }

        // Show the dialog that shows all available channel tags. When the
        // user has selected a tag, restart the loader to get the updated channel list
        final ChannelTagListAdapter channelTagListAdapter = new ChannelTagListAdapter(channelTagList, selectedTagId);
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .title(R.string.tags)
                .adapter(channelTagListAdapter, null)
                .build();
        // Set the callback to handle clicks. This needs to be done after the
        // dialog creation so that the inner method has access to the dialog variable
        channelTagListAdapter.setCallback(which -> {
            if (callback != null) {
                callback.menuTagSelected(which);
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void handleMenuDownloadSelection(long recId) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        Intent intent = new Intent(activity, DownloadActivity.class);
        intent.putExtra(Constants.BUNDLE_RECORDING_ID, recId);
        intent.putExtra(Constants.BUNDLE_ACTION, Constants.ACTION_DOWNLOAD);
        activity.startActivity(intent);
    }

    public void handleMenuSearchWebSelection(String title) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        try {
            String url = URLEncoder.encode(title, "utf-8");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("imdb:///find?s=tt&q=" + url));
            PackageManager packageManager = activity.getPackageManager();
            if (packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                intent.setData(Uri.parse("http://www.imdb.org/find?s=tt&q=" + url));
            }
            activity.startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            // NOP
        }
    }

    public void handleMenuSearchEpgSelection(String title) {
        handleMenuSearchEpgSelection(title, 0);
    }

    public void handleMenuSearchEpgSelection(String title, long channelId) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        Intent intent = new Intent(activity, SearchResultActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, title);
        if (channelId > 0) {
            intent.putExtra("channelId", channelId);
        }
        activity.startActivity(intent);
    }

    public void handleMenuSeriesRecordSelection(long channelId, String title) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        final Intent intent = new Intent(activity, HTSService.class);
        intent.setAction("addAutorecEntry");
        intent.putExtra("title", title);
        intent.putExtra("channelId", channelId);

        final Connection connection = DatabaseHelper.getInstance(activity).getSelectedConnection();
        final Profile profile = DatabaseHelper.getInstance(activity).getProfile(connection.recording_profile_id);
        if (profile != null
                && profile.enabled
                && mHtspVersion >= 16
                && mIsUnlocked) {
            intent.putExtra("configName", profile.name);
        }
        activity.startService(intent);
    }

    public void handleMenuPlaySelection(long channelId, long dvrId) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        Intent intent = new Intent(activity, PlayActivity.class);
        intent.putExtra("channelId", channelId);
        intent.putExtra("dvrId", dvrId);
        activity.startActivity(intent);
    }

    public void handleMenuStopRecordingSelection(long dvrId, String title) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        // Show a confirmation dialog before stopping the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_stop)
                .content(activity.getString(R.string.stop_recording, title))
                .negativeText(R.string.cancel)
                .positiveText(R.string.stop)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, HTSService.class);
                    intent.setAction("stopDvrEntry");
                    intent.putExtra("id", dvrId);
                    activity.startService(intent);
                })
                .show();
    }

    public void handleMenuRemoveRecordingSelection(long dvrId, String title) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        // Show a confirmation dialog before removing the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove)
                .content(activity.getString(R.string.remove_recording, title))
                .negativeText(R.string.cancel)
                .positiveText(R.string.remove)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, HTSService.class);
                    intent.setAction("deleteDvrEntry");
                    intent.putExtra("id", dvrId);
                    activity.startService(intent);
                })
                .show();
    }

    public void handleMenuCancelRecordingSelection(long dvrId, String title) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        // Show a confirmation dialog before cancelling the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove)
                .content(activity.getString(R.string.cancel_recording, title))
                .negativeText(R.string.discard)
                .positiveText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, HTSService.class);
                    intent.setAction("cancelDvrEntry");
                    intent.putExtra("id", dvrId);
                    activity.startService(intent);
                })
                .show();
    }

    public void handleMenuRemoveSeriesRecordingSelection(String id, String title) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        // Show a confirmation dialog before removing the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove)
                .content(activity.getString(R.string.remove_series_recording, title))
                .negativeText(R.string.cancel)
                .positiveText(R.string.remove)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, HTSService.class);
                    intent.setAction("deleteAutorecEntry");
                    intent.putExtra("id", id);
                    activity.startService(intent);
                })
                .show();
    }

    public void handleMenuRemoveTimerRecordingSelection(String id, String title) {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        // Show a confirmation dialog before removing the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove)
                .content(activity.getString(R.string.remove_timer_recording, title))
                .negativeText(R.string.cancel)
                .positiveText(R.string.remove)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, HTSService.class);
                    intent.setAction("deleteTimerecEntry");
                    intent.putExtra("id", id);
                    activity.startService(intent);
                })
                .show();
    }
}
