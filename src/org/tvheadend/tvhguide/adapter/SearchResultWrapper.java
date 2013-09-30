package org.tvheadend.tvhguide.adapter;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.TVHGuideApplication;
import org.tvheadend.tvhguide.Utils;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Program;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchResultWrapper {

    private SparseArray<String> contentTypes;
    Context ctx;
    TextView title;
    TextView channel;
    TextView time;
    TextView date;
    TextView duration;
    TextView description;
    ImageView icon;
    ImageView state;

    public SearchResultWrapper(Context context, View base) {
        ctx = context;
        title = (TextView) base.findViewById(R.id.sr_title);
        channel = (TextView) base.findViewById(R.id.sr_channel);
        description = (TextView) base.findViewById(R.id.sr_desc);

        time = (TextView) base.findViewById(R.id.sr_time);
        date = (TextView) base.findViewById(R.id.sr_date);
        duration = (TextView) base.findViewById(R.id.sr_duration);
        icon = (ImageView) base.findViewById(R.id.sr_icon);
        state = (ImageView) base.findViewById(R.id.sr_state);
        
        contentTypes = TVHGuideApplication.getContentTypes(ctx);
    }

    public void repaint(Program p) {
        Channel ch = p.channel;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
        Boolean showIcons = prefs.getBoolean("showIconPref", false);
        icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);
        icon.setImageBitmap(ch.iconBitmap);

        title.setText(p.title);

        if (p.recording == null) {
            state.setImageDrawable(null);
        }
        else if (p.recording.error != null) {
            state.setImageResource(R.drawable.ic_error_small);
        }
        else if ("completed".equals(p.recording.state)) {
            state.setImageResource(R.drawable.ic_success_small);
        }
        else if ("invalid".equals(p.recording.state)) {
            state.setImageResource(R.drawable.ic_error_small);
        }
        else if ("missed".equals(p.recording.state)) {
            state.setImageResource(R.drawable.ic_error_small);
        }
        else if ("recording".equals(p.recording.state)) {
            state.setImageResource(R.drawable.ic_rec_small);
        }
        else if ("scheduled".equals(p.recording.state)) {
            state.setImageResource(R.drawable.ic_schedule_small);
        }
        else {
            state.setImageDrawable(null);
        }

        title.invalidate();

        String s = Utils.buildSeriesInfoString(ctx, p.seriesInfo);
        if (s.length() == 0) {
            s = p.description;
        }

        // Get the start and end times so we can show them
        // and calculate the duration. Then show the duration in minutes
        double durationTime = (p.stop.getTime() - p.start.getTime());
        durationTime = (durationTime / 1000 / 60);
        if (durationTime > 0) {
            duration.setText(duration.getContext().getString(R.string.ch_minutes, (int) durationTime));
        }
        else {
            duration.setVisibility(View.GONE);
        }
        duration.invalidate();

        description.setText(s);
        description.invalidate();

        String contentType = contentTypes.get(p.contentType, "");
        if (contentType.length() > 0) {
            channel.setText(ch.name + " (" + contentType + ")");
        }
        else {
            channel.setText(ch.name);
        }
        channel.invalidate();

        date.setText(Utils.getStartDate(ctx, p.start));
        date.invalidate();

        time.setText(DateFormat.getTimeFormat(time.getContext()).format(p.start) + " - "
                + DateFormat.getTimeFormat(time.getContext()).format(p.stop));
        time.invalidate();
    }
}
