package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import android.app.Application
import android.content.Context
import android.view.LayoutInflater
import androidx.media3.ui.TrackSelectionView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.tvhclient.R

/**
 * The track selection screen inflates the app's own layout instead of the one media3-ui ships, whose
 * layout file and id are private resources. Inflating it here proves that the app's copy resolves
 * the public [TrackSelectionView] and the id the fragment looks up, which compiling alone does not.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application: MainApplication runs the database migration in onCreate.
@Config(sdk = [34], application = Application::class)
class TrackSelectionViewLayoutTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theLayoutInflatesTheTrackSelectionViewTheFragmentLooksUp() {
        val root = LayoutInflater.from(context).inflate(R.layout.track_selection_view, null)

        assertThat(root.findViewById<TrackSelectionView>(R.id.track_selection_view)).isNotNull()
    }
}
