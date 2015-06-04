package org.tvheadend.tvhclient.adapter;

import java.util.Comparator;
import java.util.List;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.SeriesRecording;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SeriesRecordingListAdapter extends ArrayAdapter<SeriesRecording> {

    Activity context;
    List<SeriesRecording> list;
    private int selectedPosition = 0;
    private int layout;

    public SeriesRecordingListAdapter(Activity context, List<SeriesRecording> list, int layout) {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
    }

    public void sort(final int type) {
        switch (type) {
        case Constants.RECORDING_SORT_ASCENDING:
            sort(new Comparator<SeriesRecording>() {
                public int compare(SeriesRecording x, SeriesRecording y) {
                    if (x != null && y != null && x.title != null && y.title != null) {
                        return (y.title.compareTo(x.title));
                    }
                    return 0;
                }
            });
        break;
        case Constants.RECORDING_SORT_DESCENDING:
            sort(new Comparator<SeriesRecording>() {
                public int compare(SeriesRecording x, SeriesRecording y) {
                    if (x != null && y != null && x.title != null && y.title != null) {
                        return (x.title.compareTo(y.title));
                    }
                    return 0;
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
        public TextView channel;
        public TextView daysOfWeek;
        public TextView isEnabled;
        public ImageView dual_pane_list_item_selection;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(layout, parent, false);
            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.channel = (TextView) view.findViewById(R.id.channel);
            holder.daysOfWeek = (TextView) view.findViewById(R.id.daysOfWeek);
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
        SeriesRecording srec = getItem(position);
        if (srec != null) {
            holder.title.setText(srec.title);
            if (holder.channel != null) {
                if (srec.channel != null) {
                    holder.channel.setText(srec.channel.name);
                } else {
                    holder.channel.setText(R.string.all_channels);
                }
            }
            Utils.setChannelIcon(holder.icon, null, srec.channel);
            Utils.setDaysOfWeek(context, null, holder.daysOfWeek, srec.daysOfWeek);

            if (holder.isEnabled != null) {
                TVHClientApplication app = (TVHClientApplication) context.getApplication();
                holder.isEnabled.setVisibility(app.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_ENABLED ? View.VISIBLE : View.GONE);
                holder.isEnabled.setText(srec.enabled ? R.string.recording_enabled : R.string.recording_disabled);
            }
        }
        return view;
    }

    public void update(SeriesRecording srec) {
        int length = list.size();

        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id.compareTo(srec.id) == 0) {
                list.set(i, srec);
                break;
            }
        }
    }

    public SeriesRecording getSelectedItem() {
        if (list.size() > 0 && list.size() > selectedPosition) {
            return list.get(selectedPosition);
        }
        return null;
    }
}