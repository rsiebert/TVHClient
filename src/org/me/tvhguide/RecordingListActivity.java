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

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.me.tvhguide.htsp.HTSListener;
import org.me.tvhguide.htsp.HTSService;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.Recording;

/**
 *
 * @author john-tornblom
 */
public class RecordingListActivity extends ListActivity implements HTSListener {

    private RecordingListAdapter recAdapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        TVHGuideApplication app = (TVHGuideApplication) getApplication();

        List<Recording> recList = new ArrayList<Recording>();
        recList.addAll(app.getRecordings());
        recAdapter = new RecordingListAdapter(this, recList);
        recAdapter.sort(new Comparator<Recording>() {

            public int compare(Recording x, Recording y) {
                return (int) (y.start.getTime() - x.start.getTime());
            }
        });
        setListAdapter(recAdapter);
        registerForContextMenu(getListView());
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
        Recording rec = (Recording) recAdapter.getItem(position);

        Intent intent = new Intent(this, RecordingActivity.class);
        intent.putExtra("id", rec.id);
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuItem item = menu.add(ContextMenu.NONE, R.string.menu_remove, ContextMenu.NONE, R.string.menu_remove);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Recording rec = recAdapter.getItem(info.position);

        menu.setHeaderTitle(rec.title);
        Intent intent = new Intent(RecordingListActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_DVR_DELETE);
        intent.putExtra("id", rec.id);
        item.setIntent(intent);

        item = menu.add(ContextMenu.NONE, R.string.menu_cancel, ContextMenu.NONE, R.string.menu_cancel);
        intent = new Intent(RecordingListActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_DVR_CANCEL);
        intent.putExtra("id", rec.id);
        item.setIntent(intent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.menu_cancel:
            case R.string.menu_remove: {
                startService(item.getIntent());
                return true;
            }
            default: {
                return false;
            }
        }
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_LOADING) && !(Boolean) obj) {

            runOnUiThread(new Runnable() {

                public void run() {
                    TVHGuideApplication app = (TVHGuideApplication) getApplication();
                    recAdapter.list.clear();
                    recAdapter.list.addAll(app.getRecordings());
                    recAdapter.notifyDataSetChanged();
                    recAdapter.sort(new Comparator<Recording>() {

                        public int compare(Recording x, Recording y) {
                            return (int) (y.start.getTime() - x.start.getTime());
                        }
                    });
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_ADD)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    recAdapter.add((Recording) obj);
                    recAdapter.notifyDataSetChanged();
                    recAdapter.sort(new Comparator<Recording>() {

                        public int compare(Recording x, Recording y) {
                            return (int) (y.start.getTime() - x.start.getTime());
                        }
                    });
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_DELETE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    recAdapter.remove((Recording) obj);
                    recAdapter.notifyDataSetChanged();
                    recAdapter.sort(new Comparator<Recording>() {

                        public int compare(Recording x, Recording y) {
                            return (int) (y.start.getTime() - x.start.getTime());
                        }
                    });
                }
            });
        }
    }

    private class ViewWarpper {

        TextView title;
        TextView channel;
        TextView time;
        TextView date;
        TextView message;
        ImageView icon;

        public ViewWarpper(View base) {
            title = (TextView) base.findViewById(R.id.rec_title);
            channel = (TextView) base.findViewById(R.id.rec_channel);

            time = (TextView) base.findViewById(R.id.rec_time);
            date = (TextView) base.findViewById(R.id.rec_date);
            message = (TextView) base.findViewById(R.id.rec_message);
            icon = (ImageView) base.findViewById(R.id.rec_icon);
        }
    }

    class RecordingListAdapter extends ArrayAdapter<Recording> {

        Activity context;
        List<Recording> list;

        RecordingListAdapter(Activity context, List<Recording> list) {
            super(context, R.layout.rec_widget, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewWarpper wrapper = null;

            Recording rec = list.get(position);
            Channel ch = rec.channel;

            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(R.layout.rec_widget, null, false);

                wrapper = new ViewWarpper(row);
                row.setTag(wrapper);

            } else {
                wrapper = (ViewWarpper) row.getTag();
            }

            wrapper.title.setText(rec.title);
            wrapper.icon.setBackgroundDrawable(ch.iconDrawable);
            wrapper.channel.setText(ch.name);
            wrapper.date.setText(DateFormat.getMediumDateFormat(getContext()).format(rec.start));

            if (rec.error != null) {
                wrapper.message.setText(rec.error == null ? rec.state : rec.error);
                wrapper.icon.setImageResource(R.drawable.ic_error_small);
            } else if ("completed".equals(rec.state)) {
                wrapper.message.setText(getString(R.string.pvr_completed));
                wrapper.icon.setImageResource(R.drawable.ic_success_small);
            } else if ("invalid".equals(rec.state)) {
                wrapper.message.setText(getString(R.string.pvr_invalid));
                wrapper.icon.setImageResource(R.drawable.ic_error_small);
            } else if ("missed".equals(rec.state)) {
                wrapper.message.setText(getString(R.string.pvr_missed));
                wrapper.icon.setImageResource(R.drawable.ic_error_small);
            } else if ("recording".equals(rec.state)) {
                wrapper.message.setText(getString(R.string.pvr_recording));
                wrapper.icon.setImageResource(R.drawable.ic_rec_small);
            } else if ("scheduled".equals(rec.state)) {
                wrapper.message.setText(getString(R.string.pvr_scheduled));
                wrapper.icon.setImageDrawable(null);
            } else {
                wrapper.message.setText("");
                wrapper.icon.setImageDrawable(null);
            }

            wrapper.time.setText(
                    DateFormat.getTimeFormat(getContext()).format(rec.start)
                    + " - "
                    + DateFormat.getTimeFormat(getContext()).format(rec.stop));
            return row;
        }
    }
}
