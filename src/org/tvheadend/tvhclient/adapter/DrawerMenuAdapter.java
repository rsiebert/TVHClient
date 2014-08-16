package org.tvheadend.tvhclient.adapter;

import java.util.List;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.DrawerMenuItem;

import android.app.Activity;
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
    private List<DrawerMenuItem> list;
    private int layout;
    private int selectedPosition;

    public DrawerMenuAdapter(Activity context, List<DrawerMenuItem> list, int layout) {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
    }
    
    static class ViewHolder {
        public LinearLayout headerLayout;
        public TextView header;
        public LinearLayout menuLayout;
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
            holder.headerLayout = (LinearLayout) view.findViewById(R.id.header_layout);
            holder.header = (TextView) view.findViewById(R.id.header);
            holder.menuLayout = (LinearLayout) view.findViewById(R.id.menu_layout);
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.count = (TextView) view.findViewById(R.id.count);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        
        if (selectedPosition == position) {
//            final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
//            holder.dual_pane_list_item_selection.setBackgroundResource(icon);
        } else {
//            final int icon = (lightTheme) ? R.drawable.dual_pane_selector_light : R.drawable.dual_pane_selector_dark;
//            holder.dual_pane_list_item_selection.setBackgroundResource(icon);
        }
        
        // Get the program and assign all the values
        final DrawerMenuItem m = getItem(position);
        if (m != null) {
            
            // Show the header layout and the text if the menu item entry is not
            // marked as a menu 
            if (holder.headerLayout != null) {
                holder.headerLayout.setVisibility(m.isMenu ? View.GONE : View.VISIBLE);
            }
            if (holder.header != null) {
                holder.header.setText(m.header);
                holder.header.setVisibility((m.header.length() > 0) ? View.VISIBLE : View.GONE);
            }
            
            // Show the menu layout and the title and optionally the count if
            // the menu item entry is marked as a menu
            if (holder.menuLayout != null) {
                holder.menuLayout.setVisibility(m.isMenu ? View.VISIBLE : View.GONE);
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
