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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.ChannelTag;

/**
 *
 * @author john-tornblom
 */
public class ChannelListActivity extends ListActivity implements HTSListener {

    private ChannelListAdapter chAdapter;
    ArrayAdapter<ChannelTag> tagAdapter;
    private AlertDialog tagDialog;
    private TextView tagTextView;
    private ImageView tagImageView;
    private View tagBtn;
    private ProgressBar pb;
    private ChannelTag currentTag;

    @Override
    public void onCreate(Bundle icicle) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        setTheme(theme ? R.style.CustomTheme_Light : R.style.CustomTheme);
        
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        chAdapter = new ChannelListAdapter(this, new ArrayList<Channel>());
        setListAdapter(chAdapter);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.channel_list_title);
        tagTextView = (TextView) findViewById(R.id.ct_title);
        tagImageView = (ImageView) findViewById(R.id.ct_logo);

        pb = (ProgressBar) findViewById(R.id.ct_loading);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_tags);

        tagAdapter = new ArrayAdapter<ChannelTag>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<ChannelTag>());

        builder.setAdapter(tagAdapter, new android.content.DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int pos) {
                setCurrentTag(tagAdapter.getItem(pos));
                populateList();
            }
        });

        tagDialog = builder.create();
        tagBtn = findViewById(R.id.ct_btn);
        tagBtn.setOnClickListener(new android.view.View.OnClickListener() {

            public void onClick(View arg0) {
                tagDialog.show();
            }
        });

        registerForContextMenu(getListView());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.ch_play: {
                startActivity(item.getIntent());
                return true;
            }
            case R.string.search_hint: {
                startSearch(null, false, item.getIntent().getExtras(), false);
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
        Intent intent = new Intent(this, PlaybackActivity.class);
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String hostname = prefs.getString("serverHostPref", "localhost");
        int port = Integer.parseInt(prefs.getString("serverPortPref", "9982"));
        String username = prefs.getString("usernamePref", "");
        String password = prefs.getString("passwordPref", "");

        Intent intent = new Intent(ChannelListActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_CONNECT);
        intent.putExtra("hostname", hostname);
        intent.putExtra("port", port);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        intent.putExtra("force", force);

        startService(intent);
    }

    private void setCurrentTag(ChannelTag t) {
        currentTag = t;

        if (t == null) {
            tagTextView.setText(R.string.pr_all_channels);
            tagImageView.setImageResource(R.drawable.logo_72);
        } else {
            tagTextView.setText(currentTag.name);
            if (currentTag.iconBitmap != null) {
                tagImageView.setImageBitmap(currentTag.iconBitmap);
            } else {
                tagImageView.setImageResource(R.drawable.logo_72);
            }
        }
    }

    private void populateList() {
        TVHGuideApplication app = (TVHGuideApplication) getApplication();

        chAdapter.clear();

        for (Channel ch : app.getChannels()) {
            if (currentTag == null || ch.hasTag(currentTag.id)) {
                chAdapter.add(ch);
            }
        }

        chAdapter.sort();
        chAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_settings: {
                Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
                startActivityForResult(intent, R.id.mi_settings);
                return true;
            }
            case R.id.mi_refresh: {
                connect(true);
                return true;
            }
            case R.id.mi_recordings: {
                Intent intent = new Intent(getBaseContext(), RecordingListActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.mi_search: {
                onSearchRequested();
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
    protected void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addListener(this);

        connect(false);
        setLoading(app.isLoading());
    }

    @Override
    protected void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.removeListener(this);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Channel ch = (Channel) chAdapter.getItem(position);

        if (ch.epg.isEmpty()) {
            return;
        }

        Intent intent = new Intent(getBaseContext(), ProgrammeListActivity.class);
        intent.putExtra("channelId", ch.id);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        tagBtn.setEnabled(!loading);
        if (loading) {
            pb.setVisibility(ProgressBar.VISIBLE);
            tagTextView.setText(R.string.inf_load);
            tagImageView.setVisibility(ImageView.INVISIBLE);
        } else {
            pb.setVisibility(ProgressBar.GONE);
            tagImageView.setVisibility(ImageView.VISIBLE);

            TVHGuideApplication app = (TVHGuideApplication) getApplication();
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

            runOnUiThread(new Runnable() {

                public void run() {
                    boolean loading = (Boolean) obj;
                    setLoading(loading);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_CHANNEL_ADD)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    chAdapter.add((Channel) obj);
                    chAdapter.notifyDataSetChanged();
                    chAdapter.sort();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_CHANNEL_DELETE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    chAdapter.remove((Channel) obj);
                    chAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_CHANNEL_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Channel channel = (Channel) obj;
                    chAdapter.updateView(getListView(), channel);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_TAG_ADD)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.add(tag);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_TAG_DELETE)) {
            runOnUiThread(new Runnable() {

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
