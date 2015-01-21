package org.tvheadend.tvhclient.adapter;

import java.util.List;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.Channel;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SpinnerChannelItemAdapter extends ArrayAdapter<Channel> {

    @SuppressWarnings("unused")
    private final static String TAG = DrawerMenuAdapter.class.getSimpleName();

    private Activity context;
    private int layout;

    public SpinnerChannelItemAdapter(Activity context, List<Channel> list, int layout) {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView name;
        public TextView number;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(layout, null);

            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.name = (TextView) view.findViewById(R.id.name);
            holder.number = (TextView) view.findViewById(R.id.number);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Get the program and assign all the values
        final Channel c = getItem(position);
        if (c != null) {
            Utils.setChannelIcon(holder.icon, null, c);
            if (holder.name != null) {
                holder.name.setText(c.name);
            }
            if (holder.number != null) {
                holder.number.setText(String.valueOf(c.number));
            }
        }

        return view;
    }
}
