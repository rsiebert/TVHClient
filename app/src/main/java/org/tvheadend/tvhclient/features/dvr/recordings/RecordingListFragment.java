package org.tvheadend.tvhclient.features.dvr.recordings;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.download.DownloadPermissionGrantedInterface;
import org.tvheadend.tvhclient.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RecordingListFragment extends BaseFragment implements RecyclerViewClickCallback, DownloadPermissionGrantedInterface {

    final static int REC_TYPE_COMPLETED = 1;
    final static int REC_TYPE_SCHEDULED = 2;
    final static int REC_TYPE_FAILED = 3;
    final static int REC_TYPE_REMOVED = 4;

    RecordingViewModel viewModel;
    RecordingRecyclerViewAdapter recyclerViewAdapter;
    @BindView(R.id.recycler_view)
    protected RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    protected ProgressBar progressBar;
    int selectedListPosition;
    String searchQuery;
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

        recyclerViewAdapter = new RecordingRecyclerViewAdapter(isDualPane, this, htspVersion);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity.getApplicationContext(), LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);
        viewModel = ViewModelProviders.of(activity).get(RecordingViewModel.class);

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
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
                return menuUtils.handleMenuRemoveAllRecordingsSelection(list);

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
        // Do not show the search menu when no recordings are available
        menu.findItem(R.id.menu_search).setVisible(recyclerViewAdapter.getItemCount() > 0);
    }

    void showRecordingDetails(int position) {
        selectedListPosition = position;
        recyclerViewAdapter.setPosition(position);
        Recording recording = recyclerViewAdapter.getItem(position);
        if (recording == null || !isVisible()) {
            return;
        }

        if (!isDualPane) {
            Fragment fragment = RecordingDetailsFragment.newInstance(recording.getId());
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.main, fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(null);
            ft.commit();
        } else {
            // Check what fragment is currently shown, replace if needed.
            Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.details);
            if (!(fragment instanceof RecordingDetailsFragment)
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

    private void showPopupMenu(View view) {
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
                popupMenu.getMenu().findItem(R.id.menu_cast).setVisible(MiscUtils.getCastSession(activity) != null);
                popupMenu.getMenu().findItem(R.id.menu_download).setVisible(isUnlocked);

            } else if (recording.isScheduled() && !recording.isRecording()) {
                popupMenu.getMenu().findItem(R.id.menu_record_cancel).setVisible(true);
                popupMenu.getMenu().findItem(R.id.menu_edit).setVisible(isUnlocked);

            } else if (recording.isRecording()) {
                popupMenu.getMenu().findItem(R.id.menu_record_stop).setVisible(true);
                popupMenu.getMenu().findItem(R.id.menu_play).setVisible(true);
                popupMenu.getMenu().findItem(R.id.menu_cast).setVisible(MiscUtils.getCastSession(activity) != null);
                popupMenu.getMenu().findItem(R.id.menu_edit).setVisible(isUnlocked);

            } else if (recording.isFailed() || recording.isFileMissing() || recording.isMissed() || recording.isAborted()) {
                popupMenu.getMenu().findItem(R.id.menu_record_remove).setVisible(true);
            }
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    return menuUtils.handleMenuSearchImdbWebsite(recording.getTitle());

                case R.id.menu_search_fileaffinity:
                    return menuUtils.handleMenuSearchFileAffinityWebsite(recording.getTitle());

                case R.id.menu_search_youtube:
                    return menuUtils.handleMenuSearchYoutube(recording.getTitle());

                case R.id.menu_search_google:
                    return menuUtils.handleMenuSearchGoogle(recording.getTitle());

                case R.id.menu_search_epg:
                    return menuUtils.handleMenuSearchEpgSelection(recording.getTitle());

                case R.id.menu_record_stop:
                    return menuUtils.handleMenuStopRecordingSelection(recording, null);

                case R.id.menu_record_cancel:
                    return menuUtils.handleMenuCancelRecordingSelection(recording, null);

                case R.id.menu_record_remove:
                    return menuUtils.handleMenuRemoveRecordingSelection(recording, null);

                case R.id.menu_play:
                    return menuUtils.handleMenuPlayRecording(recording.getId());

                case R.id.menu_cast:
                    return menuUtils.handleMenuCast("dvrId", recording.getId());

                case R.id.menu_download:
                    return menuUtils.handleMenuDownloadSelection(recording.getId());

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
            if (recyclerViewAdapter.getItemCount() > 0) {
                Recording recording = recyclerViewAdapter.getItem(position);
                menuUtils.handleMenuPlayRecordingIcon(recording.getId());
            }
        } else {
            showRecordingDetails(position);
        }
    }

    @Override
    public void onLongClick(View view, int position) {
        showPopupMenu(view);
    }

    @Override
    public void downloadRecording() {
        Recording recording = recyclerViewAdapter.getItem(selectedListPosition);
        if (recording != null) {
            menuUtils.handleMenuDownloadSelection(recording.getId());
        }
    }
}
