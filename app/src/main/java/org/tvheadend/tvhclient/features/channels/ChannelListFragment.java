package org.tvheadend.tvhclient.features.channels;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
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
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.repository.ChannelAndProgramRepository;
import org.tvheadend.tvhclient.features.programs.ProgramListActivity;
import org.tvheadend.tvhclient.features.programs.ProgramListFragment;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.MenuUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelClickCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTagSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTimeSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewTouchCallback;
import org.tvheadend.tvhclient.features.shared.listener.RecyclerTouchListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

// TODO show programs from time in dual pane, program list not updated
// TODO use recycler view filter for search by channel name
// TODO sorting should consider minor major channel numbers
// TODO use the channel tag from the server status

public class ChannelListFragment extends BaseFragment implements ChannelClickCallback, ChannelTimeSelectionCallback, ChannelTagSelectionCallback, SearchRequestInterface, ChannelsLoadedCallback {

    protected ChannelRecyclerViewAdapter recyclerViewAdapter;
    protected RecyclerView recyclerView;
    protected ProgressBar progressBar;
    private List<Channel> channels = new ArrayList<>();

    private long selectedTime;
    private int channelTagId;
    private int selectedTimeOffset;
    private int selectedListPosition;
    private String searchQuery;
    private ChannelViewModel viewModel;
    private Runnable channelUpdateTask;
    private final Handler channelUpdateHandler = new Handler();
    private ChannelAndProgramRepository channelRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Timber.d("onCreateView() called");
        View view = inflater.inflate(R.layout.recyclerview_fragment, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("listPosition", 0);
            selectedTimeOffset = savedInstanceState.getInt("timeOffset");
            channelTagId = savedInstanceState.getInt("channelTagId");
            selectedTime = savedInstanceState.getLong("selectedTime");
            // In case the app was resumed and the selected time is in the past,
            // then use the current time to show the current channels.
            long currentTime = new Date().getTime();
            if (selectedTime < currentTime) {
                selectedTime = currentTime;
            }
        } else {
            selectedListPosition = 0;
            selectedTimeOffset = 0;
            channelTagId = 0;
            selectedTime = new Date().getTime();
        }

        recyclerViewAdapter = new ChannelRecyclerViewAdapter(activity, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(activity.getApplicationContext(), recyclerView, new RecyclerViewTouchCallback() {
            @Override
            public void onClick(View view, int position) {
                showChannelDetails(position);
            }

            @Override
            public void onLongClick(View view, int position) {
                showPopupMenu(view);
            }
        }));

        channelRepository = new ChannelAndProgramRepository(activity);
        viewModel = ViewModelProviders.of(activity).get(ChannelViewModel.class);

        updateAdapterAndToolbar();
        if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
            showChannelDetails(selectedListPosition);
        }

        // Get all recordings for the given channel to check if it belongs to a certain program
        // so the recording status of the particular program can be updated. This is required
        // because the programs are not updated automatically when recordings change.
        viewModel.getAllRecordings().observe(this, recordings -> {
            recyclerViewAdapter.addRecordings(recordings);
        });

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        channelUpdateTask = new Runnable() {
            public void run() {
                long currentTime = new Date().getTime();
                if (selectedTime < currentTime) {
                    selectedTime = currentTime;
                    loadAllChannelsByTimeAndTagSync(selectedTime, channelTagId);
                }
                channelUpdateHandler.postDelayed(channelUpdateTask, 60000);
            }
        };
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("listPosition", selectedListPosition);
        outState.putInt("timeOffset", selectedTimeOffset);
        outState.putInt("channelTagId", channelTagId);
        outState.putLong("selectedTime", selectedTime);
    }

    @Override
    public void onPause() {
        super.onPause();
        channelUpdateHandler.removeCallbacks(channelUpdateTask);
    }

    @Override
    public void onResume() {
        super.onResume();
        channelUpdateHandler.post(channelUpdateTask);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.channel_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);

        final boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", false);
        final boolean showChannelTagMenu = sharedPreferences.getBoolean("channel_tag_menu_enabled", true);

        menu.findItem(R.id.menu_genre_color_info_channels).setVisible(showGenreColors);
        menu.findItem(R.id.menu_timeframe).setVisible(isUnlocked);

        // Prevent the channel tag menu item from going into the overlay menu
        if (showChannelTagMenu) {
            menu.findItem(R.id.menu_tags).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags:
                menuUtils.handleMenuChannelTagsSelection(channelTagId, this);
                return true;
            case R.id.menu_timeframe:
                menuUtils.handleMenuTimeSelection(selectedTimeOffset, this);
                return true;
            case R.id.menu_genre_color_info_channels:
                menuUtils.handleMenuGenreColorSelection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTimeSelected(int which) {
        selectedTimeOffset = which;

        // Add the selected list index as extra hours to the current time.
        // If the first index was selected then use the current time.
        Calendar c = Calendar.getInstance();
        if (which > 0) {
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + which);
            selectedTime = c.getTimeInMillis();
        } else {
            selectedTime = new Date().getTime();
        }
        //recyclerViewAdapter.addItems(viewModel.getAllChannelsByTimeAndTagSync(selectedTime, channelTagId));
        loadAllChannelsByTimeAndTagSync(selectedTime, channelTagId);
    }

    @Override
    public void onChannelTagIdSelected(int id) {
        channelTagId = id;
        //updateAdapterAndToolbar();
        loadAllChannelsByTimeAndTagSync(selectedTime, channelTagId);
    }

    /**
     * Updates adapter with the channels filtered by time and channel tag.
     * Also updates the title with the name of the selected channel tag or with all channels
     * if no channel tag is set. The subtitle will show the number of visible channels.
     */
    private void updateAdapterAndToolbar() {
        //Timber.d("Updating adapter and toolbar");
        //List<Channel> channelList = getAllChannelsByTimeAndTagSync();
        Timber.d("Loaded " + channels.size() + " channels");
        recyclerViewAdapter.addItems(channels);

        ChannelTag channelTag = viewModel.getChannelTagByIdSync(channelTagId);
        toolbarInterface.setTitle((channelTag == null) ? getString(R.string.all_channels) : channelTag.getTagName());
        toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.items,
                channels.size(), channels.size()));
    }

    /**
     * Show the program list when a channel was selected. In single pane mode a separate
     * activity is called. In dual pane mode a list fragment will be shown in the right side.
     *
     * @param position The selected position in the list
     */
    protected void showChannelDetails(int position) {
        selectedListPosition = position;
        Channel channel = recyclerViewAdapter.getItem(position);
        if (channel == null) {
            return;
        }
        if (!isDualPane) {
            // Launch a new activity to display the program list of the selected channel.
            Intent intent = new Intent(activity, ProgramListActivity.class);
            intent.putExtra("channelName", channel.getName());
            intent.putExtra("channelId", channel.getId());
            intent.putExtra("selectedTime", selectedTime);
            activity.startActivity(intent);
        } else {
            FragmentManager fm = getFragmentManager();
            if (fm != null) {
                // Check if the fragment for the selected channel is already shown, if not replace it with a new fragment.
                ProgramListFragment programListFragment = (ProgramListFragment) fm.findFragmentById(R.id.details);
                if (programListFragment == null || programListFragment.getShownChannelId() != channel.getId()) {
                    programListFragment = ProgramListFragment.newInstance(channel.getName(), channel.getId(), selectedTime);
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(R.id.details, programListFragment);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    ft.commit();
                }
            }
        }
    }

    public void showPopupMenu(View view) {

        Channel channel = (Channel) view.getTag();
        Program program = viewModel.getProgramByIdSync(channel.getProgramId());
        Recording recording = viewModel.getRecordingByEventIdSync(channel.getProgramId());

        if (activity == null) {
            return;
        }

        Timber.d("showPopupMenu: program " + program.getEventId() + ", " + program.getTitle());

        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.channel_list_program_popup_menu, popupMenu.getMenu());
        menuUtils.onPreparePopupMenu(popupMenu.getMenu(), program, recording);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchWebSelection(channel.getProgramTitle());
                    return true;

                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(channel.getProgramTitle(), channel.getId());
                    return true;

                case R.id.menu_record_remove:
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
                    menuUtils.handleMenuRecordSelection(channel.getProgramId());
                    return true;

                case R.id.menu_record_once_custom_profile:
                    menuUtils.handleMenuCustomRecordSelection(channel.getProgramId(), channel.getId());
                    return true;

                case R.id.menu_record_series:
                    menuUtils.handleMenuSeriesRecordSelection(channel.getProgramTitle());
                    return true;

                case R.id.menu_play:
                    // Open a new activity to stream the current program to this device
                    menuUtils.handleMenuPlayChannelSelection(channel.getId());
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

    @Override
    public void onSearchRequested(String query) {
        Timber.d("onSearchRequested() called with: query = [" + query + "]");
        /*
        // Start searching for programs on all channels
        Intent searchIntent = new Intent(activity, SearchActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra("type", "programs");
        startActivity(searchIntent);
*/
        // filter recycler view when query submitted
        //recyclerViewAdapter.getFilter().filter(query);
    }

    @Override
    public void onChannelClick(int id) {
        new MenuUtils(activity).handleMenuPlayChannelSelection(id);
    }

    @Override
    public void onChannelsLoaded(List<Channel> channels) {
        Timber.d("Loading channels done");
        this.channels = channels;
        updateAdapterAndToolbar();

        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    void loadAllChannelsByTimeAndTagSync(long currentTime, int channelTagId) {
        Timber.d("Loading channels started");
        channelRepository.getAllChannelsByTimeAndTagSync(currentTime, channelTagId, this);
    }
}
