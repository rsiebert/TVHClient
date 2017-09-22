package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Recording;

import java.util.Comparator;
import java.util.List;

public class RecordingListAdapter extends ArrayAdapter<Recording> {

    private final Activity context;
    private final List<Recording> list;
    private int selectedPosition = 0;
    private final int layout;

    public RecordingListAdapter(Activity context, List<Recording> list, int layout) {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
    }

    public void sort(final int type) {
        switch (type) {
        case Constants.RECORDING_SORT_ASCENDING:
            sort(new Comparator<Recording>() {
                public int compare(Recording x, Recording y) {
                    return (y.start.compareTo(x.start));
                }
            });
        break;
        case Constants.RECORDING_SORT_DESCENDING:
            sort(new Comparator<Recording>() {
                public int compare(Recording x, Recording y) {
                    return (x.start.compareTo(y.start));
                }
            });
            break;
        }
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    static class ViewHolder {
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
        public TextView isEnabled;
        public ImageView dual_pane_list_item_selection;
    }
    
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (view == null) {
            view = context.getLayoutInflater().inflate(layout, parent, false);
            holder = new ViewHolder();
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
            holder.isEnabled = (TextView) view.findViewById(R.id.enabled);
            holder.dual_pane_list_item_selection = (ImageView) view.findViewById(R.id.dual_pane_list_item_selection);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (holder.dual_pane_list_item_selection != null) {
            // Set the correct indication when the dual pane mode is active
            // If the item is selected the the arrow will be shown, otherwise
            // only a vertical separation line is displayed.                
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean lightTheme = prefs.getBoolean("lightThemePref", true);

            if (selectedPosition == position) {
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
                holder.dual_pane_list_item_selection.setBackgroundResource(icon);
            } else {
                final int icon = R.drawable.dual_pane_selector_inactive;
                holder.dual_pane_list_item_selection.setBackgroundResource(icon);
            }
        }

        // Get the program and assign all the values
        final Recording rec = getItem(position);
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

            // If activated in the settings allow playing the recording by  
            // selecting the channel icon if the recording is completed or currently
            // being recorded
            if (holder.icon != null && (rec.isCompleted() || rec.isRecording())) {
                holder.icon.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (context instanceof FragmentStatusInterface) {
                            ((FragmentStatusInterface) context).onListItemSelected(position, rec, Constants.TAG_CHANNEL_ICON);
                        }
                    }
                });
            }

            if (holder.isEnabled != null) {
                holder.isEnabled.setVisibility((DataStorage.getInstance().getProtocolVersion() >= Constants.MIN_API_VERSION_DVR_FIELD_ENABLED && !rec.enabled) ? View.VISIBLE : View.GONE);
                holder.isEnabled.setText(rec.enabled ? R.string.recording_enabled : R.string.recording_disabled);
            }
        }
        return view;
    }

    public void update(Recording rec) {
        int length = list.size();

        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id == rec.id) {
                list.set(i, rec);
                break;
            }
        }
    }
    
    public Recording getSelectedItem() {
        if (list.size() > 0 && list.size() > selectedPosition) {
            return list.get(selectedPosition);
        }
        return null;
    }
}