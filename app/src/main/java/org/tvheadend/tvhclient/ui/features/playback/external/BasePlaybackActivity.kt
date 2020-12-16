package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.api.AuthenticationFailureReason
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionFailureReason
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.PlayActivityBinding
import org.tvheadend.tvhclient.service.*
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.util.extensions.showSnackbarMessage
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

abstract class BasePlaybackActivity : AppCompatActivity(), SyncStateReceiver.Listener, ServerTicketReceiver.Listener {

    private lateinit var serverTicketReceiver: ServerTicketReceiver
    private lateinit var syncStateReceiver: SyncStateReceiver
    lateinit var binding: PlayActivityBinding
    lateinit var viewModel: ExternalPlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        binding = PlayActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel = ViewModelProvider(this).get(ExternalPlayerViewModel::class.java)
        syncStateReceiver = SyncStateReceiver(this)
        serverTicketReceiver = ServerTicketReceiver(this)
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    public override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(syncStateReceiver, IntentFilter(SyncStateReceiver.ACTION))
        LocalBroadcastManager.getInstance(this).registerReceiver(serverTicketReceiver, IntentFilter(ServerTicketReceiver.ACTION))
    }

    public override fun onResume() {
        super.onResume()
        intent.action = "getTicket"
        ConnectionIntentService.enqueueWork(this, intent)
    }

    public override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serverTicketReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(syncStateReceiver)
    }

    protected abstract fun onTicketReceived()

    internal fun startExternalPlayer(intent: Intent) {
        Timber.d("Starting external player for given intent")
        // Start playing the video in the UI thread
        this.runOnUiThread {
            Timber.d("Getting list of activities that can play the intent")
            val activities: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (activities.isNotEmpty()) {
                Timber.d("Found activities, starting external player")
                startActivity(intent)
                finish()
            } else {
                Timber.d("List of available activities is empty, can't start external media player")
                binding.status.setText(R.string.no_media_player)

                // Show a confirmation dialog before deleting the recording
                MaterialDialog(this@BasePlaybackActivity).show {
                    title(R.string.no_media_player)
                    message(R.string.show_play_store)
                    positiveButton(android.R.string.yes) {
                        try {
                            Timber.d("Opening play store to download external players")
                            val installIntent = Intent(Intent.ACTION_VIEW)
                            installIntent.data = Uri.parse("market://search?q=free%20video%20player&c=apps")
                            startActivity(installIntent)
                        } catch (t2: Throwable) {
                            Timber.d("Could not startPlayback google play store")
                        } finally {
                            finish()
                        }
                    }
                    negativeButton(android.R.string.no) { finish() }
                }
            }
        }
    }

    override fun onSyncStateChanged(result: SyncStateResult) {
        Timber.d("Received sync state $result")
        when (result) {
            is SyncStateResult.Connecting -> {
                when (result.reason) {
                    is ConnectionStateResult.Idle -> {
                        Timber.d("Connection is idle")
                        binding.progressBar.gone()
                    }
                    is ConnectionStateResult.Closed -> {
                        Timber.d("Connection failed or closed")
                        binding.progressBar.gone()
                        binding.status.setText(R.string.connection_failed)
                    }
                    is ConnectionStateResult.Connecting -> {
                        Timber.d("Connecting")
                        binding.status.setText(R.string.connecting_to_server)
                    }
                    is ConnectionStateResult.Connected -> {
                        Timber.d("Connected")
                        binding.progressBar.gone()
                        binding.status.text = getString(R.string.connected_to_server)
                    }
                    is ConnectionStateResult.Failed -> {
                        Timber.d("Connected failed")
                        binding.progressBar.gone()
                        when (result.reason.reason) {
                            is ConnectionFailureReason.Interrupted -> binding.status.text = getString(R.string.failed_during_connection_attempt)
                            is ConnectionFailureReason.UnresolvedAddress -> binding.status.text = getString(R.string.failed_to_resolve_address)
                            is ConnectionFailureReason.ConnectingToServer -> binding.status.text = getString(R.string.failed_connecting_to_server)
                            is ConnectionFailureReason.SocketException -> binding.status.text = getString(R.string.failed_opening_socket)
                            is ConnectionFailureReason.Other -> binding.status.text = getString(R.string.connection_failed)
                        }
                    }
                }
            }
            is SyncStateResult.Authenticating -> {
                when (result.reason) {
                    is AuthenticationStateResult.Idle -> {}
                    is AuthenticationStateResult.Authenticating -> binding.status.text = getString(R.string.authentication_successful)
                    is AuthenticationStateResult.Authenticated -> {
                        binding.progressBar.gone()
                        binding.status.text = getString(R.string.authentication_successful)
                    }
                    is AuthenticationStateResult.Failed -> {
                        binding.progressBar.gone()
                        when (result.reason.reason) {
                            is AuthenticationFailureReason.BadCredentials -> binding.status.text = getString(R.string.bad_username_or_password)
                            is AuthenticationFailureReason.Other -> binding.status.text = getString(R.string.authentication_failed)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    override fun onServerTicketReceived(intent: Intent) {
        val path = intent.getStringExtra(ServerTicketReceiver.PATH) ?: ""
        val ticket = intent.getStringExtra(ServerTicketReceiver.TICKET) ?: ""
        Timber.d("Received path '$path' and ticket '$ticket' from server")
        viewModel.path = path
        viewModel.ticket = ticket
        onTicketReceived()
    }
}
