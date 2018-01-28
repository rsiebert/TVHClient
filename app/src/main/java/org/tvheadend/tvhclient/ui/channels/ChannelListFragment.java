package org.tvheadend.tvhclient.ui.channels;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
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
// TODO use contraintlayout

public class ChannelListFragment extends BaseFragment implements RecyclerViewClickCallback, ChannelClickCallback, ChannelTimeSelectionCallback, ChannelTagSelectionCallback, SearchRequestInterface {
    private String TAG = getClass().getSimpleName();

    protected ChannelRecyclerViewAdapter recyclerViewAdapter;
    protected RecyclerView recyclerView;

    private int channelTimeSelection;
    private int selectedListPosition;
    private String searchQuery;
    private ChannelViewModel viewModel;
    private Runnable channelUpdateTask;
    private final Handler channelUpdateHandler = new Handler();

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

        Bundle bundle = getArguments();
        if (bundle != null) {
            channelTimeSelection = bundle.getInt("channel_time_selection");
        }

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("list_position", 0);
            channelTimeSelection = savedInstanceState.getInt("channel_time_selection");
        } else {
            selectedListPosition = 0;
            channelTimeSelection = 0;
        }

        recyclerViewAdapter = new ChannelRecyclerViewAdapter(activity, new ArrayList<>(), this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        Log.d(TAG, "onActivityCreated() called with: savedInstanceState = [" + savedInstanceState + "]");
        viewModel = ViewModelProviders.of(activity).get(ChannelViewModel.class);
        viewModel.getChannelsByTime().observe(this, channels -> {
            Log.d(TAG, "onActivityCreated: observe");
            int channelCount = 0;
            if (channels != null) {
                channelCount = channels.size();
                recyclerViewAdapter.addItems(channels);
                if (isDualPane && channels.size() > 0) {
                    showChannelDetails(selectedListPosition);
                }
            }
            // TODO get the tag and so on
            ChannelTag currentTag = null;
            toolbarInterface.setTitle((currentTag == null) ? getString(R.string.all_channels) : currentTag.getTagName());
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.items, channelCount, channelCount));
        });

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        channelUpdateTask = new Runnable() {
            public void run() {
                long currentTime = new Date().getTime();
                Log.d(TAG, "run: viewModel.getTime() " + viewModel.getTime() + ", current time " + currentTime);
                if (viewModel.getTime() < currentTime) {
                    viewModel.setTime(currentTime);
                }
                channelUpdateHandler.postDelayed(channelUpdateTask, 60000);
            }
        };
        channelUpdateHandler.post(channelUpdateTask);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("list_position", selectedListPosition);
        outState.putInt("channel_time_selection", channelTimeSelection);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called, removing callbacks");
        channelUpdateHandler.removeCallbacks(channelUpdateTask);
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
    public void onTimeSelected(int which) {
        Log.d(TAG, "onTimeSelected() called with: which = [" + which + "]");
        channelTimeSelection = which;

        // Get the current time and create the new time from the selection value.
        // 0 is the current time, 1 is 1 hours ahead, 2 is 2 hours ahead and so on
        Calendar c = Calendar.getInstance();
        if (which > 0) {
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            if (which > 0) {
                c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + which);
            }
            viewModel.setTime(c.getTimeInMillis());
        } else {
            viewModel.setTime(new Date().getTime());
        }
    }

    @Override
    public void onChannelTagIdSelected(int which) {
        Log.d(TAG, "onChannelTagIdSelected() called with: which = [" + which + "]");
        // TODO update the channel tag stuff
        Connection connection = DatabaseHelper.getInstance(activity.getApplicationContext()).getSelectedConnection();
        if (connection != null) {
            connection.channelTag = which;
            DatabaseHelper.getInstance(activity.getApplicationContext()).updateConnection(connection);
        }
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
            Intent intent = new Intent(activity, ProgramListActivity.class);
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
        final Program program = channel.getProgram();
        if (activity == null || program == null) {
            return true;
        }
        PopupMenu popupMenu = new PopupMenu(activity, view);
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
                    final Recording recording = channel.getRecording();
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

    @Override
    public void onSearchRequested(String query) {
        // Start searching for programs on all channels
        Intent searchIntent = new Intent(activity, SearchActivity.class);
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
