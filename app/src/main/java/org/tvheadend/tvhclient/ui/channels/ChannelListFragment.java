package org.tvheadend.tvhclient.ui.channels;

import android.app.Activity;
import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.ChannelTag;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.ui.programs.ProgramListActivity;
import org.tvheadend.tvhclient.ui.programs.ProgramListFragment;
import org.tvheadend.tvhclient.ui.search.SearchActivity;
import org.tvheadend.tvhclient.ui.search.SearchRequestInterface;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.Utils;
import org.tvheadend.tvhclient.utils.callbacks.ChannelTagSelectionCallback;
import org.tvheadend.tvhclient.utils.callbacks.ChannelTimeSelectionCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

// TODO show programs from time after orientation change
// TODO show programs from time in dual pane, program list not updated
// TODO change getting channel tag

public class ChannelListFragment extends Fragment implements RecyclerViewClickCallback, ChannelClickCallback, ChannelTimeSelectionCallback, ChannelTagSelectionCallback, SearchRequestInterface {

    private Activity activity;
    private ToolbarInterface toolbarInterface;
    protected ChannelRecyclerViewAdapter recyclerViewAdapter;
    protected RecyclerView recyclerView;

    private boolean isDualPane = false;
    private Runnable channelUpdateTask;
    private final Handler channelUpdateHandler = new Handler();
    private int channelTimeSelection;
    //private long showProgramsFromTime;
    private MenuUtils menuUtils;
    private boolean isUnlocked;
    private int selectedListPosition;
    private String searchQuery;
    private ChannelViewModel viewModel;

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

        activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        menuUtils = new MenuUtils(getActivity());
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = getActivity().findViewById(R.id.details);
        isDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        Bundle bundle = getArguments();
        if (bundle != null) {
            channelTimeSelection = bundle.getInt("channel_time_selection");
            //showProgramsFromTime = bundle.getLong("show_programs_from_time");
        }

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("list_position", 0);
            channelTimeSelection = savedInstanceState.getInt("channel_time_selection");
            //showProgramsFromTime = savedInstanceState.getLong("show_programs_from_time");
        } else {
            selectedListPosition = 0;
            channelTimeSelection = 0;
            //showProgramsFromTime = new Date().getTime();
        }

        setHasOptionsMenu(true);

        recyclerViewAdapter = new ChannelRecyclerViewAdapter(activity, new ArrayList<>(), this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        viewModel = ViewModelProviders.of(this).get(ChannelViewModel.class);
        viewModel.getChannels().observe(this, channels -> {
            recyclerViewAdapter.addItems(channels);
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.items, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));

            if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
                showChannelDetails(selectedListPosition);
            }
        });

        // Initiate a timer that will update the adapter every minute
        // so that the progress bars will be displayed correctly
        // Also update the current adapter time if the current
        // time was selected from the channel time dialog, otherwise
        // old programs will not be removed when they are over
        channelUpdateTask = new Runnable() {
            public void run() {
                if (channelTimeSelection == 0) {
                    viewModel.setTime(new Date().getTime());
                }
                channelUpdateHandler.postDelayed(channelUpdateTask, 60000);
            }
        };
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("list_position", selectedListPosition);
        outState.putInt("channel_time_selection", channelTimeSelection);
        //outState.putLong("show_programs_from_time", showProgramsFromTime);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.channel_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsChannelsPref", false);
        menu.findItem(R.id.menu_genre_color_info_channels).setVisible(showGenreColors);

        // Playing a channel shall not be available in channel only mode or in
        // single pane mode, because no channel is preselected.
        menu.findItem(R.id.menu_play).setVisible(isDualPane);

        menu.findItem(R.id.menu_timeframe).setVisible(isUnlocked);

        // Prevent the channel tag menu item from going into the overlay menu
        if (prefs.getBoolean("visibleMenuIconTagsPref", true)) {
            menu.findItem(R.id.menu_tags).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_play:
                menuUtils.handleMenuPlaySelection(recyclerViewAdapter.getSelectedItem().getChannelId(), -1);
                return true;
            case R.id.menu_tags:
                ChannelTag tag = Utils.getChannelTag(activity);
                menuUtils.handleMenuTagsSelection((tag != null ? tag.getTagId() : -1), this);
                return true;
            case R.id.menu_timeframe:
                menuUtils.handleMenuTimeSelection(channelTimeSelection, this);
                return true;
            case R.id.menu_genre_color_info_channels:
                menuUtils.handleMenuGenreColorSelection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        /*TVHClientApplication.getInstance().addListener(this);

        if (!DataStorage.getInstance().isLoading()) {
            populateList();
            if (isDualPane) {
                showChannelDetails(selectedListPosition);
            }
        }*/
        // Start the timer that updates the adapter so
        // it only shows programs within the current time
        channelUpdateHandler.post(channelUpdateTask);
    }

    @Override
    public void onPause() {
        super.onPause();
        //TVHClientApplication.getInstance().removeListener(this);
        channelUpdateHandler.removeCallbacks(channelUpdateTask);
    }

    /**
     * Fills the adapter with the available channel data. Only those channels
     * will be added to the adapter that contain the selected channel tag.
     * Additionally some status information will be shown in the action bar.
     */
    /*
    private void populateList() {
        ChannelTag currentTag = null;
        Connection connection = DatabaseHelper.getInstance(getActivity().getApplicationContext()).getSelectedConnection();
        if (connection != null) {
            currentTag = DataStorage.getInstance().getTagFromArray(connection.channelTag);
        }

        adapter.clear();
        for (Channel channel : DataStorage.getInstance().getChannelsFromArray().values()) {
            if (currentTag == null || channel.getTags().contains(currentTag.getTagId())) {
                adapter.add(channel);
            }
        }

        adapter.sort(Utils.getChannelSortOrder(activity));
        adapter.setTime(showProgramsFromTime);
        adapter.notifyDataSetChanged();

        // Show the name of the selected channel tag and the number of channels
        // in the action bar. If enabled show also the channel tag icon.
        toolbarInterface.setTitle((currentTag == null) ? getString(R.string.all_channels) : currentTag.getTagName());
        String items = getResources().getQuantityString(R.plurals.items, adapter.getCount(), adapter.getCount());
        toolbarInterface.setSubtitle(items);
        */
/*
        if (sharedPreferences.getBoolean("showIconPref", true)
                && sharedPreferences.getBoolean("showTagIconPref", false)
                && currentTag != null
                && currentTag.tagId != 0) {
            Bitmap iconBitmap = MiscUtils.getCachedIcon(activity, currentTag.tagIcon);
            toolbarInterface.setIcon(iconBitmap);
        } else {
            toolbarInterface.setActionBarIcon(R.mipmap.ic_launcher);
        }
    }
*/

    @Override
    public void onTimeSelected(int which) {
        channelTimeSelection = which;

        // Get the current time and create the new time from the selection value.
        // 0 is the current time, 1 is 2 hours ahead, 2 is 4 hours ahead and so on
        Calendar c = Calendar.getInstance();
        if (which > 0) {
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            if (which > 0) {
                c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + which);
            }
        }

        viewModel.setTime(c.getTimeInMillis());
        //showProgramsFromTime = c.getTimeInMillis();
        //adapter.setTime(showProgramsFromTime);
        //adapter.notifyDataSetChanged();
    }

    @Override
    public void onChannelTagIdSelected(int which) {
        Connection connection = DatabaseHelper.getInstance(getActivity().getApplicationContext()).getSelectedConnection();
        if (connection != null) {
            connection.channelTag = which;
            DatabaseHelper.getInstance(getActivity().getApplicationContext()).updateConnection(connection);
        }
        //populateList();
    }

    @Override
    public void onClick(View view, int position) {
        showChannelDetails(position);
    }

    protected void showChannelDetails(int position) {
        selectedListPosition = position;
        Channel channel = recyclerViewAdapter.getItem(position);
        if (channel == null) {
            return;
        }
        if (!isDualPane) {
            // Launch a new activity to display the program list of the selected channel.
            Intent intent = new Intent(getActivity(), ProgramListActivity.class);
            intent.putExtra("channelName", channel.getChannelName());
            intent.putExtra("channelId", channel.getChannelId());
            intent.putExtra("show_programs_from_time", viewModel.getTime());
            activity.startActivity(intent);
        } else {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the program details fragment.
            // TODO getListView().setItemChecked(position, true);
            // Check what fragment is currently shown, replace if needed.
            ProgramListFragment programListFragment = (ProgramListFragment) getFragmentManager().findFragmentById(R.id.details);
            if (programListFragment == null || programListFragment.getShownChannelId() != channel.getChannelId()) {
                // Make new fragment to show this selection.
                programListFragment = ProgramListFragment.newInstance(channel.getChannelName(), channel.getChannelId(), viewModel.getTime());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.details, programListFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        final Channel channel = (Channel) view.getTag();
        final Program program = getCurrentProgram(channel);
        if (getActivity() == null || program == null) {
            return true;
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.channel_list_program_popup_menu, popupMenu.getMenu());
        menuUtils.onPreparePopupMenu(popupMenu.getMenu(), program);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchWebSelection(program.getTitle());
                    return true;
                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(program.getTitle(), channel.getChannelId());
                    return true;
                case R.id.menu_record_remove:
                    Recording rec = DataStorage.getInstance().getRecordingFromArray(program.getDvrId());
                    if (rec != null) {
                        if (rec.isRecording()) {
                            menuUtils.handleMenuStopRecordingSelection(rec.getId(), rec.getTitle());
                        } else if (rec.isScheduled()) {
                            menuUtils.handleMenuCancelRecordingSelection(rec.getId(), rec.getTitle());
                        } else {
                            menuUtils.handleMenuRemoveRecordingSelection(rec.getId(), rec.getTitle());
                        }
                    }
                    return true;
                case R.id.menu_record_once:
                    menuUtils.handleMenuRecordSelection(program.getEventId());
                    return true;
                case R.id.menu_record_once_custom_profile:
                    menuUtils.handleMenuCustomRecordSelection(program.getEventId(), channel.getChannelId());
                    return true;
                case R.id.menu_record_series:
                    menuUtils.handleMenuSeriesRecordSelection(program.getTitle());
                    return true;
                case R.id.menu_play:
                    // Open a new activity to stream the current program to this device
                    menuUtils.handleMenuPlaySelection(channel.getChannelId(), -1);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
        return true;
    }

    private Program getCurrentProgram(Channel channel) {
        if (channel == null) {
            return null;
        }
        for (Program program : DataStorage.getInstance().getProgramsFromArray().values()) {
            if (program.getChannelId() == channel.getChannelId()) {
                if (program.getStart() <= viewModel.getTime() && program.getStop() > viewModel.getTime()) {
                    return program;
                }
            }
        }
        return null;
    }

    @Override
    public void onSearchRequested(String query) {
        Log.d("X", "onSearchRequested() called with: query = [" + query + "]");
        // Start searching for programs on all channels
        Intent searchIntent = new Intent(getActivity(), SearchActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra("type", "programs");
        startActivity(searchIntent);
    }

    @Override
    public void onChannelClick(int id) {
        new MenuUtils(activity).handleMenuPlaySelection(id, -1);
    }
}
