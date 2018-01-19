package org.tvheadend.tvhclient.ui.search;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SearchResultAdapter extends ArrayAdapter<Program> implements Filterable {

    private final static String TAG = SearchResultAdapter.class.getSimpleName();
    private final Activity context;
    private final SharedPreferences sharedPreferences;
    private List<Program> originalData = null;
    private List<Program> filteredData = null;
    private final ItemFilter mFilter = new ItemFilter();

    public SearchResultAdapter(Activity context, List<Program> list) {
        super(context, R.layout.search_result_adapter, list);
        this.context = context;
        originalData = list;
        filteredData = list;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void sort() {
        sort(new Comparator<Program>() {
            public int compare(Program x, Program y) {
                if (x != null && y != null) {
                    if (x.getStart() > y.getStart()) {
                        return 1;
                    } else if (x.getStart() < y.getStart()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
                /*
                if (x instanceof Recording && y instanceof Recording) {
                    if (((Recording)x).startExtra < ((Recording)y).startExtra) {
                        return -1;
                    } else if (((Recording)x).startExtra > ((Recording)y).startExtra) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
                */
                return 0;
            }
        });
    }

    static class ProgramViewHolder {
        public ImageView iconImageView;
        public TextView iconTextView;
        public TextView titleTextView;
        public TextView channelTextView;
        public TextView time;
        public TextView date;
        public TextView duration;
        public TextView description;
        public TextView seriesInfo;
        public TextView contentType;
        public ImageView state;
        public TextView genreTextView;
    }

    static class RecordingViewHolder {
        public ImageView icon;
        public TextView icon_text;
        public TextView title;
        public TextView subtitle;
        public ImageView state;
        public TextView is_series_recording;
        public TextView is_timer_recording;
        public TextView channel;
        public TextView time;
        public TextView date;
        public TextView duration;
        public TextView summary;
        public TextView description;
        public TextView failed_reason;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        Program m = getItem(position);
        //if (m instanceof Program) {
            view = getProgramView(position, convertView, parent);
        /*} else if (m instanceof Recording) {
            view = getRecordingView(position, convertView, parent);
        }*/
        return view;
    }
/*
    private View getRecordingView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        RecordingViewHolder holder;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.recording_list_widget, parent, false);
            holder = new RecordingViewHolder();
            holder.iconImageView = view.findViewById(R.id.iconImageView);
            holder.iconTextView = view.findViewById(R.id.iconTextView);
            holder.titleTextView = view.findViewById(R.id.titleTextView);
            holder.subtitle = view.findViewById(R.id.subtitle);
            holder.state = view.findViewById(R.id.state);
            holder.is_series_recording = view.findViewById(R.id.is_series_recording);
            holder.is_timer_recording = view.findViewById(R.id.is_timer_recording);
            holder.channelTextView = view.findViewById(R.id.channelTextView);
            holder.time = view.findViewById(R.id.time);
            holder.date = view.findViewById(R.id.date);
            holder.duration = view.findViewById(R.id.duration);
            holder.summary = view.findViewById(R.id.summary);
            holder.description = view.findViewById(R.id.description);
            holder.failed_reason = view.findViewById(R.id.failed_reason);
            view.setTag(holder);
        } else {
            holder = (RecordingViewHolder) view.getTag();
        }

        // Get the program and assign all the values
        Recording rec = (Recording) getItem(position);
        if (rec != null) {
            holder.titleTextView.setText(rec.titleTextView);
            if (holder.channelTextView != null && rec.channelTextView != null) {
                holder.channelTextView.setText(rec.channelTextView.name);
            }

            if (rec.subtitle != null && rec.subtitle.length() > 0) {
                holder.subtitle.setVisibility(View.VISIBLE);
                holder.subtitle.setText(rec.subtitle);
            } else {
                holder.subtitle.setVisibility(View.GONE);
            }
            
            Utils.setChannelIcon(holder.iconImageView, holder.iconTextView, rec.channelTextView);
            Utils.setDate(holder.date, rec.start);
            Utils.setTime(holder.time, rec.start, rec.stop);
            Utils.setDuration(holder.duration, rec.start, rec.stop);
            Utils.setDescription(null, holder.summary, rec.summary);
            Utils.setDescription(null, holder.description, rec.description);
            Utils.setFailedReason(holder.failed_reason, rec);

            // Show only the recording icon
            if (holder.state != null) {
                if (rec.isRecording()) {
                    holder.state.setImageResource(R.drawable.ic_rec_small);
                    holder.state.setVisibility(ImageView.VISIBLE);
                } else {
                    holder.state.setVisibility(ImageView.GONE);
                }
            }

            // Show the information if the recording belongs to a series recording
            if (holder.is_series_recording != null) {
                if (rec.autorecId != null) {
                    holder.is_series_recording.setVisibility(ImageView.VISIBLE);
                } else {
                    holder.is_series_recording.setVisibility(ImageView.GONE);
                }
            }
            // Show the information if the recording belongs to a series recording
            if (holder.is_timer_recording != null) {
                if (rec.timerecId != null) {
                    holder.is_timer_recording.setVisibility(ImageView.VISIBLE);
                } else {
                    holder.is_timer_recording.setVisibility(ImageView.GONE);
                }
            }
        }
        return view;
    }
*/
    private View getProgramView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ProgramViewHolder holder;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.search_result_adapter, parent, false);
            holder = new ProgramViewHolder();
            holder.iconImageView = view.findViewById(R.id.icon);
            holder.iconTextView = view.findViewById(R.id.icon_text);
            holder.titleTextView = view.findViewById(R.id.title);
            holder.channelTextView = view.findViewById(R.id.channel);
            holder.state = view.findViewById(R.id.state);
            holder.time = view.findViewById(R.id.time);
            holder.date = view.findViewById(R.id.date);
            holder.duration = view.findViewById(R.id.duration);
            holder.seriesInfo = view.findViewById(R.id.series_info);
            holder.contentType = view.findViewById(R.id.content_type);
            holder.description = view.findViewById(R.id.description);
            holder.genreTextView = view.findViewById(R.id.genre);
            view.setTag(holder);
        } else {
            holder = (ProgramViewHolder) view.getTag();
        }

        boolean showChannelIcons = sharedPreferences.getBoolean("showIconPref", true);
        boolean showChannelName = sharedPreferences.getBoolean("showChannelNamePref", true);
        boolean showGenreColors = sharedPreferences.getBoolean("showGenreColorsSearchPref", false);

        // Get the program and assign all the values
        Program program = getItem(position);
        if (program != null) {
            holder.titleTextView.setText(program.getTitle());

            Channel channel = DataStorage.getInstance().getChannelFromArray(program.getChannelId());
            holder.channelTextView.setText(channel.getChannelName());
            holder.channelTextView.setVisibility(showChannelName ? View.VISIBLE : View.GONE);

            // Show the regular or large channel icons. Otherwise show the channel name only
            // Assign the channel icon image or a null image
            Bitmap iconBitmap = MiscUtils.getCachedIcon(context, channel.getChannelIcon());
            holder.iconImageView.setImageBitmap(iconBitmap);
            holder.iconTextView.setText(channel.getChannelName());

            // Show or hide the regular or large channel icon or name text views
            holder.iconImageView.setVisibility(showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.iconTextView.setVisibility(showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);

            Drawable drawable = UIUtils.getRecordingState(context, program.getDvrId());
            holder.state.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
            holder.state.setImageDrawable(drawable);

            holder.date.setText(UIUtils.getDate(getContext(), program.getStart()));

            String time = UIUtils.getTime(getContext(), program.getStart()) + " - " + UIUtils.getTime(getContext(), program.getStop());
            holder.time.setText(time);

            String durationTime = getContext().getString(R.string.minutes, (int) ((program.getStop() - program.getStart()) / 1000 / 60));
            holder.duration.setText(durationTime);

            holder.description.setText(program.getDescription());

            holder.contentType.setText(UIUtils.getContentTypeText(getContext(), program.getContentType()));
            holder.seriesInfo.setText(UIUtils.getSeriesInfo(getContext(), program));

            if (showGenreColors) {
                int color = UIUtils.getGenreColor(context, program.getContentType(), 0);
                holder.genreTextView.setBackgroundColor(color);
                holder.genreTextView.setVisibility(View.VISIBLE);
            } else {
                holder.genreTextView.setVisibility(View.GONE);
            }
        }
        return view;
    }

    public void update(Program p) {
        final int length = originalData.size();
        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (originalData.get(i).getEventId() == p.getEventId()) {
                originalData.set(i, p);
                break;
            }
        }
    }

    public Program getItem(int position) {
        return filteredData.get(position);
    }

    public int getFullCount() {
        if (originalData != null) {
            return originalData.size();
        }
        return 0;
    }

    public int getCount() {
        if (filteredData != null) {
            return filteredData.size();
        }
        return 0;
    }

    public List<Program> getFullList() {
        return originalData;
    }

    public List<Program> getList() {
        return filteredData;
    }

    public Filter getFilter() {
        return mFilter;
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            final String filterString = constraint.toString().toLowerCase(Locale.getDefault());
            FilterResults results = new FilterResults();

            final int count = originalData.size();
            final ArrayList<Program> newList = new ArrayList<>(count);

            Program p;
            for (int i = 0; i < count; i++) {
                p = originalData.get(i);
                if (p.getTitle().toLowerCase(Locale.getDefault()).contains(filterString)) {
                    newList.add(p);
                }

                /*
                if (p instanceof Recording) {
                    if (((Recording)p).title.toLowerCase(Locale.getDefault()).contains(filterString)) {
                        newList.add(p);
                    }
                }
                */
            }

            results.values = newList;
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (ArrayList<Program>) results.values;
            notifyDataSetChanged();
        }
    }
}
