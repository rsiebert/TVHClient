package org.tvheadend.tvhclient.ui.features.purchase;

import com.android.billingclient.api.Purchase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BillingHandler implements BillingUpdatesListener {

    private final Set<BillingUpdatesListener> listeners = new HashSet<>();

    public void addListener(BillingUpdatesListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(BillingUpdatesListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void onBillingClientSetupFinished() {
        for (BillingUpdatesListener listener : listeners) {
            listener.onBillingClientSetupFinished();
        }
    }

    @Override
    public void onConsumeFinished(String token, int result) {
        for (BillingUpdatesListener listener : listeners) {
            listener.onConsumeFinished(token, result);
        }
    }

    @Override
    public void onPurchaseSuccessful(List<Purchase> purchases) {
        for (BillingUpdatesListener listener : listeners) {
            listener.onPurchaseSuccessful(purchases);
        }
    }

    @Override
    public void onPurchaseCancelled() {
        for (BillingUpdatesListener listener : listeners) {
            listener.onPurchaseCancelled();
        }
    }

    @Override
    public void onPurchaseError(int errorCode) {
        for (BillingUpdatesListener listener : listeners) {
            listener.onPurchaseError(errorCode);
        }
    }
}
