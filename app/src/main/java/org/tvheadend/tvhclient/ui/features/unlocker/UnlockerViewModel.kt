package org.tvheadend.tvhclient.ui.features.unlocker

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.util.billing.BillingFlowResult

/**
 * Owns the state of the unlocker screen.
 *
 * The unlock state is taken from the billing data source instead of a fragment argument, so the
 * screen is correct no matter how it was opened and it updates as soon as the purchase is
 * acknowledged by Google Play.
 */
class UnlockerViewModel(application: Application) : BaseViewModel(application) {

    /**
     * Emits the outcome of each purchase attempt so the screen can tell the user what happened.
     * Google Play reports failures only to the billing listener, so without this a rejected or
     * unstartable purchase looks like nothing happened at all.
     */
    val purchaseResults: LiveData<BillingFlowResult> = billingFlowResults.asLiveData()
}
