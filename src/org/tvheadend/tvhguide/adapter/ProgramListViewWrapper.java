package org.tvheadend.tvhguide.adapter;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.TVHGuideApplication;
import org.tvheadend.tvhguide.Utils;
import org.tvheadend.tvhguide.model.Program;

import android.app.Activity;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ProgramListViewWrapper {

    Context ctx;
    TextView title;
    TextView time;
    TextView seriesInfo;
    TextView date;
    TextView duration;
    TextView description;
    ImageView state;
    SparseArray<String> contentTypes;
    
    public ProgramListViewWrapper(Activity context, View base) {
        
        ctx = context;
        title = (TextView) base.findViewById(R.id.pr_title);
        description = (TextView) base.findViewById(R.id.pr_desc);
        seriesInfo = (TextView) base.findViewById(R.id.pr_series_info);
        
        time = (TextView) base.findViewById(R.id.pr_time);
        date = (TextView) base.findViewById(R.id.pr_date);
        duration = (TextView) base.findViewById(R.id.pr_duration);

        state = (ImageView) base.findViewById(R.id.pr_state);
        contentTypes = TVHGuideApplication.getContentTypes(context);
    }

    public void repaint(Program p) {
        title.setText(p.title);
        title.invalidate();
        
        Utils.setState(state, p.recording);

        String s = Utils.buildSeriesInfoString(ctx, p.seriesInfo);
        if(s.length() == 0) {
            s = contentTypes.get(p.contentType);
        }
        
        seriesInfo.setText(s);
        seriesInfo.invalidate();

        if (p.description.length() > 0) {
            description.setText(p.description);
            description.setVisibility(TextView.VISIBLE);
        } else {
            description.setText("");
            description.setVisibility(TextView.GONE);
        }
        description.invalidate();

        date.setText(Utils.getStartDate(date.getContext(), p.start));
        date.invalidate();

        time.setText(
                DateFormat.getTimeFormat(time.getContext()).format(p.start)
                + " - "
                + DateFormat.getTimeFormat(time.getContext()).format(p.stop));
        time.invalidate();
        
        // Show the duration in minutes
        double durationTime = (p.stop.getTime() - p.start.getTime());
        durationTime = (durationTime / 1000 / 60);
        duration.setText(duration.getContext().getString(R.string.ch_minutes, (int)durationTime));
    }
}
