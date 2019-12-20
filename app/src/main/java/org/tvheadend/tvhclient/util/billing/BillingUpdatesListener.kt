package org.tvheadend.tvhclient.util.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase

interface BillingUpdatesListener {

    fun onBillingClientSetupFinished()

    fun onConsumeFinished(token: String, @BillingClient.BillingResponseCode result: Int)

    fun onPurchaseSuccessful(purchases: List<Purchase>)

    fun onPurchaseCancelled()

    fun onPurchaseError(errorCode: Int)
}
