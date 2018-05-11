package org.tvheadend.tvhclient.features.programs;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.features.search.SearchActivity;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewTouchCallback;
import org.tvheadend.tvhclient.features.shared.listener.BottomReachedListener;
import org.tvheadend.tvhclient.features.shared.listener.RecyclerTouchListener;

import java.util.Date;

import timber.log.Timber;

public class ProgramListFragment extends BaseFragment implements BottomReachedListener, SearchRequestInterface {

    private ProgramRecyclerViewAdapter recyclerViewAdapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private long selectedTime;
    private int selectedListPosition;
    private int channelId;
    private String channelName;

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
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            channelId = savedInstanceState.getInt("channelId", 0);
            channelName = savedInstanceState.getString("channelName");
            selectedTime = savedInstanceState.getLong("selectedTime");
            selectedListPosition = savedInstanceState.getInt("listPosition", 0);
        } else {
            selectedListPosition = 0;
            selectedTime = new Date().getTime();

            Bundle bundle = getArguments();
            if (bundle != null) {
                channelId = bundle.getInt("channelId", 0);
                channelName = bundle.getString("channelName");
            }
        }

        toolbarInterface.setTitle(channelName);

        recyclerViewAdapter = new ProgramRecyclerViewAdapter(activity, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(activity.getApplicationContext(), recyclerView, new RecyclerViewTouchCallback() {
            @Override
            public void onClick(View view, int position) {
                showProgramDetails(position);
            }

            @Override
            public void onLongClick(View view, int position) {
                showPopupMenu(view);
            }
        }));

        // Get all programs for the given channel starting from the current time.
        // Get them as live data so that any newly added programs will be fetched automatically.
        ProgramViewModel viewModel = ViewModelProviders.of(activity).get(ProgramViewModel.class);
        viewModel.getProgramsByChannelFromTime(channelId, selectedTime).observe(this, programs -> {
            recyclerViewAdapter.addItems(programs);
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.programs, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));

            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
                showProgramDetails(selectedListPosition);
            }
        });

        // Get all recordings for the given channel to check if it belongs to a certain program
        // so the recording status of the particular program can be updated. This is required
        // because the programs are not updated automatically when recordings change.
        viewModel.getRecordingsByChannelId(channelId).observe(this, recordings -> {
            recyclerViewAdapter.addRecordings(recordings);
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("channelId", channelId);
        outState.putString("channelName", channelName);
        outState.putLong("selectedTime", selectedTime);
        outState.putInt("listPosition", selectedListPosition);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.program_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu in dual pane mode or if no genre colors shall be shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("genre_colors_for_programs_enabled", false);
        menu.findItem(R.id.menu_genre_color_info_programs).setVisible(!isDualPane && showGenreColors);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;

            case R.id.menu_play:
                // Open a new activity that starts playing the first program that is
                // currently transmitted over this channel
                menuUtils.handleMenuPlayChannelSelection(channelId);
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

        Timber.d("showProgramDetails: program " + program.getEventId() + ", " + program.getTitle());

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

        Timber.d("showPopupMenu: program " + program.getEventId() + ", " + program.getTitle());

        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.channel_list_program_popup_menu, popupMenu.getMenu());
        menuUtils.onPreparePopupMenu(popupMenu.getMenu(), program, program.getRecording());

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchWebSelection(program.getTitle());
                    return true;

                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(program.getTitle(), channelId);
                    return true;

                case R.id.menu_record_remove:
                    final Recording recording = program.getRecording();
                    if (recording != null) {
                        if (recording.isRecording()) {
                            menuUtils.handleMenuStopRecordingSelection(recording.getId(), recording.getTitle());
                        } else if (recording.isScheduled()) {
                            menuUtils.handleMenuCancelRecordingSelection(recording.getId(), recording.getTitle(), null);
                        } else {
                            menuUtils.handleMenuRemoveRecordingSelection(recording.getId(), recording.getTitle(), null);
                        }
                    }
                    return true;

                case R.id.menu_record_once:
                    menuUtils.handleMenuRecordSelection(program.getEventId());
                    return true;

                case R.id.menu_record_once_custom_profile:
                    menuUtils.handleMenuCustomRecordSelection(program.getEventId(), channelId);
                    return true;

                case R.id.menu_record_series:
                    menuUtils.handleMenuSeriesRecordSelection(program.getTitle());
                    return true;

                case R.id.menu_play:
                    menuUtils.handleMenuPlayChannelSelection(channelId);
                    return true;

                case R.id.menu_add_notification:
                    menuUtils.handleMenuAddNotificationSelection(program);
                    return true;

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
        Timber.d("onSearchRequested() called with: query = [" + query + "]");

        // Start searching for programs on the given channelTextView
        Intent searchIntent = new Intent(activity, SearchActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra("type", "programs");
        // Pass a the bundle. The contents will be forwarded to the
        // fragment that is responsible for displaying the search results
        Bundle bundle = new Bundle();
        bundle.putInt("channelId", channelId);
        searchIntent.putExtra(SearchManager.APP_DATA, bundle);
        startActivity(searchIntent);

        // filter recycler view when query submitted
        //recyclerViewAdapter.getFilter().filter(query);
    }

    @Override
    public void onBottomReached(int position) {
        Timber.d("Bottom of program list reached");
        Program lastProgram = recyclerViewAdapter.getItem(position);
        Intent intent = new Intent(activity, EpgSyncService.class);
        intent.setAction("getEvents");
        intent.putExtra("eventId", lastProgram.getNextEventId());
        intent.putExtra("channelId", lastProgram.getChannelId());
        intent.putExtra("numFollowing", 25);
        intent.putExtra("showMessage", true);
        activity.startService(intent);
    }
}
