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
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Recording;

/**
 *
 * @author john-tornblom
 */
public class RecordingListActivity extends ListActivity implements HTSListener {

    private RecordingListAdapter recAdapter;

    @Override
    public void onCreate(Bundle icicle) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        setTheme(theme ? R.style.CustomTheme_Light : R.style.CustomTheme);

        super.onCreate(icicle);

        TVHGuideApplication app = (TVHGuideApplication) getApplication();

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        List<Recording> recList = new ArrayList<Recording>();
        recList.addAll(app.getRecordings());
        recAdapter = new RecordingListAdapter(this, recList);
        recAdapter.sort();
        setListAdapter(recAdapter);
        registerForContextMenu(getListView());
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.recording_list_title);
        TextView t = (TextView) findViewById(R.id.ct_title);

        t.setText(R.string.menu_recordings);
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

        MenuItem item = null;
        Intent intent = null;

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Recording rec = recAdapter.getItem(info.position);

        menu.setHeaderTitle(rec.title);

        intent = new Intent(RecordingListActivity.this, HTSService.class);
        intent.putExtra("id", rec.id);

        if (rec.isRecording() || rec.isScheduled()) {
            intent.setAction(HTSService.ACTION_DVR_CANCEL);
            item = menu.add(ContextMenu.NONE, R.string.menu_record_cancel, ContextMenu.NONE, R.string.menu_record_cancel);
            item.setIntent(intent);
        } else {
            intent.setAction(HTSService.ACTION_DVR_DELETE);
            item = menu.add(ContextMenu.NONE, R.string.menu_record_remove, ContextMenu.NONE, R.string.menu_record_remove);
            item.setIntent(intent);

            item = menu.add(ContextMenu.NONE, R.string.ch_play, ContextMenu.NONE, R.string.ch_play);
            intent = new Intent(this, ExternalPlaybackActivity.class);
            intent.putExtra("dvrId", rec.id);
            item.setIntent(intent);
            item.setIcon(android.R.drawable.ic_menu_view);
        }

        item = menu.add(ContextMenu.NONE, R.string.search_hint, ContextMenu.NONE, R.string.search_hint);
        item.setIntent(new SearchEPGIntent(this, rec.title));
        item.setIcon(android.R.drawable.ic_menu_search);

        item = menu.add(ContextMenu.NONE, ContextMenu.NONE, ContextMenu.NONE, "IMDb");
        item.setIntent(new SearchIMDbIntent(this, rec.title));
        item.setIcon(android.R.drawable.ic_menu_info_details);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.string.menu_record:
            case R.string.menu_record_cancel: {
                startService(item.getIntent());
                return true;
            }
            case R.string.menu_record_remove: {
                
                new AlertDialog.Builder(this)
                    .setTitle(R.string.menu_record_remove)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) { 
                            startService(item.getIntent());
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) { 
                            //NOP
                        }
                    })
                    .show();

                return true;
            }
            default: {
                return super.onContextItemSelected(item);
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
                    recAdapter.sort();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_ADD)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    recAdapter.add((Recording) obj);
                    recAdapter.notifyDataSetChanged();
                    recAdapter.sort();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_DELETE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    recAdapter.remove((Recording) obj);
                    recAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Recording rec = (Recording) obj;
                    recAdapter.updateView(getListView(), rec);
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
        TextView desc;
        ImageView icon;
        ImageView state;

        public ViewWarpper(View base) {
            title = (TextView) base.findViewById(R.id.rec_title);
            channel = (TextView) base.findViewById(R.id.rec_channel);

            time = (TextView) base.findViewById(R.id.rec_time);
            date = (TextView) base.findViewById(R.id.rec_date);
            message = (TextView) base.findViewById(R.id.rec_message);
            desc = (TextView) base.findViewById(R.id.rec_desc);
            icon = (ImageView) base.findViewById(R.id.rec_icon);
            state = (ImageView) base.findViewById(R.id.rec_state);
        }

        public void repaint(Recording rec) {
            Channel ch = rec.channel;

            title.setText(rec.title);
            title.invalidate();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
            Boolean showIcons = prefs.getBoolean("showIconPref", false);
            
            icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);
            if(ch != null) {
            	icon.setImageBitmap(ch.iconBitmap);
            	channel.setText(ch.name);
            } else {
            	icon.setImageBitmap(null);
            	channel.setText("");
            }
            channel.invalidate();

            if (DateUtils.isToday(rec.start.getTime())) {
                date.setText(getString(R.string.today));
            } else if(rec.start.getTime() < System.currentTimeMillis() + 1000*60*60*24*2 &&
                      rec.start.getTime() > System.currentTimeMillis() - 1000*60*60*24*2) {
                date.setText(DateUtils.getRelativeTimeSpanString(rec.start.getTime(),
                        System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS));
            } else if(rec.start.getTime() < System.currentTimeMillis() + 1000*60*60*24*6 &&
            		  rec.start.getTime() > System.currentTimeMillis() - 1000*60*60*24*2
            		) {
            	date.setText(new SimpleDateFormat("EEEE").format(rec.start.getTime()));
            } else {
                date.setText(DateFormat.getDateFormat(date.getContext()).format(rec.start));
            }

            date.invalidate();

            String msg = "";
            if (rec.error != null) {
                msg = rec.error;
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("completed".equals(rec.state)) {
                msg = getString(R.string.pvr_completed);
                state.setImageResource(R.drawable.ic_success_small);
            } else if ("invalid".equals(rec.state)) {
                msg = getString(R.string.pvr_invalid);
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("missed".equals(rec.state)) {
                msg = getString(R.string.pvr_missed);
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("recording".equals(rec.state)) {
                msg = getString(R.string.pvr_recording);
                state.setImageResource(R.drawable.ic_rec_small);
            } else if ("scheduled".equals(rec.state)) {
                msg = getString(R.string.pvr_scheduled);
                state.setImageResource(R.drawable.ic_schedule_small);
            } else {
                state.setImageDrawable(null);
            }
            if (msg.length() > 0) {
                message.setText("(" + msg + ")");
            } else {
                message.setText(msg);
            }
            message.invalidate();

            desc.setText(rec.description);
            desc.invalidate();

            icon.invalidate();

            time.setText(
                    DateFormat.getTimeFormat(time.getContext()).format(rec.start)
                    + " - "
                    + DateFormat.getTimeFormat(time.getContext()).format(rec.stop));
            time.invalidate();
        }
    }

    class RecordingListAdapter extends ArrayAdapter<Recording> {

        Activity context;
        List<Recording> list;

        RecordingListAdapter(Activity context, List<Recording> list) {
            super(context, R.layout.recording_list_widget, list);
            this.context = context;
            this.list = list;
        }

        public void sort() {
            sort(new Comparator<Recording>() {

                public int compare(Recording x, Recording y) {
                    return x.compareTo(y);
                }
            });
        }

        public void updateView(ListView listView, Recording recording) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                View view = listView.getChildAt(i);
                int pos = listView.getPositionForView(view);
                Recording rec = (Recording) listView.getItemAtPosition(pos);

                if (view.getTag() == null || rec == null) {
                    continue;
                }

                if (recording.id != rec.id) {
                    continue;
                }

                ViewWarpper wrapper = (ViewWarpper) view.getTag();
                wrapper.repaint(recording);
                break;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewWarpper wrapper = null;

            Recording rec = list.get(position);

            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(R.layout.recording_list_widget, null, false);

                wrapper = new ViewWarpper(row);
                row.setTag(wrapper);

            } else {
                wrapper = (ViewWarpper) row.getTag();
            }

            wrapper.repaint(rec);
            return row;
        }
    }
}
