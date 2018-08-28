package org.tvheadend.tvhclient.features.channels;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import org.tvheadend.tvhclient.data.entity.Channel;

import java.util.List;

class ChannelListDiffCallback extends DiffUtil.Callback {
    private final List<Channel> oldList;
    private final List<Channel> newList;

    ChannelListDiffCallback(List<Channel> oldList, List<Channel> newList) {
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
