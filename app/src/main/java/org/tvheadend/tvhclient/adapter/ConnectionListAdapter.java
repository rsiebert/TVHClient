package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.Connection;

import java.util.Comparator;
import java.util.List;

public class ConnectionListAdapter extends ArrayAdapter<Connection> {

    private Activity context;
    private List<Connection> list;

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
        ViewHolder holder;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.connection_list_widget, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.summary = (TextView) view.findViewById(R.id.summary);
            holder.selected = (ImageView) view.findViewById(R.id.selected);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Get the connection and assign all the values
        Connection c = getItem(position);
        if (c != null) {
            holder.title.setText(c.name);
            String summary = c.address + ":" + c.port;
            holder.summary.setText(summary);
            
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
