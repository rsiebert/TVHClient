package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.Program;
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
    private final SharedPreferences prefs;

    public ProgramListAdapter(Activity context, List<Program> list) {
        super(context, R.layout.program_list_widget, list);
        this.context = context;
        this.list = list;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
        @BindView(R.id.title) TextView title;
        @BindView(R.id.time) TextView time;
        @BindView(R.id.date) TextView date;
        @BindView(R.id.duration) TextView duration;
        @BindView(R.id.progress) TextView progress;
        @BindView(R.id.summary) TextView summary;
        @BindView(R.id.description) TextView description;
        @BindView(R.id.series_info) TextView seriesInfo;
        @BindView(R.id.subtitle) TextView subtitle;
        @BindView(R.id.content_type) TextView contentType;
        @BindView(R.id.state) ImageView state;
        @BindView(R.id.genre) TextView genre;

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

        // Get the program and assign all the values
        Program p = getItem(position);
        if (p != null) {
            holder.title.setText(p.title);
            Utils.setState(context, holder.state, p);
            Utils.setDate(holder.date, p.start);
            Utils.setTime(holder.time, p.start, p.stop);
            Utils.setDuration(holder.duration, p.start, p.stop);
            Utils.setProgressText(holder.progress, p.start, p.stop);
            Utils.setDescription(null, holder.summary, p.summary);
            Utils.setDescription(null, holder.subtitle, p.subtitle);
            Utils.setDescription(null, holder.description, p.description);
            Utils.setContentType(null, holder.contentType, p.contentType);
            Utils.setSeriesInfo(getContext(), null, holder.seriesInfo, p);
            MiscUtils.setGenreColor(context, holder.genre, p.contentType, TAG);
            holder.subtitle.setVisibility(prefs.getBoolean("showProgramSubtitlePref", true) ? View.VISIBLE : View.GONE);
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
