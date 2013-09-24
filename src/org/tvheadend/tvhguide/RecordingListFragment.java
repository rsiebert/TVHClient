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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Recording;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 *
 * @author john-tornblom
 */
public class RecordingListFragment extends Fragment implements HTSListener {

    private RecordingListAdapter recAdapter;
    private ListView recListView;
    private int tabIndex = 0;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null)
            return null;
        
        View v = inflater.inflate(R.layout.recording_list, container, false);
        recListView = (ListView) v.findViewById(R.id.recording_list);
        
        // Get the passed argument so we know which recording type to display
        Bundle bundle = getArguments();
        if (bundle != null)
            tabIndex = bundle.getInt("tabIndex", 0);
        
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<Recording> recList = new ArrayList<Recording>();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        
        // Show only the recordings that belong to the tab
        for (Recording rec : app.getRecordings()) {
            Log.i("Recordings", "Tab: " + tabIndex + ", title: " + rec.title + ", state: " + rec.state);
            if (tabIndex == 0 && rec.state.equals("completed")) {
                recList.add(rec);
            }
            else if (tabIndex == 1 && (rec.state.equals("scheduled") || rec.state.equals("recording"))) {
                recList.add(rec);
            }
            else if (tabIndex == 2 && (rec.state.equals("missed") || rec.state.equals("invalid"))) {
                recList.add(rec);
            }
            else if (tabIndex == 3 && rec.state.equals("autorec")) {
                recList.add(rec);
            }
        }

        recAdapter = new RecordingListAdapter(getActivity(), recList);
        recAdapter.sort();
        recListView.setAdapter(recAdapter);
        registerForContextMenu(recListView);
        
        getActivity().getActionBar().setSubtitle(recList.size() + " " + getString(R.string.menu_recordings));

        recListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Recording rec = (Recording) recAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), RecordingActivity.class);
                intent.putExtra("id", rec.id);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.removeListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuItem item = null;
        Intent intent = null;

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Recording rec = recAdapter.getItem(info.position);

        menu.setHeaderTitle(rec.title);

        intent = new Intent(getActivity(), HTSService.class);
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
            intent = new Intent(getActivity(), ExternalPlaybackActivity.class);
            intent.putExtra("dvrId", rec.id);
            item.setIntent(intent);
            item.setIcon(android.R.drawable.ic_menu_view);
        }

        item = menu.add(ContextMenu.NONE, R.string.search_hint, ContextMenu.NONE, R.string.search_hint);
        item.setIntent(new SearchEPGIntent(getActivity(), rec.title));
        item.setIcon(android.R.drawable.ic_menu_search);

        item = menu.add(ContextMenu.NONE, ContextMenu.NONE, ContextMenu.NONE, "IMDb");
        item.setIntent(new SearchIMDbIntent(getActivity(), rec.title));
        item.setIcon(android.R.drawable.ic_menu_info_details);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.string.menu_record:
            case R.string.menu_record_cancel: {
                getActivity().startService(item.getIntent());
                return true;
            }
            case R.string.menu_record_remove: {
                new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.menu_record_remove)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) { 
                            getActivity().startService(item.getIntent());
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
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
                    recAdapter.list.clear();
                    recAdapter.list.addAll(app.getRecordings());
                    recAdapter.notifyDataSetChanged();
                    recAdapter.sort();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_ADD)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    recAdapter.add((Recording) obj);
                    recAdapter.notifyDataSetChanged();
                    recAdapter.sort();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_DELETE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    recAdapter.remove((Recording) obj);
                    recAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_UPDATE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Recording rec = (Recording) obj;
                    recAdapter.updateView(recListView, rec);
                }
            });
        }
    }

    private class ViewWrapper {

        TextView title;
        TextView channel;
        TextView time;
        TextView date;
        TextView duration;
        TextView desc;
        ImageView icon;
        ImageView state;

        public ViewWrapper(View base) {
            title = (TextView) base.findViewById(R.id.rec_title);
            channel = (TextView) base.findViewById(R.id.rec_channel);
            time = (TextView) base.findViewById(R.id.rec_time);
            date = (TextView) base.findViewById(R.id.rec_date);
            duration = (TextView) base.findViewById(R.id.rec_duration);
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
                // Show the string today
                date.setText(getString(R.string.today));
            } else if(rec.start.getTime() < System.currentTimeMillis() + 1000*60*60*24*2 &&
                      rec.start.getTime() > System.currentTimeMillis() - 1000*60*60*24*2) {
                // Show a string like "42 minutes ago"
                date.setText(DateUtils.getRelativeTimeSpanString(rec.start.getTime(),
                        System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS));
            } else if (rec.start.getTime() < System.currentTimeMillis() + 1000*60*60*24*6 &&
            		   rec.start.getTime() > System.currentTimeMillis() - 1000*60*60*24*2) {
                // Show the day of the week, like Monday or Tuesday
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
            	date.setText(sdf.format(rec.start.getTime()));
            } else {
                // Show the regular date format like 31.07.2013
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
                date.setText(sdf.format(rec.start.getTime()));
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

            desc.setText(rec.description);
            desc.invalidate();

            icon.invalidate();

            time.setText(
                    DateFormat.getTimeFormat(time.getContext()).format(rec.start)
                    + " - "
                    + DateFormat.getTimeFormat(time.getContext()).format(rec.stop));
            time.invalidate();
            
            // Get the start and end times so we can show them 
            // and calculate the duration.
            double durationTime = (rec.stop.getTime() - rec.start.getTime());
            
            // Show the duration in minutes
            durationTime = (durationTime / 1000 / 60);
            duration.setText(duration.getContext().getString(R.string.ch_minutes, (int)durationTime));
            duration.invalidate();
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

                ViewWrapper wrapper = (ViewWrapper) view.getTag();
                wrapper.repaint(recording);
                break;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewWrapper wrapper = null;

            Recording rec = list.get(position);

            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(R.layout.recording_list_widget, null, false);

                wrapper = new ViewWrapper(row);
                row.setTag(wrapper);

            } else {
                wrapper = (ViewWrapper) row.getTag();
            }

            wrapper.repaint(rec);
            return row;
        }
    }
}
