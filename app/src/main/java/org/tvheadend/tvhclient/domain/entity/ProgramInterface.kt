package org.tvheadend.tvhclient.domain.entity

interface ProgramInterface {

    var eventId: Int                // u32   required   Event ID
    var nextEventId: Int            // u32   optional   ID of next event on the same channel.
    var channelId: Int              // u32   required   The channel this event is related to.
    var start: Long                 // u64   required   Start time of event, UNIX time.
    var stop: Long                  // u64   required   Ending time of event, UNIX time.
    var title: String?              // str   optional   Title of event.
    var subtitle: String?           // str   optional   Subtitle of event.
    var summary: String?            // str   optional   Short description of the event (Added in version 6).
    var description: String?        // str   optional   Long description of the event.

    var credits: String?            // str   optional
    var category: String?           // str   optional
    var keyword: String?            // str   optional

    var serieslinkId: Int           // u32   optional   Series Link ID (Added in version 6).
    var episodeId: Int              // u32   optional   Episode ID (Added in version 6).
    var seasonId: Int               // u32   optional   Season ID (Added in version 6).
    var brandId: Int                // u32   optional   Brand ID (Added in version 6).

    var contentType: Int            // u32   optional   DVB content code (Added in version 4, Modified in version 6*).
    var ageRating: Int              // u32   optional   Minimum age rating (Added in version 6).
    var starRating: Int             // u32   optional   Star rating (1-5) (Added in version 6).
    var copyrightYear: Int          // str   optional   The copyright year (Added in version 33)
    var firstAired: Long            // s64   optional   Original broadcast time, UNIX time (Added in version 6).

    var seasonNumber: Int           // u32   optional   Season number (Added in version 6).
    var seasonCount: Int            // u32   optional   Show season count (Added in version 6).
    var episodeNumber: Int          // u32   optional   Episode number (Added in version 6).
    var episodeCount: Int           // u32   optional   Season episode count (Added in version 6).
    var partNumber: Int             // u32   optional   Multi-part episode part number (Added in version 6).
    var partCount: Int              // u32   optional   Multi-part episode part count (Added in version 6).
    var episodeOnscreen: String?    // str   optional   Textual representation of episode number (Added in version 6).

    var image: String?              // str   optional   URL to a still capture from the episode (Added in version 6).
    var dvrId: Int                  // u32   optional   ID of a recording (Added in version 5).
    var serieslinkUri: String?      // str   optional
    var episodeUri: String?         // str   optional

    var connectionId: Int
    var channelName: String?
    var channelIcon: String?

    var recording: Recording?

    val duration: Int
    val progress: Int
}