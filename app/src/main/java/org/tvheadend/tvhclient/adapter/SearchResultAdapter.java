package org.tvheadend.tvhclient.adapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.Model;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchResultAdapter extends ArrayAdapter<Model> implements Filterable {

    private final static String TAG = SearchResultAdapter.class.getSimpleName();
    private Activity context;
    private List<Model> originalData = null;
    private List<Model> filteredData = null;
    private ItemFilter mFilter = new ItemFilter();

    public SearchResultAdapter(Activity context, List<Model> list) {
        super(context, R.layout.search_result_widget, list);
        this.context = context;
        originalData = list;
        filteredData = list;
    }

    public void sort() {
        sort(new Comparator<Model>() {
            public int compare(Model x, Model y) {
                if (x instanceof Program && y instanceof Program) {
                    Log.d(TAG, "Comparing programs");
                    return (((Program)x).start.compareTo(((Program)y).start));
                }
                if (x instanceof Recording && y instanceof Recording) {
                    Log.d(TAG, "Comparing recordings");
                    if (((Recording)x).startExtra < ((Recording)y).startExtra) {
                        return -1;
                    } else if (((Recording)x).startExtra > ((Recording)y).startExtra) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
                return 0;
            }
        });
    }

    static class ProgramViewHolder {
        public ImageView icon;
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
        Model m = getItem(position);
        if (m instanceof Program) {
            view = getProgramView(position, convertView, parent);
        } else if (m instanceof Recording) {
            view = getRecordingView(position, convertView, parent);
        }
        return view;
    }

    public View getRecordingView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        RecordingViewHolder holder;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.recording_list_widget, parent, false);
            holder = new RecordingViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
            holder.state = (ImageView) view.findViewById(R.id.state);
            holder.is_series_recording = (TextView) view.findViewById(R.id.is_series_recording);
            holder.is_timer_recording = (TextView) view.findViewById(R.id.is_timer_recording);
            holder.channel = (TextView) view.findViewById(R.id.channel);
            holder.time = (TextView) view.findViewById(R.id.time);
            holder.date = (TextView) view.findViewById(R.id.date);
            holder.duration = (TextView) view.findViewById(R.id.duration);
            holder.summary = (TextView) view.findViewById(R.id.summary);
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.failed_reason = (TextView) view.findViewById(R.id.failed_reason);
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
            
            Utils.setChannelIcon(holder.icon, null, rec.channel);
            Utils.setDate(holder.date, rec.start);
            Utils.setTime(holder.time, rec.start, rec.stop);
            Utils.setDuration(holder.duration, rec.start, rec.stop);
            Utils.setDescription(null, holder.summary, rec.summary);
            Utils.setDescription(null, holder.description, rec.description);
            Utils.setFailedReason(holder.failed_reason, rec);

            // Show only the recording icon
            if (holder.state != null) {
                if (rec.state.equals("recording")) {
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

    public View getProgramView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ProgramViewHolder holder;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.search_result_widget, parent, false);
            holder = new ProgramViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.channel = (TextView) view.findViewById(R.id.channel);
            holder.state = (ImageView) view.findViewById(R.id.state);
            holder.time = (TextView) view.findViewById(R.id.time);
            holder.date = (TextView) view.findViewById(R.id.date);
            holder.duration = (TextView) view.findViewById(R.id.duration);
            holder.seriesInfo = (TextView) view.findViewById(R.id.series_info);
            holder.contentType = (TextView) view.findViewById(R.id.content_type);
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.genre = (TextView) view.findViewById(R.id.genre);
            view.setTag(holder);
        } else {
            holder = (ProgramViewHolder) view.getTag();
        }

        // Get the program and assign all the values
        Program p = (Program)getItem(position);
        if (p != null) {
            holder.title.setText(p.title);
            if (holder.channel != null && p.channel != null) {
                holder.channel.setText(p.channel.name);
            }
            Utils.setChannelIcon(holder.icon, null, p.channel);
            Utils.setState(context, holder.state, p);
            Utils.setDate(holder.date, p.start);
            Utils.setTime(holder.time, p.start, p.stop);
            Utils.setDuration(holder.duration, p.start, p.stop);
            Utils.setDescription(null, holder.description, p.description);
            Utils.setContentType(null, holder.contentType, p.contentType);
            Utils.setSeriesInfo(null, holder.seriesInfo, p.seriesInfo);
            Utils.setGenreColor(context, holder.genre, p, TAG);
        }
        return view;
    }

    public void update(Model p) {
        synchronized(originalData) {
            final int length = originalData.size();
            // Go through the list of programs and find the
            // one with the same id. If its been found, replace it.
            for (int i = 0; i < length; ++i) {
                if (originalData.get(i).id == p.id) {
                    originalData.set(i, p);
                    break;
                }
            }
        }
    }

    public Model getItem(int position) {
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

    public List<Model> getFullList() {
        return originalData;
    }

    public List<Model> getList() {
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

            final List<Model> list = originalData;
            final int count = list.size();
            final ArrayList<Model> newList = new ArrayList<>(count);

            Model p;
            for (int i = 0; i < count; i++) {
                p = list.get(i);
                if (p instanceof Program) {
                    if (((Program)p).title.toLowerCase(Locale.getDefault()).contains(filterString)) {
                        newList.add(p);
                    }
                }
                if (p instanceof Recording) {
                    if (((Recording)p).title.toLowerCase(Locale.getDefault()).contains(filterString)) {
                        newList.add(p);
                    }
                }
            }

            results.values = newList;
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (ArrayList<Model>) results.values;
            notifyDataSetChanged();
        }
    }
}
