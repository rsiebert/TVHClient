package org.tvheadend.tvhclient.features.dvr.recordings;

import android.app.SearchManager;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.download.DownloadPermissionGrantedInterface;
import org.tvheadend.tvhclient.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.features.playback.PlayRecordingActivity;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RecordingListFragment extends BaseFragment implements RecyclerViewClickCallback, Filter.FilterListener, DownloadPermissionGrantedInterface {

    protected RecordingRecyclerViewAdapter recyclerViewAdapter;
    @BindView(R.id.recycler_view)
    protected RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    protected ProgressBar progressBar;
    protected int selectedListPosition;
    protected String searchQuery;
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

        recyclerViewAdapter = new RecordingRecyclerViewAdapter(activity, isDualPane, this, htspVersion);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);
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
                intent.putExtra("type", "recording");
                activity.startActivity(intent);
                return true;

            case R.id.menu_record_remove_all:
                CopyOnWriteArrayList<Recording> list = new CopyOnWriteArrayList<>(recyclerViewAdapter.getItems());
                menuUtils.handleMenuRemoveAllRecordingsSelection(list);
                return true;

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
        // Hide the casting icon as a default.
        menu.findItem(R.id.media_route_menu_item).setVisible(false);
        // Do not show the search icon when no recordings are available
        menu.findItem(R.id.menu_search).setVisible((recyclerViewAdapter.getItemCount() > 0));
    }

    protected void showRecordingDetails(int position) {
        selectedListPosition = position;
        recyclerViewAdapter.setPosition(position);
        Recording recording = recyclerViewAdapter.getItem(position);
        if (recording == null) {
            return;
        }
        if (!isDualPane) {
            // Launch a new activity to display the program list of the selected channel.
            Intent intent = new Intent(activity, RecordingDetailsActivity.class);
            intent.putExtra("id", recording.getId());
            intent.putExtra("type", "recording");
            activity.startActivity(intent);
        } else {
            // Check what fragment is currently shown, replace if needed.
            Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.details);
            if (fragment == null
                    || !(fragment instanceof RecordingDetailsFragment)
                    || ((RecordingDetailsFragment) fragment).getShownDvrId() != recording.getId()) {
                // Make new fragment to show this selection.
                fragment = RecordingDetailsFragment.newInstance(recording.getId());
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.details, fragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    public void showPopupMenu(View view) {
        final Recording recording = (Recording) view.getTag();
        if (activity == null || recording == null) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.recordings_popup_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());
        menuUtils.onPreparePopupSearchMenu(popupMenu.getMenu(), isNetworkAvailable);

        if (isNetworkAvailable) {
            if (recording.isCompleted()) {
                popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
                popupMenu.getMenu().findItem(R.id.menu_play).setVisible(true);
                popupMenu.getMenu().findItem(R.id.menu_download).setVisible(isUnlocked);

            } else if (recording.isScheduled() && !recording.isRecording()) {
                popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
                popupMenu.getMenu().findItem(R.id.menu_edit).setVisible(isUnlocked);

            } else if (recording.isRecording()) {
                popupMenu.getMenu().findItem(R.id.menu_record_stop).setVisible(true);
                popupMenu.getMenu().findItem(R.id.menu_play).setVisible(true);
                popupMenu.getMenu().findItem(R.id.menu_edit).setVisible(isUnlocked);

            } else if (recording.isFailed() || recording.isRemoved() || recording.isMissed() || recording.isAborted()) {
                popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
            }
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchImdbWebsite(recording.getTitle());
                    return true;

                case R.id.menu_search_fileaffinity:
                    menuUtils.handleMenuSearchFileAffinityWebsite(recording.getTitle());
                    return true;

                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(recording.getTitle());
                    return true;

                case R.id.menu_record_stop:
                    menuUtils.handleMenuStopRecordingSelection(recording.getId(), recording.getTitle());
                    return true;

                case R.id.menu_record_remove:
                    if (recording.isScheduled()) {
                        menuUtils.handleMenuCancelRecordingSelection(recording.getId(), recording.getTitle(), null);
                    } else {
                        menuUtils.handleMenuRemoveRecordingSelection(recording.getId(), recording.getTitle(), null);
                    }
                    return true;

                case R.id.menu_play:
                    menuUtils.handleMenuPlayRecording(recording.getId());
                    return true;

                case R.id.menu_download:
                    menuUtils.handleMenuDownloadSelection(recording.getId());
                    return true;

                case R.id.menu_edit:
                    Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                    intent.putExtra("id", recording.getId());
                    intent.putExtra("type", "recording");
                    activity.startActivity(intent);
                    return true;

                default:
                    return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onClick(View view, int position) {
        selectedListPosition = position;
        if (view.getId() == R.id.icon || view.getId() == R.id.icon_text) {
            Recording recording = recyclerViewAdapter.getItem(position);
            Intent playIntent = new Intent(activity, PlayRecordingActivity.class);
            playIntent.putExtra("dvrId", recording.getId());
            activity.startActivity(playIntent);
        } else {
            showRecordingDetails(position);
        }
    }

    @Override
    public void onLongClick(View view, int position) {
        showPopupMenu(view);
    }

    @Override
    public void onFilterComplete(int i) {
        toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.results,
                recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
    }

    @Override
    public void downloadRecording() {
        Recording recording = recyclerViewAdapter.getItem(selectedListPosition);
        if (recording != null) {
            menuUtils.handleMenuDownloadSelection(recording.getId());
        }
    }
}
