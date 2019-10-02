package org.tvheadend.tvhclient.ui.features.information

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.features.settings.RemoveFragmentFromBackstackInterface
import timber.log.Timber

class StartupPrivacyPolicyFragment : WebViewFragment(), BackPressedInterface {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        website = "privacy_policy"

        toolbarInterface.setTitle(getString(R.string.pref_privacy_policy))
        toolbarInterface.setSubtitle("")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.accept_reject_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_accept -> {
                acceptPrivacyPolicy()
                true
            }
            R.id.menu_reject -> {
                rejectPrivacyPolicy()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun rejectPrivacyPolicy() {
        Timber.d("Privacy policy was rejected")
        activity?.finish()
    }

    private fun acceptPrivacyPolicy() {
        Timber.d("Privacy policy was accepted")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putBoolean("showPrivacyPolicy", false)
        editor.apply()

        activity.let {
            if (it is RemoveFragmentFromBackstackInterface) {
                it.removeFragmentFromBackstack()
            }
        }
    }

    override fun onBackPressed() {
        context?.let {
            MaterialDialog(it).show {
                message(text = "Do you accept the privacy policy")
                positiveButton(text = "Accept") {
                    acceptPrivacyPolicy()
                }
                negativeButton(text = "Reject") {
                    rejectPrivacyPolicy()
                }
            }
        }
    }
}