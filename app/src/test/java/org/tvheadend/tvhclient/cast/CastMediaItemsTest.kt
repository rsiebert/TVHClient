package org.tvheadend.tvhclient.cast

import android.app.Application
import android.net.Uri
import androidx.media3.common.MediaItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CastMediaItemsTest {

    private val artwork = Uri.parse("http://server:9981/imagecache/1")

    @Test
    fun channelItemCarriesStreamDetailsAndIsLive() {
        val item = CastMediaItems.channel(
                url = "http://server:9981/stream/channel/1?ticket=t&profile=pass",
                mimeType = "video/mp2t",
                title = "Title",
                subtitle = "Subtitle",
                artworkUri = artwork
        )

        assertThat(item.localConfiguration?.uri?.toString())
                .isEqualTo("http://server:9981/stream/channel/1?ticket=t&profile=pass")
        assertThat(item.localConfiguration?.mimeType).isEqualTo("video/mp2t")
        assertThat(item.mediaMetadata.title).isEqualTo("Title")
        assertThat(item.mediaMetadata.subtitle).isEqualTo("Subtitle")
        assertThat(item.mediaMetadata.artworkUri).isEqualTo(artwork)
        // A LiveConfiguration that differs from the all-unset sentinel is what makes the converter
        // emit STREAM_TYPE_LIVE.
        assertThat(item.liveConfiguration).isNotEqualTo(MediaItem.LiveConfiguration.UNSET)
    }

    @Test
    fun recordingItemIsNotLiveAndAllowsMissingArtwork() {
        val item = CastMediaItems.recording(
                url = "http://server:9981/dvrfile/2?ticket=t&profile=pass",
                mimeType = "video/x-matroska",
                title = "Recording",
                subtitle = "Sub",
                artworkUri = null
        )

        assertThat(item.localConfiguration?.mimeType).isEqualTo("video/x-matroska")
        assertThat(item.mediaMetadata.title).isEqualTo("Recording")
        assertThat(item.mediaMetadata.artworkUri).isNull()
        // The unset default keeps it VOD.
        assertThat(item.liveConfiguration).isEqualTo(MediaItem.LiveConfiguration.UNSET)
    }
}
