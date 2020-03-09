package org.tvheadend.data.entity

import androidx.room.ColumnInfo
import androidx.room.Ignore

data class EpgProgram(

        override var eventId: Int = 0,
        override var nextEventId: Int = 0,
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
        override var serieslinkUri: String? = null,
        override var episodeUri: String? = null,
        override var modifiedTime: Long = 0,

        override var connectionId: Int = 0,
        override var channelName: String? = null,
        override var channelIcon: String? = null,

        override var recording: Recording? = null,
        override val progress: Int = 0

) : ProgramInterface {
        override val duration: Int
                get() = ((stop - start) / 1000 / 60).toInt()
}

internal data class EpgProgramEntity(

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
        override val duration: Int = 0,
        @Ignore
        override val progress: Int = 0

) : ProgramInterface {
        companion object {
                fun from(program: EpgProgram): EpgProgramEntity {
                        return EpgProgramEntity(
                                program.eventId,
                                program.nextEventId,
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

        fun toEpgProgram(): EpgProgram {
                return EpgProgram(eventId, nextEventId, channelId, start, stop, title, subtitle, summary, description, credits, category, keyword, serieslinkId, episodeId, seasonId, brandId, contentType, ageRating, starRating, copyrightYear, firstAired, seasonNumber, seasonCount, episodeNumber, episodeCount, partNumber, partCount, episodeOnscreen, image, dvrId, serieslinkUri, episodeUri, modifiedTime, connectionId, channelName, channelIcon, recording)
        }
}
