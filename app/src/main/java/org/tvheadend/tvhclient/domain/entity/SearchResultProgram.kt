package org.tvheadend.tvhclient.domain.entity

import androidx.room.ColumnInfo
import androidx.room.Ignore
import java.util.*

data class SearchResultProgram(

        @ColumnInfo(name = "id")
        override var eventId: Int = 0,
        @Ignore
        override var nextEventId: Int = 0,
        @ColumnInfo(name = "channel_id")
        override var channelId: Int = 0,
        override var start: Long = 0,
        override var stop: Long = 0,
        override var title: String? = null,
        override var subtitle: String? = null,
        override var summary: String? = null,
        override var description: String? = null,
        @Ignore
        override var credits: String? = null,
        @Ignore
        override var category: String? = null,
        @Ignore
        override var keyword: String? = null,
        @Ignore
        override var serieslinkId: Int = 0,
        @Ignore
        override var episodeId: Int = 0,
        @Ignore
        override var seasonId: Int = 0,
        @Ignore
        override var brandId: Int = 0,
        @ColumnInfo(name = "content_type")
        override var contentType: Int = 0,
        @Ignore
        override var ageRating: Int = 0,
        @Ignore
        override var starRating: Int = 0,
        @Ignore
        override var copyrightYear: Int = 0,
        @Ignore
        override var firstAired: Long = 0,
        @ColumnInfo(name = "season_number")
        override var seasonNumber: Int = 0,
        @Ignore
        override var seasonCount: Int = 0,
        @ColumnInfo(name = "episode_number")
        override var episodeNumber: Int = 0,
        @Ignore
        override var episodeCount: Int = 0,
        @ColumnInfo(name = "part_number")
        override var partNumber: Int = 0,
        @Ignore
        override var partCount: Int = 0,
        @ColumnInfo(name = "episode_on_screen")
        override var episodeOnscreen: String? = null,
        @Ignore
        override var image: String? = null,
        @Ignore
        override var dvrId: Int = 0,
        @Ignore
        override var serieslinkUri: String? = null,
        @Ignore
        override var episodeUri: String? = null,

        @Ignore
        override var connectionId: Int = 0,
        @ColumnInfo(name = "channel_name")
        override var channelName: String? = null,
        @ColumnInfo(name = "channel_icon")
        override var channelIcon: String? = null,

        @Ignore
        override var recording: Recording? = null

) : ProgramInterface {
    override val duration: Int
        get() = ((stop - start) / 1000 / 60).toInt()

    override val progress: Int
        get() {
            var percentage = 0.0
            // Get the start and end times to calculate the progress.
            val durationTime = (stop - start).toDouble()
            val elapsedTime = (Date().time - start).toDouble()
            // Show the progress as a percentage
            if (durationTime > 0 && elapsedTime > 0) {
                percentage = elapsedTime / durationTime
            }
            return Math.floor(percentage * 100).toInt()
        }
}
