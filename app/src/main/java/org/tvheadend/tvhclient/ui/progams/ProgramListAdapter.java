package org.tvheadend.tvhclient.ui.progams;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

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
        super(context, R.layout.program_list_widget, list);
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
            view = context.getLayoutInflater().inflate(R.layout.program_list_widget, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        boolean showProgramSubtitle = sharedPreferences.getBoolean("showProgramSubtitlePref", true);

        // Get the program and assign all the values
        Program p = getItem(position);
        if (p != null) {
            holder.titleTextView.setText(p.title);
            Utils.setState(context, holder.stateTextView, p);
            Utils.setDate(holder.dateTextView, p.start);
            Utils.setTime(holder.timeTextView, p.start, p.stop);
            Utils.setDuration(holder.durationTextView, p.start, p.stop);
            Utils.setProgressText(holder.progressTextView, p.start, p.stop);
            Utils.setDescription(null, holder.summaryTextView, p.summary);
            Utils.setDescription(null, holder.subtitleTextView, p.subtitle);
            Utils.setDescription(null, holder.descriptionTextView, p.description);
            Utils.setContentType(null, holder.contentTypeTextView, p.contentType);
            Utils.setSeriesInfo(getContext(), null, holder.seriesInfoTextView, p);
            MiscUtils.setGenreColor(context, holder.genreTextView, p.contentType, TAG);
            holder.subtitleTextView.setVisibility(showProgramSubtitle ? View.VISIBLE : View.GONE);
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
