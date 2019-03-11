package org.tvheadend.tvhclient.ui.features.channels;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.domain.entity.ChannelTag;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.domain.entity.Program;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.ui.base.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.ui.features.dialogs.ChannelTagSelectionDialog;
import org.tvheadend.tvhclient.ui.features.dialogs.GenreColorDialog;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment;
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.util.menu.PopupMenuUtil;
import org.tvheadend.tvhclient.util.menu.SearchMenuUtils;
import org.tvheadend.tvhclient.util.tasks.WakeOnLanTask;

import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import timber.log.Timber;

public class ChannelListFragment extends BaseFragment implements RecyclerViewClickCallback, ChannelDisplayOptionListener, SearchRequestInterface, Filter.FilterListener {

    @BindView(R.id.recycler_view)
    protected RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    protected ProgressBar progressBar;

    private ChannelRecyclerViewAdapter recyclerViewAdapter;
    private ChannelViewModel viewModel;
    private int selectedTimeOffset;
    private int selectedListPosition;
    private String searchQuery;
    private Unbinder unbinder;

    // Used in the time selection dialog to show a time entry every x hours.
    private final int intervalInHours = 2;
    private int programIdToBeEditedWhenBeingRecorded = 0;
    private List<ChannelTag> channelTags;
    private long selectedTime;

    private Runnable currentTimeUpdateTask;
    private final Handler currentTimeUpdateHandler = new Handler();

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

        recyclerViewAdapter = new ChannelRecyclerViewAdapter(isDualPane, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity.getApplicationContext(), LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        viewModel = ViewModelProviders.of(activity).get(ChannelViewModel.class);

        Timber.d("Observing selected time");
        viewModel.getSelectedTime().observe(getViewLifecycleOwner(), time -> {
            Timber.d("View model returned selected time " + time);
            if (time != null) {
                selectedTime = time;
            }
        });

        Timber.d("Observing channel tags");
        viewModel.getChannelTags().observe(getViewLifecycleOwner(), tags -> {
            if (tags != null) {
                Timber.d("View model returned " + tags.size() + " channel tags");
                channelTags = tags;
            }
        });

        Timber.d("Observing channels");
        viewModel.getChannels().observe(getViewLifecycleOwner(), channels -> {
            if (channels != null) {
                Timber.d("View model returned " + channels.size() + " channels");
                recyclerViewAdapter.addItems(channels);
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
        });

        // Get all recordings for the given channel to check if it belongs to a certain program
        // so the recording status of the particular program can be updated. This is required
        // because the programs are not updated automatically when recordings change.
        Timber.d("Observing recordings");
        viewModel.getAllRecordings().observe(getViewLifecycleOwner(), recordings -> {
            if (recordings != null) {
                Timber.d("View model returned " + recordings.size() + " recordings");
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

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        currentTimeUpdateTask = () -> {
            long currentTime = System.currentTimeMillis();
            Timber.d("Checking if selected time " + selectedTime + " is past current time " + currentTime);
            if (selectedTime < currentTime) {
                Timber.d("Updated selected time to current time");
                viewModel.setSelectedTime(currentTime);
            }
            currentTimeUpdateHandler.postDelayed(currentTimeUpdateTask, 60000);
        };
        currentTimeUpdateHandler.post(currentTimeUpdateTask);
    }

    private void showChannelTagOrChannelCount() {
        // Show either all channels or the name of the selected
        // channel tag and the channel count in the toolbar
        String toolbarTitle = viewModel.getSelectedChannelTagName(activity);
        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setTitle(toolbarTitle);
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
        // of the channel list have changed.
        //noinspection ConstantConditions
        viewModel.setChannelSortOrder(Integer.valueOf(sharedPreferences.getString("channel_sort_order", activity.getResources().getString(R.string.pref_default_channel_sort_order))));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.channel_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        final boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled",
                activity.getResources().getBoolean(R.bool.pref_default_genre_colors_for_channels_enabled));
        final boolean showChannelTagMenu = sharedPreferences.getBoolean("channel_tag_menu_enabled",
                activity.getResources().getBoolean(R.bool.pref_default_channel_tag_menu_enabled));

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

        menu.findItem(R.id.menu_wol).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags:
                return ChannelTagSelectionDialog.showDialog(activity, channelTags, appRepository.getChannelData().getItems().size(), this);

            case R.id.menu_timeframe:
                return menuUtils.handleMenuTimeSelection(selectedTimeOffset, intervalInHours, 12, this);

            case R.id.menu_genre_color_info_channels:
                return GenreColorDialog.showDialog(activity);

            case R.id.menu_sort_order:
                return menuUtils.handleMenuChannelSortOrderSelection(this);

            case R.id.menu_wol:
                Connection connection = appRepository.getConnectionData().getActiveItem();
                new WakeOnLanTask(activity, connection).execute();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTimeSelected(int which) {
        selectedTimeOffset = which;
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        // Add the selected list index as extra hours to the current time.
        // If the first index was selected then use the current time.
        long timeInMillis = System.currentTimeMillis();
        timeInMillis += (1000 * 60 * 60 * which * intervalInHours);
        viewModel.setSelectedTime(timeInMillis);
    }

    @Override
    public void onChannelTagIdsSelected(@NonNull Set<Integer> ids) {
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        viewModel.setSelectedChannelTagIds(ids);
    }

    @Override
    public void onChannelSortOrderSelected(int id) {
        viewModel.setChannelSortOrder(id);
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
        if (channel == null || !isVisible()
                || !activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            return;
        }

        FragmentManager fm = activity.getSupportFragmentManager();
        if (!isDualPane) {
            // Show the fragment to display the program list of the selected channel.
            Bundle bundle = new Bundle();
            bundle.putString("channelName", channel.getName());
            bundle.putInt("channelId", channel.getId());
            bundle.putLong("selectedTime", selectedTime);

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
                fragment = ProgramListFragment.newInstance(channel.getName(), channel.getId(), selectedTime);
                fm.beginTransaction()
                        .replace(R.id.details, fragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            } else {
                ((ProgramListFragment) fragment).updatePrograms(selectedTime);
            }
        }
    }

    private void showPopupMenu(View view, int position) {
        Channel channel = recyclerViewAdapter.getItem(position);
        if (activity == null || channel == null) {
            return;
        }

        Program program = appRepository.getProgramData().getItemById(channel.getProgramId());
        Recording recording = appRepository.getRecordingData().getItemByEventId(channel.getProgramId());

        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());

        PopupMenuUtil.prepareMenu(activity, popupMenu.getMenu(), program, recording, isNetworkAvailable, htspVersion, isUnlocked);
        PopupMenuUtil.prepareSearchMenu(popupMenu.getMenu(), channel.getProgramTitle(), isNetworkAvailable);
        popupMenu.getMenu().findItem(R.id.menu_play).setVisible(isNetworkAvailable);

        popupMenu.setOnMenuItemClickListener(item -> {
            if (SearchMenuUtils.onMenuSelected(activity, item.getItemId(), channel.getProgramTitle())) {
                return true;
            }
            switch (item.getItemId()) {
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
                    if (program != null) {
                        return menuUtils.handleMenuAddNotificationSelection(program);
                    } else {
                        return false;
                    }

                default:
                    return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onSearchRequested(@NonNull String query) {
        searchQuery = query;
        recyclerViewAdapter.getFilter().filter(query, this);
    }

    @Override
    public boolean onSearchResultsCleared() {
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

    @NonNull
    @Override
    public String getQueryHint() {
        return getString(R.string.search_channels);
    }

    @Override
    public void onClick(@NonNull View view, int position) {
        //noinspection ConstantConditions
        if ((view.getId() == R.id.icon || view.getId() == R.id.icon_text)
                && Integer.valueOf(sharedPreferences.getString("channel_icon_action", activity.getResources().getString(R.string.pref_default_channel_icon_action))) > 0
                && recyclerViewAdapter.getItemCount() > 0
                && isNetworkAvailable) {
            Channel channel = recyclerViewAdapter.getItem(position);
            menuUtils.handleMenuPlayChannelIcon(channel.getId());

        } else {
            showChannelDetails(position);
        }
    }

    @Override
    public boolean onLongClick(@NonNull View view, int position) {
        showPopupMenu(view, position);
        return true;
    }

    @Override
    public void onFilterComplete(int count) {
        showChannelTagOrChannelCount();
        // Preselect the first result item in the details screen
        if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
            showChannelDetails(0);
        }
    }
}
