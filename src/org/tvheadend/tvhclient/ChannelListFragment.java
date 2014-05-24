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
package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.Iterator;

import org.tvheadend.tvhclient.adapter.ChannelListAdapter;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.ProgramGuideInterface;
import org.tvheadend.tvhclient.interfaces.ProgramGuideScrollingInterface;
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

public class ChannelListFragment extends Fragment implements HTSListener, ProgramGuideScrollingInterface {

    private final static String TAG = ChannelListFragment.class.getSimpleName();

    private Activity activity;
    private ProgramGuideInterface programGuideInterface;
	private OnChannelListListener channelListListener;
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

    private boolean enableScrolling = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null) {
            return null;
        }
        // Get the passed layout id. Only used in the program guide icon list.
        Bundle bundle = getArguments();
        if (bundle != null) {
            viewLayout = bundle.getInt("viewLayout", R.layout.list_layout);
            adapterLayout = bundle.getInt("adapterLayout", R.layout.channel_list_widget);
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

        if (activity instanceof OnChannelListListener) {
            channelListListener = (OnChannelListListener) activity;
        }
        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (activity instanceof ProgramGuideInterface) {
            programGuideInterface = (ProgramGuideInterface) activity;
        }
        if (activity instanceof ChannelListTabsActivity) {
            setHasOptionsMenu(true);
        }

        adapter = new ChannelListAdapter(activity, new ArrayList<Channel>(), adapterLayout);
        listView.setAdapter(adapter);
        
        // Show the details of the program when the user clicked on the channel 
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Channel ch = (Channel) adapter.getItem(position);
                if (channelListListener != null) {
                    channelListListener.onChannelSelected(position, ch);
                }
                adapter.setPosition(position);
                adapter.notifyDataSetChanged();
            }
        });
        
        // Create a scroll listener to inform the parent activity about
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Enables scrolling when the user has touch the screen and
                // starts scrolling. When the user is done, scrolling will be
                // disabled to prevent unwanted calls to the interface. 
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    enableScrolling = true;
                } else if (scrollState == SCROLL_STATE_IDLE && enableScrolling) {
                    if (programGuideInterface != null) {
                        enableScrolling = false;
                        programGuideInterface.onScrollStateIdle(TAG);
                    }
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (programGuideInterface != null && enableScrolling) {
                    int index = view.getFirstVisiblePosition();
                    View v = view.getChildAt(0);
                    int position = (v == null) ? 0 : v.getTop();
                    programGuideInterface.onScrollingChanged(index, position, TAG);
                }
            }
        });

        // Create the dialog where the user can select the different tags
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.menu_tags);
        tagAdapter = new ArrayAdapter<ChannelTag>(
                activity,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<ChannelTag>());
        builder.setAdapter(tagAdapter, new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int pos) {
                Utils.setChannelTagId(pos);
                populateList();
            }
        });
        tagDialog = builder.create();

        if (activity instanceof ChannelListTabsActivity) {
            registerForContextMenu(listView);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu item of not required
        MenuItem genreItem = menu.findItem(R.id.menu_genre_color_info);
        if (genreItem != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            genreItem.setVisible(prefs.getBoolean("showGenreColorsChannelsPref", false));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.channel_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        
        // Get the currently selected channel from the list
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Program program = null;
        Channel channel = adapter.getItem(info.position);
        if (channel != null) {
            synchronized(channel.epg) {
                Iterator<Program> it = channel.epg.iterator();
                if (channel.isTransmitting && it.hasNext()) {
                    program = it.next();
                }
            }
        }

        switch (item.getItemId()) {
        case R.id.menu_search:
            Bundle bundle = new Bundle();
            bundle.putLong("channelId", program.channel.id);
            activity.startSearch(null, false, bundle, false);
            return true;

        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, program.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, program.title));
            return true;
            
        case R.id.menu_record_remove:
            Utils.removeProgram(activity, program.recording);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(activity, program.recording);
            return true;

        case R.id.menu_record:
            Utils.recordProgram(activity, program.id, program.channel.id);
            return true;

        case R.id.menu_play:
            // Open a new activity to stream the current program to this device
            Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
            intent.putExtra("channelId", channel.id);
            startActivity(intent);
            return true;

        case R.id.menu_tags:
            tagDialog.show();
            return true;

        case R.id.menu_genre_color_info:
            Utils.showGenreColorDialog(activity);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        activity.getMenuInflater().inflate(R.menu.program_context_menu, menu);
        
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Program program = null;
        Channel channel = adapter.getItem(info.position);
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
        }
        Utils.setProgramMenu(menu, program);
    }

    /**
     * Fills the list with the available channel data. Only the channels that
     * are part of the selected tag are shown.
     */
    public void populateList() {
        
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        ChannelTag currentTag = Utils.getChannelTag(app);
        adapter.clear();
        for (Channel ch : app.getChannels()) {
            if (currentTag == null || ch.hasTag(currentTag.id)) {
                adapter.add(ch);
            }
        }
        adapter.sort();
        adapter.notifyDataSetChanged();

        // Show the number of channels that are in the selected tag
        updateItemCount(currentTag);

        // Inform the listeners that the channel list is populated.
        // They could then define the preselected list item.
        if (channelListListener != null) {
            channelListListener.onChannelListPopulated();
        }

        // Set the scroll position of the list view. Only required when this
        // class is used in the program guide where the channels are shown on
        // the the left side and shall scroll with the guide data.
        if (programGuideInterface != null) {
            listView.setSelectionFromTop(
                    programGuideInterface.getScrollingSelectionIndex(),
                    programGuideInterface.getScrollingSelectionPosition());
        }
    }

    /**
     * Shows the currently visible number of the channels that are in the
     * selected channel tag. This method is also called from the program guide
     * activity to remove the loading indication and show the numbers again.  
     * 
     * @param currentTag
     */
    public void updateItemCount(ChannelTag currentTag) {
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.items), TAG);
            actionBarInterface.setActionBarTitle((currentTag == null) ? getString(R.string.all_channels) : currentTag.name, TAG);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_search:
            activity.onSearchRequested();
            return true;

        case R.id.menu_tags:
            tagDialog.show();
            return true;

        case R.id.menu_genre_color_info:
            Utils.showGenreColorDialog(activity);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        setLoading(app.isLoading());
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    @Override
    public void onDetach() {
        programGuideInterface = null;
        channelListListener = null;
        actionBarInterface = null;
        super.onDetach();
    }

    /**
     * Show that either no connection (and no data) is available, the data is
     * loaded or calls the method to display it.
     * 
     * @param loading
     */
    private void setLoading(boolean loading) {
        if (DatabaseHelper.getInstance() != null && 
                DatabaseHelper.getInstance().getSelectedConnection() == null) {
            adapter.clear();
            adapter.notifyDataSetChanged();
            // Only update the header when the channels are shown. Do not do
            // this when this class is used by the program guide.
            if (actionBarInterface != null) {
                actionBarInterface.setActionBarSubtitle(getString(R.string.no_connections), TAG);
            }
            showCreateConnectionDialog();
        } else {
            if (loading) {
                adapter.clear();
                adapter.notifyDataSetChanged();
                // Only update the header when the channels are shown. Do not do
                // this when this class is used by the program guide.
                if (actionBarInterface != null) {
                    actionBarInterface.setActionBarSubtitle(getString(R.string.loading), TAG);
                }
            } else {
                // Fill the tag adapter with the available tags
                TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                tagAdapter.clear();
                for (ChannelTag t : app.getChannelTags()) {
                    tagAdapter.add(t);
                }
                // Update the list with the new channel data
                populateList();
            }
        }
    }

    /**
     * Shows a dialog to the user where he can choose to go directly to the
     * connection screen. This dialog is only shown after the start of the
     * application when no connection is available.
     */
    private void showCreateConnectionDialog() {
        // Show confirmation dialog to cancel 
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(getString(R.string.create_new_connections));
        builder.setTitle(getString(R.string.no_connections));

        // Define the action of the yes button
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Show the manage connections activity where
                // the user can choose a connection
                Intent intent = new Intent(activity, SettingsAddConnectionActivity.class);
                startActivityForResult(intent, Constants.RESULT_CODE_CONNECTIONS);
            }
        });
        // Define the action of the no button
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHClientApplication.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    setLoading(loading);
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_CHANNEL_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.add((Channel) obj);
                    adapter.notifyDataSetChanged();
                    adapter.sort();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_CHANNEL_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.remove((Channel) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_CHANNEL_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Channel) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_TAG_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.add(tag);
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_TAG_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.remove(tag);
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_TAG_UPDATE)) {
            //NOP
        }
    }

    @Override
    public void scrollListViewTo(int index) {
        if (listView != null)
            listView.setSelection(index);
    }

    @Override
    public void scrollListViewToPosition(int index, int pos) {
        if (listView != null) {
            listView.setSelectionFromTop(index, pos);
        }
    }

    /**
     * Sets the selected item in the list to the desired position. Any listener
     * is then informed that a new channel item has been selected.
     * 
     * @param position
     */
    public void setSelectedItem(int position) {
        if (listView.getCount() > position && adapter.getCount() > position) {
            adapter.setPosition(position);
            channelListListener.onChannelSelected(position, adapter.getItem(position));
        }
    }

    public interface OnChannelListListener {
        public void onChannelSelected(int position, Channel channel);
        public void onChannelListPopulated();
    }
}
