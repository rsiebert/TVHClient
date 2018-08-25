package org.tvheadend.tvhclient.features.programs;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.features.search.SearchActivity;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.BottomReachedCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

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
    private boolean loadingProgramAllowed;
    private Runnable loadingProgramsAllowedTask;
    private final Handler loadingProgramAllowedHandler = new Handler();
    private Unbinder unbinder;

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
            channelId = savedInstanceState.getInt("channelId", 0);
            channelName = savedInstanceState.getString("channelName");
            selectedTime = savedInstanceState.getLong("selectedTime");
            selectedListPosition = savedInstanceState.getInt("listPosition", 0);
            searchQuery = savedInstanceState.getString(SearchManager.QUERY);
        } else {
            selectedListPosition = 0;
            selectedTime = new Date().getTime();

            Bundle bundle = getArguments();
            if (bundle != null) {
                channelId = bundle.getInt("channelId", 0);
                channelName = bundle.getString("channelName");
                searchQuery = bundle.getString(SearchManager.QUERY);
            }
        }

        if (!isDualPane) {
            toolbarInterface.setTitle(channelName);
        }

        recyclerViewAdapter = new ProgramRecyclerViewAdapter(activity, this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        // Get all programs for the given channel starting from the current time.
        // Get them as live data so that any newly added programs will be fetched automatically.
        ProgramViewModel viewModel = ViewModelProviders.of(activity).get(ProgramViewModel.class);
        viewModel.getProgramsByChannelFromTime(channelId, selectedTime).observe(this, programs -> {
            if (programs != null) {
                recyclerViewAdapter.addItems(programs);
            }
            if (!TextUtils.isEmpty(searchQuery)) {
                recyclerViewAdapter.getFilter().filter(searchQuery, this);
            }
            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            if (!isDualPane) {
                toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.programs,
                        recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
            }
            // Invalidate the menu so that the search menu item is shown in
            // case the adapter contains items now.
            activity.invalidateOptionsMenu();
        });

        // Get all recordings for the given channel to check if it belongs to a certain program
        // so the recording status of the particular program can be updated. This is required
        // because the programs are not updated automatically when recordings change.
        viewModel.getRecordingsByChannelId(channelId).observe(this, recordings -> {
            recyclerViewAdapter.addRecordings(recordings);
        });

        loadingProgramAllowed = true;
        loadingProgramsAllowedTask = new Runnable() {
            @Override
            public void run() {
                loadingProgramAllowed = true;
            }
        };
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
        menu.findItem(R.id.menu_search).setVisible((recyclerViewAdapter.getItemCount() > 0));
        menu.findItem(R.id.menu_play).setVisible(isNetworkAvailable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;

            case R.id.menu_play:
                menuUtils.handleMenuPlayChannel(channelId);
                return true;

            case R.id.menu_genre_color_info_programs:
                menuUtils.handleMenuGenreColorSelection();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void showProgramDetails(int position) {
        selectedListPosition = position;
        Program program = recyclerViewAdapter.getItem(position);
        if (program == null) {
            return;
        }
        // Launch a new activity to display the program list of the selected channelTextView.
        Intent intent = new Intent(activity, ProgramDetailsActivity.class);
        intent.putExtra("eventId", program.getEventId());
        intent.putExtra("channelId", program.getChannelId());
        activity.startActivity(intent);
    }

    public void showPopupMenu(View view) {
        final Program program = (Program) view.getTag();
        if (activity == null || program == null) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.channel_list_program_popup_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());
        menuUtils.onPreparePopupMenu(popupMenu.getMenu(), program, program.getRecording(), isNetworkAvailable);
        menuUtils.onPreparePopupSearchMenu(popupMenu.getMenu(), isNetworkAvailable);

        // Show the play menu item when the current
        // time is between the program start and end time
        if (isNetworkAvailable) {
            long currentTime = new Date().getTime();
            if (currentTime > program.getStart() && currentTime < program.getStop()) {
                popupMenu.getMenu().findItem(R.id.menu_play).setVisible(true);
                CastSession castSession = CastContext.getSharedInstance(activity).getSessionManager().getCurrentCastSession();
                popupMenu.getMenu().findItem(R.id.menu_cast).setVisible(castSession != null);
            }
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    return menuUtils.handleMenuSearchImdbWebsite(program.getTitle());

                case R.id.menu_search_fileaffinity:
                    return menuUtils.handleMenuSearchFileAffinityWebsite(program.getTitle());

                case R.id.menu_search_epg:
                    return menuUtils.handleMenuSearchEpgSelection(program.getTitle(), channelId);

                case R.id.menu_record_remove:
                    final Recording recording = program.getRecording();
                    if (recording != null) {
                        if (recording.isRecording()) {
                            return menuUtils.handleMenuStopRecordingSelection(recording.getId(), recording.getTitle());
                        } else if (recording.isScheduled()) {
                            return menuUtils.handleMenuCancelRecordingSelection(recording.getId(), recording.getTitle(), null);
                        } else {
                            return menuUtils.handleMenuRemoveRecordingSelection(recording.getId(), recording.getTitle(), null);
                        }
                    }
                    return false;

                case R.id.menu_record_once:
                    return menuUtils.handleMenuRecordSelection(program.getEventId());

                case R.id.menu_record_once_custom_profile:
                    return menuUtils.handleMenuCustomRecordSelection(program.getEventId(), channelId);

                case R.id.menu_record_series:
                    return menuUtils.handleMenuSeriesRecordSelection(program.getTitle());

                case R.id.menu_play:
                    return menuUtils.handleMenuPlayChannel(channelId);

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
        Intent searchIntent = new Intent(activity, SearchActivity.class);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.putExtra("type", "programs");
        searchIntent.putExtra("channelId", channelId);
        searchIntent.putExtra("channelName", channelName);
        startActivity(searchIntent);
    }

    @Override
    public void onBottomReached(int position) {
        // Do not load more programs when a search query was given.
        // Show only the results of the existing programs
        if (!TextUtils.isEmpty(searchQuery) || !loadingProgramAllowed) {
            return;
        }

        loadingProgramAllowed = false;
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
        toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.results,
                recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
    }

    @Override
    public void onClick(View view, int position) {
        showProgramDetails(position);
    }

    @Override
    public void onLongClick(View view, int position) {
        showPopupMenu(view);
    }
}
