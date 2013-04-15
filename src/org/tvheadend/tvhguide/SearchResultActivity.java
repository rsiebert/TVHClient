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
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.SparseArray;
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
import java.util.regex.Pattern;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.R.string;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Programme;
import org.tvheadend.tvhguide.model.Recording;
import org.tvheadend.tvhguide.model.SeriesInfo;

/**
 *
 * @author john-tornblom
 */
public class SearchResultActivity extends ListActivity implements HTSListener {

    private SearchResultAdapter srAdapter;
    private SparseArray<String> contentTypes;
    private Pattern pattern;
    private Channel channel;

    @Override
    public void onCreate(Bundle icicle) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        setTheme(theme ? R.style.CustomTheme_Light : R.style.CustomTheme);

        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        registerForContextMenu(getListView());
        
        contentTypes = TVHGuideApplication.getContentTypes(this);

        List<Programme> srList = new ArrayList<Programme>();
        srAdapter = new SearchResultAdapter(this, srList);
        srAdapter.sort();
        setListAdapter(srAdapter);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.search_result_title);

        View v = findViewById(R.id.ct_btn);
        v.setOnClickListener(new android.view.View.OnClickListener() {

            public void onClick(View arg0) {
                onSearchRequested();
            }
        });

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (!Intent.ACTION_SEARCH.equals(intent.getAction())
                || !intent.hasExtra(SearchManager.QUERY)) {
            return;
        }

        TVHGuideApplication app = (TVHGuideApplication) getApplication();

        Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            channel = app.getChannel(appData.getLong("channelId"));
        } else {
            channel = null;
        }

        srAdapter.clear();

        String query = intent.getStringExtra(SearchManager.QUERY);
        pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        intent = new Intent(SearchResultActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_EPG_QUERY);
        intent.putExtra("query", query);
        if (channel != null) {
            intent.putExtra("channelId", channel.id);
        }

        startService(intent);

        if (channel == null) {
            for (Channel ch : app.getChannels()) {
                for (Programme p : ch.epg) {
                    if (pattern.matcher(p.title).find()) {
                        srAdapter.add(p);
                    }
                }
            }
        } else {
            for (Programme p : channel.epg) {
                if (pattern.matcher(p.title).find()) {
                    srAdapter.add(p);
                }
            }
        }

        ImageView iv = (ImageView) findViewById(R.id.ct_logo);
        if (channel != null && channel.iconBitmap != null) {
            iv.setImageBitmap(channel.iconBitmap);
        } else {
            iv.setImageResource(R.drawable.logo_72);
        }

        TextView t = (TextView) findViewById(R.id.ct_title);
        t.setText(this.getString(android.R.string.search_go) + ": " + query);
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
        Programme p = (Programme) srAdapter.getItem(position);

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
        Programme p = srAdapter.getItem(info.position);

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
                    if (pattern != null && pattern.matcher(p.title).find()) {
                        srAdapter.add(p);
                        srAdapter.notifyDataSetChanged();
                        srAdapter.sort();
                    }
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_DELETE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Programme p = (Programme) obj;
                    srAdapter.remove(p);
                    srAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Programme p = (Programme) obj;
                    srAdapter.updateView(getListView(), p);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Recording rec = (Recording) obj;
                    for (Programme p : srAdapter.list) {
                        if (rec == p.recording) {
                            srAdapter.updateView(getListView(), p);
                            return;
                        }
                    }
                }
            });
        }
    }

	public String buildSeriesInfoString(SeriesInfo info) {
		if (info.onScreen != null && info.onScreen.length() > 0)
			return info.onScreen;

		String s = "";
		String season = this.getResources().getString(string.pr_season);
		String episode = this.getResources().getString(string.pr_episode);
		String part = this.getResources().getString(string.pr_part);
		
		if(info.onScreen.length() > 0) {
			return info.onScreen;
		}
		
		if (info.seasonNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %02d", season.toLowerCase(), info.seasonNumber);
		}
		if (info.episodeNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %02d", episode.toLowerCase(), info.episodeNumber);
		}
		if (info.partNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %d", part.toLowerCase(), info.partNumber);
		}

		if(s.length() > 0) {
			s = s.substring(0,1).toUpperCase() + s.substring(1);
		}
		
		return s;
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
            title = (TextView) base.findViewById(R.id.sr_title);
            channel = (TextView) base.findViewById(R.id.sr_channel);
            description = (TextView) base.findViewById(R.id.sr_desc);

            time = (TextView) base.findViewById(R.id.sr_time);
            date = (TextView) base.findViewById(R.id.sr_date);

            icon = (ImageView) base.findViewById(R.id.sr_icon);
            state = (ImageView) base.findViewById(R.id.sr_state);
        }

        public void repaint(Programme p) {
            Channel ch = p.channel;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
            Boolean showIcons = prefs.getBoolean("showIconPref", false);
            icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);
            icon.setImageBitmap(ch.iconBitmap);

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

            String s = buildSeriesInfoString(p.seriesInfo);
            if(s.length() == 0) {
            	s = p.description;
            }
            
            description.setText(s);
            description.invalidate();
            
            String contentType = contentTypes.get(p.contentType, "");
            if (contentType.length() > 0) {
                channel.setText(ch.name + " (" + contentType + ")");
            } else {
                channel.setText(ch.name);
            }
            channel.invalidate();

            if (DateUtils.isToday(p.start.getTime())) {
                date.setText(getString(R.string.today));
            } else if(p.start.getTime() < System.currentTimeMillis() + 1000*60*60*24*2 &&
                      p.start.getTime() > System.currentTimeMillis() - 1000*60*60*24*2) {
                date.setText(DateUtils.getRelativeTimeSpanString(p.start.getTime(),
                        System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS));
            } else if(p.start.getTime() < System.currentTimeMillis() + 1000*60*60*24*6 &&
            		  p.start.getTime() > System.currentTimeMillis() - 1000*60*60*24*2) {
            	date.setText(new SimpleDateFormat("EEEE").format(p.start.getTime()));
            } else {
                date.setText(DateFormat.getDateFormat(date.getContext()).format(p.start));
            }
            
            date.invalidate();

            
            time.setText(
                    DateFormat.getTimeFormat(time.getContext()).format(p.start)
                    + " - "
                    + DateFormat.getTimeFormat(time.getContext()).format(p.stop));
            time.invalidate();
        }
    }

    class SearchResultAdapter extends ArrayAdapter<Programme> {

        Activity context;
        List<Programme> list;

        SearchResultAdapter(Activity context, List<Programme> list) {
            super(context, R.layout.search_result_widget, list);
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
                break;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewWarpper wrapper = null;

            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(R.layout.search_result_widget, null, false);

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
