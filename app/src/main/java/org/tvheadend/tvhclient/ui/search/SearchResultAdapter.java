package org.tvheadend.tvhclient.ui.search;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SearchResultAdapter extends ArrayAdapter<Program> implements Filterable {

    private final static String TAG = SearchResultAdapter.class.getSimpleName();
    private final Activity context;
    private List<Program> originalData = null;
    private List<Program> filteredData = null;
    private final ItemFilter mFilter = new ItemFilter();

    public SearchResultAdapter(Activity context, List<Program> list) {
        super(context, R.layout.adapter_search_results, list);
        this.context = context;
        originalData = list;
        filteredData = list;
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
        public ImageView icon;
        public TextView icon_text;
        public TextView title;
        public TextView channel;
        public TextView time;
        public TextView date;
        public TextView duration;
        public TextView description;
        public TextView seriesInfo;
        public TextView contentType;
        public ImageView state;
        public TextView genre;
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
            holder.icon = view.findViewById(R.id.icon);
            holder.icon_text = view.findViewById(R.id.icon_text);
            holder.title = view.findViewById(R.id.title);
            holder.subtitle = view.findViewById(R.id.subtitle);
            holder.state = view.findViewById(R.id.state);
            holder.is_series_recording = view.findViewById(R.id.is_series_recording);
            holder.is_timer_recording = view.findViewById(R.id.is_timer_recording);
            holder.channel = view.findViewById(R.id.channel);
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
            holder.title.setText(rec.title);
            if (holder.channel != null && rec.channel != null) {
                holder.channel.setText(rec.channel.name);
            }

            if (rec.subtitle != null && rec.subtitle.length() > 0) {
                holder.subtitle.setVisibility(View.VISIBLE);
                holder.subtitle.setText(rec.subtitle);
            } else {
                holder.subtitle.setVisibility(View.GONE);
            }
            
            Utils.setChannelIcon(holder.icon, holder.icon_text, rec.channel);
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
            view = context.getLayoutInflater().inflate(R.layout.adapter_search_results, parent, false);
            holder = new ProgramViewHolder();
            holder.icon = view.findViewById(R.id.icon);
            holder.icon_text = view.findViewById(R.id.icon_text);
            holder.title = view.findViewById(R.id.title);
            holder.channel = view.findViewById(R.id.channel);
            holder.state = view.findViewById(R.id.state);
            holder.time = view.findViewById(R.id.time);
            holder.date = view.findViewById(R.id.date);
            holder.duration = view.findViewById(R.id.duration);
            holder.seriesInfo = view.findViewById(R.id.series_info);
            holder.contentType = view.findViewById(R.id.content_type);
            holder.description = view.findViewById(R.id.description);
            holder.genre = view.findViewById(R.id.genre);
            view.setTag(holder);
        } else {
            holder = (ProgramViewHolder) view.getTag();
        }

        // Get the program and assign all the values
        Program p = getItem(position);
        if (p != null) {
            holder.title.setText(p.title);
            Channel channel = DataStorage.getInstance().getChannelFromArray(p.channelId);
            if (holder.channel != null && channel != null) {
                holder.channel.setText(channel.channelName);
            }
            Utils.setChannelIcon(context, holder.icon, holder.icon_text, channel);
            //Utils.setState(context, holder.state, p);
            Utils.setDate(holder.date, p.start);
            Utils.setTime(holder.time, p.start, p.stop);
            Utils.setDuration(holder.duration, p.start, p.stop);
            Utils.setDescription(null, holder.description, p.description);
            Utils.setContentType(null, holder.contentType, p.contentType);
            Utils.setSeriesInfo(context, null, holder.seriesInfo, p);
            MiscUtils.setGenreColor(context, holder.genre, p.contentType, TAG);
        }
        return view;
    }

    public void update(Program p) {
        final int length = originalData.size();
        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (originalData.get(i).eventId == p.eventId) {
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
                if (p.title.toLowerCase(Locale.getDefault()).contains(filterString)) {
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
