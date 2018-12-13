package org.tvheadend.tvhclient.features.purchase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import java.util.HashSet;
import java.util.Set;

public class BillingHandler implements BillingProcessor.IBillingHandler {

    private Set<BillingProcessor.IBillingHandler> listeners = new HashSet<>();

    @Override
    public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
        for (BillingProcessor.IBillingHandler listener : listeners) {
            listener.onProductPurchased(productId, details);
        }
    }

    @Override
    public void onPurchaseHistoryRestored() {
        for (BillingProcessor.IBillingHandler listener : listeners) {
            listener.onPurchaseHistoryRestored();
        }
    }

    @Override
    public void onBillingError(int errorCode, @Nullable Throwable error) {
        for (BillingProcessor.IBillingHandler listener : listeners) {
            listener.onBillingError(errorCode, error);
        }
    }

    @Override
    public void onBillingInitialized() {
        for (BillingProcessor.IBillingHandler listener : listeners) {
            listener.onBillingInitialized();
        }
    }

    public void addListener(BillingProcessor.IBillingHandler listener) {
        this.listeners.add(listener);
    }

    public void removeListener(BillingProcessor.IBillingHandler listener) {
        this.listeners.remove(listener);
    }
}
