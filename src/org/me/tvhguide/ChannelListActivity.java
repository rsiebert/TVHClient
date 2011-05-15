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

import android.content.DialogInterface;
import org.me.tvhguide.htsp.HTSService;
import org.me.tvhguide.model.Programme;
import org.me.tvhguide.model.Channel;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.ClipDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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
        chAdapter.sort();
        setListAdapter(chAdapter);
        Intent intent = new Intent(ChannelListActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_CONNECT);
        startService(intent);
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
            case R.id.mi_recordings: {
                Intent intent = new Intent(getBaseContext(), RecordingListActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.mi_help: {
                return true;
            }
            case R.id.mi_tags: {
                final TVHGuideApplication app = (TVHGuideApplication) getApplication();

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.menu_tags);

                final ArrayAdapter<ChannelTag> tagAdapter = new ArrayAdapter<ChannelTag>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        app.getChannelTags());

                builder.setAdapter(tagAdapter, new OnClickListener() {

                    public void onClick(DialogInterface arg0, int pos) {

                        chAdapter.clear();
                        ChannelTag tag = tagAdapter.getItem(pos);
                        for (Channel ch : app.getChannels()) {
                            if (ch.hasTag(tag.id)) {
                                chAdapter.add(ch);
                            }
                        }
                        chAdapter.sort();
                        chAdapter.notifyDataSetChanged();
                    }
                });

                builder.show();

                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
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
                    chAdapter.sort();
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
                    chAdapter.sort();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_CHANNEL_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Channel channel = (Channel) obj;
                    chAdapter.updateView(getListView(), channel);
                }
            });
        }
    }

    private class ViewWarpper {

        private TextView name;
        private TextView nowTitle;
        private TextView nowTime;
        private TextView nextTitle;
        private TextView nextTime;
        private ImageView icon;
        private ClipDrawable nowProgress;
        public final long channelId;

        public ViewWarpper(View base, long channelId) {
            name = (TextView) base.findViewById(R.id.ch_name);
            nowTitle = (TextView) base.findViewById(R.id.ch_now_title);

            TableRow tblRow = (TableRow) base.findViewById(R.id.ch_now_row);
            tblRow.setBackgroundResource(android.R.drawable.progress_horizontal);
            nowProgress = new ClipDrawable(tblRow.getBackground(), Gravity.LEFT, ClipDrawable.HORIZONTAL);
            nowProgress.setAlpha(64);

            tblRow.setBackgroundDrawable(nowProgress);

            nowTime = (TextView) base.findViewById(R.id.ch_now_time);
            nextTitle = (TextView) base.findViewById(R.id.ch_next_title);
            nextTime = (TextView) base.findViewById(R.id.ch_next_time);
            icon = (ImageView) base.findViewById(R.id.ch_icon);

            this.channelId = channelId;
        }

        public void repaint(Channel channel) {
            nowTime.setText("");
            nowTitle.setText("");
            nextTime.setText("");
            nextTitle.setText("");
            nowProgress.setLevel(0);

            name.setText(channel.getName());
            name.invalidate();
            icon.setBackgroundDrawable(channel.getDrawableIcon());
            if (channel.isRecording()) {
                icon.setImageResource(R.drawable.ic_rec_small);
            } else {
                icon.setImageDrawable(null);
            }
            icon.invalidate();

            Iterator<Programme> it = channel.epg.iterator();
            if (it.hasNext()) {
                Programme p = it.next();
                nowTime.setText(
                        DateFormat.getTimeFormat(nowTime.getContext()).format(p.start)
                        + " - "
                        + DateFormat.getTimeFormat(nowTime.getContext()).format(p.stop));

                double duration = (p.stop.getTime() - p.start.getTime());
                double elapsed = new Date().getTime() - p.start.getTime();
                double percent = elapsed / duration;

                nowProgress.setLevel((int) Math.floor(percent * 10000));
                nowTitle.setText(p.title);
            }
            nowTime.invalidate();
            nowTitle.invalidate();

            if (it.hasNext()) {
                Programme p = it.next();
                nextTime.setText(
                        DateFormat.getTimeFormat(nextTime.getContext()).format(p.start)
                        + " - "
                        + DateFormat.getTimeFormat(nextTime.getContext()).format(p.stop));

                nextTitle.setText(p.title);
            }
            nextTime.invalidate();
            nextTitle.invalidate();
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

                if (view.getTag() == null) {
                    continue;
                }
                ViewWarpper wrapper = (ViewWarpper) view.getTag();
                if (wrapper.channelId == channel.id) {
                    wrapper.repaint(channel);
                    break;
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewWarpper wrapper = null;

            Channel ch = list.get(position);

            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(R.layout.ch_widget, null, false);
                row.requestLayout();
                wrapper = new ViewWarpper(row, ch.id);
                row.setTag(wrapper);

            } else {
                wrapper = (ViewWarpper) row.getTag();
            }

            wrapper.repaint(ch);
            return row;
        }
    }
}
