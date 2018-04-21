package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.shared.UIUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

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
public class ProgramGuideTimeDialogAdapter extends RecyclerView.Adapter<ProgramGuideTimeDialogAdapter.ViewHolder> {

    private final Context context;
    private Callback mCallback;
    private List<ProgramGuideViewPagerFragment.ProgramGuideTimeDialogItem> list;

    public interface Callback {
        void onItemClicked(int index);
    }

    public ProgramGuideTimeDialogAdapter(Context context, List<ProgramGuideViewPagerFragment.ProgramGuideTimeDialogItem> list) {
        this.list = list;
        this.context = context;
    }

    public void setCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.program_guide_time_selection_dialog_adapter, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    /**
     * Applies the values to the available layout items
     */
    public void onBindViewHolder(ViewHolder holder, int position) {
        ProgramGuideViewPagerFragment.ProgramGuideTimeDialogItem item = list.get(position);
        if (item != null) {
            Timber.d("onBindViewHolder: start " + item.start);
            // Convert the dates into a nice string representation
            final SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm", Locale.US);
            String time = sdf1.format(item.start) + " - " + sdf1.format(item.end);
            holder.time.setText(time);

            final SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM.yyyy", Locale.US);

            holder.date1.setText(UIUtils.getDate(context, item.start));
            holder.date2.setText(sdf2.format(item.start));

            if (holder.date1.getText().equals(holder.date2.getText())) {
                holder.date2.setVisibility(View.GONE);
            } else {
                holder.date2.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView date1;
        final TextView date2;
        final TextView time;
        final ProgramGuideTimeDialogAdapter adapter;

        public ViewHolder(View view, ProgramGuideTimeDialogAdapter adapter) {
            super(view);
            this.date1 = view.findViewById(R.id.date1);
            this.date2 = view.findViewById(R.id.date2);
            this.time = view.findViewById(R.id.time);

            this.adapter = adapter;
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (adapter.mCallback == null) {
                return;
            }
            adapter.mCallback.onItemClicked(getAdapterPosition());
        }
    }
}

