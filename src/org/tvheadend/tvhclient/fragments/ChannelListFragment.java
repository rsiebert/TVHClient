/*
 *  Copyright (C) 2013 Robert Siebert
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;
import java.util.Iterator;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.PlaybackSelectionActivity;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.ChannelListAdapter;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Program;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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

public class ChannelListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    private final static String TAG = ChannelListFragment.class.getSimpleName();

    private Activity activity;
    private FragmentStatusInterface fragmentStatusInterface;
    private FragmentScrollInterface fragmentScrollInterface;
	private ActionBarInterface actionBarInterface;

    private ChannelListAdapter adapter;
    ArrayAdapter<ChannelTag> tagAdapter;
    private AlertDialog tagDialog;
    private ListView listView;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't exist because the fragment
        // will not be shown.
        if (container == null) {
            return null;
        }

        // Check if only channels shall be visible. This is only the case when
        // this fragment is part of the program guide view.
        Bundle bundle = getArguments();
        if (bundle != null) {
            showOnlyChannels = bundle.getBoolean(Constants.BUNDLE_SHOWS_ONLY_CHANNELS, false);
            isDualPane  = bundle.getBoolean(Constants.BUNDLE_DUAL_PANE, false);
        }
        // When only channels shall be seen, a reduced adapter and list view
        // layout is used.
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
        
        // Inform the activity when a channel has been selected
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

        // Create the dialog with the available channel tags
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.menu_tags);
        tagAdapter = new ArrayAdapter<ChannelTag>(activity,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<ChannelTag>());
        builder.setAdapter(tagAdapter, new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int pos) {
                Utils.setChannelTagId(pos);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.channelTagChanged(TAG);
                }
            }
        });
        tagDialog = builder.create();
        
        // Only enable the context menu when the full fragment is shown and not
        // only the channels
        if (!showOnlyChannels) {
            registerForContextMenu(listView);
        }
        // Enable the action bar menu. Even in the channel only mode the tags
        // shall be available to set
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu in dual pane mode or if no genre colors
        // shall be shown or only channels shall be shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsChannelsPref", false);
        (menu.findItem(R.id.menu_genre_color_info_channels)).setVisible(!showOnlyChannels && showGenreColors);

        // Playing a channel shall not be available in channel only mode or in
        // single pane mode, because no channel is preselected.
        if (!showOnlyChannels || !isDualPane) {
            (menu.findItem(R.id.menu_play)).setVisible(false);
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
            Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
            Channel channel = adapter.getSelectedItem();
            if (channel != null) {
                intent.putExtra(Constants.BUNDLE_CHANNEL_ID, channel.id);
            }
            startActivity(intent);
            return true;

        case R.id.menu_tags:
            tagDialog.show();
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
        // Get the currently selected channel. Also get the program that is
        // currently being transmitting by this channel.
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
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

        // Stop if the program is still null. this should't happen because the
        // user has selected the context menu of an available program. 
        if (program == null) {
            return super.onContextItemSelected(item);
        }

        // Check if the context menu call came from the list in this fragment
        // (needed for support for multiple fragments in one screen)
        if (info.targetView.getParent() != getView().findViewById(R.id.item_list)) {
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

        case R.id.menu_record:
            Utils.recordProgram(activity, program);
            return true;

        case R.id.menu_play:
            // Open a new activity to stream the current program to this device
            Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
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
                if (channel.isTransmitting && it.hasNext()) {
                    program = it.next();
                }
            }
        }
        if (program != null) {
            menu.setHeaderTitle(program.title);
            Utils.setProgramMenu(menu, program);
        }
    }

    /**
     * Fills the adapter with the available channel data. Only those channels
     * that added to the adapter that are part of the selected channel tag.
     */
    public void populateList() {
        // Clear the list and add the channels that contain the selected tag
        adapter.clear();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        ChannelTag currentTag = Utils.getChannelTag(app);
        for (Channel ch : app.getChannels()) {
            if (currentTag == null || ch.hasTag(currentTag.id)) {
                adapter.add(ch);
            }
        }
        adapter.sort(Utils.getChannelSortOrder(activity));
        adapter.notifyDataSetChanged();

        // Fill the tag adapter with the available tags so the dialog can
        // actually show some.
        tagAdapter.clear();
        for (ChannelTag t : app.getChannelTags()) {
            tagAdapter.add(t);
        }

        // Inform the activity to show the currently visible number of the
        // channels that are in the selected channel tag and that the channel
        // list has been filled with data.
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle((currentTag == null) ? getString(R.string.all_channels) : currentTag.name, TAG);
            actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.items), TAG);
            // If activated show the the channel tag icon
            if (Utils.showChannelIcons(activity) && Utils.showChannelTagIcon(activity)
                    && currentTag != null 
                    && currentTag.id != 0) {
                actionBarInterface.setActionBarIcon(currentTag.iconBitmap, TAG);
            } else {
                actionBarInterface.setActionBarIcon(R.drawable.ic_launcher, TAG);
            }
        }
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        if (!app.isLoading()) {
            populateList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
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
        // Set the position in the adapter so that we can show the selected
        // channel in the theme with the arrow.
        if (adapter != null && adapter.getCount() > position) {
            adapter.setPosition(position);
            // Simulate a click in the list item to inform the activity
            Channel ch = (Channel) adapter.getItem(position);
            if (fragmentStatusInterface != null) {
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
}
