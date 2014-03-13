/*
 *  Copyright (C) 2013 Robert Siebert
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
package org.tvheadend.tvhclient.adapter;

import java.util.Comparator;
import java.util.List;

import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.R;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ProgramListAdapter extends ArrayAdapter<Program> {

    Activity context;
    List<Program> list;

    public ProgramListAdapter(Activity context, List<Program> list) {
        super(context, R.layout.program_list_widget, list);
        this.context = context;
        this.list = list;
    }

    public void sort() {
        sort(new Comparator<Program>() {
            public int compare(Program x, Program y) {
                return x.compareTo(y);
            }
        });
    }

    static class ViewHolder {
        public TextView title;
        public TextView time;
        public TextView date;
        public TextView duration;
        public TextView progress;
        public TextView description;
        public TextView seriesInfo;
        public TextView contentType;
        public ImageView state;
        public TextView genre;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.program_list_widget, null);
            holder = new ViewHolder();
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.state = (ImageView) view.findViewById(R.id.state);
            holder.time = (TextView) view.findViewById(R.id.time);
            holder.date = (TextView) view.findViewById(R.id.date);
            holder.duration = (TextView) view.findViewById(R.id.duration);
            holder.progress = (TextView) view.findViewById(R.id.progress);
            holder.seriesInfo = (TextView) view.findViewById(R.id.series_info);
            holder.contentType = (TextView) view.findViewById(R.id.content_type);
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.genre = (TextView) view.findViewById(R.id.genre);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Get the program and assign all the values
        Program p = getItem(position);
        if (p != null) {
            holder.title.setText(p.title);
            Utils.setState(holder.state, p.recording);
            Utils.setDate(holder.date, p.start);
            Utils.setTime(holder.time, p.start, p.stop);
            Utils.setDuration(holder.duration, p.start, p.stop);
            Utils.setProgressText(holder.progress, p.start, p.stop);
            Utils.setDescription(null, holder.description, p.description);
            Utils.setContentType(null, holder.contentType, p.contentType);
            Utils.setSeriesInfo(null, holder.seriesInfo, p.seriesInfo);
            Utils.setGenreColor(context, holder.genre, p.contentType);
        }
        return view;
    }

    public void update(Program p) {
        int length = list.size();

        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id == p.id) {
                list.set(i, p);
                break;
            }
        }
    }

    public List<Program> getList() {
        return list;
    }
}
