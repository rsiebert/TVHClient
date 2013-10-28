/*
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
package org.tvheadend.tvhguide;

import java.util.ArrayList;

import org.tvheadend.tvhguide.adapter.ChannelListAdapter;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.ChannelTag;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

/**
 *
 * @author john-tornblom
 */
public class ChannelListFragment extends Fragment implements HTSListener {

    private ChannelListAdapter chAdapter;
    ArrayAdapter<ChannelTag> tagAdapter;
    private AlertDialog tagDialog;
    private ChannelTag currentTag;
    private ListView channelListView;
    private int channelTagId;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null)
            return null;

        View v = inflater.inflate(R.layout.list_layout, container, false);
        channelListView = (ListView) v.findViewById(R.id.item_list);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        
        chAdapter = new ChannelListAdapter(getActivity(), new ArrayList<Channel>());
        channelListView.setAdapter(chAdapter);

        channelListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Channel ch = (Channel) chAdapter.getItem(position);
                if (ch.epg.isEmpty()) {
                    return;
                }
                Intent intent = new Intent(getActivity().getBaseContext(), ProgramListActivity.class);
                intent.putExtra("channelId", ch.id);
                startActivity(intent);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.menu_tags);

        tagAdapter = new ArrayAdapter<ChannelTag>(
                getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<ChannelTag>());

        builder.setAdapter(tagAdapter, new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int pos) {
                setCurrentTag(tagAdapter.getItem(pos));
                channelTagId = pos;
                populateList();
            }
        });
        tagDialog = builder.create();

        registerForContextMenu(channelListView);
        
        // Restore the previously selected tag
        if (savedInstanceState != null) {
            channelTagId = savedInstanceState.getInt("selected_channel_tag_id", 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_channel_tag_id", channelTagId);
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
        Channel channel = chAdapter.getItem(info.position);
        
        Intent intent = null;
        switch (item.getItemId()) {
        case R.id.menu_play:
            intent = new Intent(getActivity(), PlaybackActivity.class);
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
        Channel channel = chAdapter.getItem(info.position);
        menu.setHeaderTitle(channel.name);
    }

    private void setCurrentTag(ChannelTag tag) {
        currentTag = tag;
        if (tag == null) {
            getActivity().getActionBar().setTitle(R.string.all_channels);
        } else {
            getActivity().getActionBar().setTitle(currentTag.name);
        }
    }

    private void populateList() {
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        chAdapter.clear();
        for (Channel ch : app.getChannels()) {
            if (currentTag == null || ch.hasTag(currentTag.id)) {
                chAdapter.add(ch);
            }
        }
        chAdapter.sort();
        chAdapter.notifyDataSetChanged();
        getActivity().getActionBar().setSubtitle(chAdapter.getCount() + " " + getString(R.string.items));
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

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.addListener(this);
        setLoading(app.isLoading());
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.removeListener(this);
    }

    /**
     * Shows either that the channel data is still being loaded or fills the
     * list with the available channel data. Additionally only the channels with
     * the previously selected tag will be shown. This happens usually after an
     * orientation change (screen rotation).
     * 
     * @param loading
     */
    private void setLoading(boolean loading) {
        if (loading) {
            // Clear any channels in the list and 
            // show that we are still loading data.
            chAdapter.clear();
            chAdapter.notifyDataSetChanged();
            getActivity().getActionBar().setSubtitle(R.string.loading);
        } 
        else {
            // Fill the tag adapter with the available channel tags
            TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
            tagAdapter.clear();
            for (ChannelTag t : app.getChannelTags()) {
                tagAdapter.add(t);
            }

            // Check if tags exist and set the previously used one
            if (tagAdapter.getCount() > channelTagId)
                currentTag = tagAdapter.getItem(channelTagId);

            // Update the action bar text and fill the channel list
            setCurrentTag(currentTag);
            populateList();
        }
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_LOADING)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    setLoading(loading);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_CHANNEL_ADD)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    chAdapter.add((Channel) obj);
                    chAdapter.notifyDataSetChanged();
                    chAdapter.sort();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_CHANNEL_DELETE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    chAdapter.remove((Channel) obj);
                    chAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_CHANNEL_UPDATE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    chAdapter.update((Channel) obj);
                    chAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_TAG_ADD)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.add(tag);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_TAG_DELETE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.remove(tag);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_TAG_UPDATE)) {
            //NOP
        }
    }
}
