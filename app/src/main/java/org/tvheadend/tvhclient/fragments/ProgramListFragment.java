package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.DetailsActivity;
import org.tvheadend.tvhclient.activities.ToolbarInterfaceLight;
import org.tvheadend.tvhclient.adapter.ProgramListAdapter;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.ArrayList;
import java.util.Date;

// TODO search menu not shown in single pane list view, check dual pane

public class ProgramListFragment extends ListFragment implements HTSListener, OnItemClickListener, AdapterView.OnItemLongClickListener, OnScrollListener {

    private Activity activity;
    private ToolbarInterfaceLight toolbarInterface;
    private ProgramListAdapter adapter;
    private boolean isDualPane;
    private long showProgramsFromTime;

    // Prevents loading more data on each scroll event.
    // Only when scrolling has stopped loading shall be allowed
    private boolean allowLoading = false;

    private MenuUtils menuUtils;
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();
        if (activity instanceof ToolbarInterfaceLight) {
            toolbarInterface = (ToolbarInterfaceLight) activity;
        }
        menuUtils = new MenuUtils(getActivity());

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = getActivity().findViewById(R.id.right_fragment);
        isDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
        setHasOptionsMenu(true);

        Bundle bundle = getArguments();
        if (bundle != null) {
            channelName = bundle.getString("channelName");
            channelId = bundle.getInt("channelId", 0);
            showProgramsFromTime = bundle.getLong("show_programs_from_time", new Date().getTime());
        }
        if (savedInstanceState != null) {
            channelName = savedInstanceState.getString("channelName");
            channelId = savedInstanceState.getInt("channelId", 0);
            showProgramsFromTime = bundle.getLong("show_programs_from_time", new Date().getTime());
            selectedListPosition = savedInstanceState.getInt("list_position", 0);
        }

        toolbarInterface.setTitle(channelName);
        adapter = new ProgramListAdapter(activity, new ArrayList<>());
        setListAdapter(adapter);
        getListView().setFastScrollEnabled(true);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
        getListView().setOnScrollListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
        inflater.inflate(R.menu.program_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu in dual pane mode or if no genre colors shall be shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsProgramsPref", false);
        menu.findItem(R.id.menu_genre_color_info_programs).setVisible(!isDualPane && showGenreColors);
        menu.findItem(R.id.menu_play).setVisible(!isDualPane);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
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

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);
        setListShown(!DataStorage.getInstance().isLoading());

        if (!DataStorage.getInstance().isLoading()) {
            populateList();
            if (adapter.getCount() < 15) {
                getMorePrograms();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
    }

    private void populateList() {
        // Clear the list and add the recordings
        adapter.clear();
        // Get all programs that belong to the given channel
        int nextId = 0;
        for (Program program : DataStorage.getInstance().getProgramsFromArray().values()) {
            if (program.channelId == channelId) {
                if (program.start <= showProgramsFromTime && program.stop > showProgramsFromTime) {
                    // Add the program that shall be shown first
                    adapter.add(program);
                    nextId = program.nextEventId;
                    break;
                }
            }
        }
        // Add the program with the given nextEventId to the adapter.
        // Do this in a loop until no nextEventId is available
        while (nextId != 0) {
            Program p = DataStorage.getInstance().getProgramFromArray(nextId);
            if (p != null && p.nextEventId > 0) {
                adapter.add(p);
                nextId = p.nextEventId;
            } else {
                nextId = 0;
            }
        }
        adapter.notifyDataSetChanged();
        // Show the number of recordings
        String items = getResources().getQuantityString(R.plurals.programs, adapter.getCount(), adapter.getCount());
        toolbarInterface.setSubtitle(items);
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the specified action will be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        switch (action) {
            case Constants.ACTION_LOADING:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        boolean loading = (Boolean) obj;
                        setListShown(!loading);
                        if (!loading) {
                            populateList();
                        }
                    }
                });
                break;
            case "eventAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Program p = (Program) obj;
                        if (p.channelId == channelId) {
                            adapter.add(p);
                            adapter.sort();
                            adapter.notifyDataSetChanged();
                            String items = getResources().getQuantityString(R.plurals.programs, adapter.getCount(), adapter.getCount());
                            toolbarInterface.setSubtitle(items);
                        }
                    }
                });
                break;
            case "eventDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Program p = (Program) obj;
                        // Get the position of the recording that is to be
                        // deleted so the previous one can be selected
                        if (--selectedListPosition < 0) {
                            selectedListPosition = 0;
                        }
                        adapter.remove(p);
                        adapter.notifyDataSetChanged();
                        String items = getResources().getQuantityString(R.plurals.programs, adapter.getCount(), adapter.getCount());
                        toolbarInterface.setSubtitle(items);
                        // Select the previous recording to show its details
                        if (isDualPane) {
                            showProgramDetails(selectedListPosition);
                        }
                    }
                });
                break;
            case "eventUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Program p = (Program) obj;
                        adapter.update(p);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        showProgramDetails(position);
    }

    protected void showProgramDetails(int position) {
        selectedListPosition = position;
        Program program = adapter.getItem(position);
        if (program == null) {
            return;
        }
        // Launch a new activity to display the program list of the selected channel.
        Intent intent = new Intent(getActivity(), DetailsActivity.class);
        intent.putExtra("eventId", program.eventId);
        intent.putExtra("type", "program");
        activity.startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        final Program program = adapter.getItem(position);
        if (getActivity() == null || program == null) {
            return true;
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.program_context_menu, popupMenu.getMenu());
        menuUtils.onPreparePopupMenu(popupMenu.getMenu(), program);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    menuUtils.handleMenuSearchWebSelection(program.title);
                    return true;
                case R.id.menu_search_epg:
                    menuUtils.handleMenuSearchEpgSelection(program.title, channelId);
                    return true;
                case R.id.menu_record_remove:
                    Recording rec = DataStorage.getInstance().getRecordingFromArray(program.dvrId);
                    if (rec != null) {
                        if (rec.isRecording()) {
                            menuUtils.handleMenuStopRecordingSelection(rec.id, rec.title);
                        } else if (rec.isScheduled()) {
                            menuUtils.handleMenuCancelRecordingSelection(rec.id, rec.title);
                        } else {
                            menuUtils.handleMenuRemoveRecordingSelection(rec.id, rec.title);
                        }
                    }
                    return true;
                case R.id.menu_record_once:
                    menuUtils.handleMenuRecordSelection(program.eventId);
                    return true;
                case R.id.menu_record_once_custom_profile:
                    menuUtils.handleMenuCustomRecordSelection(program.eventId, channelId);
                    return true;
                case R.id.menu_record_series:
                    menuUtils.handleMenuSeriesRecordSelection(program.title);
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
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // If loading is allowed and the scrolling has stopped, load more data
        if (scrollState == SCROLL_STATE_IDLE && allowLoading) {
            toolbarInterface.setSubtitle(getString(R.string.loading));
            getMorePrograms();
            allowLoading = false;
        }
    }

    private void getMorePrograms() {
        if (adapter.getCount() > 0) {
            Program lastProgram = adapter.getItem(adapter.getCount() - 1);
            Intent intent = new Intent(getActivity(), HTSService.class);
            intent.setAction("getEvents");
            intent.putExtra("eventId", lastProgram.nextEventId);
            intent.putExtra("channelId", lastProgram.channelId);
            intent.putExtra("count", 15);
            getActivity().startService(intent);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Enable loading when the user has scrolled pretty much to the end of the list
        if ((++firstVisibleItem + visibleItemCount) > totalItemCount) {
            allowLoading = true;
        }
    }

    public int getShownChannelId() {
        return channelId;
    }
}
