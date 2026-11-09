package org.tvheadend.tvhclient.ui.features.unlocker

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tvheadend.tvhclient.MainRepository.Companion.UNLOCKER
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment
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
        context?.let {
            val dialog = MaterialAlertDialogBuilder(it)
                    .setTitle(R.string.purchase_successful)
                    .setMessage(R.string.thank_you)
                    .setPositiveButton(R.string.restart) { _, _ ->
                        restartApplication()
                    }
                    .create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }

    private fun restartApplication() {
        context?.let {
            it.stopService(Intent(it, ConnectionService::class.java))
            val intent = Intent(it, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            it.startActivity(intent)
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
