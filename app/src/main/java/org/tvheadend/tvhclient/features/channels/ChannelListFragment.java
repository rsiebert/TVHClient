package org.tvheadend.tvhclient.features.channels;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.playback.PlayChannelActivity;
import org.tvheadend.tvhclient.features.programs.ProgramListActivity;
import org.tvheadend.tvhclient.features.programs.ProgramListFragment;
import org.tvheadend.tvhclient.features.search.SearchActivity;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTagSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTimeSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.Calendar;
import java.util.Date;

import timber.log.Timber;

// TODO show programs from time in dual pane, program list not updated
// TODO use recycler view filter for search by channel name
// TODO sorting should consider minor major channel numbers
// TODO use the channel tag from the server status

public class ChannelListFragment extends BaseFragment implements RecyclerViewClickCallback, ChannelTimeSelectionCallback, ChannelTagSelectionCallback, SearchRequestInterface, Filter.FilterListener {//}, ChannelsLoadedCallback {

    protected ChannelRecyclerViewAdapter recyclerViewAdapter;
    protected RecyclerView recyclerView;
    protected ProgressBar progressBar;
    private int selectedTimeOffset;
    private int selectedListPosition;
    private String searchQuery;
    private ChannelViewModel viewModel;

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
            selectedListPosition = savedInstanceState.getInt("listPosition", 0);
            selectedTimeOffset = savedInstanceState.getInt("timeOffset");
            searchQuery = savedInstanceState.getString("searchQuery");
        } else {
            selectedListPosition = 0;
            selectedTimeOffset = 0;
            Bundle bundle = getArguments();
            if (bundle != null) {
                searchQuery = bundle.getString(SearchManager.QUERY);
            }
        }

        recyclerViewAdapter = new ChannelRecyclerViewAdapter(activity, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);
        /*
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
        */

        viewModel = ViewModelProviders.of(activity).get(ChannelViewModel.class);
        viewModel.getChannels().observe(this, channels -> {

            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            recyclerViewAdapter.addItems(channels);
            if (!TextUtils.isEmpty(searchQuery)) {
                recyclerViewAdapter.getFilter().filter(searchQuery, this);
            }

            // Show either all channels or the name of the selected
            // channel tag and the channel count in the toolbar
            ChannelTag channelTag = viewModel.getChannelTag();
            toolbarInterface.setTitle((channelTag == null) ? getString(R.string.all_channels) : channelTag.getTagName());
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.items,
                    recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        });

        if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
            showChannelDetails(selectedListPosition);
        }

        // Get all recordings for the given channel to check if it belongs to a certain program
        // so the recording status of the particular program can be updated. This is required
        // because the programs are not updated automatically when recordings change.
        viewModel.getAllRecordings().observe(this, recordings -> recyclerViewAdapter.addRecordings(recordings));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("listPosition", selectedListPosition);
        outState.putInt("timeOffset", selectedTimeOffset);
        outState.putString("searchQuery", searchQuery);
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
        menu.findItem(R.id.menu_search).setVisible((recyclerViewAdapter.getItemCount() > 0));

        // Prevent the channel tag menu item from going into the overlay menu
        if (showChannelTagMenu) {
            menu.findItem(R.id.menu_tags).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags:
                menuUtils.handleMenuChannelTagsSelection(viewModel.getChannelTagId(), this);
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

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        // Add the selected list index as extra hours to the current time.
        // If the first index was selected then use the current time.
        Calendar c = Calendar.getInstance();
        if (which > 0) {
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + which);
            viewModel.setSelectedTime(c.getTimeInMillis());
        } else {
            viewModel.setSelectedTime(new Date().getTime());
        }
    }

    @Override
    public void onChannelTagIdSelected(int id) {
        viewModel.setChannelTagId(id);
        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
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
            intent.putExtra("selectedTime", viewModel.getSelectedTime());
            activity.startActivity(intent);
        } else {
            FragmentManager fm = getFragmentManager();
            if (fm != null) {
                // Check if the fragment for the selected channel is already shown, if not replace it with a new fragment.
                ProgramListFragment programListFragment = (ProgramListFragment) fm.findFragmentById(R.id.details);
                if (programListFragment == null || programListFragment.getShownChannelId() != channel.getId()) {
                    programListFragment = ProgramListFragment.newInstance(channel.getName(), channel.getId(), viewModel.getSelectedTime());
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
        Program program = appRepository.getProgramData().getItemById(channel.getProgramId());
        Recording recording = appRepository.getRecordingData().getItemByEventId(channel.getProgramId());

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
                    Intent intent = new Intent(activity, PlayChannelActivity.class);
                    intent.putExtra("channelId", channel.getId());
                    activity.startActivity(intent);
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
        // Start searching for programs on all channels
        Intent searchIntent = new Intent(activity, SearchActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra("type", "channels");
        startActivity(searchIntent);
    }

    @Override
    public void onClick(View view, int position) {
        if (view.getId() == R.id.icon || view.getId() == R.id.icon_text) {
            Channel channel = recyclerViewAdapter.getItem(position);
            Intent intent = new Intent(activity, PlayChannelActivity.class);
            intent.putExtra("channelId", channel.getId());
            activity.startActivity(intent);
        } else {
            showChannelDetails(position);
        }
    }

    @Override
    public void onLongClick(View view, int position) {
        showPopupMenu(view);
    }

    @Override
    public void onFilterComplete(int count) {
        // Show either all channels or the name of the selected
        // channel tag and the channel count in the toolbar
        ChannelTag channelTag = viewModel.getChannelTag();
        toolbarInterface.setTitle((channelTag == null) ? getString(R.string.all_channels) : channelTag.getTagName());
        toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.items,
                recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
    }
}
