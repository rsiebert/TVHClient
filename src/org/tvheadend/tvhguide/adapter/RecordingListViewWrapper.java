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
    TextView desc;
    ImageView icon;

    public RecordingListViewWrapper(View base) {
        title = (TextView) base.findViewById(R.id.rec_title);
        channel = (TextView) base.findViewById(R.id.rec_channel);
        time = (TextView) base.findViewById(R.id.rec_time);
        date = (TextView) base.findViewById(R.id.rec_date);
        duration = (TextView) base.findViewById(R.id.rec_duration);
        desc = (TextView) base.findViewById(R.id.rec_desc);
        icon = (ImageView) base.findViewById(R.id.rec_icon);
    }

    public void repaint(Recording rec) {

        title.setText(rec.title);
        title.invalidate();

        Utils.setChannelIcon(icon, channel, rec.channel);
        
        Utils.setDate(date, rec.start);
        Utils.setTime(time, rec.start, rec.stop);
        Utils.setDuration(duration, rec.start, rec.stop);
        
        Utils.setDescription(desc, rec.description);
    }
}
