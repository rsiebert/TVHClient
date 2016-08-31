package org.tvheadend.tvhclient.adapter;

import java.util.List;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.ChannelTag;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ChannelTagListAdapter extends ArrayAdapter<ChannelTag> {

    @SuppressWarnings("unused")
    private final static String TAG = ChannelTagListAdapter.class.getSimpleName();

    private final Activity context;
    private final SharedPreferences prefs;
    private final boolean showIcons;

    public ChannelTagListAdapter(Activity context, List<ChannelTag> list) {
        super(context, R.layout.list_layout, list);
        this.context = context;

        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.showIcons = prefs.getBoolean("showTagIconPref", true);
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView title;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        final ChannelTag m = getItem(position);
        if (view == null) {
            // Inflate the section layout if a section shall be shown, otherwise
            // inflate the regular menu item layout or one that has no contents
            // and is pretty much invisible
            view = context.getLayoutInflater().inflate(R.layout.channeltag_list_item, parent, false);
            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        
        // Apply the values to the available layout items
        if (m != null) {
            if (holder.icon != null) {
                holder.icon.setImageBitmap((m.iconBitmap != null) ? m.iconBitmap : null);
                holder.icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);
            }
            if (holder.title != null) {
                holder.title.setText(m.name);
            }
        }
        return view;
    }
}
