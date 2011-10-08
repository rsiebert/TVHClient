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
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.me.tvhguide.htsp.HTSListener;
import org.me.tvhguide.htsp.HTSService;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.Programme;
import org.me.tvhguide.model.Recording;

/**
 *
 * @author john-tornblom
 */
public class ProgrammeListActivity extends ListActivity implements HTSListener {

    private ProgrammeListAdapter prAdapter;
    private Channel channel;
    private String[] contentTypes;
    private Pattern pattern;
    private boolean hideIcons;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        List<Programme> prList = new ArrayList<Programme>();
        Intent intent = getIntent();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
            Intent sIntent = new Intent(ProgrammeListActivity.this, HTSService.class);
            sIntent.setAction(HTSService.ACTION_EPG_QUERY);
            sIntent.putExtra("query", query);
            startService(sIntent);
        } else {
            TVHGuideApplication app = (TVHGuideApplication) getApplication();
            channel = app.getChannel(getIntent().getLongExtra("channelId", 0));
        }
        if (pattern == null && channel == null) {
            finish();
            return;
        }

        if (channel != null && !channel.epg.isEmpty()) {
            setTitle(channel.name);
            prList.addAll(channel.epg);

            Button btn = new Button(this);
            btn.setText(R.string.pr_get_more);
            btn.setOnClickListener(new OnClickListener() {

                public void onClick(View view) {
                    Programme p = null;

                    Iterator<Programme> it = channel.epg.iterator();
                    long nextId = 0;

                    while (it.hasNext()) {
                        p = it.next();
                        if (p.id != nextId && nextId != 0) {
                            break;
                        }
                        nextId = p.nextId;
                    }

                    if (nextId == 0) {
                        nextId = p.nextId;
                    }
                    if (nextId == 0) {
                        nextId = p.id;
                    }
                    Intent intent = new Intent(ProgrammeListActivity.this, HTSService.class);
                    intent.setAction(HTSService.ACTION_GET_EVENTS);
                    intent.putExtra("eventId", nextId);
                    intent.putExtra("channelId", channel.id);
                    intent.putExtra("count", 10);
                    startService(intent);
                }
            });
            getListView().addFooterView(btn);

        } else if (pattern != null) {
            TVHGuideApplication app = (TVHGuideApplication) getApplication();

            for (Channel ch : app.getChannels()) {
                for (Programme p : ch.epg) {
                    if (pattern.matcher(p.title).find()) {
                        prList.add(p);
                    }
                }
            }
        } else {
            finish();
            return;
        }

        registerForContextMenu(getListView());
        contentTypes = getResources().getStringArray(R.array.pr_type);

        prAdapter = new ProgrammeListAdapter(this, prList);
        prAdapter.sort();
        setListAdapter(prAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean b = !prefs.getBoolean("loadIcons", false);
        if (b != hideIcons) {
            prAdapter.notifyDataSetInvalidated();
        }
        hideIcons = b;
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
        Programme p = (Programme) prAdapter.getItem(position);

        Intent intent = new Intent(this, ProgrammeActivity.class);
        intent.putExtra("eventId", p.id);
        intent.putExtra("channelId", p.channel.id);
        startActivity(intent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.menu_record:
            case R.string.menu_record_cancel:
            case R.string.menu_record_remove: {
                startService(item.getIntent());
                return true;
            }
            default: {
                return super.onContextItemSelected(item);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Programme p = prAdapter.getItem(info.position);

        menu.setHeaderTitle(p.title);

        Intent intent = new Intent(this, HTSService.class);

        MenuItem item = null;

        if (p.recording == null) {
            intent.setAction(HTSService.ACTION_DVR_ADD);
            intent.putExtra("eventId", p.id);
            intent.putExtra("channelId", p.channel.id);
            item = menu.add(ContextMenu.NONE, R.string.menu_record, ContextMenu.NONE, R.string.menu_record);
        } else if ("recording".equals(p.recording.state) || "scheduled".equals(p.recording.state)) {
            intent.setAction(HTSService.ACTION_DVR_CANCEL);
            intent.putExtra("id", p.recording.id);
            item = menu.add(ContextMenu.NONE, R.string.menu_record_cancel, ContextMenu.NONE, R.string.menu_record_cancel);
        } else {
            intent.setAction(HTSService.ACTION_DVR_DELETE);
            intent.putExtra("id", p.recording.id);
            item = menu.add(ContextMenu.NONE, R.string.menu_record_remove, ContextMenu.NONE, R.string.menu_record_remove);
        }

        item.setIntent(intent);
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_ADD)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Programme p = (Programme) obj;
                    if (channel != null && p.channel.id == channel.id) {
                        prAdapter.add(p);
                        prAdapter.notifyDataSetChanged();
                        prAdapter.sort();
                    } else if (pattern != null && pattern.matcher(p.title).find()) {
                        prAdapter.add(p);
                        prAdapter.notifyDataSetChanged();
                        prAdapter.sort();
                    }
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_DELETE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Programme p = (Programme) obj;
                    prAdapter.remove(p);
                    prAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Programme p = (Programme) obj;
                    prAdapter.updateView(getListView(), p);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Recording rec = (Recording) obj;
                    for (Programme p : prAdapter.list) {
                        if (rec == p.recording) {
                            prAdapter.updateView(getListView(), p);
                            return;
                        }
                    }
                }
            });
        }
    }

    private class ViewWarpper {

        TextView title;
        TextView channel;
        TextView time;
        TextView date;
        TextView description;
        ImageView icon;
        ImageView state;

        public ViewWarpper(View base) {
            title = (TextView) base.findViewById(R.id.pr_title);
            channel = (TextView) base.findViewById(R.id.pr_channel);
            description = (TextView) base.findViewById(R.id.pr_desc);

            time = (TextView) base.findViewById(R.id.pr_time);
            date = (TextView) base.findViewById(R.id.pr_date);

            icon = (ImageView) base.findViewById(R.id.pr_icon);
            state = (ImageView) base.findViewById(R.id.pr_state);
        }

        public void repaint(Programme p) {
            Channel ch = p.channel;

            if (hideIcons || pattern == null) {
                icon.setVisibility(ImageView.GONE);
            } else {
                icon.setBackgroundDrawable(ch.iconDrawable);
                icon.setVisibility(ImageView.VISIBLE);
            }

            title.setText(p.title);

            if (p.recording == null) {
                state.setImageDrawable(null);
            } else if (p.recording.error != null) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("completed".equals(p.recording.state)) {
                state.setImageResource(R.drawable.ic_success_small);
            } else if ("invalid".equals(p.recording.state)) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("missed".equals(p.recording.state)) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("recording".equals(p.recording.state)) {
                state.setImageResource(R.drawable.ic_rec_small);
            } else if ("scheduled".equals(p.recording.state)) {
                state.setImageResource(R.drawable.ic_schedule_small);
            } else {
                state.setImageDrawable(null);
            }

            title.invalidate();

            date.setText(DateFormat.getMediumDateFormat(date.getContext()).format(p.start));
            date.invalidate();

            description.setText(p.description);
            description.invalidate();

            if (p.type > 0 && p.type < 11) {
                String str = contentTypes[p.type - 1];
                channel.setText(ch.name + " (" + str + ")");
            } else {
                channel.setText(ch.name);
            }
            channel.invalidate();

            date.setText(DateFormat.getMediumDateFormat(date.getContext()).format(p.start));
            date.invalidate();

            time.setText(
                    DateFormat.getTimeFormat(time.getContext()).format(p.start)
                    + " - "
                    + DateFormat.getTimeFormat(time.getContext()).format(p.stop));
            time.invalidate();
        }
    }

    class ProgrammeListAdapter extends ArrayAdapter<Programme> {

        Activity context;
        List<Programme> list;

        ProgrammeListAdapter(Activity context, List<Programme> list) {
            super(context, R.layout.pr_widget, list);
            this.context = context;
            this.list = list;
        }

        public void sort() {
            sort(new Comparator<Programme>() {

                public int compare(Programme x, Programme y) {
                    return x.compareTo(y);
                }
            });
        }

        public void updateView(ListView listView, Programme programme) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                View view = listView.getChildAt(i);
                int pos = listView.getPositionForView(view);
                Programme pr = (Programme) listView.getItemAtPosition(pos);

                if (view.getTag() == null || pr == null) {
                    continue;
                }

                if (programme.id != pr.id) {
                    continue;
                }

                ViewWarpper wrapper = (ViewWarpper) view.getTag();
                wrapper.repaint(programme);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewWarpper wrapper = null;

            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(R.layout.pr_widget, null, false);

                wrapper = new ViewWarpper(row);
                row.setTag(wrapper);

            } else {
                wrapper = (ViewWarpper) row.getTag();
            }

            Programme p = getItem(position);
            wrapper.repaint(p);
            return row;
        }
    }
}
