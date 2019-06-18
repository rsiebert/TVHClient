package org.tvheadend.tvhclient.ui.base

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.ui.common.showConfirmationToReconnectToServer
import org.tvheadend.tvhclient.ui.features.MainViewModel
import timber.log.Timber

abstract class BaseFragment : Fragment() {

    protected lateinit var mainViewModel: MainViewModel
    protected lateinit var sharedPreferences: SharedPreferences
    protected lateinit var toolbarInterface: ToolbarInterface
    protected var isDualPane: Boolean = false
    protected var isUnlocked: Boolean = false
    protected var htspVersion: Int = 13
    protected var isNetworkAvailable: Boolean = false

    protected lateinit var connection: Connection
    protected lateinit var serverStatus: ServerStatus

    private var mainFrameLayout: FrameLayout? = null
    private var detailsFrameLayout: FrameLayout? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        mainFrameLayout = activity?.findViewById(R.id.main)
        detailsFrameLayout = activity?.findViewById(R.id.details)

        mainViewModel = ViewModelProviders.of(activity as BaseActivity).get(MainViewModel::class.java)
        mainViewModel.networkStatus.observe(viewLifecycleOwner, Observer { status ->
            Timber.d("Network availability changed to $status")
            isNetworkAvailable = (status == NetworkStatus.NETWORK_IS_UP || status == NetworkStatus.NETWORK_IS_STILL_UP)
        })

        connection = mainViewModel.connection
        serverStatus = mainViewModel.serverStatus
        htspVersion = serverStatus.htspVersion
        isUnlocked = MainApplication.instance.isUnlocked

        // Check if we have a frame in which to embed the details fragment.
        // Make the frame layout visible and set the weights again in case
        // it was hidden by the call to forceSingleScreenLayout()
        isDualPane = detailsFrameLayout != null
        if (isDualPane) {
            detailsFrameLayout?.visible()
            val param = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0.65f
            )
            mainFrameLayout?.layoutParams = param
        }

        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                return true
            }
            R.id.menu_reconnect_to_server -> showConfirmationToReconnectToServer(ctx, mainViewModel)
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected fun forceSingleScreenLayout() {
        if (detailsFrameLayout != null) {
            detailsFrameLayout?.gone()
            val param = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.0f
            )
            mainFrameLayout?.layoutParams = param
        }
    }
}
