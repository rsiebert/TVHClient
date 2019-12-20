package org.tvheadend.tvhclient.ui.features.unlocker

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment
import org.tvheadend.tvhclient.util.billing.BillingHandler
import org.tvheadend.tvhclient.util.billing.BillingManager
import org.tvheadend.tvhclient.util.billing.BillingManager.UNLOCKER
import org.tvheadend.tvhclient.util.billing.BillingUpdatesListener
import timber.log.Timber

class UnlockerFragment : WebViewFragment(), BillingUpdatesListener {

    private lateinit var billingManager: BillingManager
    private lateinit var billingHandler: BillingHandler
    private lateinit var baseViewModel: BaseViewModel
    private var isUnlocked: Boolean = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbarInterface.setTitle(getString(R.string.pref_unlocker))
        toolbarInterface.setSubtitle("")

        billingManager = MainApplication.billingManager
        billingHandler = MainApplication.billingHandler

        baseViewModel = ViewModelProviders.of(activity as BaseActivity).get(BaseViewModel::class.java)
        baseViewModel.isUnlocked.observe(viewLifecycleOwner, Observer { unlocked ->
            Timber.d("Received live data, unlocked changed to $unlocked")
            isUnlocked = unlocked
        })

        website = "features"
    }

    override fun onResume() {
        super.onResume()
        billingHandler.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        billingHandler.removeListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.unlocker_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_purchase -> {
                if (!isUnlocked) {
                    Timber.d("Unlocker not purchased")
                    billingManager.initiatePurchaseFlow(activity, UNLOCKER, null, BillingClient.SkuType.INAPP)
                } else {
                    Timber.d("Unlocker already purchased")
                    showPurchasedAlreadyMadeDialog()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPurchaseNotSuccessfulDialog() {
        Timber.d("Unlocker purchase not successful")
        context?.let {
            MaterialDialog(it).show {
                title(R.string.purchase_not_successful)
                message(R.string.contact_developer)
                cancelOnTouchOutside(false)
                positiveButton(android.R.string.ok) { dismiss() }
            }
        }
    }

    private fun showPurchaseSuccessfulDialog() {
        Timber.d("Unlocker purchase successful")
        context?.let {
            MaterialDialog(it).show {
                title(R.string.purchase_successful)
                message(R.string.thank_you)
                cancelOnTouchOutside(false)
                positiveButton(R.string.restart) {
                    baseViewModel.updateConnectionAndRestartApplication(context, false)
                }
            }
        }
    }

    private fun showPurchasedAlreadyMadeDialog() {
        Timber.d("Unlocker already purchased")
        context?.let {
            MaterialDialog(it).show {
                title(R.string.thank_you)
                message(R.string.purchase_already_made)
                cancelOnTouchOutside(false)
                positiveButton(android.R.string.ok) {
                    dismiss()
                }
            }
        }
    }

    override fun onBillingClientSetupFinished() {
        Timber.d("Billing client setup finished")
    }

    override fun onConsumeFinished(token: String, result: Int) {
        Timber.d("Token $token has been consumed with result $result")
    }

    override fun onPurchaseSuccessful(purchases: List<Purchase>?) {
        Timber.d("Purchase was successful")
        showPurchaseSuccessfulDialog()
    }

    override fun onPurchaseCancelled() {
        Timber.d("Purchase was cancelled")
    }

    override fun onPurchaseError(errorCode: Int) {
        Timber.d("Purchase was not successful")
        showPurchaseNotSuccessfulDialog()
    }
}
