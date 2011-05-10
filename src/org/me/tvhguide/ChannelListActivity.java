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
package org.me.tvhguide;

import org.me.tvhguide.htsp.HTSService;
import org.me.tvhguide.model.Programme;
import org.me.tvhguide.model.Channel;
import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.me.tvhguide.model.ChannelTag;
import org.me.tvhguide.htsp.HTSListener;

/**
 *
 * @author john-tornblom
 */
public class ChannelListActivity extends ListActivity implements HTSListener {

    private ChannelListAdapter chAdapter;
    private ProgressDialog pd;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        TVHGuideApplication app = (TVHGuideApplication) getApplication();

        List<Channel> chList = new ArrayList<Channel>();
        chList.addAll(app.getChannels());
        chAdapter = new ChannelListAdapter(this, chList);
        chAdapter.sort(new Comparator<Channel>() {

            public int compare(Channel x, Channel y) {
                return x.number - y.number;
            }
        });
        setListAdapter(chAdapter);
        Intent intent = new Intent(ChannelListActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_CONNECT);
        startService(intent);
        registerForContextMenu(getListView());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
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
                Intent intent = new Intent(ChannelListActivity.this, HTSService.class);
                intent.setAction(HTSService.ACTION_REFRESH);
                startService(intent);
                return true;
            }
            case R.id.mi_help: {
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle(R.string.menu_tags);
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        menu.add(0, R.string.pr_all_channels, 0, getString(R.string.pr_all_channels));
        for (ChannelTag tag : app.getChannelTags()) {
            menu.add(0, tag.hashCode(), 0, tag.name);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        for (ChannelTag tag : app.getChannelTags()) {
            if (tag.hashCode() == item.getItemId()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == R.id.mi_settings) {
            Intent intent = new Intent(ChannelListActivity.this, HTSService.class);
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addListener(this);
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

        Intent intent = new Intent(getBaseContext(), ProgrammeActivity.class);
        intent.putExtra("channelId", ch.id);
        startActivity(intent);
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_LOADING)) {

            runOnUiThread(new Runnable() {

                public void run() {
                    boolean loading = (Boolean) obj;
                    if (loading) {
                        pd = ProgressDialog.show(ChannelListActivity.this,
                                getString(R.string.inf_load),
                                getString(R.string.inf_load_ext), true);
                        return;
                    } else if (pd != null) {
                        pd.cancel();
                    }

                    TVHGuideApplication app = (TVHGuideApplication) getApplication();
                    chAdapter.list.clear();
                    chAdapter.list.addAll(app.getChannels());
                    chAdapter.notifyDataSetChanged();
                    chAdapter.sort(new Comparator<Channel>() {

                        public int compare(Channel x, Channel y) {
                            return x.number - y.number;
                        }
                    });
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_CHANNEL_ADD)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    chAdapter.add((Channel) obj);
                    chAdapter.notifyDataSetChanged();
                    chAdapter.sort(new Comparator<Channel>() {

                        public int compare(Channel x, Channel y) {
                            return x.number - y.number;
                        }
                    });
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_CHANNEL_DELETE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    chAdapter.remove((Channel) obj);
                    chAdapter.notifyDataSetChanged();
                    chAdapter.sort(new Comparator<Channel>() {

                        public int compare(Channel x, Channel y) {
                            return x.number - y.number;
                        }
                    });
                }
            });
        }
    }

    private class ViewWarpper {

        TextView name;
        TextView nowTitle;
        TextView nowTime;
        TextView nextTitle;
        TextView nextTime;
        ImageView icon;

        public ViewWarpper(View base) {
            name = (TextView) base.findViewById(R.id.ch_name);
            nowTitle = (TextView) base.findViewById(R.id.ch_now_title);
            nowTime = (TextView) base.findViewById(R.id.ch_now_time);
            nextTitle = (TextView) base.findViewById(R.id.ch_next_title);
            nextTime = (TextView) base.findViewById(R.id.ch_next_time);
            icon = (ImageView) base.findViewById(R.id.ch_icon);
        }
    }

    class ChannelListAdapter extends ArrayAdapter<Channel> {

        Activity context;
        List<Channel> list;

        ChannelListAdapter(Activity context, List<Channel> list) {
            super(context, R.layout.ch_widget, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewWarpper wrapper = null;

            Channel ch = list.get(position);

            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(R.layout.ch_widget, null, false);

                wrapper = new ViewWarpper(row);
                row.setTag(wrapper);

            } else {
                wrapper = (ViewWarpper) row.getTag();
            }

            wrapper.name.setText(ch.name);
            wrapper.icon.setImageBitmap(ch.iconBitmap);
            //Reset
            wrapper.nowTime.setText("");
            wrapper.nowTitle.setText("");
            wrapper.nextTime.setText("");
            wrapper.nextTitle.setText("");

            Iterator<Programme> it = ch.epg.iterator();
            if (it.hasNext()) {
                Programme p = it.next();
                wrapper.nowTime.setText(
                        DateFormat.getTimeFormat(getContext()).format(p.start)
                        + " - "
                        + DateFormat.getTimeFormat(getContext()).format(p.stop));

                wrapper.nowTitle.setText(p.title);
            }
            if (it.hasNext()) {
                Programme p = it.next();
                wrapper.nextTime.setText(
                        DateFormat.getTimeFormat(getContext()).format(p.start)
                        + " - "
                        + DateFormat.getTimeFormat(getContext()).format(p.stop));

                wrapper.nextTitle.setText(p.title);
            }

            return row;
        }
    }
}
