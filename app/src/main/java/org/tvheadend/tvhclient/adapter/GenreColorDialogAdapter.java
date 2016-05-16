package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.GenreColorDialogItem;

import java.util.List;

public class GenreColorDialogAdapter extends ArrayAdapter<GenreColorDialogItem> {
    private LayoutInflater inflater;
    private ViewHolder holder = null;
    
    public GenreColorDialogAdapter(Activity context, final List<GenreColorDialogItem> items) {
        super(context, R.layout.genre_color_dialog, items);
        this.inflater = context.getLayoutInflater();
    }

    public class ViewHolder {
        public TextView color;
        public TextView genre;
    }
    
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.genre_color_dialog, parent, false);
            holder = new ViewHolder();
            holder.color = (TextView) view.findViewById(R.id.color);
            holder.genre = (TextView) view.findViewById(R.id.genre);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        GenreColorDialogItem item = getItem(position);
        if (item != null) {
            holder.color.setBackgroundColor(item.color);
            holder.genre.setText(item.genre);
        }
        return view;
    }
}
