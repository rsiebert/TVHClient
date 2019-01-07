package org.tvheadend.tvhclient.features.dvr.recordings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.Filter;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScheduledRecordingListFragment extends RecordingListFragment implements SearchRequestInterface, Filter.FilterListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbarInterface.setTitle(TextUtils.isEmpty(searchQuery)
                ? getString(R.string.scheduled_recordings) : getString(R.string.search_results));

        recyclerViewAdapter.setRecordingType(REC_TYPE_SCHEDULED);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start observing the recordings here because the onActivityCreated method is not
        // called when the user has returned from the settings activity. In this case
        // the changes to the recording UI like hiding duplicates would not become active.
        viewModel.getScheduledRecordings().observe(getViewLifecycleOwner(), this::handleObservedRecordings);
    }

    private void handleObservedRecordings(List<Recording> recordings) {
        if (recordings != null) {
            // Remove all recordings from the list that are duplicated
            if (sharedPreferences.getBoolean("hide_duplicate_scheduled_recordings_enabled",
                    activity.getResources().getBoolean(R.bool.pref_default_hide_duplicate_scheduled_recordings_enabled))) {
                for (Recording recording : new CopyOnWriteArrayList<>(recordings)) {
                    if (recording.getDuplicate() == 1) {
                        recordings.remove(recording);
                    }
                }
            }
            recyclerViewAdapter.addItems(recordings);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.items, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        } else {
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.upcoming_recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        }

        if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
            showRecordingDetails(selectedListPosition);
        }
        // Invalidate the menu so that the search menu item is shown in
        // case the adapter contains items now.
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_add).setVisible(isUnlocked);
    }

    @Override
    public void onSearchRequested(String query) {
        searchQuery = query;
        recyclerViewAdapter.getFilter().filter(query, this);
    }

    @Override
    public boolean onSearchResultsCleared() {
        if (!TextUtils.isEmpty(searchQuery)) {
            searchQuery = "";
            recyclerViewAdapter.getFilter().filter("", this);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onFilterComplete(int i) {
        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.items, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        } else {
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.upcoming_recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        }
        // Preselect the first result item in the details screen
        if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
            showRecordingDetails(0);
        }
    }

    @Override
    public String getQueryHint() {
        return getString(R.string.search_scheduled_recordings);
    }
}
