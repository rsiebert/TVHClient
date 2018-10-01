package org.tvheadend.tvhclient.features.dvr.series_recordings;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
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
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.features.dvr.recordings.RecordingDetailsActivity;
import org.tvheadend.tvhclient.features.search.SearchActivity;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.concurrent.CopyOnWriteArrayList;

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
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recyclerview_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
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

        recyclerViewAdapter = new SeriesRecordingRecyclerViewAdapter(activity, isDualPane, this, htspVersion);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        SeriesRecordingViewModel viewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel.class);
        viewModel.getRecordings().observe(this, recordings -> {
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
                toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.series_recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
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
    public boolean onOptionsItemSelected(MenuItem item) {
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.recording_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (sharedPreferences.getBoolean("delete_all_recordings_menu_enabled", false)
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
        if (recording == null) {
            return;
        }
        if (!isDualPane) {
            // Launch a new activity to display the program list of the selected channel.
            Intent intent = new Intent(getActivity(), RecordingDetailsActivity.class);
            intent.putExtra("id", recording.getId());
            intent.putExtra("type", "series_recording");
            activity.startActivity(intent);
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

    private void showPopupMenu(View view) {
        final SeriesRecording seriesRecording = (SeriesRecording) view.getTag();
        if (getActivity() == null || seriesRecording == null) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.series_recordings_popup_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());

        menuUtils.onPreparePopupSearchMenu(popupMenu.getMenu(), isNetworkAvailable);
        popupMenu.getMenu().findItem(R.id.menu_edit).setVisible(isUnlocked);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_edit:
                    Intent intent = new Intent(getActivity(), RecordingAddEditActivity.class);
                    intent.putExtra("id", seriesRecording.getId());
                    intent.putExtra("type", "series_recording");
                    getActivity().startActivity(intent);
                    return true;

                case R.id.menu_search_imdb:
                    return menuUtils.handleMenuSearchImdbWebsite(seriesRecording.getTitle());

                case R.id.menu_search_fileaffinity:
                    return menuUtils.handleMenuSearchFileAffinityWebsite(seriesRecording.getTitle());

                case R.id.menu_search_epg:
                    return menuUtils.handleMenuSearchEpgSelection(seriesRecording.getTitle());

                case R.id.menu_record_remove:
                    return menuUtils.handleMenuRemoveSeriesRecordingSelection(seriesRecording, null);

                default:
                    return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onClick(View view, int position) {
        showRecordingDetails(position);
    }

    @Override
    public void onLongClick(View view, int position) {
        showPopupMenu(view);
    }

    @Override
    public void onFilterComplete(int i) {
        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.items, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        } else {
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.series_recordings, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        }
    }

    @Override
    public void onSearchRequested(String query) {
        Intent searchIntent = new Intent(activity, SearchActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra("type", "series_recordings");
        startActivity(searchIntent);
    }
}
