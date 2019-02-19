package org.tvheadend.tvhclient.util.billing;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;

import java.util.List;

public interface BillingUpdatesListener {

    void onBillingClientSetupFinished();

    void onConsumeFinished(String token, @BillingClient.BillingResponse int result);

    void onPurchaseSuccessful(List<Purchase> purchases);

    void onPurchaseCancelled();

    void onPurchaseError(int errorCode);
}
