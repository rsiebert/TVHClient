package org.tvheadend.tvhclient.ui.features.dvr.series_recordings;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.SeriesRecording;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.ui.base.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.util.menu.PopupMenuUtil;
import org.tvheadend.tvhclient.util.menu.SearchMenuUtils;

import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SeriesRecordingListFragment extends BaseFragment implements RecyclerViewClickCallback, SearchRequestInterface, Filter.FilterListener {

    private SeriesRecordingRecyclerViewAdapter recyclerViewAdapter;
    @BindView(R.id.recycler_view)
    protected RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    protected ProgressBar progressBar;
    private int selectedListPosition;
    private String searchQuery;
    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recyclerview_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        recyclerView.setAdapter(null);
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("listPosition", 0);
            searchQuery = savedInstanceState.getString(SearchManager.QUERY);
        } else {
            selectedListPosition = 0;
            Bundle bundle = getArguments();
            if (bundle != null) {
                searchQuery = bundle.getString(SearchManager.QUERY);
            }
        }

        toolbarInterface.setTitle(TextUtils.isEmpty(searchQuery)
                ? getString(R.string.series_recordings) : getString(R.string.search_results));

        recyclerViewAdapter = new SeriesRecordingRecyclerViewAdapter(isDualPane, this, htspVersion);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity.getApplicationContext(), LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        SeriesRecordingViewModel viewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel.class);
        viewModel.getRecordings().observe(getViewLifecycleOwner(), recordings -> {
            if (recordings != null) {
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
                toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.series_recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("listPosition", selectedListPosition);
        outState.putString(SearchManager.QUERY, searchQuery);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                intent.putExtra("type", "series_recording");
                activity.startActivity(intent);
                return true;

            case R.id.menu_record_remove_all:
                CopyOnWriteArrayList<SeriesRecording> list = new CopyOnWriteArrayList<>(recyclerViewAdapter.getItems());
                return menuUtils.handleMenuRemoveAllSeriesRecordingSelection(list);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.recording_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (sharedPreferences.getBoolean("delete_all_recordings_menu_enabled",
                activity.getResources().getBoolean(R.bool.pref_default_delete_all_recordings_menu_enabled))
                && recyclerViewAdapter.getItemCount() > 1
                && isNetworkAvailable) {
            menu.findItem(R.id.menu_record_remove_all).setVisible(true);
        }
        menu.findItem(R.id.menu_add).setVisible(isUnlocked && isNetworkAvailable);
        menu.findItem(R.id.menu_search).setVisible((recyclerViewAdapter.getItemCount() > 0));
        menu.findItem(R.id.media_route_menu_item).setVisible(false);
    }

    private void showRecordingDetails(int position) {
        selectedListPosition = position;
        recyclerViewAdapter.setPosition(position);
        SeriesRecording recording = recyclerViewAdapter.getItem(position);
        if (recording == null || !isVisible()
                || !activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            return;
        }
        if (!isDualPane) {
            Fragment fragment = SeriesRecordingDetailsFragment.newInstance(recording.getId());
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.main, fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(null);
            ft.commit();
        } else {
            // Check what fragment is currently shown, replace if needed.
            Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.details);
            if (!(fragment instanceof SeriesRecordingDetailsFragment)
                    || ((SeriesRecordingDetailsFragment) fragment).getShownId().equals(recording.getId())) {
                // Make new fragment to show this selection.
                fragment = SeriesRecordingDetailsFragment.newInstance(recording.getId());
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.details, fragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    private void showPopupMenu(View view, int position) {
        final SeriesRecording seriesRecording = recyclerViewAdapter.getItem(position);
        if (getActivity() == null || seriesRecording == null) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.series_recordings_popup_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());

        PopupMenuUtil.prepareSearchMenu(popupMenu.getMenu(), seriesRecording.getTitle(), isNetworkAvailable);
        popupMenu.getMenu().findItem(R.id.menu_edit).setVisible(isUnlocked);

        popupMenu.setOnMenuItemClickListener(item -> {
            if (SearchMenuUtils.onMenuSelected(activity, item.getItemId(), seriesRecording.getTitle())) {
                return true;
            }
            switch (item.getItemId()) {
                case R.id.menu_edit:
                    Intent intent = new Intent(getActivity(), RecordingAddEditActivity.class);
                    intent.putExtra("id", seriesRecording.getId());
                    intent.putExtra("type", "series_recording");
                    getActivity().startActivity(intent);
                    return true;

                case R.id.menu_record_remove:
                    return menuUtils.handleMenuRemoveSeriesRecordingSelection(seriesRecording, null);

                default:
                    return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onClick(@NonNull View view, int position) {
        showRecordingDetails(position);
    }

    @Override
    public boolean onLongClick(@NonNull View view, int position) {
        showPopupMenu(view, position);
        return true;
    }

    @Override
    public void onFilterComplete(int i) {
        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.items, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        } else {
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.series_recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        }
        // Preselect the first result item in the details screen
        if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
            showRecordingDetails(0);
        }
    }

    @Override
    public void onSearchRequested(@NonNull String query) {
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

    @NonNull
    @Override
    public String getQueryHint() {
        return getString(R.string.search_series_recordings);
    }
}
