package org.tvheadend.tvhclient.ui.features.playback

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Tests for [PlaybackLogic], the pure playback logic extracted from the two player ViewModels.
 *
 * Deliberately a plain JVM test with no Robolectric: the whole point of the extraction is that this
 * logic needs no Android framework, and a plain test proves it.
 */
class PlaybackLogicTest {

    private lateinit var originalLocale: Locale

    @Before
    fun captureLocale() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        // The aspect-ratio tests change the default locale, which is JVM-wide.
        Locale.setDefault(originalLocale)
    }

    // ---- seekPosition -----------------------------------------------------------------

    @Test
    fun seekPositionComesFromTheServerClockWhileTimeshifting() {
        // Buffer started 5 s ago (microseconds), the server says it is 2 s into its offset, and the
        // local clock reads 10 s. The player's own position must be ignored.
        val position = PlaybackLogic.seekPosition(
            timeshiftStartTime = 5_000_000,
            timeshiftStartPts = 0,
            timeshiftOffsetPts = 2_000_000,
            currentPosition = 999_999,
            now = 10_000,
            offsetSeconds = 0
        )

        assertThat(position).isEqualTo(7_000)
    }

    @Test
    fun seekPositionMovesTheServerClockByTheRequestedOffset() {
        val position = PlaybackLogic.seekPosition(
            timeshiftStartTime = 5_000_000,
            timeshiftStartPts = 0,
            timeshiftOffsetPts = 2_000_000,
            currentPosition = 0,
            now = 10_000,
            offsetSeconds = 30
        )

        // The test above gives 7_000 with offset 0, so a 30 s forward seek adds 30.
        assertThat(position).isEqualTo(7_030)
    }

    @Test
    fun seekPositionNeverGoesBackBeforeTheStartOfTheBuffer() {
        // A large backward jump: the server clock reads 7 s but the buffer starts at 20 s, so the
        // result is pinned to the start of the buffer.
        val position = PlaybackLogic.seekPosition(
            timeshiftStartTime = 20_000_000,
            timeshiftStartPts = 0,
            timeshiftOffsetPts = 2_000_000,
            currentPosition = 0,
            now = 5_000,
            offsetSeconds = -10_000
        )

        assertThat(position).isEqualTo(0)
    }

    @Test
    fun seekPositionFallsBackToThePlayerPositionWhenTheCallerHasNoTimeshiftValues() {
        // This is what the view model passes when the data source is null: zeros, not the sentinel.
        val position = PlaybackLogic.seekPosition(
            timeshiftStartTime = 0,
            timeshiftStartPts = 0,
            timeshiftOffsetPts = 0,
            currentPosition = 60_000,
            now = 10_000,
            offsetSeconds = 30
        )

        assertThat(position).isEqualTo(10_030)
    }

    // ---- videoAspect ------------------------------------------------------------------

    @Test
    fun videoAspectUsesTheTrackPixelRatioWhenNothingIsForced() {
        val aspect = PlaybackLogic.videoAspect(
            width = 1920,
            height = 1080,
            pixelWidthHeightRatio = 1f,
            forceAspectRatioForSdContent = false
        )

        assertThat(aspect.width).isEqualTo(1920)
        assertThat(aspect.height).isEqualTo(1080)
    }

    @Test
    fun videoAspectAppliesThePixelRatioWhenNothingIsForced() {
        val aspect = PlaybackLogic.videoAspect(
            width = 720,
            height = 576,
            pixelWidthHeightRatio = 2f,
            forceAspectRatioForSdContent = false
        )

        assertThat(aspect.width).isEqualTo(1440)
        assertThat(aspect.height).isEqualTo(576)
    }

    @Test
    fun videoAspectStretchesA5By4VideoTo16By9InACommaDecimalLocale() {
        Locale.setDefault(Locale.GERMANY)

        val aspect = PlaybackLogic.videoAspect(
            width = 1280,
            height = 1024,
            pixelWidthHeightRatio = 1f,
            forceAspectRatioForSdContent = true
        )

        // (16/9) * 1024 / 1280 = 1.4222, so the picture is stretched to 1820 wide.
        assertThat(aspect.width).isEqualTo(1820)
        assertThat(aspect.height).isEqualTo(1024)
    }

    @Test
    fun videoAspectDoesNotStretchA5By4VideoInAnEnglishLocale() {
        // CHARACTERISATION of a locale-dependent defect, not desired behaviour: the 5:4 check
        // compares the formatted ratio against "1,25", but DecimalFormat renders 1.25 as "1.25"
        // here, so the correction never fires. Same video as the test above, different result.
        Locale.setDefault(Locale.US)

        val aspect = PlaybackLogic.videoAspect(
            width = 1280,
            height = 1024,
            pixelWidthHeightRatio = 1f,
            forceAspectRatioForSdContent = true
        )

        assertThat(aspect.width).isEqualTo(1280)
    }

    @Test
    fun videoAspectLeavesOtherRatiosAloneEvenWhenForcingIsEnabled() {
        Locale.setDefault(Locale.GERMANY)

        // 4:3, not 5:4, so the track's own pixel ratio is kept. (720x576 would be 5:4 and the
        // correction would fire, which is a useful reminder of how narrow this rule is.)
        val aspect = PlaybackLogic.videoAspect(
            width = 768,
            height = 576,
            pixelWidthHeightRatio = 1.5f,
            forceAspectRatioForSdContent = true
        )

        assertThat(aspect.width).isEqualTo(1152)
        assertThat(aspect.height).isEqualTo(576)
    }

    // ---- playbackUrl ------------------------------------------------------------------

    @Test
    fun playbackUrlCarriesThePathTicketAndProfile() {
        val url = PlaybackLogic.playbackUrl(
            baseUrl = "http://192.168.1.10:9981",
            path = "/dvrfile/42",
            ticket = "abc123",
            profileName = "pass"
        )

        assertThat(url).isEqualTo("http://192.168.1.10:9981/dvrfile/42?ticket=abc123&profile=pass")
    }

    @Test
    fun playbackUrlKeepsAWebrootInTheBaseUrl() {
        val url = PlaybackLogic.playbackUrl(
            baseUrl = "http://example.org:9981/tvh",
            path = "/stream/channel/1",
            ticket = "t",
            profileName = "matroska"
        )

        assertThat(url).isEqualTo("http://example.org:9981/tvh/stream/channel/1?ticket=t&profile=matroska")
    }
}
