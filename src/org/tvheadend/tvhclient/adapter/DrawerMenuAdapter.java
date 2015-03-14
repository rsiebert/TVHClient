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
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DrawerMenuAdapter extends ArrayAdapter<DrawerMenuItem> {

    @SuppressWarnings("unused")
    private final static String TAG = DrawerMenuAdapter.class.getSimpleName();

    private Activity context;
    private SharedPreferences prefs;
    private List<DrawerMenuItem> list;
    private int selectedPosition;
    private boolean lightTheme;

    public DrawerMenuAdapter(Activity context, List<DrawerMenuItem> list) {
        super(context, R.layout.list_layout, list);
        this.context = context;
        this.list = list;

        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.lightTheme = prefs.getBoolean("lightThemePref", true);
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    static class ViewHolder {
        public View divider;
        public RelativeLayout itemLayout;
        public ImageView icon;
        public TextView title;
        public TextView count;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        final DrawerMenuItem m = getItem(position);
        if (view == null) {
            // Inflate the section layout if a section shall be shown, otherwise
            // inflate the regular menu item layout or one that has no contents
            // and is pretty much invisible
            if (m.isSection) {
                view = context.getLayoutInflater().inflate(R.layout.drawer_list_section, parent, false);
            } else {
                if (m.isVisible) {
                    view = context.getLayoutInflater().inflate(R.layout.drawer_list_item, parent, false);
                } else {
                    view = context.getLayoutInflater().inflate(R.layout.drawer_list_item_empty, parent, false);
                }
            }
            holder = new ViewHolder();
            holder.divider = (View) view.findViewById(R.id.divider);
            holder.itemLayout = (RelativeLayout) view.findViewById(R.id.item_layout);
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.count = (TextView) view.findViewById(R.id.count);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Highlight the selected position with a different color. This can't be
        // done with the list position because the section menu items mess up
        // the positions. So we need to check if the id of the menu item is the
        // right one.
        if (selectedPosition == m.id) {
            final int color = (lightTheme) ? context.getResources().getColor(
                    R.color.drawer_selected_light) : context.getResources().getColor(
                    R.color.drawer_selected_dark);
            if (holder.itemLayout != null) {
                holder.itemLayout.setBackgroundColor(color);
            }
        } else {
            final int color = context.getResources().getColor(android.R.color.transparent);
            if (holder.itemLayout != null) {
                holder.itemLayout.setBackgroundColor(color);
            }
        }
        
        // Apply the values to the available layout items
        if (m != null) {
            if (m.isSection) {
                view.setOnClickListener(null);
                view.setOnLongClickListener(null);
                view.setLongClickable(false);
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
        }
        return view;
    }

    /**
     * The main activity has access to the menu entries in the adapter to change
     * the visible value. The list position can't be used because the section
     * menu items mess up the positions. So the id is used to get the correct
     * menu item from the list.
     * 
     * @param id
     * @return
     */
    public DrawerMenuItem getItemById(final int id) {
        for (DrawerMenuItem item : list) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }
}
