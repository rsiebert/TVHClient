package org.tvheadend.tvhguide.adapter;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.Utils;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Recording;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
        icon.invalidate();
        
        Utils.setDate(date, rec.start);
        Utils.setTime(time, rec.start, rec.stop);
        
        desc.setText(rec.description);
        desc.invalidate();
        
        // Show the duration in minutes
        Utils.setDuration(duration, rec.start, rec.stop);
    }
}
