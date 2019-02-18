package org.tvheadend.tvhclient.features.shared.adapter;


import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.databinding.GenreColorDialogAdapterBinding;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GenreColorDialogAdapter extends RecyclerView.Adapter<GenreColorDialogAdapter.ViewHolder> {

    private final String[] contentInfo;

    public GenreColorDialogAdapter(String[] contentInfo) {
        this.contentInfo = contentInfo;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        GenreColorDialogAdapterBinding itemBinding = GenreColorDialogAdapterBinding.inflate(layoutInflater, parent, false);
        return new GenreColorDialogAdapter.ViewHolder(itemBinding);
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
        private final GenreColorDialogAdapterBinding binding;

        ViewHolder(GenreColorDialogAdapterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(int contentType, String contentName) {
            binding.setContentType(contentType);
            binding.setContentName(contentName);
        }
    }
}
