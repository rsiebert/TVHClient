package org.tvheadend.tvhclient.features.channels;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import org.tvheadend.tvhclient.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.features.programs.ProgramListFragment;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTagSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTimeSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class ChannelListFragment extends BaseFragment implements RecyclerViewClickCallback, ChannelTimeSelectionCallback, ChannelTagSelectionCallback, SearchRequestInterface, Filter.FilterListener {

    private ChannelRecyclerViewAdapter recyclerViewAdapter;
    @BindView(R.id.recycler_view)
    protected RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    protected ProgressBar progressBar;
    private int selectedTimeOffset;
    private int selectedListPosition;
    private String searchQuery;
    private ChannelViewModel viewModel;
    private Unbinder unbinder;

    // Used in the time selection dialog to show a time entry every x hours.
    private final int intervalInHours = 2;
    private int programIdToBeEditedWhenBeingRecorded = 0;

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
        Timber.d("start");

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("listPosition", 0);
            selectedTimeOffset = savedInstanceState.getInt("timeOffset");
            searchQuery = savedInstanceState.getString(SearchManager.QUERY);
        } else {
            selectedListPosition = 0;
            selectedTimeOffset = 0;
            Bundle bundle = getArguments();
            if (bundle != null) {
                searchQuery = bundle.getString(SearchManager.QUERY);
            }
        }

        recyclerViewAdapter = new ChannelRecyclerViewAdapter(activity, isDualPane, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        viewModel = ViewModelProviders.of(activity).get(ChannelViewModel.class);
        viewModel.getChannels().observe(this, channels -> {
            Timber.d("observe start");
            if (channels != null) {
                recyclerViewAdapter.addItems(channels);
            }
            if (!TextUtils.isEmpty(searchQuery)) {
                recyclerViewAdapter.getFilter().filter(searchQuery, this);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            showChannelTagOrChannelCount();

            if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
                showChannelDetails(selectedListPosition);
            }
            Timber.d("observe end");
        });
        // Get all recordings for the given channel to check if it belongs to a certain program
        // so the recording status of the particular program can be updated. This is required
        // because the programs are not updated automatically when recordings change.
        viewModel.getAllRecordings().observe(this, recordings -> {
            if (recordings != null) {
                recyclerViewAdapter.addRecordings(recordings);
                for (Recording recording : recordings) {
                    // Show the edit recording screen of the scheduled recording
                    // in case the user has selected the record and edit menu item.
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
        });

        Timber.d("end");
    }

    private void showChannelTagOrChannelCount() {
        // Show either all channels or the name of the selected
        // channel tag and the channel count in the toolbar
        ChannelTag channelTag = viewModel.getChannelTag();

        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setTitle((channelTag == null) ?
                    getString(R.string.all_channels) : channelTag.getTagName());
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.items,
                    recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        } else {
            toolbarInterface.setTitle(getString(R.string.search_results));
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.channels,
                    recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("listPosition", selectedListPosition);
        outState.putInt("timeOffset", selectedTimeOffset);
        outState.putString(SearchManager.QUERY, searchQuery);
    }

    @Override
    public void onResume() {
        super.onResume();
        // When the user returns from the settings only the onResume method is called, not the
        // onActivityCreated, so we need to check if any values that affect the representation
        // of the channel list have changed. This can be the case when returning from the
        // settings screen or when a channel tag has changed from another screen.
        viewModel.checkAndUpdateChannels();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.channel_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", false);
        final boolean showChannelTagMenu = sharedPreferences.getBoolean("channel_tag_menu_enabled", true);

        if (TextUtils.isEmpty(searchQuery)) {
            menu.findItem(R.id.menu_genre_color_info_channels).setVisible(showGenreColors);
            menu.findItem(R.id.menu_timeframe).setVisible(isUnlocked);
            menu.findItem(R.id.menu_search).setVisible((recyclerViewAdapter.getItemCount() > 0));

            // Prevent the channel tag menu item from going into the overlay menu
            if (showChannelTagMenu) {
                menu.findItem(R.id.menu_tags).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        } else {
            menu.findItem(R.id.menu_genre_color_info_channels).setVisible(false);
            menu.findItem(R.id.menu_timeframe).setVisible(false);
            menu.findItem(R.id.menu_search).setVisible(false);
            menu.findItem(R.id.menu_tags).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags:
                return menuUtils.handleMenuChannelTagsSelection(viewModel.getChannelTagId(), this);

            case R.id.menu_timeframe:
                return menuUtils.handleMenuTimeSelection(selectedTimeOffset, intervalInHours, 12, this);

            case R.id.menu_genre_color_info_channels:
                return menuUtils.handleMenuGenreColorSelection();

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
        long timeInMillis = Calendar.getInstance().getTimeInMillis();
        timeInMillis += (1000 * 60 * 60 * which * intervalInHours);
        viewModel.setSelectedTime(timeInMillis);
    }

    @Override
    public void onChannelTagIdSelected(int id) {
        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        viewModel.setChannelTagId(id);
    }

    /**
     * Show the program list when a channel was selected. In single pane mode a separate
     * activity is called. In dual pane mode a list fragment will be shown in the right side.
     *
     * @param position The selected position in the list
     */
    private void showChannelDetails(int position) {
        selectedListPosition = position;
        recyclerViewAdapter.setPosition(position);
        Channel channel = recyclerViewAdapter.getItem(position);
        if (channel == null) {
            return;
        }

        FragmentManager fm = activity.getSupportFragmentManager();
        if (!isDualPane) {
            // Show the fragment to display the program list of the selected channel.
            Bundle bundle = new Bundle();
            bundle.putString("channelName", channel.getName());
            bundle.putInt("channelId", channel.getId());
            bundle.putLong("selectedTime", viewModel.getSelectedTime());

            ProgramListFragment fragment = new ProgramListFragment();
            fragment.setArguments(bundle);
            fm.beginTransaction()
                    .replace(R.id.main, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            // Check if an instance of the program list fragment for the selected channel is
            // already available. If an instance exist already then update the selected time
            // that was selected from the channel list.
            Fragment fragment = fm.findFragmentById(R.id.details);
            if (!(fragment instanceof ProgramListFragment)
                    || ((ProgramListFragment) fragment).getShownChannelId() != channel.getId()) {
                fragment = ProgramListFragment.newInstance(channel.getName(), channel.getId(), viewModel.getSelectedTime());
                fm.beginTransaction()
                        .replace(R.id.details, fragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            } else {
                ((ProgramListFragment) fragment).updatePrograms(viewModel.getSelectedTime());
            }
        }
    }

    private void showPopupMenu(View view) {

        Channel channel = (Channel) view.getTag();
        Program program = appRepository.getProgramData().getItemById(channel.getProgramId());
        Recording recording = appRepository.getRecordingData().getItemByEventId(channel.getProgramId());

        if (activity == null) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());
        menuUtils.onPreparePopupMenu(popupMenu.getMenu(), program, recording, isNetworkAvailable);
        menuUtils.onPreparePopupSearchMenu(popupMenu.getMenu(), isNetworkAvailable);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    return menuUtils.handleMenuSearchImdbWebsite(channel.getProgramTitle());

                case R.id.menu_search_fileaffinity:
                    return menuUtils.handleMenuSearchFileAffinityWebsite(channel.getProgramTitle());

                case R.id.menu_search_youtube:
                    return menuUtils.handleMenuSearchYoutube(channel.getProgramTitle());

                case R.id.menu_search_google:
                    return menuUtils.handleMenuSearchGoogle(channel.getProgramTitle());

                case R.id.menu_search_epg:
                    return menuUtils.handleMenuSearchEpgSelection(channel.getProgramTitle(), channel.getId());

                case R.id.menu_record_stop:
                    return menuUtils.handleMenuStopRecordingSelection(recording, null);

                case R.id.menu_record_cancel:
                    return menuUtils.handleMenuCancelRecordingSelection(recording, null);

                case R.id.menu_record_remove:
                    return menuUtils.handleMenuRemoveRecordingSelection(recording, null);

                case R.id.menu_record_once:
                    return menuUtils.handleMenuRecordSelection(channel.getProgramId());

                case R.id.menu_record_once_and_edit:
                    programIdToBeEditedWhenBeingRecorded = channel.getProgramId();
                    return menuUtils.handleMenuRecordSelection(channel.getProgramId());

                case R.id.menu_record_once_custom_profile:
                    return menuUtils.handleMenuCustomRecordSelection(channel.getProgramId(), channel.getId());

                case R.id.menu_record_series:
                    return menuUtils.handleMenuSeriesRecordSelection(channel.getProgramTitle());

                case R.id.menu_play:
                    return menuUtils.handleMenuPlayChannel(channel.getId());

                case R.id.menu_cast:
                    return menuUtils.handleMenuCast("channelId", channel.getId());

                case R.id.menu_add_notification:
                    return menuUtils.handleMenuAddNotificationSelection(program);

                default:
                    return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onSearchRequested(String query) {
        Timber.d("Searching for " + query);
        searchQuery = query;
        recyclerViewAdapter.getFilter().filter(query, this);
    }

    @Override
    public boolean onSearchResultsCleared() {
        Timber.d("Clearing search results");
        if (!TextUtils.isEmpty(searchQuery)) {
            Timber.d("Search result not empty, clearing filter and returning true");
            searchQuery = "";
            recyclerViewAdapter.getFilter().filter("", this);
            return true;
        } else {
            Timber.d("Search results empty, returning false");
            return false;
        }
    }

    @Override
    public void onClick(View view, int position) {
        if (view.getId() == R.id.icon || view.getId() == R.id.icon_text) {
            if (recyclerViewAdapter.getItemCount() > 0) {
                Channel channel = recyclerViewAdapter.getItem(position);
                menuUtils.handleMenuPlayChannel(channel.getId());
            }
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
        showChannelTagOrChannelCount();
    }
}
