package org.tvheadend.tvhclient.adapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.ProgramGuideTimeDialogItem;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * A private custom adapter that contains the list of
 * ProgramGuideTimeDialogItem. This is pretty much only a list of dates and
 * times that can be shown from a dialog. This is used to improve the look of
 * the dialog. A simple adapter with two line does not provide the amount of
 * styling flexibility.
 * 
 * @author rsiebert
 * 
 */
public class ProgramGuideTimeDialogAdapter extends ArrayAdapter<ProgramGuideTimeDialogItem> {

    private LayoutInflater inflater;
    public ViewHolder holder = null;
    
    public ProgramGuideTimeDialogAdapter(Activity activity, final List<ProgramGuideTimeDialogItem> times) {
        super(activity, R.layout.program_guide_time_dialog, times);
        this.inflater = activity.getLayoutInflater();
    }

    public class ViewHolder {
        public TextView date1;
        public TextView date2;
        public TextView time;
    }
    
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.program_guide_time_dialog, parent, false);
            holder = new ViewHolder();
            holder.date1 = (TextView) view.findViewById(R.id.date1);
            holder.date2 = (TextView) view.findViewById(R.id.date2);
            holder.time = (TextView) view.findViewById(R.id.time);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        ProgramGuideTimeDialogItem item = getItem(position);
        if (item != null) {
            // Get the date objects from the millisecond values
            final Date startDate = new Date(item.start);
            final Date endDate = new Date(item.end);

            // Convert the dates into a nice string representation
            final SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm", Locale.US);
            String time = sdf1.format(startDate) + " - " + sdf1.format(endDate);
            holder.time.setText(time);

            final SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
            Utils.setDate(holder.date1, startDate);
            holder.date2.setText(sdf2.format(startDate));
            
            if (holder.date1.getText().equals(holder.date2.getText())) {
                holder.date2.setVisibility(View.GONE);
            } else {
                holder.date2.setVisibility(View.VISIBLE);
            }
        }
        return view;
    }
}

