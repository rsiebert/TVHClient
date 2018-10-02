package org.tvheadend.tvhclient.features.dvr.recordings;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Filter;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;

public class FailedRecordingListFragment extends RecordingListFragment implements SearchRequestInterface, Filter.FilterListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbarInterface.setTitle(TextUtils.isEmpty(searchQuery)
                ? getString(R.string.failed_recordings) : getString(R.string.search_results));

        recyclerViewAdapter.setRecordingType(REC_TYPE_FAILED);

        RecordingViewModel viewModel = ViewModelProviders.of(activity).get(RecordingViewModel.class);
        viewModel.getFailedRecordings().observe(this, recordings -> {
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings);
            }
            if (!TextUtils.isEmpty(searchQuery)) {
                recyclerViewAdapter.getFilter().filter(searchQuery, this);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            if (TextUtils.isEmpty(searchQuery)) {
                toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.items, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
            } else {
                toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.failed_recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
            }

            if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
                showRecordingDetails(selectedListPosition);
            }
            // Invalidate the menu so that the search menu item is shown in
            // case the adapter contains items now.
            activity.invalidateOptionsMenu();
        });
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
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.items, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        } else {
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.failed_recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        }
    }
}
