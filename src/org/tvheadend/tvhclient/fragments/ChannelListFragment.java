package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.ExternalActionActivity;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.ChannelListAdapter;
import org.tvheadend.tvhclient.adapter.ChannelTagListAdapter;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Program;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

public class ChannelListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    private final static String TAG = ChannelListFragment.class.getSimpleName();

    private Activity activity;
    private FragmentStatusInterface fragmentStatusInterface;
    private FragmentScrollInterface fragmentScrollInterface;
	private ActionBarInterface actionBarInterface;

    private ChannelListAdapter adapter;
    ArrayAdapter<ChannelTag> tagAdapter;
    private MaterialDialog tagDialog;
    private ListView listView;

    // The dialog that allows the user to select a certain time frame
    private MaterialDialog channelTimeDialog;

    // This is the default view for the channel list adapter. Other views can be
    // passed to the adapter to show less information. This is used in the
    // program guide where only the channel icon is relevant.
    private int viewLayout = R.layout.list_layout;
    private int adapterLayout = R.layout.channel_list_widget;

    // Enables scrolling when the user has touch the screen and starts
    // scrolling. When the user is done, scrolling will be disabled to prevent
    // unwanted calls to the interface. 
    private boolean enableScrolling = false;
    
    // Indication if the channel list shall be used in the program guide view.
    // In this mode it will only show the channels.
    private boolean showOnlyChannels = false;

    private boolean isDualPane = false;

    private TVHClientApplication app = null;

    private Runnable channelUpdateTask;
    private Handler channelUpdateHandler = new Handler();

    private int channelTimeSelection;
    private long showProgramsFromTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // If the view group does not exist, the fragment would not be shown. So
        // we can return anyway.
        if (container == null) {
            return null;
        }

        // Check if only channels without any program information shall be
        // visible. This is only the case when this fragment is part of the
        // program guide view.
        Bundle bundle = getArguments();
        if (bundle != null) {
            showOnlyChannels = bundle.getBoolean(Constants.BUNDLE_SHOWS_ONLY_CHANNELS, false);
            isDualPane  = bundle.getBoolean(Constants.BUNDLE_DUAL_PANE, false);
            channelTimeSelection = bundle.getInt(Constants.BUNDLE_CHANNEL_TIME_SELECTION);
            showProgramsFromTime = bundle.getLong(Constants.BUNDLE_SHOW_PROGRAMS_FROM_TIME);
        }
        // When only channels shall be seen, a reduced adapter layout and list
        // view layout is used.
        if (showOnlyChannels) {
            viewLayout = R.layout.program_guide_channel_list_layout;
            adapterLayout = R.layout.program_guide_channel_item;
        }

        View v = inflater.inflate(viewLayout, container, false);
        listView = (ListView) v.findViewById(R.id.item_list);
        return v;
    }

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
        if (activity instanceof FragmentScrollInterface) {
            fragmentScrollInterface = (FragmentScrollInterface) activity;
        }

        adapter = new ChannelListAdapter(activity, new ArrayList<Channel>(), adapterLayout);
        listView.setAdapter(adapter);

        // Inform the activity when a channel has been selected.
        if (!showOnlyChannels) {
            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final Channel ch = (Channel) adapter.getItem(position);
                    if (fragmentStatusInterface != null) {
                        fragmentStatusInterface.onListItemSelected(position, ch, TAG);
                    }
                    adapter.setPosition(position);
                    adapter.notifyDataSetChanged();
                }
            });
        }

        // Inform the activity when the channel list is scrolling or has
        // finished scrolling. This is only valid in the program guide
        // where only the channels are shown.
        if (showOnlyChannels) {
            listView.setOnScrollListener(new OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                        enableScrolling = true;
                    } else if (scrollState == SCROLL_STATE_IDLE && enableScrolling) {
                        if (fragmentScrollInterface != null) {
                            enableScrolling = false;
                            fragmentScrollInterface.onScrollStateIdle(TAG);
                        }
                    }
                }
                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (fragmentScrollInterface != null && enableScrolling) {
                        int position = view.getFirstVisiblePosition();
                        View v = view.getChildAt(0);
                        int offset = (v == null) ? 0 : v.getTop();
                        fragmentScrollInterface.onScrollingChanged(position, offset, TAG);
                    }
                }
            });
        }

        // Create the dialog that will list the available channel tags
        tagAdapter = new ChannelTagListAdapter(activity, new ArrayList<ChannelTag>());
        tagDialog = new MaterialDialog.Builder(activity)
        .title(R.string.tags)
        .adapter(tagAdapter, new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                Utils.setChannelTagId(activity, which);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.channelTagChanged(TAG);
                }
                tagDialog.dismiss();
            }
        })
        .build();

        // Disable the context menu when the channels are shown only. The
        // functionality behind the context menu shall not be available when the
        // program guide is displayed
        if (!showOnlyChannels) {
            registerForContextMenu(listView);
        }
        // Enable the action bar menu
        setHasOptionsMenu(true);

        // Initiate a timer that will update the adapter every minute
        // so that the progress bars will be displayed correctly
        // Also update the current adapter time if the current 
        // time was selected from the channel time dialog, otherwise
        // old programs will not be removed when they are over
        channelUpdateTask = new Runnable() {
            public void run() {
                if (channelTimeSelection == 0) {
                    adapter.setTime(new Date().getTime());
                }
                adapter.notifyDataSetChanged();
                channelUpdateHandler.postDelayed(channelUpdateTask, 60000);
            }
        };
    }

    @SuppressLint({ "InlinedApi", "NewApi" })
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsChannelsPref", false);
        (menu.findItem(R.id.menu_genre_color_info_channels)).setVisible(showGenreColors);

        // Playing a channel shall not be available in channel only mode or in
        // single pane mode, because no channel is preselected.
        if (!showOnlyChannels || !isDualPane) {
            (menu.findItem(R.id.menu_play)).setVisible(false);
        }

        (menu.findItem(R.id.menu_timeframe)).setVisible(app.isUnlocked());

        // Prevent the channel tag menu item from going into the overlay menu 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (prefs.getBoolean("visibleMenuIconTagsPref", true)) {
                menu.findItem(R.id.menu_tags).setShowAsActionFlags(
                        MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.channel_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_play:
            // Open a new activity to stream the current program to this device
            Intent intent = new Intent(activity, ExternalActionActivity.class);
            Channel channel = adapter.getSelectedItem();
            if (channel != null) {
                intent.putExtra(Constants.BUNDLE_CHANNEL_ID, channel.id);
                startActivity(intent);
            }
            return true;

        case R.id.menu_tags:
            tagDialog.show();
            return true;

        case R.id.menu_timeframe:
            createChannelTimeDialog();
            channelTimeDialog.show();
            return true;

        case R.id.menu_genre_color_info_channels:
            Utils.showGenreColorDialog(activity);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (getUserVisibleHint() == false) {
            return false;
        }

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        // Check for a valid adapter, its size and if the context menu call came
        // from the list in this fragment (needed to support multiple fragments
        // in one screen)
        if (info == null || adapter == null || (adapter.getCount() <= info.position)
                || (info.targetView.getParent() != getView().findViewById(R.id.item_list))) {
            return super.onContextItemSelected(item);
        }

        // Get the currently selected channel. Also get the program that is
        // currently being transmitting by this channel.
        Program program = null;
        final Channel channel = adapter.getItem(info.position);
        if (channel != null) {
            synchronized(channel.epg) {
                Iterator<Program> it = channel.epg.iterator();
                if (channel.isTransmitting && it.hasNext()) {
                    program = it.next();
                }
            }
        }

        // Return if the program is null. This is just a precaution and should
        // not happen because the user has selected the context menu of an
        // available program.
        if (program == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, program.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, channel, program.title));
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
            final Program prog = program;
            final String[] dcList = dvrConfigList;

            // Create the dialog to show the available profiles
            new MaterialDialog.Builder(activity)
            .title(R.string.select_dvr_config)
            .items(dvrConfigList)
            .itemsCallbackSingleChoice((int) dvrConfigNameValue, new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                    // Pass over the 
                    Intent intent = new Intent(activity, HTSService.class);
                    intent.setAction(Constants.ACTION_ADD_DVR_ENTRY);
                    intent.putExtra("eventId", prog.id);
                    intent.putExtra("channelId", prog.channel.id);
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
            // Open a new activity to stream the current program to this device
            Intent intent = new Intent(activity, ExternalActionActivity.class);
            intent.putExtra(Constants.BUNDLE_CHANNEL_ID, channel.id);
            startActivity(intent);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        activity.getMenuInflater().inflate(R.menu.program_context_menu, menu);

        // Get the currently selected channel. Also get the program that is
        // currently being transmitting by this channel.
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Program program = null;
        final Channel channel = adapter.getItem(info.position);
        if (channel != null) {
            synchronized(channel.epg) {
                Iterator<Program> it = channel.epg.iterator();
                while (it.hasNext()) {
                    program = it.next();
                    if (channel.isTransmitting && 
                            channelTimeSelection == 0 &&
                            program.start.getTime() >= showProgramsFromTime || 
                            program.stop.getTime() >= showProgramsFromTime) {
                        break;
                    }
                }
            }
        }
        if (program != null) {
            menu.setHeaderTitle(program.title);
            Utils.setProgramMenu(app, menu, program);
        }
    }

    /**
     * Fills the adapter with the available channel data. Only those channels
     * will be added to the adapter that contain the selected channel tag.
     * Populates the channel tag adapter so the user can select a new one.
     * Additionally some status information will be shown in the action bar.
     */
    private void populateList() {
        // Add only those channels that contain the selected tag
        adapter.clear();
        ChannelTag currentTag = Utils.getChannelTag(activity);
        for (Channel ch : app.getChannels()) {
            if (currentTag == null || ch.hasTag(currentTag.id)) {
                adapter.add(ch);
            }
        }
        adapter.sort(Utils.getChannelSortOrder(activity));
        adapter.setTime(showProgramsFromTime);
        adapter.notifyDataSetChanged();

        // Fill the tag adapter with the available tags
        tagAdapter.clear();
        for (ChannelTag t : app.getChannelTags()) {
            tagAdapter.add(t);
        }

        // Show the name of the selected channel tag and the number of channels
        // in the action bar. If enabled show also the channel tag icon.
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle((currentTag == null) ? getString(R.string.all_channels) : currentTag.name);
            String items = getResources().getQuantityString(R.plurals.items, adapter.getCount(), adapter.getCount());
            actionBarInterface.setActionBarSubtitle(items);

            if (Utils.showChannelIcons(activity) && Utils.showChannelTagIcon(activity)
                    && currentTag != null 
                    && currentTag.id != 0) {
                actionBarInterface.setActionBarIcon(currentTag.iconBitmap);
            } else {
                actionBarInterface.setActionBarIcon(R.drawable.ic_launcher);
            }
        }

        // Inform the activity that the channel list has been populated. The
        // activity will then inform the fragment to select the first item in
        // the list or scroll to the previously selected one in case the
        // orientation has changed
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        app.addListener(this);
        if (!app.isLoading()) {
            populateList();
        }

        // Start the timer that updates the adapter so 
        // it only shows programs within the current time
        if (!showOnlyChannels) {
            channelUpdateHandler.post(channelUpdateTask);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
        channelUpdateHandler.removeCallbacks(channelUpdateTask);
    }

    @Override
    public void onDetach() {
        fragmentStatusInterface = null;
        fragmentScrollInterface = null;
        actionBarInterface = null;
        super.onDetach();
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the specified action will be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_LOADING)) {
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
        } else if (action.equals(Constants.ACTION_CHANNEL_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.add((Channel) obj);
                    adapter.sort(Utils.getChannelSortOrder(activity));
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(Constants.ACTION_CHANNEL_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.remove((Channel) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(Constants.ACTION_CHANNEL_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Channel) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(Constants.ACTION_TAG_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.add(tag);
                }
            });
        } else if (action.equals(Constants.ACTION_TAG_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.remove(tag);
                }
            });
        } else if (action.equals(Constants.ACTION_PROGRAM_UPDATE)
                || action.equals(Constants.ACTION_PROGRAM_DELETE)
                || action.equals(Constants.ACTION_DVR_ADD)
                || action.equals(Constants.ACTION_DVR_UPDATE)) {
            // An existing program has been updated
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        } 
    }

    @Override
    public void reloadData() {
        populateList();
    }

    @Override
    public void setSelection(final int position, final int offset) {
        if (listView != null && listView.getCount() > position && position >= 0) {
            listView.setSelectionFromTop(position, offset);
        }
    }

    @Override
    public void setInitialSelection(final int position) {
        setSelection(position, 0);

        if (adapter != null && adapter.getCount() > position) {
            adapter.setPosition(position);

            if (fragmentStatusInterface != null) {
                final Channel ch = (Channel) adapter.getItem(position);
                fragmentStatusInterface.onListItemSelected(position, ch, TAG);
            }
        }
    }

    @Override
    public Object getSelectedItem() {
        return adapter.getSelectedItem();
    }

    @Override
    public int getItemCount() {
        return adapter.getCount();
    }

    /**
     * Prepares a dialog that shows certain times the user can
     * choose from. These are usually the noon, afternoon and prime times
     */
    private void createChannelTimeDialog() {

        Calendar c = Calendar.getInstance();
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        final int hour = c.get(Calendar.HOUR_OF_DAY);

        String[] times = new String[(24 - hour) / 2];
        times[0] = getString(R.string.current_time);
        int j = 1;
        for (int i = 2; i < (24 - hour); i += 2) {
            if (times.length > j) {
                times[j] = String.valueOf(hour + i) + ":00";
            }
            j++;
        }

        channelTimeDialog = new MaterialDialog.Builder(activity)
        .title(R.string.select_time)
        .items(times)
        .itemsCallbackSingleChoice(channelTimeSelection, new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                channelTimeSelection = which;

                // Get the current time and create the new time from the selection value. 
                // 0 is the current time, 1 is 2 hours ahead, 2 is 4 hours ahead and so on 
                Calendar c = Calendar.getInstance();
                if (which > 0) {
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    if (which > 0) {
                        c.set(Calendar.HOUR_OF_DAY, hour + (which*2));
                    }
                }
                channelTimeDialog.dismiss();

                showProgramsFromTime = c.getTimeInMillis();
                adapter.setTime(showProgramsFromTime);
                adapter.notifyDataSetChanged();

                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.onChannelTimeSelected(channelTimeSelection, showProgramsFromTime);
                    fragmentStatusInterface.onListPopulated(TAG);
                }

                return true;
            }
        })
        .build();
    }
}
