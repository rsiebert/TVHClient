package org.tvheadend.tvhclient.features.shared.adapter;


import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;

import java.util.List;

public class GenreColorDialogAdapter extends RecyclerView.Adapter<GenreColorDialogAdapter.ViewHolder> {

    private final List<GenreColorDialogItem> list;

    public GenreColorDialogAdapter(List<GenreColorDialogItem> list) {
        this.list = list;
    }

    public static class GenreColorDialogItem {
        public int color;
        public String genre;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.genre_color_dialog_adapter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Applies the values to the available layout items
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final GenreColorDialogItem item = list.get(position);
        if (item != null) {
            if (holder.color != null) {
                holder.color.setBackgroundColor(item.color);
            }
            if (holder.genre != null) {
                holder.genre.setText(item.genre);
                holder.genre.setTag(position);
            }
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView color;
        final TextView genre;

        ViewHolder(View view) {
            super(view);
            this.color = view.findViewById(R.id.color);
            this.genre = view.findViewById(R.id.genre);
        }
    }
}
