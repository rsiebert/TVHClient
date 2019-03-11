package org.tvheadend.tvhclient.ui.features.dvr.recordings;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.ui.base.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.util.menu.PopupMenuUtil;
import org.tvheadend.tvhclient.util.menu.SearchMenuUtils;
import org.tvheadend.tvhclient.ui.features.download.DownloadPermissionGrantedInterface;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.util.MiscUtils;

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

public class RecordingListFragment extends BaseFragment implements RecyclerViewClickCallback, DownloadPermissionGrantedInterface {

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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
        // Hide the casting icon as a default.
        menu.findItem(R.id.media_route_menu_item).setVisible(false);
        // Do not show the search menu when no recordings are available
        menu.findItem(R.id.menu_search).setVisible(recyclerViewAdapter.getItemCount() > 0);
    }

    void showRecordingDetails(int position) {
        selectedListPosition = position;
        recyclerViewAdapter.setPosition(position);
        Recording recording = recyclerViewAdapter.getItem(position);
        if (recording == null || !isVisible()
                || !activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
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

    private void showPopupMenu(View view, int position) {
        final Recording recording = recyclerViewAdapter.getItem(position);
        if (activity == null || recording == null) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.recordings_popup_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());
        PopupMenuUtil.prepareSearchMenu(popupMenu.getMenu(), recording.getTitle(), isNetworkAvailable);

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
            if (SearchMenuUtils.onMenuSelected(activity, item.getItemId(), recording.getTitle())) {
                return true;
            }
            switch (item.getItemId()) {
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
    public void onClick(@NonNull View view, int position) {
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
    public boolean onLongClick(@NonNull View view, int position) {
        showPopupMenu(view, position);
        return true;
    }

    @Override
    public void downloadRecording() {
        Recording recording = recyclerViewAdapter.getItem(selectedListPosition);
        if (recording != null) {
            menuUtils.handleMenuDownloadSelection(recording.getId());
        }
    }
}
