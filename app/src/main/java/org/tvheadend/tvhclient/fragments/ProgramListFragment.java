package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.ProgramListAdapter;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.intent.PlayIntent;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProgramListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    private final static String TAG = ProgramListFragment.class.getSimpleName();

    private Activity activity;
    private ActionBarInterface actionBarInterface;
    private FragmentStatusInterface fragmentStatusInterface;

    private ProgramListAdapter adapter;
    private ListView listView;
    private Channel channel;
    private boolean isDualPane = false;
    private long showProgramsFromTime;

    // Prevents loading more data on each scroll event. Only when scrolling has
    // stopped loading shall be allowed
    private boolean allowLoading = false;

    private TVHClientApplication app;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // If the view group does not exist, the fragment would not be shown. So
        // we can return anyway.
        if (container == null) {
            return null;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            channel = app.getChannel(bundle.getLong(Constants.BUNDLE_CHANNEL_ID, 0));
            isDualPane = bundle.getBoolean(Constants.BUNDLE_DUAL_PANE, false);
            showProgramsFromTime = bundle.getLong(Constants.BUNDLE_SHOW_PROGRAMS_FROM_TIME, new Date().getTime());
        }

        View v = inflater.inflate(R.layout.list_layout, container, false);
        listView = (ListView) v.findViewById(R.id.item_list);
        return v;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }

        // If the channel is null exit
        if (channel == null) {
            activity.finish();
            return;
        }

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onListItemSelected(position, adapter.getItem(position), TAG);
                }
            }
        });

        List<Program> list = new ArrayList<>();
        adapter = new ProgramListAdapter(activity, list);
        listView.setAdapter(adapter);

        setHasOptionsMenu(true);
        registerForContextMenu(listView);
    }

    /**
     * Activated the scroll listener to more programs can be loaded when the end
     * of the program list has been reached.
     */
    private void enableScrollListener() {
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                app.log(TAG, "Scrolling, first item " + firstVisibleItem + ", visible items " + visibleItemCount + ", total item " + totalItemCount);
                // Enable loading when the user has scrolled pretty much to the end of the list
                if ((++firstVisibleItem + visibleItemCount) > totalItemCount) {
                    allowLoading = true;
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If loading is allowed and the scrolling has stopped, load more data 
                if (scrollState == SCROLL_STATE_IDLE && allowLoading) {
                    app.log(TAG, "Scrolling stopped");
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
        app.addListener(this);
        if (!app.isLoading()) {
            populateList();
        }
    }
    
    /**
     * Fills the adapter with all program that are part of the given channel
     */
    private void populateList() {
        // This is required because addAll is only available in API 11 and higher
        if (channel != null) {
            CopyOnWriteArrayList<Program> epg = new CopyOnWriteArrayList<>(channel.epg);

            int availableProgramCount = epg.size();
            boolean currentProgramFound = false;

            // Search through the EPG and find the first program that is currently running.
            // Also count how many programs are available without counting the ones in the past.
            for (Program p : epg) {
                if (p.start.getTime() >= showProgramsFromTime ||
                        p.stop.getTime() >= showProgramsFromTime) {
                    currentProgramFound = true;
                    adapter.add(p);
                } else {
                    availableProgramCount--;
                }
            }

            if (!currentProgramFound || availableProgramCount < Constants.PROGRAMS_VISIBLE_BEFORE_LOADING_MORE) {
                Log.d(TAG, "Channel '" + channel.name
                        + "', loading programs, current program exists: "
                        + currentProgramFound + ", epg program count: "
                        + availableProgramCount);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.moreDataRequired(channel, TAG);
                }
            }
        }
        adapter.notifyDataSetChanged();

        // Inform the activity to show the currently visible number of the
        // programs and that the program list has been filled with data.
        if (actionBarInterface != null && channel != null) {
            actionBarInterface.setActionBarTitle(channel.name);
            String items = getResources().getQuantityString(R.plurals.programs, adapter.getCount(), adapter.getCount());
            actionBarInterface.setActionBarSubtitle(items);
            if (!isDualPane) {
                if (Utils.showChannelIcons(activity)) {
                    actionBarInterface.setActionBarIcon(channel.iconBitmap);
                } else {
                    actionBarInterface.setActionBarIcon(R.mipmap.ic_launcher);
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
        app.removeListener(this);
        listView.setOnScrollListener(null);
    }

    @Override
    public void onDetach() {
        fragmentStatusInterface = null;
        actionBarInterface = null;
        super.onDetach();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!getUserVisibleHint()) {
            return false;
        }
        // Get the currently selected program from the list where the context
        // menu has been triggered
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // Check for a valid adapter size and objects
        if (info == null || adapter == null || adapter.getCount() <= info.position) {
            return super.onContextItemSelected(item);
        }

        final Program program = adapter.getItem(info.position);

        // Check if the context menu call came from the list in this fragment
        // (needed for support for multiple fragments in one screen)
        if (getView() != null && info.targetView.getParent() != getView().findViewById(R.id.item_list)) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, program.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, program.title));
            return true;

        case R.id.menu_record_remove:
            Utils.confirmRemoveRecording(activity, program.recording);
            return true;

        case R.id.menu_record_cancel:
            Utils.confirmCancelRecording(activity, program.recording);
            return true;

        case R.id.menu_record_once:
            Utils.recordProgram(activity, program, false);
            return true;

        case R.id.menu_record_once_custom_profile:
            // Create the list of available recording profiles that the user can select from
            String[] dvrConfigList = new String[app.getDvrConfigs().size()];
            for (int i = 0; i < app.getDvrConfigs().size(); i++) {
                dvrConfigList[i] = app.getDvrConfigs().get(i).name;
            }

            // Get the selected recording profile to highlight the 
            // correct item in the list of the selection dialog
            int dvrConfigNameValue = 0;
            DatabaseHelper dbh = DatabaseHelper.getInstance(activity);
            final Connection conn = dbh.getSelectedConnection();
            final Profile p = dbh.getProfile(conn.recording_profile_id);
            if (p != null) {
                for (int i = 0; i < dvrConfigList.length; i++) {
                    if (dvrConfigList[i].equals(p.name)) {
                        dvrConfigNameValue = i;
                        break;
                    }
                }
            }

            // Create new variables because the dialog needs them as final
            final String[] dcList = dvrConfigList;

            // Create the dialog to show the available profiles
            new MaterialDialog.Builder(activity)
            .title(R.string.select_dvr_config)
            .items(dvrConfigList)
            .itemsCallbackSingleChoice(dvrConfigNameValue, new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                    // Pass over the 
                    Intent intent = new Intent(activity, HTSService.class);
                    intent.setAction(Constants.ACTION_ADD_DVR_ENTRY);
                    intent.putExtra("eventId", program.id);
                    intent.putExtra("channelId", program.channel.id);
                    intent.putExtra("configName", dcList[which]);
                    activity.startService(intent);
                    return true;
                }
            })
            .show();
            return true;

        case R.id.menu_record_series:
            Utils.recordProgram(activity, program, true);
            return true;

        case R.id.menu_play:
            startActivity(new PlayIntent(activity, program));
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        activity.getMenuInflater().inflate(R.menu.program_context_menu, menu);
        
        // Get the currently selected program from the list where the context
        // menu has been triggered
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Program program = adapter.getItem(info.position);
        
        // Set the title of the context menu and show or hide 
        // the menu items depending on the program state
        if (program != null) {
            menu.setHeaderTitle(program.title);
            Utils.setProgramMenu(app, menu, program);
        }
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
            startActivity(new PlayIntent(activity, channel));
            return true;

        case R.id.menu_genre_color_info_programs:
            Utils.showGenreColorDialog(activity);
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
            case Constants.ACTION_PROGRAM_ADD:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Program p = (Program) obj;
                        if (channel != null && p.channel.id == channel.id) {
                            adapter.add(p);
                            adapter.notifyDataSetChanged();
                            adapter.sort();
                            if (actionBarInterface != null) {
                                String items = getResources().getQuantityString(R.plurals.programs, adapter.getCount(), adapter.getCount());
                                actionBarInterface.setActionBarSubtitle(items);
                            }
                        }
                    }
                });
                break;
            case Constants.ACTION_PROGRAM_DELETE:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.remove((Program) obj);
                        adapter.notifyDataSetChanged();
                        if (actionBarInterface != null) {
                            String items = getResources().getQuantityString(R.plurals.programs, adapter.getCount(), adapter.getCount());
                            actionBarInterface.setActionBarSubtitle(items);
                        }
                    }
                });
                break;
            case Constants.ACTION_PROGRAM_UPDATE:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.update((Program) obj);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
            case Constants.ACTION_DVR_UPDATE:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Recording rec = (Recording) obj;
                        for (Program p : adapter.getList()) {
                            if (rec == p.recording) {
                                adapter.update(p);
                                adapter.notifyDataSetChanged();
                                return;
                            }
                        }
                    }
                });
                break;
            case Constants.ACTION_DVR_ADD:
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
        if (listView != null && listView.getCount() > position && position >= 0) {
            listView.setSelectionFromTop(position, index);
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
}
