package org.tvheadend.tvhclient.debug

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Covers the naming rule of [DemoRecordingAsset].
 *
 * The debug assets folder always ships a README, and the demo clip is optional, so the loader has to
 * pick out the clip by name and ignore everything else. Getting this wrong would either feed the
 * README to the player or refuse to serve a clip that is present.
 */
class DemoRecordingAssetTest {

    @Test
    fun picksTheClipWhateverItsExtensionIs() {
        for (extension in listOf("mkv", "mp4", "webm", "ts", "wav")) {
            val names = listOf("README.md", "demo_recording.$extension", "licence.txt")

            assertThat(DemoRecordingAsset.assetName(names)).isEqualTo("demo_recording.$extension")
        }
    }

    @Test
    fun ignoresTheOtherAssetsSoTheDemoFallsBackToTheGeneratedWav() {
        assertThat(DemoRecordingAsset.assetName(listOf("README.md", "licence.txt"))).isNull()
        assertThat(DemoRecordingAsset.assetName(emptyList())).isNull()
    }

    @Test
    fun doesNotMatchANameThatMerelyContainsThePrefix() {
        // The clip has to be at the start of the name: "my_demo_recording.mkv" is not ours.
        assertThat(DemoRecordingAsset.assetName(listOf("my_demo_recording.mkv"))).isNull()
    }
}
