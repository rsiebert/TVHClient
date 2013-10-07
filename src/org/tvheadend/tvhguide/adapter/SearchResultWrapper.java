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

        title = (TextView) base.findViewById(R.id.title);
        channel = (TextView) base.findViewById(R.id.channel);
        description = (TextView) base.findViewById(R.id.desc);
        contentType = (TextView) base.findViewById(R.id.content_type);
        seriesInfo = (TextView) base.findViewById(R.id.series_info);
        time = (TextView) base.findViewById(R.id.time);
        date = (TextView) base.findViewById(R.id.date);
        duration = (TextView) base.findViewById(R.id.duration);
        icon = (ImageView) base.findViewById(R.id.icon);
        state = (ImageView) base.findViewById(R.id.state);
    }

    public void repaint(Program p) {

        title.setText(p.title);
        title.invalidate();

        Utils.setChannelIcon(icon, channel, p.channel);
        Utils.setState(state, p.recording);

        Utils.setDate(date, p.start);
        Utils.setTime(time, p.start, p.stop);
        Utils.setDuration(duration, p.start, p.stop);

        Utils.setDescription(null, description, p.description);        
        Utils.setContentType(null, contentType, p.contentType);
        Utils.setSeriesInfo(null, seriesInfo, p.seriesInfo);
    }
}
