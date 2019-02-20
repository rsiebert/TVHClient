package org.tvheadend.tvhclient.ui.base.adapter;


import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.databinding.GenreColorListAdapterBinding;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GenreColorListAdapter extends RecyclerView.Adapter<GenreColorListAdapter.ViewHolder> {

    private final String[] contentInfo;

    public GenreColorListAdapter(String[] contentInfo) {
        this.contentInfo = contentInfo;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        GenreColorListAdapterBinding itemBinding = GenreColorListAdapterBinding.inflate(layoutInflater, parent, false);
        return new GenreColorListAdapter.ViewHolder(itemBinding);
    }

    @Override
    public int getItemCount() {
        return contentInfo.length;
    }

    /**
     * Applies the values to the available layout items
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(((position + 1) * 16), contentInfo[position]);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final GenreColorListAdapterBinding binding;

        ViewHolder(GenreColorListAdapterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(int contentType, String contentName) {
            binding.setContentType(contentType);
            binding.setContentName(contentName);
        }
    }
}
