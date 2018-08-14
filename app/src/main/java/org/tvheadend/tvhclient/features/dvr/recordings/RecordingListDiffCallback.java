package org.tvheadend.tvhclient.features.dvr.recordings;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import org.tvheadend.tvhclient.data.entity.Recording;

import java.util.List;

public class RecordingListDiffCallback extends DiffUtil.Callback {
    private List<Recording> oldList;
    private List<Recording> newList;

    public RecordingListDiffCallback(List<Recording> oldList, List<Recording> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList != null ? oldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newList != null ? newList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return newList.get(newItemPosition).getId() == oldList.get(oldItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return newList.get(newItemPosition).equals(oldList.get(oldItemPosition));
        /*
        Recording newRecording = newList.get(newItemPosition);
        Recording oldRecording = oldList.get(oldItemPosition);

        boolean areContentsTheSame = TextUtils.equals(newRecording.getState(), oldRecording.getState())
                && TextUtils.equals(newRecording.getError(), oldRecording.getError());

        Timber.d("areContentsTheSame " + areContentsTheSame);
        return areContentsTheSame;
        */
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        // One can return particular field for changed item.
        return super.getChangePayload(oldItemPosition, newItemPosition);
        /*
        Channel newProduct = newList.get(newItemPosition);
        Channel oldProduct = oldList.get(oldItemPosition);
        Bundle diffBundle = new Bundle();
        if (newProduct.hasDiscount() != oldProduct.hasDiscount()) {
            diffBundle.putBoolean(KEY_DISCOUNT, newProduct.hasDiscount());
        }
        if (newProduct.getReviews().size() != oldProduct.getReviews().size()) {
            diffBundle.putInt(Product.KEY_REVIEWS_COUNT, newProduct.getReviews().size());
        }
        if (newProduct.getPrice() != oldProduct.getPrice()) {
            diffBundle.putFloat(Product.KEY_PRICE, newProduct.getPrice());
        }
        if (diffBundle.size() == 0) return null;
        return diffBundle;
        */
    }
}
