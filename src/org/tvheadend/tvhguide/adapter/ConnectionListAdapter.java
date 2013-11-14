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
package org.tvheadend.tvhguide.adapter;

import java.util.Comparator;
import java.util.List;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.model.Connection;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ConnectionListAdapter extends ArrayAdapter<Connection> {

    Activity context;
    List<Connection> list;

    public ConnectionListAdapter(Activity context, List<Connection> list) {
        super(context, R.layout.connection_list_widget, list);
        this.context = context;
        this.list = list;
    }

    public void sort() {
        sort(new Comparator<Connection>() {
            public int compare(Connection x, Connection y) {
                return (x.name.equals(y.name)) ? 0 : 1;
            }
        });
    }

    static class ViewHolder {
        public TextView title;
        public TextView summary;
        public ImageView selected;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.connection_list_widget, null);

            holder = new ViewHolder();
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.summary = (TextView) view.findViewById(R.id.summary);
            holder.selected = (ImageView) view.findViewById(R.id.selected);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder) view.getTag();
        }

        // Get the connection and assign all the values
        Connection c = getItem(position);
        if (c != null) {
            holder.title.setText(c.name);
            holder.summary.setText(c.address + ":" + c.port);
//            holder.selected.setVisibility(c.selected ? View.VISIBLE : View.GONE);
            
           // Set the active / inactive icon depending on the theme and selection status
           if (Utils.getThemeId(context) == R.style.CustomTheme_Light) {
               holder.selected.setImageResource(c.selected ? R.drawable.item_active_light : R.drawable.item_not_active_light);
           } else {
               holder.selected.setImageResource(c.selected ? R.drawable.item_active_dark : R.drawable.item_not_active_dark);
           }
        }
        return view;
    }

    public void update(Connection c) {
        int length = list.size();

        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id == c.id) {
                list.set(i, c);
                break;
            }
        }
    }
}
