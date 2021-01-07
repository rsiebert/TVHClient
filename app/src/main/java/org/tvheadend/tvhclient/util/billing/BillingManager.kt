package org.tvheadend.tvhclient.util.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.Purchase.PurchasesResult
import org.tvheadend.data.AppRepository
import org.tvheadend.tvhclient.MainApplication
import timber.log.Timber
import javax.inject.Inject

class BillingManager(private val billingUpdatesListener: BillingUpdatesListener) : PurchasesUpdatedListener {

    @Inject
    lateinit var context: Context
    @Inject
    lateinit var repository: AppRepository

    private var billingClient: BillingClient
    private var isServiceConnected = false
    private var skuDetailsList = HashMap<String, SkuDetails>()

    init {
        MainApplication.component.inject(this)
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        startServiceConnection { billingUpdatesListener.onBillingClientSetupFinished() }
    }

    private fun startServiceConnection(runnable: Runnable) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Billing setup finished successfully")
                    isServiceConnected = true
                    runnable.run()
                } else {
                    Timber.d("Billing setup did not finish successfully")
                }
            }

            override fun onBillingServiceDisconnected() {
                Timber.d("Billing service has been disconnected")
                isServiceConnected = false
            }
        })
    }

    private fun executeServiceRequest(runnable: Runnable) {
        if (isServiceConnected) {
            runnable.run()
        } else {
            // If the billing service disconnects, try to reconnect once.
            startServiceConnection(runnable)
        }
    }

    /**
     * Get all purchases and subscriptions details (if supported) for all the items bought within your app.
     * A cache of Google Play Store app without initiating a network request is used.
     * Then the SKU details to the items via the {@link onQueryPurchasesFinished(purchasesResult)} method
     * are loaded and the item will be checked if it was purchased and if it needs to be acknowledged
     * to prevent an automatic refund withing a certain time
     */
    fun queryPurchases() {
        Timber.d("Querying purchases")
        val queryToExecute = Runnable {
            Timber.d("Querying available in-app items")
            val purchasesResult = billingClient.queryPurchases(SkuType.INAPP)
            if (areSubscriptionsSupported()) {
                Timber.d("Subscriptions are supported")
                val subscriptionResult = billingClient.queryPurchases(SkuType.SUBS)
                if (subscriptionResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Adding available subscriptions")
                    subscriptionResult.purchasesList?.let {
                        purchasesResult.purchasesList?.addAll(it)
                    }
                } else {
                    Timber.d("Error while querying for available subscriptions")
                }
            } else if (purchasesResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.d("Subscriptions are not supported, skipping")
            } else {
                Timber.d("Error checking for supported features")
            }
            onQueryPurchasesFinished(purchasesResult)
        }
        executeServiceRequest(queryToExecute)
    }

    private fun areSubscriptionsSupported(): Boolean {
        val result = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.w("Got error response ${result.responseCode} while checking if subscriptions are supported")
        }
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * The querying of the local purchases and subscriptions is done.
     * Get the details to the available items and check if they were purchased.
     */
    private fun onQueryPurchasesFinished(result: PurchasesResult) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.d("Billing result code (${result.responseCode}) was bad – quitting")
            return
        }
        Timber.d("Query inventory was successful.")
        querySkuDetails()
        // Update the UI and purchases inventory with new list of purchases
        onPurchasesUpdated(result.billingResult, result.purchasesList)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Timber.d("Purchase update was successful")
                if (purchases != null) {
                    billingUpdatesListener.onPurchaseSuccessful(purchases)
                    for (purchase in purchases) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("User cancelled the purchase flow – skipping")
                billingUpdatesListener.onPurchaseCancelled()
            }
            else -> {
                Timber.d("Error ${billingResult.responseCode} occurred during purchase")
                billingUpdatesListener.onPurchaseError(billingResult.responseCode)
            }
        }
    }

    /**
     * Perform a network query to get SKU details for the defined
     * purchasable item and return the result asynchronously
     */
    private fun querySkuDetails() {
        Timber.d("Loading sku details")
        val skuList = ArrayList<String>()
        skuList.add(UNLOCKER)

        val params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(SkuType.INAPP)
                .build()

        billingClient.querySkuDetailsAsync(params) { billingResult, list ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Timber.d("Received details of ${list?.size} purchase items")
                    list?.forEach {
                        Timber.d("Received details of purchase item ${it.sku}")
                        skuDetailsList[it.sku] = it
                    }
                }
                else -> Timber.e(billingResult.debugMessage)
            }
        }
    }

    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * BillingClient.acknowledgePurchaseAsync inside your app.
     *
     * @param purchase The purchase that shall be acknowledged
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            Timber.d("Item ${purchase.sku} was purchased")
            // Grant the item to the user, and then acknowledge the purchase

            if (purchase.sku == UNLOCKER) {
                Timber.d("Setting purchase item $UNLOCKER to unlocked")
                repository.setIsUnlocked(true)
            }

            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                billingClient.acknowledgePurchase(params) { billingResult ->
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            Timber.d("Successfully activated purchased item ${purchase.sku}")
                        }
                        else -> Timber.d("Acknowledgement of purchase response is ${billingResult.debugMessage}")
                    }
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Timber.d("Item ${purchase.sku} purchase is pending")
            // Here you can confirm to the user that they've started the pending purchase, and to complete it,
            // they should follow instructions that are given to them. You can also choose to remind the user
            // in the future to complete the purchase if you detect that it is still pending.
        }
    }

    fun initiatePurchaseFlow(activity: Activity?, skuId: String?) {
        Timber.d("Initiating purchase flow for $skuId")
        val purchaseFlowRequest = Runnable {
            val skuDetails = skuDetailsList[skuId]
            if (skuDetails != null && activity != null) {
                val mParams: BillingFlowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails)
                        .build()
                billingClient.launchBillingFlow(activity, mParams)
            }
        }
        executeServiceRequest(purchaseFlowRequest)
    }

    companion object {
        // Product id for the in-app billing item to unlock the application
        const val UNLOCKER = "unlocker"
    }
}