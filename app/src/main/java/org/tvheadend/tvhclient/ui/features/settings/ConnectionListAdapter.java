package org.tvheadend.tvhclient.ui.features.settings;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.util.MiscUtils;

class ConnectionListAdapter extends ArrayAdapter<Connection> {

    private final Activity context;

    ConnectionListAdapter(Activity context) {
        super(context, R.layout.connection_list_adapter);
        this.context = context;
    }

    static class ViewHolder {
        private TextView title;
        private TextView summary;
        private ImageView selected;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.connection_list_adapter, parent, false);
            holder = new ViewHolder();
            holder.title = view.findViewById(R.id.title);
            holder.summary = view.findViewById(R.id.summary);
            holder.selected = view.findViewById(R.id.selected);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Get the connection and assign all the values
        Connection c = getItem(position);
        if (c != null) {
            holder.title.setText(c.getName());
            String summary = c.getHostname() + ":" + c.getPort();
            holder.summary.setText(summary);

            // Set the active / inactive icon depending on the theme and selection status
            if (MiscUtils.getThemeId(context) == R.style.CustomTheme_Light) {
                holder.selected.setImageResource(c.isActive() ? R.drawable.item_active_light : R.drawable.item_not_active_light);
            } else {
                holder.selected.setImageResource(c.isActive() ? R.drawable.item_active_dark : R.drawable.item_not_active_dark);
            }
        }
        return view;
    }
}
