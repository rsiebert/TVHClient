package org.tvheadend.tvhclient.ui.programs;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProgramListAdapter extends ArrayAdapter<Program> {

    private final static String TAG = ProgramListAdapter.class.getSimpleName();
    private final Activity context;
    private final List<Program> list;
    private final SharedPreferences sharedPreferences;

    public ProgramListAdapter(Activity context, List<Program> list) {
        super(context, R.layout.program_list_adapter, list);
        this.context = context;
        this.list = list;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void sort() {
        sort(new Comparator<Program>() {
            public int compare(Program x, Program y) {
                if (x != null && y != null) {
                    if (x.start > y.start) {
                        return 1;
                    } else if (x.start < y.start) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
                return 0;
            }
        });
    }

    static class ViewHolder {
        @BindView(R.id.title) TextView titleTextView;
        @BindView(R.id.time) TextView timeTextView;
        @BindView(R.id.date) TextView dateTextView;
        @BindView(R.id.duration) TextView durationTextView;
        @BindView(R.id.progress) TextView progressTextView;
        @BindView(R.id.summary) TextView summaryTextView;
        @BindView(R.id.description) TextView descriptionTextView;
        @BindView(R.id.series_info) TextView seriesInfoTextView;
        @BindView(R.id.subtitle) TextView subtitleTextView;
        @BindView(R.id.content_type) TextView contentTypeTextView;
        @BindView(R.id.state) ImageView stateTextView;
        @BindView(R.id.genre) TextView genreTextView;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = context.getLayoutInflater().inflate(R.layout.program_list_adapter, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        boolean showProgramSubtitle = sharedPreferences.getBoolean("showProgramSubtitlePref", true);
        boolean showGenreColors = sharedPreferences.getBoolean("showGenreColorsProgramsPref", false);

        // Get the program and assign all the values
        Program p = getItem(position);
        if (p != null) {
            holder.titleTextView.setText(p.title);

            Drawable drawable = UIUtils.getRecordingState(context, p.dvrId);
            holder.stateTextView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
            holder.stateTextView.setImageDrawable(drawable);

            holder.dateTextView.setText(UIUtils.getDate(context, p.start));

            String time = UIUtils.getTime(context, p.start) + " - " + UIUtils.getTime(context, p.stop);
            holder.timeTextView.setText(time);

            String durationTime = context.getString(R.string.minutes, (int) ((p.stop - p.start) / 1000 / 60));
            holder.durationTextView.setText(durationTime);

            String progressText = UIUtils.getProgressText(getContext(), p.start, p.stop);
            holder.progressTextView.setVisibility(!TextUtils.isEmpty(progressText) ? View.VISIBLE : View.GONE);
            holder.progressTextView.setText(progressText);

            String contentType = UIUtils.getContentTypeText(getContext(), p.contentType);
            holder.contentTypeTextView.setVisibility(!TextUtils.isEmpty(contentType) ? View.VISIBLE : View.GONE);
            holder.contentTypeTextView.setText(contentType);

            String seriesInfo = UIUtils.getSeriesInfo(getContext(), p);
            holder.seriesInfoTextView.setVisibility(!TextUtils.isEmpty(seriesInfo) ? View.VISIBLE : View.GONE);
            holder.seriesInfoTextView.setText(seriesInfo);

            holder.subtitleTextView.setVisibility(showProgramSubtitle && !TextUtils.isEmpty(p.subtitle)? View.VISIBLE : View.GONE);
            holder.subtitleTextView.setText(p.subtitle);

            holder.descriptionTextView.setVisibility(!TextUtils.isEmpty(p.description)? View.VISIBLE : View.GONE);
            holder.descriptionTextView.setText(p.description);

            holder.summaryTextView.setVisibility(!TextUtils.isEmpty(p.summary)? View.VISIBLE : View.GONE);
            holder.summaryTextView.setText(p.summary);

            if (showGenreColors) {
                int color = UIUtils.getGenreColor(context, p.contentType, 0);
                holder.genreTextView.setBackgroundColor(color);
                holder.genreTextView.setVisibility(View.VISIBLE);
            } else {
                holder.genreTextView.setVisibility(View.GONE);
            }
        }
        return view;
    }

    public void update(Program p) {
        int length = list.size();

        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).eventId == p.eventId) {
                list.set(i, p);
                break;
            }
        }
    }

    public List<Program> getList() {
        return list;
    }
}
