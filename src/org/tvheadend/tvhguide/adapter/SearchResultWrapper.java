package org.tvheadend.tvhguide.adapter;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.Utils;
import org.tvheadend.tvhguide.model.Program;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchResultWrapper {

    Context ctx;
    TextView title;
    TextView channel;
    TextView time;
    TextView date;
    TextView duration;
    TextView description;
    TextView contentType;
    TextView seriesInfo;
    ImageView icon;
    ImageView state;

    public SearchResultWrapper(Context context, View base) {
        ctx = context;

        title = (TextView) base.findViewById(R.id.sr_title);
        channel = (TextView) base.findViewById(R.id.sr_channel);
        description = (TextView) base.findViewById(R.id.sr_desc);
        contentType = (TextView) base.findViewById(R.id.sr_content_type);
        seriesInfo = (TextView) base.findViewById(R.id.sr_series_info);
        time = (TextView) base.findViewById(R.id.sr_time);
        date = (TextView) base.findViewById(R.id.sr_date);
        duration = (TextView) base.findViewById(R.id.sr_duration);
        icon = (ImageView) base.findViewById(R.id.sr_icon);
        state = (ImageView) base.findViewById(R.id.sr_state);
    }

    public void repaint(Program p) {

        title.setText(p.title);
        title.invalidate();

        Utils.setChannelIcon(icon, channel, p.channel);
        Utils.setState(state, p.recording);

        Utils.setDate(date, p.start);
        Utils.setTime(time, p.start, p.stop);
        Utils.setDuration(duration, p.start, p.stop);

        Utils.setDescription(description, p.description);        
        Utils.setContentType(contentType, p.contentType);
        Utils.setSeriesInfo(seriesInfo, p.seriesInfo);
    }
}
