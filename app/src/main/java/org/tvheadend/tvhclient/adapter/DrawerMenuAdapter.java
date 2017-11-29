package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.DrawerMenuItem;

import java.util.List;

public class DrawerMenuAdapter extends ArrayAdapter<DrawerMenuItem> {

    @SuppressWarnings("unused")
    private final static String TAG = DrawerMenuAdapter.class.getSimpleName();

    private final Activity context;
    private final SharedPreferences prefs;
    private final List<DrawerMenuItem> list;
    private int selectedPosition;
    private final boolean lightTheme;

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

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;
        final DrawerMenuItem m = getItem(position);

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
        RelativeLayout itemLayout = view.findViewById(R.id.item_layout);
        ImageView icon = view.findViewById(R.id.icon);
        TextView title = view.findViewById(R.id.title);
        TextView count = view.findViewById(R.id.count);

        // Highlight the selected position with a different color. This can't be
        // done with the list position because the section menu items mess up
        // the positions. So we need to check if the id of the menu item is the
        // right one.
        if (selectedPosition == m.id) {
            final int color = (lightTheme) ? context.getResources().getColor(
                    R.color.drawer_selected_light) : context.getResources().getColor(
                    R.color.drawer_selected_dark);
            if (itemLayout != null) {
                itemLayout.setBackgroundColor(color);
            }
        } else {
            final int color = context.getResources().getColor(android.R.color.transparent);
            if (itemLayout != null) {
                itemLayout.setBackgroundColor(color);
            }
        }
        
        // Apply the values to the available layout items
        if (m.isSection) {
            view.setOnClickListener(null);
            view.setOnLongClickListener(null);
            view.setLongClickable(false);
        }
        if (icon != null) {
            icon.setImageResource(m.icon);
            icon.setVisibility((m.icon != 0) ? ImageView.VISIBLE : ImageView.GONE);
        }
        if (title != null) {
            title.setText(m.title);
        }
        if (count != null) {
            count.setText(String.valueOf(m.count));
            count.setVisibility((m.count > 0) ? View.VISIBLE : View.GONE);
        }

        return view;
    }

    /**
     * The main activity has access to the menu entries in the adapter to change
     * the visible value. The list position can't be used because the section
     * menu items mess up the positions. So the id is used to get the correct
     * menu item from the list.
     * 
     * @param id Id of the menu item
     * @return Item object
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
