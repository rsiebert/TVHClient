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

import org.tvheadend.tvhclient.adapter.ChannelListAdapter;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ChannelListFragment extends Fragment implements HTSListener {

    private final static String TAG = ChannelListFragment.class.getSimpleName();

	private OnChannelListListener channelListListener;
	private ActionBarInterface actionBarInterface;
    private ChannelListAdapter adapter;
    ArrayAdapter<ChannelTag> tagAdapter;
    private AlertDialog tagDialog;
    private ListView channelListView;

    // This is the default view for the channel list adapter. Other views can be
    // passed to the adapter to show less information. This is used in the
    // program guide where only the channel icon is relevant.
    private int viewLayout = R.layout.list_layout;
    private int adapterLayout = R.layout.channel_list_widget;

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
        channelListView = (ListView) v.findViewById(R.id.item_list);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (getActivity() instanceof ChannelListTabsActivity) {
            setHasOptionsMenu(true);
        }

        try {
            channelListListener = (OnChannelListListener) getActivity();
        } catch (Exception e) {

        }

        try {
            actionBarInterface = (ActionBarInterface) getActivity();
        } catch (Exception e) {

        }

        adapter = new ChannelListAdapter(getActivity(), new ArrayList<Channel>(), adapterLayout);
        channelListView.setAdapter(adapter);
        
        // Show the details of the program when the user clicked on the channel 
        channelListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Channel ch = (Channel) adapter.getItem(position);
                if (ch.epg.isEmpty()) {
                    return;
                }
                if (channelListListener != null) {
                    channelListListener.onChannelSelected(position, ch.id);
                }
            }
        });

        // Create the dialog where the user can select the different tags
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.menu_tags);
        tagAdapter = new ArrayAdapter<ChannelTag>(
                getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<ChannelTag>());
        builder.setAdapter(tagAdapter, new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int pos) {
                Utils.setChannelTagId(pos);
                populateList();
            }
        });
        tagDialog = builder.create();

        if (getActivity() instanceof ChannelListTabsActivity) {
            registerForContextMenu(channelListView);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu item of not required
        MenuItem genreItem = menu.findItem(R.id.menu_genre_color_info);
        if (genreItem != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
        Channel channel = adapter.getItem(info.position);
        
        Intent intent = null;
        switch (item.getItemId()) {
        case R.id.menu_play:
            intent = new Intent(getActivity(), PlaybackSelectionActivity.class);
            intent.putExtra("channelId", channel.id);
            startActivity(intent);
            return true;

        case R.id.menu_search:
            // TODO show "Search this Channel" in the input line
            intent = new Intent();
            intent.putExtra("channelId", channel.id);
            getActivity().startSearch(null, false, intent.getExtras(), false);
            return true;

        default:
            return false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.channel_context_menu, menu);
        
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Channel channel = adapter.getItem(info.position);
        menu.setHeaderTitle(channel.name);
    }

    /**
     * Fills the list with the available channel data. Only the channels that
     * are part of the selected tag are shown.
     */
    public void populateList() {
        
        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
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
        if (getActivity() instanceof ProgramGuideTabsActivity) { 
            channelListView.setSelection(((ProgramGuideTabsActivity) getActivity()).getScrollingSelectionIndex());
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
            getActivity().onSearchRequested();
            return true;

        case R.id.menu_tags:
            tagDialog.show();
            return true;

        case R.id.menu_genre_color_info:
            Utils.showGenreColorDialog(getActivity());
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
        app.addListener(this);
        setLoading(app.isLoading());
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
        app.removeListener(this);
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
                TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.create_new_connections));
        builder.setTitle(getString(R.string.no_connections));

        // Define the action of the yes button
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Show the manage connections activity where
                // the user can choose a connection
                Intent intent = new Intent(getActivity(), SettingsAddConnectionActivity.class);
                startActivityForResult(intent, Utils.getResultCode(R.id.menu_connections));
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
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    setLoading(loading);
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_CHANNEL_ADD)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    adapter.add((Channel) obj);
                    adapter.notifyDataSetChanged();
                    adapter.sort();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_CHANNEL_DELETE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    adapter.remove((Channel) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_CHANNEL_UPDATE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Channel) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_TAG_ADD)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.add(tag);
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_TAG_DELETE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.remove(tag);
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_TAG_UPDATE)) {
            //NOP
        }
    }

    /**
     * Scrolls the channel list to the given position. The scrolling is not per
     * pixel but only per row. This is used when the program guide screen is
     * visible. The channel list is scrolled parallel with the program guide
     * view.
     * 
     * @param index The index (starting at 0) of the channel item to be selected
     */
    public void scrollListViewTo(int index) {
        if (channelListView != null)
            channelListView.setSelection(index);
    }

    /**
     * Scrolls the channel list to the given pixel position. The scrolling is
     * accurate because the pixel value is used. This method is also used when
     * the program guide screen is visible. The channel list is scrolled
     * parallel with the program guide.
     * 
     * @param index The index (starting at 0) of the channel item to be selected
     * @param pos The distance from the top edge of the channel list that the
     *            item will be positioned.
     */
    public void scrollListViewToPosition(int index, int pos) {
        if (channelListView != null) {
            channelListView.setSelectionFromTop(index, pos);
        }
    }

    /**
     * Sets the selected item in the list to the desired position. Any listener
     * is then informed that a new channel item has been selected.
     * 
     * @param position
     */
    public void setSelectedItem(int position) {
        if (channelListView.getCount() > position && adapter.getCount() > position) {
            channelListView.setSelection(position);
            channelListListener.onChannelSelected(position, adapter.getItem(position).id);
        }
    }

    public interface OnChannelListListener {
        public void onChannelSelected(int position, long id);
        public void onChannelListPopulated();
    }
}
