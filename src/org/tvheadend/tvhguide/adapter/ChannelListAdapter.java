package org.tvheadend.tvhguide.adapter;

import java.util.Comparator;
import java.util.List;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.model.Channel;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ChannelListAdapter extends ArrayAdapter<Channel> {

    public ChannelListAdapter(Activity context, List<Channel> list) {
        super(context, R.layout.channel_list_widget, list);
    }

    public void sort() {
        sort(new Comparator<Channel>() {

            public int compare(Channel x, Channel y) {
                return x.compareTo(y);
            }
        });
    }

    public void updateView(ListView listView, Channel channel) {
        for (int i = 0; i < listView.getChildCount(); i++) {
            View view = listView.getChildAt(i);
            int pos = listView.getPositionForView(view);
            Channel ch = (Channel) listView.getItemAtPosition(pos);

            if (view.getTag() == null || ch == null) {
                continue;
            }

            if (channel.id != ch.id) {
                continue;
            }

            ChannelListViewWrapper wrapper = (ChannelListViewWrapper) view.getTag();
            wrapper.repaint(channel);
            break;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ChannelListViewWrapper wrapper;

        Channel ch = getItem(position);
        Activity activity = (Activity) getContext();

        if (row == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            row = inflater.inflate(R.layout.channel_list_widget, null, false);
            row.requestLayout();
            wrapper = new ChannelListViewWrapper(row);
            row.setTag(wrapper);

        } else {
            wrapper = (ChannelListViewWrapper) row.getTag();
        }

        wrapper.repaint(ch);
        return row;
    }
}
