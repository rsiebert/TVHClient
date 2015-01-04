package org.tvheadend.tvhclient.adapter;

import java.util.List;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.DrawerMenuItem;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DrawerMenuAdapter extends ArrayAdapter<DrawerMenuItem> {

    @SuppressWarnings("unused")
    private final static String TAG = DrawerMenuAdapter.class.getSimpleName();

    private Activity context;
    private SharedPreferences prefs;
    private List<DrawerMenuItem> list;
    private int layout;
    private int selectedPosition;
    private boolean lightTheme;

    public DrawerMenuAdapter(Activity context, List<DrawerMenuItem> list, int layout) {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;

        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.lightTheme = prefs.getBoolean("lightThemePref", true);
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    static class ViewHolder {
        public View divider;
        public LinearLayout itemLayout;
        public ImageView icon;
        public TextView title;
        public TextView count;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(layout, null);

            holder = new ViewHolder();
            holder.divider = (View) view.findViewById(R.id.divider);
            holder.itemLayout = (LinearLayout) view.findViewById(R.id.item_layout);
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.count = (TextView) view.findViewById(R.id.count);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (selectedPosition == position) {
            final int color = (lightTheme) ? context.getResources().getColor(
                    R.color.drawer_selected_light) : context.getResources().getColor(
                    R.color.drawer_selected_dark);
            holder.itemLayout.setBackgroundColor(color);
        } else {
            final int color = context.getResources().getColor(android.R.color.transparent);
            holder.itemLayout.setBackgroundColor(color);
        }

        // Get the program and assign all the values
        final DrawerMenuItem m = getItem(position);
        if (m != null) {
            if (holder.divider != null) {
                holder.divider.setVisibility((position == 0 || position == 7) ? View.VISIBLE : View.GONE);
            }
            if (holder.icon != null) {
                holder.icon.setImageResource(m.icon);
                holder.icon.setVisibility((m.icon != 0) ? ImageView.VISIBLE : ImageView.GONE);
            }
            if (holder.title != null) {
                holder.title.setText(m.title);
            }
            if (holder.count != null) {
                holder.count.setText(String.valueOf(m.count));
                holder.count.setVisibility((m.count > 0) ? View.VISIBLE : View.GONE);
            }
            
            // Hide the entire menu item if it shall not be visible. This is the
            // case if the server does not support series recordings.
            if (holder.itemLayout != null) {
                holder.itemLayout.setVisibility(m.isVisible ? View.VISIBLE : View.GONE);
            }
        }

        return view;
    }

    public void update(DrawerMenuItem m) {
        int length = list.size();

        // Go through the list of menu items and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id == m.id) {
                list.set(i, m);
                break;
            }
        }
    }
}
