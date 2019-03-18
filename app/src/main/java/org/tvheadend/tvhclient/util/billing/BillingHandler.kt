package org.tvheadend.tvhclient.util.billing

import com.android.billingclient.api.Purchase
import java.util.*

class BillingHandler : BillingUpdatesListener {

    private val listeners = HashSet<BillingUpdatesListener>()

    fun addListener(listener: BillingUpdatesListener) {
        this.listeners.add(listener)
    }

    fun removeListener(listener: BillingUpdatesListener) {
        this.listeners.remove(listener)
    }

    override fun onBillingClientSetupFinished() {
        for (listener in listeners) {
            listener.onBillingClientSetupFinished()
        }
    }

    override fun onConsumeFinished(token: String, result: Int) {
        for (listener in listeners) {
            listener.onConsumeFinished(token, result)
        }
    }

    override fun onPurchaseCancelled() {
        for (listener in listeners) {
            listener.onPurchaseCancelled()
        }
    }

    override fun onPurchaseError(errorCode: Int) {
        for (listener in listeners) {
            listener.onPurchaseError(errorCode)
        }
    }

    override fun onPurchaseSuccessful(purchases: List<Purchase>?) {
        for (listener in listeners) {
            listener.onPurchaseSuccessful(purchases)
        }
    }
}
