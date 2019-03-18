package org.tvheadend.tvhclient.util.billing;

import android.app.Activity;
import android.content.Context;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.List;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class BillingManager implements PurchasesUpdatedListener {

    private BillingClient billingClient;
    private BillingUpdatesListener billingUpdatesListener;
    private boolean isServiceConnected = false;

    // Product id for the in-app billing item to unlock the application
    public static final String UNLOCKER = "unlocker";

    public BillingManager(Context context, final BillingUpdatesListener billingUpdatesListener) {
        this.billingUpdatesListener = billingUpdatesListener;
        this.billingClient = BillingClient.newBuilder(context).setListener(this).build();
        startServiceConnection(billingUpdatesListener::onBillingClientSetupFinished);
    }

    private void startServiceConnection(final Runnable runnable) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(int responseCode) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    Timber.d("Billing setup finished successfully");
                    isServiceConnected = true;
                    if (runnable != null) {
                        runnable.run();
                    }
                } else {
                    Timber.d("Billing setup did not finish successfully");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Timber.d("Billing service has been disconnected");
                isServiceConnected = false;
            }
        });
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK) {
            Timber.d("Purchase update was successful");
            billingUpdatesListener.onPurchaseSuccessful(purchases);

        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            Timber.d("User cancelled the purchase flow – skipping");
            billingUpdatesListener.onPurchaseCancelled();

        } else {
            Timber.d("Error " + responseCode + " occurred during purchase");
            billingUpdatesListener.onPurchaseError(responseCode);
        }
    }

    private void executeServiceRequest(Runnable runnable) {
        if (isServiceConnected) {
            runnable.run();
        } else {
            // If the billing service disconnects, try to reconnect once.
            startServiceConnection(runnable);
        }
    }

    public void queryPurchases() {
        Timber.d("Querying purchases");
        Runnable queryToExecute = () -> {
            Timber.d("Adding available in-app items");
            Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            if (areSubscriptionsSupported()) {
                Timber.d("Subscriptions are supported");
                Purchase.PurchasesResult subscriptionResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
                if (subscriptionResult.getResponseCode() == BillingClient.BillingResponse.OK) {
                    Timber.d("Adding available subscriptions");
                    purchasesResult.getPurchasesList().addAll(subscriptionResult.getPurchasesList());
                } else {
                    // Handle any error response codes.
                    Timber.d("Error while querying for available subscriptions");
                }
            } else if (purchasesResult.getResponseCode() == BillingClient.BillingResponse.OK) {
                // Skip subscription purchases query as they are not supported.
                Timber.d("Subscriptions are not supported");
            } else {
                // Handle any other error response codes.
                Timber.d("Error checking for supported features");
            }
            onQueryPurchasesFinished(purchasesResult);
        };
        executeServiceRequest(queryToExecute);
    }

    private void onQueryPurchasesFinished(Purchase.PurchasesResult result) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (billingClient == null
                || result.getResponseCode() != BillingClient.BillingResponse.OK) {
            Timber.d("Billing client was null or result code (" + result.getResponseCode() + ") was bad – quitting");
            return;
        }

        Timber.d("Query inventory was successful.");
        // Update the UI and purchases inventory with new list of purchases mPurchases.clear();
        onPurchasesUpdated(BillingClient.BillingResponse.OK, result.getPurchasesList());
    }

    private boolean areSubscriptionsSupported() {
        int responseCode = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
        if (responseCode != BillingClient.BillingResponse.OK) {
            Timber.w("Got an error response: " + responseCode);
        }
        return responseCode == BillingClient.BillingResponse.OK;
    }

    public void initiatePurchaseFlow(Activity activity, final String skuId, final String oldSku, final @BillingClient.SkuType String billingType) {
        Timber.d("Initiating purchase flow for " + skuId);
        Runnable purchaseFlowRequest = () -> {
            BillingFlowParams mParams = BillingFlowParams.newBuilder()
                    .setSku(skuId)
                    .setType(billingType)
                    .setOldSku(oldSku)
                    .build();
            billingClient.launchBillingFlow(activity, mParams);
        };
        executeServiceRequest(purchaseFlowRequest);
    }
}
