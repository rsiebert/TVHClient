package org.tvheadend.tvhclient.ui.programs;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.ProgramWithRecordingsAndChannels;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.sync.EpgSyncService;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.ui.common.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.ui.search.SearchActivity;
import org.tvheadend.tvhclient.ui.search.SearchRequestInterface;

import java.util.ArrayList;
import java.util.Date;

// TODO search menu not shown in single pane list view, check dual pane
// TODO use contraintlayout

public class ProgramListFragment extends BaseFragment implements RecyclerViewClickCallback, BottomReachedListener, SearchRequestInterface {
    private String TAG = getClass().getSimpleName();

    private ProgramRecyclerViewAdapter recyclerViewAdapter;
    private RecyclerView recyclerView;
    private long showProgramsFromTime;
    private int selectedListPosition;
    private int channelId;
    private String channelName;

    public static ProgramListFragment newInstance(String channelName, int channelId, long showProgramsFromTime) {
        ProgramListFragment f = new ProgramListFragment();
        Bundle args = new Bundle();
        args.putString("channelName", channelName);
        args.putInt("channelId", channelId);
        args.putLong("show_programs_from_time", showProgramsFromTime);
        f.setArguments(args);
        return f;
    }

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
            channelName = bundle.getString("channelName");
            channelId = bundle.getInt("channelId", 0);
            showProgramsFromTime = bundle.getLong("show_programs_from_time", new Date().getTime());
        }
        if (savedInstanceState != null) {
            channelName = savedInstanceState.getString("channelName");
            channelId = savedInstanceState.getInt("channelId", 0);
            showProgramsFromTime = savedInstanceState.getLong("show_programs_from_time", new Date().getTime());
            selectedListPosition = savedInstanceState.getInt("list_position", 0);
        }

        toolbarInterface.setTitle(channelName);

        recyclerViewAdapter = new ProgramRecyclerViewAdapter(activity, new ArrayList<>(), this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        ProgramViewModel viewModel = ViewModelProviders.of(activity).get(ProgramViewModel.class);
        viewModel.getPrograms(channelId, showProgramsFromTime).observe(this, programs -> {
            Log.d(TAG, "onActivityCreated: observe programs");

            recyclerViewAdapter.addItems(programs);
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.programs, recyclerViewAdapter.getItemCount(), recyclerViewAdapter.getItemCount()));

            if (isDualPane && recyclerViewAdapter.getItemCount() > 0) {
                showProgramDetails(selectedListPosition);
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("channelName", channelName);
        outState.putInt("channelId", channelId);
        outState.putLong("show_programs_from_time", showProgramsFromTime);
        outState.putInt("list_position", selectedListPosition);
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
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsProgramsPref", false);
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
                menuUtils.handleMenuPlaySelection(channelId, -1);
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
        Program program = recyclerViewAdapter.getItem(position).getProgram();
        if (program == null) {
            return;
        }
        // Launch a new activity to display the program list of the selected channelTextView.
        Intent intent = new Intent(activity, ProgramDetailsActivity.class);
        intent.putExtra("eventId", program.getEventId());
        activity.startActivity(intent);
    }

    @Override
    public boolean onLongClick(View view) {
        final Program program = ((ProgramWithRecordingsAndChannels) view.getTag()).getProgram();
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
                    menuUtils.handleMenuSearchEpgSelection(program.getTitle(), channelId);
                    return true;
                case R.id.menu_record_remove:
                    Recording rec = DataStorage.getInstance().getRecordingFromArray(program.getDvrId());
                    if (rec != null) {
                        if (rec.isRecording()) {
                            menuUtils.handleMenuStopRecordingSelection(rec.getId(), rec.getTitle());
                        } else if (rec.isScheduled()) {
                            menuUtils.handleMenuCancelRecordingSelection(rec.getId(), rec.getTitle(), null);
                        } else {
                            menuUtils.handleMenuRemoveRecordingSelection(rec.getId(), rec.getTitle(), null);
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
                    menuUtils.handleMenuPlaySelection(channelId, -1);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
        return true;
    }

    @Override
    public void onClick(View view, int position) {
        showProgramDetails(position);
    }

    public int getShownChannelId() {
        return channelId;
    }

    @Override
    public void onSearchRequested(String query) {
        // Start searching for programs on the given channelTextView
        Intent searchIntent = new Intent(activity, SearchActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra("type", "programs");
        // Pass a the bundle. The contents will be forwarded to the
        // fragment that is responsible for displaying the search results
        Bundle bundle = new Bundle();
        bundle.putLong("channel_id", channelId);
        searchIntent.putExtra(SearchManager.APP_DATA, bundle);
        startActivity(searchIntent);
    }

    @Override
    public void onBottomReached(int position) {
        Program lastProgram = recyclerViewAdapter.getItem(position).getProgram();
        Log.d(TAG, "onBottomReached: last program was " + lastProgram.getTitle());
        Intent intent = new Intent(activity, EpgSyncService.class);
        intent.setAction("getEvents");
        intent.putExtra("eventId", lastProgram.getNextEventId());
        intent.putExtra("channelId", lastProgram.getChannelId());
        intent.putExtra("count", 15);
        activity.startService(intent);
    }
}
