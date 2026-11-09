package org.tvheadend.tvhclient.ui.features.unlocker

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tvheadend.tvhclient.MainRepository.Companion.UNLOCKER
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment
import org.tvheadend.tvhclient.util.billing.BillingFlowResult
import timber.log.Timber


class UnlockerFragment : WebViewFragment() {

    private lateinit var unlockerViewModel: UnlockerViewModel
    private var isUnlocked: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarInterface.setTitle(getString(R.string.pref_unlocker))
        toolbarInterface.setSubtitle("")

        website = "features"

        unlockerViewModel = ViewModelProvider(this)[UnlockerViewModel::class.java]

        // Observe the purchase state instead of reading it from the arguments: the drawer opens
        // this screen without any arguments, and the state also changes while the screen is
        // visible when a purchase completes.
        unlockerViewModel.isUnlockedLiveData.observe(viewLifecycleOwner) { unlocked ->
            Timber.d("Received live data, unlocked changed to $unlocked")
            isUnlocked = unlocked
            activity?.invalidateOptionsMenu()
        }

        // Purchase updates do not run through the receiver that feeds the snackbar, so the outcome
        // has to be reported here. Google Play is the only source of failures, which is why the
        // user is told about them rather than having a sheet close for an unknown reason.
        unlockerViewModel.purchaseResults.observe(viewLifecycleOwner) { result ->
            Timber.d("Received purchase result $result")
            when (result) {
                is BillingFlowResult.Purchased -> showPurchaseSuccessfulDialog()
                is BillingFlowResult.AlreadyOwned -> showPurchasedAlreadyMadeDialog()
                is BillingFlowResult.Canceled -> Timber.i("Unlocker purchase canceled by the user")
                is BillingFlowResult.Failed -> {
                    Timber.e("Unlocker purchase failed: ${result.message}")
                    showPurchaseNotSuccessfulDialog()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.unlocker_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // There is nothing left to buy once the unlocker is owned.
        menu.findItem(R.id.menu_purchase)?.isVisible = !isUnlocked
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_purchase -> {
                if (isUnlocked) {
                    Timber.d("Unlocker already purchased")
                    showPurchasedAlreadyMadeDialog()
                } else {
                    Timber.d("Unlocker not purchased")
                    unlockerViewModel.buySku(requireActivity(), UNLOCKER)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPurchaseNotSuccessfulDialog() {
        Timber.d("Unlocker purchase not successful")
        context?.let {
            val dialog = MaterialAlertDialogBuilder(it)
                    .setTitle(R.string.purchase_not_successful)
                    .setMessage(R.string.contact_developer)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }

    private fun showPurchaseSuccessfulDialog() {
        Timber.d("Unlocker purchase successful")
        // No restart is needed any more: the unlock is observed live, so the features are enabled
        // as soon as the purchase is granted.
        context?.let {
            val dialog = MaterialAlertDialogBuilder(it)
                    .setTitle(R.string.purchase_successful)
                    .setMessage(R.string.thank_you)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }

    private fun showPurchasedAlreadyMadeDialog() {
        Timber.d("Unlocker already purchased")
        context?.let {
            val dialog = MaterialAlertDialogBuilder(it)
                    .setTitle(R.string.thank_you)
                    .setMessage(R.string.purchase_already_made)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }
}
