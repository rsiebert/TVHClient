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
import java.util.Comparator;
import java.util.List;

import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.ChannelTag;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null)
            return null;

        View v = inflater.inflate(R.layout.channel_list, container, false);
        channelListView = (ListView) v.findViewById(R.id.channel_list);
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

                Intent intent = new Intent(getActivity().getBaseContext(), ProgrammeListActivity.class);
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
                populateList();
            }
        });
        tagDialog = builder.create();

        registerForContextMenu(channelListView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.ch_play: {
                startActivity(item.getIntent());
                return true;
            }
            case R.string.search_hint: {
                getActivity().startSearch(null, false, item.getIntent().getExtras(), false);
                return true;
            }
            default: {
                return false;
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuItem item = menu.add(ContextMenu.NONE, R.string.ch_play, ContextMenu.NONE, R.string.ch_play);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Channel ch = chAdapter.getItem(info.position);

        menu.setHeaderTitle(ch.name);
        Intent intent = new Intent(getActivity(), PlaybackActivity.class);
        intent.putExtra("channelId", ch.id);
        item.setIntent(intent);

        item = menu.add(ContextMenu.NONE, R.string.search_hint, ContextMenu.NONE, R.string.search_hint);
        intent = new Intent();
        intent.putExtra("channelId", ch.id);
        item.setIntent(intent);
    }

    void connect(boolean force) {
        if (force) {
            chAdapter.clear();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String hostname = prefs.getString("serverHostPref", "localhost");
        int port = Integer.parseInt(prefs.getString("serverPortPref", "9982"));
        String username = prefs.getString("usernamePref", "");
        String password = prefs.getString("passwordPref", "");

        Intent intent = new Intent(getActivity(), HTSService.class);
        intent.setAction(HTSService.ACTION_CONNECT);
        intent.putExtra("hostname", hostname);
        intent.putExtra("port", port);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        intent.putExtra("force", force);

        getActivity().startService(intent);
    }

    private void setCurrentTag(ChannelTag t) {
        currentTag = t;

        if (t == null) {
            getActivity().getActionBar().setTitle(R.string.pr_all_channels);
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
            case R.id.mi_refresh: {
                connect(true);
                return true;
            }
            case R.id.mi_search: {
                getActivity().onSearchRequested();
                return true;
            }
            case R.id.mi_tags: {
                tagDialog.show();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.addListener(this);

        connect(false);
        setLoading(app.isLoading());
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.removeListener(this);
    }

    private void setLoading(boolean loading) {

        if (loading) {
            getActivity().getActionBar().setTitle(R.string.inf_load);
        } else {
            TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
            tagAdapter.clear();
            for (ChannelTag t : app.getChannelTags()) {
                tagAdapter.add(t);
            }

            populateList();
            setCurrentTag(currentTag);
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
                    Channel channel = (Channel) obj;
                    chAdapter.updateView(channelListView, channel);
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

    class ChannelListAdapter extends ArrayAdapter<Channel> {

        ChannelListAdapter(Activity context, List<Channel> list) {
            super(context, R.layout.channel_list_widget, list);
        }

        public void sort() {
            sort(new Comparator<Channel>() {

                public int compare(Channel x, Channel y) {
                    return x.compareTo(y);
                }
            });
        }

        public void updateView(ListView listView, Channel channel) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                View view = listView.getChildAt(i);
                int pos = listView.getPositionForView(view);
                Channel ch = (Channel) listView.getItemAtPosition(pos);

                if (view.getTag() == null || ch == null) {
                    continue;
                }

                if (channel.id != ch.id) {
                    continue;
                }

                ChannelListViewWrapper wrapper = (ChannelListViewWrapper) view.getTag();
                wrapper.repaint(channel);
                break;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ChannelListViewWrapper wrapper;

            Channel ch = getItem(position);
            Activity activity = (Activity) getContext();

            if (row == null) {
                LayoutInflater inflater = activity.getLayoutInflater();
                row = inflater.inflate(R.layout.channel_list_widget, null, false);
                row.requestLayout();
                wrapper = new ChannelListViewWrapper(row);
                row.setTag(wrapper);

            } else {
                wrapper = (ChannelListViewWrapper) row.getTag();
            }

            wrapper.repaint(ch);
            return row;
        }
    }
}
