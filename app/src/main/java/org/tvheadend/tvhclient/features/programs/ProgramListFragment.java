package org.tvheadend.tvhclient.features.programs;

import android.app.SearchManager;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.search.StartSearchInterface;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.BottomReachedCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class ProgramListFragment extends BaseFragment implements RecyclerViewClickCallback, BottomReachedCallback, SearchRequestInterface, Filter.FilterListener {

    private ProgramRecyclerViewAdapter recyclerViewAdapter;
    @BindView(R.id.recycler_view)
    protected RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    protected ProgressBar progressBar;
    private long selectedTime;
    private int selectedListPosition;
    private int channelId;
    private String channelName;
    private String searchQuery;
    private boolean loadingMoreProgramAllowed;
    private Runnable loadingProgramsAllowedTask;
    private final Handler loadingProgramAllowedHandler = new Handler();
    private Unbinder unbinder;
    private int programIdToBeEditedWhenBeingRecorded = 0;
    private boolean isSearchActive;
    private ProgramViewModel viewModel;

    public static ProgramListFragment newInstance(String channelName, int channelId, long selectedTime) {
        ProgramListFragment f = new ProgramListFragment();
        Bundle args = new Bundle();
        args.putString("channelName", channelName);
        args.putInt("channelId", channelId);
        args.putLong("selectedTime", selectedTime);
        f.setArguments(args);
        return f;
    }

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
            channelId = savedInstanceState.getInt("channelId", 0);
            channelName = savedInstanceState.getString("channelName");
            selectedTime = savedInstanceState.getLong("selectedTime");
            selectedListPosition = savedInstanceState.getInt("listPosition", 0);
            searchQuery = savedInstanceState.getString(SearchManager.QUERY);
        } else {
            selectedListPosition = 0;

            Bundle bundle = getArguments();
            if (bundle != null) {
                channelId = bundle.getInt("channelId", 0);
                channelName = bundle.getString("channelName");
                selectedTime = bundle.getLong("selectedTime", new Date().getTime());
                searchQuery = bundle.getString(SearchManager.QUERY);
            }
        }

        isSearchActive = !TextUtils.isEmpty(searchQuery);

        if (!isDualPane) {
            toolbarInterface.setTitle(isSearchActive ? getString(R.string.search_results) : channelName);
        }

        // Show the channel icons when a search is active and all channels shall be searched
        boolean showProgramChannelIcon = isSearchActive && channelId == 0;

        recyclerViewAdapter = new ProgramRecyclerViewAdapter(this, this, showProgramChannelIcon);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity.getApplicationContext(), LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        viewModel = ViewModelProviders.of(activity).get(ProgramViewModel.class);
        if (!isSearchActive) {
            Timber.d("Search is not active, loading programs for channel " + channelName + " from time " + selectedTime);
            // A channel id and a channel name was given, load only the programs for the
            // specific channel and from the current time. Also load only those recordings
            // that belong to the given channel
            viewModel.getProgramsByChannelFromTime(channelId, selectedTime).observe(getViewLifecycleOwner(), this::handleObservedPrograms);
            viewModel.getRecordingsByChannelId(channelId).observe(getViewLifecycleOwner(), this::handleObservedRecordings);

            loadingMoreProgramAllowed = true;
            loadingProgramsAllowedTask = () -> loadingMoreProgramAllowed = true;

        } else {
            Timber.d("Search is active, loading programs from current time " + selectedTime);
            // No channel and channel name was given, load all programs
            // from the current time and all recordings from all channels
            viewModel.getProgramsFromTime(selectedTime).observe(getViewLifecycleOwner(), this::handleObservedPrograms);
            viewModel.getRecordings().observe(getViewLifecycleOwner(), this::handleObservedRecordings);

            loadingMoreProgramAllowed = false;
        }
    }

    private void handleObservedPrograms(List<Program> programs) {
        if (programs != null) {
            recyclerViewAdapter.addItems(programs);
        }
        if (isSearchActive) {
            if (activity instanceof StartSearchInterface) {
                ((StartSearchInterface) activity).startSearch();
            }
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (!isDualPane) {
            if (!isSearchActive) {
                toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.items, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
            } else {
                toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.programs, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
            }
        }
        // Invalidate the menu so that the search menu item is shown in
        // case the adapter contains items now.
        activity.invalidateOptionsMenu();
    }

    /**
     * Check all recordings for the given channel to see if it belongs to a certain program
     * so the recording status of the particular program can be updated. This is required
     * because the programs are not updated automatically when recordings change.
     *
     * @param recordings The list of recordings
     */
    private void handleObservedRecordings(List<Recording> recordings) {
        if (recordings != null) {
            recyclerViewAdapter.addRecordings(recordings);
            for (Recording recording : recordings) {
                if (recording.getEventId() == programIdToBeEditedWhenBeingRecorded
                        && programIdToBeEditedWhenBeingRecorded > 0) {
                    programIdToBeEditedWhenBeingRecorded = 0;
                    Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                    intent.putExtra("id", recording.getId());
                    intent.putExtra("type", "recording");
                    activity.startActivity(intent);
                    break;
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        loadingProgramAllowedHandler.removeCallbacks(loadingProgramsAllowedTask);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("channelId", channelId);
        outState.putString("channelName", channelName);
        outState.putLong("selectedTime", selectedTime);
        outState.putInt("listPosition", selectedListPosition);
        outState.putString(SearchManager.QUERY, searchQuery);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.program_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu in dual pane mode or if no genre colors shall be shown
        final boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_programs_enabled", false);
        menu.findItem(R.id.menu_genre_color_info_programs).setVisible(!isDualPane && showGenreColors);

        if (!isSearchActive && isNetworkAvailable) {
            menu.findItem(R.id.menu_play).setVisible(true);
            menu.findItem(R.id.menu_cast).setVisible(MiscUtils.getCastSession(activity) != null);
        } else {
            menu.findItem(R.id.menu_play).setVisible(false);
            menu.findItem(R.id.menu_cast).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_play:
                menuUtils.handleMenuPlayChannel(channelId);
                return true;

            case R.id.menu_cast:
                return menuUtils.handleMenuCast("channelId", channelId);

            case R.id.menu_genre_color_info_programs:
                menuUtils.handleMenuGenreColorSelection();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showProgramDetails(int position) {
        selectedListPosition = position;
        Program program = recyclerViewAdapter.getItem(position);
        if (program == null || !isVisible()
                || !activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            return;
        }

        Fragment fragment = ProgramDetailsFragment.newInstance(program.getEventId(), program.getChannelId());
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(null);
        ft.commit();
    }

    private void showPopupMenu(View view) {
        final Program program = (Program) view.getTag();
        if (activity == null || program == null) {
            return;
        }

        Recording recording = appRepository.getRecordingData().getItemByEventId(program.getEventId());

        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());
        menuUtils.onPreparePopupMenu(popupMenu.getMenu(), program, program.getRecording(), isNetworkAvailable);
        menuUtils.onPreparePopupSearchMenu(popupMenu.getMenu(), isNetworkAvailable);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    return menuUtils.handleMenuSearchImdbWebsite(program.getTitle());

                case R.id.menu_search_fileaffinity:
                    return menuUtils.handleMenuSearchFileAffinityWebsite(program.getTitle());

                case R.id.menu_search_youtube:
                    return menuUtils.handleMenuSearchYoutube(program.getTitle());

                case R.id.menu_search_google:
                    return menuUtils.handleMenuSearchGoogle(program.getTitle());

                case R.id.menu_search_epg:
                    return menuUtils.handleMenuSearchEpgSelection(program.getTitle(), channelId);

                case R.id.menu_record_stop:
                    return menuUtils.handleMenuStopRecordingSelection(recording, null);

                case R.id.menu_record_cancel:
                    return menuUtils.handleMenuCancelRecordingSelection(recording, null);

                case R.id.menu_record_remove:
                    return menuUtils.handleMenuRemoveRecordingSelection(recording, null);

                case R.id.menu_record_once:
                    return menuUtils.handleMenuRecordSelection(program.getEventId());

                case R.id.menu_record_once_and_edit:
                    programIdToBeEditedWhenBeingRecorded = program.getEventId();
                    return menuUtils.handleMenuRecordSelection(program.getEventId());

                case R.id.menu_record_once_custom_profile:
                    return menuUtils.handleMenuCustomRecordSelection(program.getEventId(), channelId);

                case R.id.menu_record_series:
                    return menuUtils.handleMenuSeriesRecordSelection(program.getTitle());

                case R.id.menu_play:
                    return menuUtils.handleMenuPlayChannel(channelId);

                case R.id.menu_cast:
                    return menuUtils.handleMenuCast("channelId", channelId);

                case R.id.menu_add_notification:
                    return menuUtils.handleMenuAddNotificationSelection(program);

                default:
                    return false;
            }
        });
        popupMenu.show();
    }

    public int getShownChannelId() {
        return channelId;
    }

    @Override
    public void onSearchRequested(String query) {
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
    public String getQueryHint() {
        return getString(R.string.search_programs);
    }

    @Override
    public void onBottomReached(int position) {
        // Do not load more programs when a search query was given or all programs were loaded.
        if (isSearchActive || !loadingMoreProgramAllowed || !isNetworkAvailable) {
            return;
        }

        loadingMoreProgramAllowed = false;
        loadingProgramAllowedHandler.postDelayed(loadingProgramsAllowedTask, 2000);

        Program lastProgram = recyclerViewAdapter.getItem(position);
        Intent intent = new Intent(activity, EpgSyncService.class);
        intent.setAction("getEvents");
        intent.putExtra("eventId", lastProgram.getNextEventId());
        intent.putExtra("channelId", lastProgram.getChannelId());
        intent.putExtra("numFollowing", 25);
        intent.putExtra("showMessage", true);
        activity.startService(intent);
    }

    @Override
    public void onFilterComplete(int count) {
        if (!isDualPane) {
            if (!isSearchActive) {
                toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.items, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
            } else {
                toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.programs, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
            }
        }
    }

    @Override
    public void onClick(View view, int position) {
        showProgramDetails(position);
    }

    @Override
    public void onLongClick(View view, int position) {
        showPopupMenu(view);
    }

    public void updatePrograms(long selectedTime) {
        this.selectedTime = selectedTime;
        viewModel.getProgramsByChannelFromTime(channelId, selectedTime).observe(getViewLifecycleOwner(), this::handleObservedPrograms);
    }
}
