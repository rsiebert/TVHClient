package org.tvheadend.tvhclient.ui.features.unlocker

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.asLiveData
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.MainRepository.Companion.UNLOCKER
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment
import timber.log.Timber


class UnlockerFragment : WebViewFragment() {

    private var isUnlocked: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarInterface.setTitle(getString(R.string.pref_unlocker))
        toolbarInterface.setSubtitle("")

        isUnlocked = arguments?.getBoolean("isUnlocked") ?: false
        website = "features"
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
                    (activity!!.application as MainApplication).appContainer.mainRepository.buySku(activity!!, UNLOCKER)
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
                    restartApplication()
                }
            }
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

    companion object {

        fun newInstance(isUnlocked: Boolean): UnlockerFragment {
            val f = UnlockerFragment()
            val args = Bundle()
            args.putBoolean("isUnlocked", isUnlocked)
            f.arguments = args
            return f
        }
    }
}
