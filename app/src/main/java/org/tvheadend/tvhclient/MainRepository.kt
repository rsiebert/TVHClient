package org.tvheadend.tvhclient

import android.app.Activity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.tvheadend.tvhclient.util.billing.BillingDataSource
import timber.log.Timber

/**
 * The repository uses data from the Billing data source and the game state model together to give
 * a unified version of the state of the game to the ViewModel. It works closely with the
 * BillingDataSource to implement consumable items, premium items, etc.
 */
class MainRepository(
    private val billingDataSource: BillingDataSource,
    private val defaultScope: CoroutineScope
) {
    private val gameMessages: MutableSharedFlow<Int> = MutableSharedFlow()

    /**
     * Sets up the event that we can use to send messages up to the UI to be used in Snackbars.
     * This collects new purchase events from the BillingDataSource, transforming the known SKU
     * strings into useful String messages, and emitting the messages into the game messages flow.
     */
    private fun postMessagesFromBillingFlow() {
        defaultScope.launch {
            try {
                billingDataSource.getNewPurchases().collect { skuList ->
                    for (sku in skuList) {
                        when (sku) {
                            UNLOCKER -> {
                                Timber.d("Unlocker found in postMessagesFromBillingFlow")
                                gameMessages.emit(R.string.pref_unlocker)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Timber.d("Collection complete")
            }
            Timber.d("Collection Coroutine Scope Exited")
        }
    }

    /**
     * Automatic support for upgrading/downgrading subscription.
     * @param activity
     * @param sku
     */
    fun buySku(activity: Activity, sku: String) {
        billingDataSource.launchBillingFlow(activity, sku)
    }

    /**
     * Return Flow that indicates whether the sku is currently purchased.
     *
     * @param sku the SKU to get and observe the value for
     * @return Flow that returns true if the sku is purchased.
     */
    fun isPurchased(sku: String): Flow<Boolean> {
        Timber.d("Calling isPurchased($sku)")
        val skuPurchased = billingDataSource.isPurchased(sku)
        Timber.d("Calling isPurchased($sku) status is ${skuPurchased.asLiveData().value}")
        return skuPurchased
    }

    /**
     * We can buy gas if:
     * 1) We can add at least one unit of gas
     * 2) The billing data source allows us to purchase, which means that the item isn't already
     *    purchased.
     * For other skus, we rely on just the data from the billing data source. For subscriptions,
     * only one can be held at a time, and purchasing one subscription will use the billing feature
     * to upgrade or downgrade the user from the other.
     *
     * @param sku the SKU to get and observe the value for
     * @return Flow<Boolean> that returns true if the sku can be purchased
     */
    fun canPurchase(sku: String): Flow<Boolean> {
        return billingDataSource.canPurchase(sku)
    }

    suspend fun refreshPurchases() {
        billingDataSource.refreshPurchases()
    }

    val billingLifecycleObserver: LifecycleObserver
        get() = billingDataSource

    val messages: Flow<Int>
        get() = gameMessages

    suspend fun sendMessage(stringId: Int) {
        gameMessages.emit(stringId)
    }

    val billingFlowInProcess: Flow<Boolean>
        get() = billingDataSource.getBillingFlowInProcess()

    fun debugConsumePremium() {
        CoroutineScope(Dispatchers.Main).launch {
            billingDataSource.consumeInappPurchase(UNLOCKER)
        }
    }

    companion object {
        // The following SKU strings must match the ones we have in the Google Play developer console.
        // SKUs for non-subscription purchases
        const val UNLOCKER = "unlocker"
        val INAPP_SKUS = arrayOf(UNLOCKER)
    }

    init {
        postMessagesFromBillingFlow()
    }
}