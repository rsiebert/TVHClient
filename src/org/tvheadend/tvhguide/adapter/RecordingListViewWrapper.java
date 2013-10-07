package org.tvheadend.tvhguide.adapter;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.Utils;
import org.tvheadend.tvhguide.model.Recording;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class RecordingListViewWrapper {

    TextView title;
    TextView channel;
    TextView time;
    TextView date;
    TextView duration;
    TextView description;
    ImageView icon;

    public RecordingListViewWrapper(View base) {
        title = (TextView) base.findViewById(R.id.title);
        channel = (TextView) base.findViewById(R.id.channel);
        time = (TextView) base.findViewById(R.id.time);
        date = (TextView) base.findViewById(R.id.date);
        duration = (TextView) base.findViewById(R.id.duration);
        description = (TextView) base.findViewById(R.id.description);
        icon = (ImageView) base.findViewById(R.id.icon);
    }

    public void repaint(Recording rec) {

        title.setText(rec.title);
        title.invalidate();

        Utils.setChannelIcon(icon, channel, rec.channel);
        
        Utils.setDate(date, rec.start);
        Utils.setTime(time, rec.start, rec.stop);
        Utils.setDuration(duration, rec.start, rec.stop);
        
        Utils.setDescription(null, description, rec.description);
    }
}
