package org.tvheadend.tvhguide.adapter;

import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.Utils;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Program;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ChannelListAdapter extends ArrayAdapter<Channel> {

    Activity context;
    List<Channel> list;

    public ChannelListAdapter(Activity context, List<Channel> list) {
        super(context, R.layout.channel_list_widget, list);
        this.context = context;
        this.list = list;
    }

    public void sort() {
        sort(new Comparator<Channel>() {
            public int compare(Channel x, Channel y) {
                return x.compareTo(y);
            }
        });
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView title;
        public TextView channel;
        public TextView time;
        public TextView duration;
        public ProgressBar progress;
        public ImageView state;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.channel_list_widget, null);

            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.channel = (TextView) view.findViewById(R.id.channel);
            holder.progress = (ProgressBar) view.findViewById(R.id.progress);
            holder.time = (TextView) view.findViewById(R.id.time);
            holder.duration = (TextView) view.findViewById(R.id.duration);
            holder.state = (ImageView) view.findViewById(R.id.state);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder) view.getTag();
        }

        // Get the program and assign all the values
        Channel c = getItem(position);
        if (c != null) {

            // Set the initial values
            holder.progress.setProgress(0);
            holder.channel.setText(c.name);
            Utils.setChannelIcon(holder.icon, null, c);
            
            // Add a small recording icon above the channel icon, if we are
            // recording the current program.
            if (c.isRecording()) {
                holder.state.setImageResource(R.drawable.ic_rec_small);
                holder.state.setVisibility(View.VISIBLE);
            } else {
                holder.state.setImageDrawable(null);
                holder.state.setVisibility(View.GONE);
            }
            
            // Get the iterator so we can check the channel status 
            Iterator<Program> it = c.epg.iterator();
            
            // Check if the channel is actually transmitting 
            // data and contains program data which can be shown.
            if (!c.isTransmitting && it.hasNext()) {
                holder.title.setText(R.string.ch_no_transmission);
            } else if (it.hasNext()) {
                
                // Get the program that is currently running
                // and set all the available values
                Program p = it.next();
                holder.title.setText(p.title);
                Utils.setTime(holder.time, p.start, p.stop);
                Utils.setDuration(holder.duration, p.start, p.stop);

                // Get the start and end times to calculate the progress.
                double durationTime = (p.stop.getTime() - p.start.getTime());
                double elapsedTime = new Date().getTime() - p.start.getTime();
                
                // Show the progress as a percentage
                double percent = 0;
                if (durationTime > 0)
                    percent = elapsedTime / durationTime;
                holder.progress.setProgress((int) Math.floor(percent * 100));
                holder.progress.setVisibility(View.VISIBLE);

            } else {
                // The channel does not provide program data. Hide the progress bar
                // and clear the time and duration texts. These two items provide 
                // some space so that the next list item is not too close.
                holder.title.setText(R.string.ch_no_data);
                holder.progress.setVisibility(View.GONE);
            }
        }
        return view;
    }

    public void update(Channel c) {
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
