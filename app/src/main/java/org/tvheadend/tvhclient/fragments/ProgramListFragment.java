package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
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
import org.tvheadend.tvhclient.adapter.ProgramListAdapter;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProgramListFragment extends ListFragment implements HTSListener, FragmentControlInterface, OnItemClickListener, AdapterView.OnItemLongClickListener {

    private final static String TAG = ProgramListFragment.class.getSimpleName();

    private Activity activity;
    private ToolbarInterface toolbarInterface;
    private FragmentStatusInterface fragmentStatusInterface;
    private ProgramListAdapter adapter;
    private Channel channel;
    private boolean isDualPane = false;
    private long showProgramsFromTime;

    // Prevents loading more data on each scroll event. Only when scrolling has
    // stopped loading shall be allowed
    private boolean allowLoading = false;

    private MenuUtils menuUtils;
    private SharedPreferences sharedPreferences;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = getActivity().findViewById(R.id.right_fragment);
        isDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        Bundle bundle = getArguments();
        if (bundle != null) {
            channel = DataStorage.getInstance().getChannelFromArray(bundle.getInt("channelId", 0));
            showProgramsFromTime = bundle.getLong("show_programs_from_time", new Date().getTime());
        }

        // If the channel is null exit
        if (channel == null) {
            activity.finish();
            return;
        }

        menuUtils = new MenuUtils(getActivity());

        List<Program> list = new ArrayList<>();
        adapter = new ProgramListAdapter(activity, list);
        setListAdapter(adapter);
        getListView().setFastScrollEnabled(true);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        setHasOptionsMenu(true);
    }

    /**
     * Activated the scroll listener to more programs can be loaded when the end
     * of the program list has been reached.
     */
    private void enableScrollListener() {
        getListView().setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // Enable loading when the user has scrolled pretty much to the end of the list
                if ((++firstVisibleItem + visibleItemCount) > totalItemCount) {
                    allowLoading = true;
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If loading is allowed and the scrolling has stopped, load more data 
                if (scrollState == SCROLL_STATE_IDLE && allowLoading) {
                    allowLoading = false;
                    if (fragmentStatusInterface != null) {
                        fragmentStatusInterface.moreDataRequired(channel, TAG);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);
        if (!DataStorage.getInstance().isLoading()) {
            populateList();
        }
    }

    /**
     * Fills the adapter with all program that are part of the given channel
     */
    private void populateList() {
        // This is required because addAll is only available in API 11 and higher
        if (channel != null) {

            int availableProgramCount = 0;
            boolean currentProgramFound = false;

            Log.d(TAG, "populateList: channel " + channel.channelName + ", showProgramsFromTime " + showProgramsFromTime);
            int nextId = 0;
            for (Program program : DataStorage.getInstance().getProgramsFromArray().values()) {
                if (program.channelId == channel.channelId) {
                    if (program.start <= showProgramsFromTime && program.stop > showProgramsFromTime) {
                        Log.d(TAG, "populateList: adding program " + program.title);
                        adapter.add(program);
                        nextId = program.nextEventId;
                        currentProgramFound = true;
                        availableProgramCount++;
                        break;
                    }
                }
            }

            while (nextId != 0) {
                Log.d(TAG, "populateList: nextid " + nextId);
                Program p = DataStorage.getInstance().getProgramFromArray(nextId);
                if (p != null && p.nextEventId > 0) {
                    adapter.add(p);
                    availableProgramCount++;
                    nextId = p.nextEventId;
                } else {
                    nextId = 0;
                }
            }

            Log.d(TAG, "populateList: currentProgramFound " + currentProgramFound + ", availableProgramCount " + availableProgramCount);
            if (!currentProgramFound || availableProgramCount < Constants.PROGRAMS_VISIBLE_BEFORE_LOADING_MORE) {
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.moreDataRequired(channel, TAG);
                }
            }
        }

        adapter.sort();
        adapter.notifyDataSetChanged();

        // Inform the activity to show the currently visible number of the
        // programs and that the program list has been filled with data.
        if (toolbarInterface != null && channel != null) {
            toolbarInterface.setActionBarTitle(channel.channelName);
            String items = getResources().getQuantityString(R.plurals.programs, adapter.getCount(), adapter.getCount());
            toolbarInterface.setActionBarSubtitle(items);
            if (!isDualPane) {
                if (sharedPreferences.getBoolean("showIconPref", true)) {
                    Bitmap iconBitmap = MiscUtils.getCachedIcon(activity, channel.channelIcon);
                    toolbarInterface.setActionBarIcon(iconBitmap);
                } else {
                    toolbarInterface.setActionBarIcon(R.mipmap.ic_launcher);
                }
            }
        }
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
        enableScrollListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
        getListView().setOnScrollListener(null);
    }

    @Override
    public void onDestroy() {
        fragmentStatusInterface = null;
        toolbarInterface = null;
        super.onDestroy();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu in dual pane mode or if no genre colors shall be shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsProgramsPref", false);
        (menu.findItem(R.id.menu_genre_color_info_programs)).setVisible(!isDualPane && showGenreColors);
        (menu.findItem(R.id.menu_play)).setVisible(!isDualPane);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.program_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_play:
            // Open a new activity that starts playing the first program that is
            // currently transmitted over this channel
            menuUtils.handleMenuPlaySelection(channel.channelId, -1);
            return true;

        case R.id.menu_genre_color_info_programs:
            menuUtils.handleMenuGenreColorSelection();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
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
                        if (loading) {
                            adapter.clear();
                            adapter.notifyDataSetChanged();
                        } else {
                            populateList();
                        }
                    }
                });
                break;
            case "eventAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Program p = (Program) obj;
                        Channel channel = DataStorage.getInstance().getChannelFromArray(p.channelId);
                        if (channel != null) {
                            adapter.add(p);
                            adapter.notifyDataSetChanged();
                            adapter.sort();
                            if (toolbarInterface != null) {
                                String items = getResources().getQuantityString(R.plurals.programs, adapter.getCount(), adapter.getCount());
                                toolbarInterface.setActionBarSubtitle(items);
                            }
                        }
                    }
                });
                break;
            case "eventDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.remove((Program) obj);
                        adapter.notifyDataSetChanged();
                        if (toolbarInterface != null) {
                            String items = getResources().getQuantityString(R.plurals.programs, adapter.getCount(), adapter.getCount());
                            toolbarInterface.setActionBarSubtitle(items);
                        }
                    }
                });
                break;
            case "eventUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.update((Program) obj);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
            case "dvrEntryUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Recording rec = (Recording) obj;
                        for (Program p : adapter.getList()) {
                            if (rec != null && rec.isRecording() && rec.id == p.dvrId) {
                                adapter.update(p);
                                adapter.notifyDataSetChanged();
                                return;
                            }
                        }
                    }
                });
                break;
            case "dvrEntryAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

    @Override
    public void reloadData() {
        // NOP
    }
    @Override
    public void setSelection(int position, int index) {
        if (getListView().getCount() > position && position >= 0) {
            getListView().setSelectionFromTop(position, index);
        }
    }

    @Override
    public void setInitialSelection(int position) {
        setSelection(position, 0);
        // Simulate a click in the list item to inform the activity
        if (adapter != null && adapter.getCount() > position) {
            Program p = adapter.getItem(position);
            if (fragmentStatusInterface != null) {
                fragmentStatusInterface.onListItemSelected(position, p, TAG);
            }
        }
    }

    @Override
    public Object getSelectedItem() {
        return channel;
    }

    @Override
    public int getItemCount() {
        return adapter.getCount();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListItemSelected(position, adapter.getItem(position), TAG);
        }
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
                    menuUtils.handleMenuSearchEpgSelection(program.title, channel.channelId);
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
                    menuUtils.handleMenuCustomRecordSelection(program.eventId, channel.channelId);
                    return true;
                case R.id.menu_record_series:
                    menuUtils.handleMenuSeriesRecordSelection(program.title);
                    return true;
                case R.id.menu_play:
                    menuUtils.handleMenuPlaySelection(channel.channelId, -1);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
        return true;
    }
}
