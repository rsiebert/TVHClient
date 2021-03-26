package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.PlayActivityBinding
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

abstract class BasePlaybackActivity : AppCompatActivity() {

    lateinit var binding: PlayActivityBinding
    lateinit var viewModel: ExternalPlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        binding = PlayActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.status.setText(R.string.connecting_to_server)

        viewModel = ViewModelProvider(this).get(ExternalPlayerViewModel::class.java)

        viewModel.isConnected.observe(this,  { isConnected ->
            if (isConnected) {
                Timber.d("Received live data, connected to server, requesting ticket")
                binding.status.setText(R.string.requesting_playback_information)
                viewModel.requestTicketFromServer(intent.extras)
            } else {
                Timber.d("Received live data, not connected to server")
                binding.progressBar.gone()
                binding.status.setText(R.string.connection_failed)
            }
        })

        viewModel.isTicketReceived.observe(this,  { isTicketReceived ->
            Timber.d("Received ticket $isTicketReceived")
            if (isTicketReceived) {
                binding.progressBar.gone()
                binding.status.text = getString(R.string.connected_to_server)
                onTicketReceived()
            }
        })
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    protected abstract fun onTicketReceived()

    internal fun startExternalPlayer(intent: Intent) {
        Timber.d("Starting external player for given intent")
        // Start playing the video in the UI thread
        this.runOnUiThread {
            Timber.d("Getting list of activities that can play the intent")
            try {
                Timber.d("Found activities, starting external player")
                startActivity(intent)
                finish()
            } catch (ex: ActivityNotFoundException) {
                Timber.d("List of available activities is empty, can't start external media player")
                binding.status.setText(R.string.no_media_player)

                // Show a confirmation dialog before deleting the recording
                MaterialDialog(this@BasePlaybackActivity).show {
                    title(R.string.no_media_player)
                    message(R.string.show_play_store)
                    positiveButton(android.R.string.ok) {
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
                    negativeButton(android.R.string.cancel) { finish() }
                }
            }
        }
    }
}
