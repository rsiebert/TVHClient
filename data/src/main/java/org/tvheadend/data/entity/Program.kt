package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import java.util.*
import kotlin.math.floor

data class Program(

        override var eventId: Int = 0,
        override var channelId: Int = 0,
        override var start: Long = 0,
        override var stop: Long = 0,
        override var title: String? = null,
        override var subtitle: String? = null,
        override var summary: String? = null,
        override var description: String? = null,
        override var credits: String? = null,
        override var category: String? = null,
        override var keyword: String? = null,
        override var serieslinkId: Int = 0,
        override var episodeId: Int = 0,
        override var seasonId: Int = 0,
        override var brandId: Int = 0,
        override var contentType: Int = 0,
        override var ageRating: Int = 0,
        override var starRating: Int = 0,
        override var copyrightYear: Int = 0,
        override var firstAired: Long = 0,
        override var seasonNumber: Int = 0,
        override var seasonCount: Int = 0,
        override var episodeNumber: Int = 0,
        override var episodeCount: Int = 0,
        override var partNumber: Int = 0,
        override var partCount: Int = 0,
        override var episodeOnscreen: String? = null,
        override var image: String? = null,
        override var dvrId: Int = 0,
        override var nextEventId: Int = 0,
        override var serieslinkUri: String? = null,
        override var episodeUri: String? = null,
        override var modifiedTime: Long = 0,

        override var connectionId: Int = 0,
        override var channelName: String? = null,
        override var channelIcon: String? = null,

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
            return floor(percentage * 100).toInt()
        }
}

@Entity(tableName = "programs", primaryKeys = ["id", "connection_id"], indices = [Index(value = ["start"]), Index(value = ["channel_id"])])
internal data class ProgramEntity(

        @ColumnInfo(name = "id")
        override var eventId: Int = 0,
        @ColumnInfo(name = "channel_id")
        override var channelId: Int = 0,
        override var start: Long = 0,
        override var stop: Long = 0,
        override var title: String? = null,
        override var subtitle: String? = null,
        override var summary: String? = null,
        override var description: String? = null,
        override var credits: String? = null,
        override var category: String? = null,
        override var keyword: String? = null,
        @ColumnInfo(name = "series_link_id")
        override var serieslinkId: Int = 0,
        @ColumnInfo(name = "episode_id")
        override var episodeId: Int = 0,
        @ColumnInfo(name = "season_id")
        override var seasonId: Int = 0,
        @ColumnInfo(name = "brand_id")
        override var brandId: Int = 0,
        @ColumnInfo(name = "content_type")
        override var contentType: Int = 0,
        @ColumnInfo(name = "age_rating")
        override var ageRating: Int = 0,
        @ColumnInfo(name = "star_rating")
        override var starRating: Int = 0,
        @ColumnInfo(name = "copyright_year")
        override var copyrightYear: Int = 0,
        @ColumnInfo(name = "first_aired")
        override var firstAired: Long = 0,
        @ColumnInfo(name = "season_number")
        override var seasonNumber: Int = 0,
        @ColumnInfo(name = "season_count")
        override var seasonCount: Int = 0,
        @ColumnInfo(name = "episode_number")
        override var episodeNumber: Int = 0,
        @ColumnInfo(name = "episode_count")
        override var episodeCount: Int = 0,
        @ColumnInfo(name = "part_number")
        override var partNumber: Int = 0,
        @ColumnInfo(name = "part_count")
        override var partCount: Int = 0,
        @ColumnInfo(name = "episode_on_screen")
        override var episodeOnscreen: String? = null,
        override var image: String? = null,
        @ColumnInfo(name = "dvr_id")
        override var dvrId: Int = 0,
        @ColumnInfo(name = "next_event_id")
        override var nextEventId: Int = 0,
        @ColumnInfo(name = "series_link_uri")
        override var serieslinkUri: String? = null,
        @ColumnInfo(name = "episode_uri")
        override var episodeUri: String? = null,
        @ColumnInfo(name = "modified_time")
        override var modifiedTime: Long = 0,

        @ColumnInfo(name = "connection_id")
        override var connectionId: Int = 0,
        @ColumnInfo(name = "channel_name")
        override var channelName: String? = null,
        @ColumnInfo(name = "channel_icon")
        override var channelIcon: String? = null,

        @Ignore
        override var recording: Recording? = null,

        @Ignore
        override var duration: Int = 0,
        @Ignore
        override var progress: Int = 0

) : ProgramInterface {
    companion object {
        fun from(program: Program): ProgramEntity {
            return ProgramEntity(
                    program.eventId,
                    program.channelId,
                    program.start,
                    program.stop,
                    program.title,
                    program.subtitle,
                    program.summary,
                    program.description,
                    program.credits,
                    program.category,
                    program.keyword,
                    program.serieslinkId,
                    program.episodeId,
                    program.seasonId,
                    program.brandId,
                    program.contentType,
                    program.ageRating,
                    program.starRating,
                    program.copyrightYear,
                    program.firstAired,
                    program.seasonNumber,
                    program.seasonCount,
                    program.episodeNumber,
                    program.episodeCount,
                    program.partNumber,
                    program.partCount,
                    program.episodeOnscreen,
                    program.image,
                    program.dvrId,
                    program.nextEventId,
                    program.serieslinkUri,
                    program.episodeUri,
                    program.modifiedTime,
                    program.connectionId,
                    program.channelName,
                    program.channelIcon,
                    program.recording,
                    program.duration,
                    program.progress
            )
        }
    }

    fun toProgram(): Program {
        return Program(eventId, channelId, start, stop, title, subtitle, summary, description, credits, category, keyword, serieslinkId, episodeId, seasonId, brandId, contentType, ageRating, starRating, copyrightYear, firstAired, seasonNumber, seasonCount, episodeNumber, episodeCount, partNumber, partCount, episodeOnscreen, image, dvrId, nextEventId, serieslinkUri, episodeUri, modifiedTime, connectionId, channelName, channelIcon, recording)
    }
}
