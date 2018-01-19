package org.tvheadend.tvhclient.ui.recordings.recordings;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
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

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingAddEditActivity;
import org.tvheadend.tvhclient.ui.search.SearchActivity;
import org.tvheadend.tvhclient.ui.search.SearchRequestInterface;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingListFragment extends Fragment implements RecyclerViewClickCallback, SearchRequestInterface {

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected RecordingRecyclerViewAdapter recyclerViewAdapter;
    protected RecyclerView recyclerView;
    boolean isUnlocked;
    protected boolean isDualPane;
    private MenuUtils menuUtils;
    protected int selectedListPosition;
    protected SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recyclerview_fragment, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        menuUtils = new MenuUtils(activity);
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = activity.findViewById(R.id.details);
        isDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("list_position", 0);
        }

        setHasOptionsMenu(true);

        recyclerViewAdapter = new RecordingRecyclerViewAdapter(activity, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("list_position", selectedListPosition);
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
        if (!sharedPreferences.getBoolean("hideMenuDeleteAllRecordingsPref", false) && recyclerViewAdapter.getItemCount() > 1) {
            menu.findItem(R.id.menu_record_remove_all).setVisible(true);
        }
        // Hide the casting icon as a default.
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        if (mediaRouteMenuItem != null) {
            mediaRouteMenuItem.setVisible(false);
        }
        // Do not show the search icon when no recordings are available
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        if (searchMenuItem != null && recyclerViewAdapter.getItemCount() == 0) {
            searchMenuItem.setVisible(false);
        }
    }

    @Override
    public void onClick(View view, int position) {
        showRecordingDetails(position);
    }

    protected void showRecordingDetails(int position) {
        selectedListPosition = position;
        Recording recording = recyclerViewAdapter.getItem(position);
        if (recording == null) {
            return;
        }
        if (!isDualPane) {
            // Launch a new activity to display the program list of the selected channel.
            Intent intent = new Intent(activity, RecordingDetailsActivity.class);
            intent.putExtra("dvrId", recording.getId());
            intent.putExtra("type", "recording");
            activity.startActivity(intent);
        } else {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the program details fragment.
            // TODO getListView().setItemChecked(position, true);
            // Check what fragment is currently shown, replace if needed.
            RecordingDetailsFragment recordingDetailsFragment = (RecordingDetailsFragment) getFragmentManager().findFragmentById(R.id.details);
            if (recordingDetailsFragment == null || recordingDetailsFragment.getShownDvrId() != recording.getId()) {
                // Make new fragment to show this selection.
                recordingDetailsFragment = RecordingDetailsFragment.newInstance(recording.getId());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.details, recordingDetailsFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        final Recording recording = (Recording) view.getTag();
        if (activity == null || recording == null) {
            return true;
        }
        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.recordings_popup_menu, popupMenu.getMenu());

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

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchWebSelection(recording.getTitle());
                    return true;
                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(recording.getTitle());
                    return true;
                case R.id.menu_record_stop:
                    menuUtils.handleMenuStopRecordingSelection(recording.getId(), recording.getTitle());
                    return true;
                case R.id.menu_record_remove:
                    if (recording.isScheduled()) {
                        menuUtils.handleMenuCancelRecordingSelection(recording.getId(), recording.getTitle());
                    } else {
                        menuUtils.handleMenuRemoveRecordingSelection(recording.getId(), recording.getTitle());
                    }
                    return true;
                case R.id.menu_play:
                    menuUtils.handleMenuPlaySelection(-1, recording.getId());
                    return true;
                case R.id.menu_download:
                    menuUtils.handleMenuDownloadSelection(recording.getId());
                    return true;
                case R.id.menu_edit:
                    Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                    intent.putExtra("dvrId", recording.getId());
                    intent.putExtra("type", "recording");
                    activity.startActivity(intent);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
        return true;
    }

    @Override
    public void onSearchRequested(String query) {
        // Start searching for recordings
        Intent searchIntent = new Intent(activity, SearchActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra("type", "recordings");
        startActivity(searchIntent);
    }
}
