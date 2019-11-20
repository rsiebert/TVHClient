package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Ignore
import org.tvheadend.data.entity.ProgramInterface
import org.tvheadend.data.entity.Recording

data class EpgProgram(

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
        @Ignore
        override var summary: String? = null,
        @Ignore
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
        @Ignore
        override var seasonNumber: Int = 0,
        @Ignore
        override var seasonCount: Int = 0,
        @Ignore
        override var episodeNumber: Int = 0,
        @Ignore
        override var episodeCount: Int = 0,
        @Ignore
        override var partNumber: Int = 0,
        @Ignore
        override var partCount: Int = 0,
        @Ignore
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
        override var modifiedTime: Long = 0,

        @ColumnInfo(name = "connection_id")
        override var connectionId: Int = 0,
        @Ignore
        override var channelName: String? = null,
        @Ignore
        override var channelIcon: String? = null,

        @Ignore
        override var recording: Recording? = null,
        @Ignore
        override val progress: Int = 0

) : ProgramInterface {
    override val duration: Int
        get() = ((stop - start) / 1000 / 60).toInt()
}
