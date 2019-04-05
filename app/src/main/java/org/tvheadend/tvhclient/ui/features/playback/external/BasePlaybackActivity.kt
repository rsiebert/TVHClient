package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.play_activity.*
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber
import javax.inject.Inject

abstract class BasePlaybackActivity : AppCompatActivity() {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    lateinit var viewModel: ExternalPlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play_activity)
        MainApplication.getComponent().inject(this)

        status.setText(R.string.connecting_to_server)

        viewModel = ViewModelProviders.of(this).get(ExternalPlayerViewModel::class.java)

        viewModel.isConnected.observe(this, Observer { isConnected ->
            if (isConnected) {
                Timber.d("Connected to server, requesting ticket")
                status.setText(R.string.requesting_playback_information)
                viewModel.requestTicketFromServer(intent.extras)
            } else {
                Timber.d("Not connected to server")
                progress_bar.visibility = View.GONE
                status.setText(R.string.connection_failed)
            }
        })

        viewModel.isTicketReceived.observe(this, Observer { isTicketReceived ->
            Timber.d("Received ticket $isTicketReceived")
            if (isTicketReceived) {
                progress_bar.visibility = View.GONE
                status.text = getString(R.string.starting_playback)
                onTicketReceived()
            }
        })
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    protected abstract fun onTicketReceived()

    internal fun startExternalPlayer(intent: Intent) {
        // Start playing the video in the UI thread
        this.runOnUiThread {
            try {
                Timber.d("Starting external player")
                startActivity(intent)
                finish()
            } catch (t: Throwable) {
                Timber.d("Can't execute external media player")
                status.setText(R.string.no_media_player)

                // Show a confirmation dialog before deleting the recording
                MaterialDialog.Builder(this@BasePlaybackActivity)
                        .title(R.string.no_media_player)
                        .content(R.string.show_play_store)
                        .positiveText(getString(android.R.string.yes))
                        .negativeText(getString(android.R.string.no))
                        .onPositive { _, _ ->
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
                        .onNegative { _, _ -> finish() }
                        .show()
            }
        }
    }
}
