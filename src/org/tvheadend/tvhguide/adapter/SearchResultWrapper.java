package org.tvheadend.tvhguide.adapter;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.TVHGuideApplication;
import org.tvheadend.tvhguide.Utils;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Program;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchResultWrapper {

    private SparseArray<String> contentTypeList;
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
        
        contentTypeList = TVHGuideApplication.getContentTypes(ctx);
    }

    public void repaint(Program p) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
        Boolean showIcons = prefs.getBoolean("showIconPref", false);
        icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);

        Channel ch = p.channel;
        if(ch != null) {
            icon.setImageBitmap(ch.iconBitmap);
            channel.setText(ch.name);
        } else {
            icon.setImageBitmap(null);
            channel.setText("");
        }
        channel.invalidate();
        
        title.setText(p.title);
        title.invalidate();

        Utils.setState(state, p.recording);

        // Set the series information if available
        String s = Utils.buildSeriesInfoString(ctx, p.seriesInfo);
        if (s.length() == 0) {
            seriesInfo.setVisibility(View.GONE);
        }
        else {
            seriesInfo.setText(s);
        }
        seriesInfo.invalidate();

        // Show the duration in minutes
        Utils.setDuration(duration, p.start, p.stop);

        description.setText(p.description);
        description.invalidate();

        String ct = contentTypeList.get(p.contentType, "");
        if (contentType.length() > 0) {
            contentType.setText(ct);
            contentType.setVisibility(TextView.VISIBLE);
        }
        else {
            contentType.setVisibility(TextView.GONE);
        }
        contentType.invalidate();

        Utils.setDate(date, p.start);
        Utils.setTime(time, p.start, p.stop);
    }
}
