package org.tvheadend.tvhguide.adapter;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.Utils;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Recording;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
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
        Channel ch = rec.channel;

        title.setText(rec.title);
        title.invalidate();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
        Boolean showIcons = prefs.getBoolean("showIconPref", false);
        
        icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);
        if(ch != null) {
            icon.setImageBitmap(ch.iconBitmap);
            channel.setText(ch.name);
        } else {
            icon.setImageBitmap(null);
            channel.setText("");
        }
        channel.invalidate();

        Utils.setDate(date, rec.start);

        desc.setText(rec.description);
        desc.invalidate();

        icon.invalidate();

        time.setText(
                DateFormat.getTimeFormat(time.getContext()).format(rec.start)
                + " - "
                + DateFormat.getTimeFormat(time.getContext()).format(rec.stop));
        time.invalidate();
        
        // Get the start and end times so we can show them 
        // and calculate the duration.
        double durationTime = (rec.stop.getTime() - rec.start.getTime());
        
        // Show the duration in minutes
        durationTime = (durationTime / 1000 / 60);
        duration.setText(duration.getContext().getString(R.string.ch_minutes, (int)durationTime));
        duration.invalidate();
    }
}
