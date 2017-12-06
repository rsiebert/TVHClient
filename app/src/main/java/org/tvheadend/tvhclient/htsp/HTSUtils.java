package org.tvheadend.tvhclient.htsp;

import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.model.TimerRecording;

public class HTSUtils {
    private HTSUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static ChannelTag convertMessageToChannelTagModel(ChannelTag tag, HTSMessage msg) {
        if (msg.containsKey("tagId")) {
            tag.tagId = msg.getInt("tagId");
        }
        if (msg.containsKey("tagName")) {
            tag.tagName = msg.getString("tagName");
        }
        if (msg.containsKey("tagIndex")) {
            tag.tagIndex = msg.getInt("tagIndex");
        }
        if (msg.containsKey("tagIcon")) {
            tag.tagIcon = msg.getString("tagIcon");
        }
        if (msg.containsKey("tagTitledIcon")) {
            tag.tagTitledIcon = msg.getInt("tagTitledIcon");
        }
        if (msg.containsKey("members")) {
            tag.members = msg.getIntList("members", tag.members);
        }
        return tag;
    }

    public static Channel convertMessageToChannelModel(Channel channel, HTSMessage msg) {
        if (msg.containsKey("channelId")) {
            channel.channelId = msg.getInt("channelId");
        }
        if (msg.containsKey("channelNumber")) {
            channel.channelNumber = msg.getInt("channelNumber");
        }
        if (msg.containsKey("channelNumberMinor")) {
            channel.channelNumberMinor = msg.getInt("channelNumberMinor");
        }
        if (msg.containsKey("channelName")) {
            channel.channelName = msg.getString("channelName");
        }
        if (msg.containsKey("channelIcon")) {
            channel.channelIcon = msg.getString("channelIcon");
        }
        if (msg.containsKey("eventId")) {
            channel.eventId = msg.getInt("eventId");
        }
        if (msg.containsKey("nextEventId")) {
            channel.nextEventId = msg.getInt("nextEventId");
        }
        if (msg.containsKey("tags")) {
            channel.tags = msg.getIntList("tags", channel.tags);
        }
        return channel;
    }

    public static Recording convertMessageToRecordingModel(Recording recording, HTSMessage msg) {
        if (msg.containsKey("id")) {
            recording.id = msg.getInt("id");
        }
        if (msg.containsKey("channel")) {
            recording.channel = msg.getInt("channel");
        }
        if (msg.containsKey("start")) {
            recording.start = msg.getLong("start");
        }
        if (msg.containsKey("stop")) {
            recording.stop = msg.getLong("stop");
        }
        if (msg.containsKey("startExtra")) {
            recording.startExtra = msg.getLong("startExtra");
        }
        if (msg.containsKey("stopExtra")) {
            recording.stopExtra = msg.getLong("stopExtra");
        }
        if (msg.containsKey("retention")) {
            recording.retention = msg.getLong("retention");
        }
        if (msg.containsKey("priority")) {
            recording.priority = msg.getInt("priority");
        }
        if (msg.containsKey("eventId")) {
            recording.eventId = msg.getInt("eventId");
        }
        if (msg.containsKey("autorecId")) {
            recording.autorecId = msg.getString("autorecId");
        }
        if (msg.containsKey("timerecId")) {
            recording.timerecId = msg.getString("timerecId");
        }
        if (msg.containsKey("contentType")) {
            recording.contentType = msg.getInt("contentType");
        }
        if (msg.containsKey("title")) {
            recording.title = msg.getString("title");
        }
        if (msg.containsKey("subtitle")) {
            recording.subtitle = msg.getString("subtitle");
        }
        if (msg.containsKey("summary")) {
            recording.summary = msg.getString("summary");
        }
        if (msg.containsKey("description")) {
            recording.description = msg.getString("description");
        }
        if (msg.containsKey("state")) {
            recording.state = msg.getString("state");
        }
        if (msg.containsKey("error")) {
            recording.error = msg.getString("error");
        }
        if (msg.containsKey("owner")) {
            recording.owner = msg.getString("owner");
        }
        if (msg.containsKey("creator")) {
            recording.creator = msg.getString("creator");
        }
        if (msg.containsKey("subscriptionError")) {
            recording.subscriptionError = msg.getString("subscriptionError");
        }
        if (msg.containsKey("streamErrors")) {
            recording.streamErrors = msg.getString("streamErrors");
        }
        if (msg.containsKey("dataErrors")) {
            recording.dataErrors = msg.getString("dataErrors");
        }
        if (msg.containsKey("path")) {
            recording.path = msg.getString("path");
        }
        if (msg.containsKey("dataSize")) {
            recording.dataSize = msg.getLong("dataSize");
        }
        if (msg.containsKey("enabled")) {
            recording.enabled = msg.getInt("enabled");
        }
        return recording;
    }

    public static Program convertMessageToProgramModel(Program program, HTSMessage msg) {

        if (msg.containsKey("eventId")) {
            program.eventId = msg.getInt("eventId");
        }
        if (msg.containsKey("channelId")) {
            program.channelId = msg.getInt("channelId");
        }
        if (msg.containsKey("start")) {
            program.start = msg.getLong("start");
        }
        if (msg.containsKey("stop")) {
            program.stop = msg.getLong("stop");
        }
        if (msg.containsKey("title")) {
            program.title = msg.getString("title");
        }
        if (msg.containsKey("subtitle")) {
            program.subtitle = msg.getString("subtitle");
        }
        if (msg.containsKey("summary")) {
            program.summary = msg.getString("summary");
        }
        if (msg.containsKey("description")) {
            program.description = msg.getString("description");
        }
        if (msg.containsKey("serieslinkId")) {
            program.serieslinkId = msg.getInt("serieslinkId");
        }
        if (msg.containsKey("episodeId")) {
            program.episodeId = msg.getInt("episodeId");
        }
        if (msg.containsKey("seasonId")) {
            program.seasonId = msg.getInt("seasonId");
        }
        if (msg.containsKey("brandId")) {
            program.brandId = msg.getInt("brandId");
        }
        if (msg.containsKey("contentType")) {
            program.contentType = msg.getInt("contentType");
        }
        if (msg.containsKey("ageRating")) {
            program.ageRating = msg.getInt("ageRating");
        }
        if (msg.containsKey("starRating")) {
            program.starRating = msg.getInt("starRating");
        }
        if (msg.containsKey("firstAired")) {
            program.firstAired = msg.getLong("firstAired");
        }
        if (msg.containsKey("seasonNumber")) {
            program.seasonNumber = msg.getInt("seasonNumber");
        }
        if (msg.containsKey("seasonCount")) {
            program.seasonCount = msg.getInt("seasonCount");
        }
        if (msg.containsKey("episodeNumber")) {
            program.episodeNumber = msg.getInt("episodeNumber");
        }
        if (msg.containsKey("episodeCount")) {
            program.episodeCount = msg.getInt("episodeCount");
        }
        if (msg.containsKey("partNumber")) {
            program.partNumber = msg.getInt("partNumber");
        }
        if (msg.containsKey("partCount")) {
            program.partCount = msg.getInt("partCount");
        }
        if (msg.containsKey("episodeOnscreen")) {
            program.episodeOnscreen = msg.getString("episodeOnscreen");
        }
        if (msg.containsKey("image")) {
            program.image = msg.getString("image");
        }
        if (msg.containsKey("dvrId")) {
            program.dvrId = msg.getInt("dvrId");
        }
        if (msg.containsKey("nextEventId")) {
            program.nextEventId = msg.getInt("nextEventId");
        }
        return program;
    }

    public static SeriesRecording convertMessageToSeriesRecordingModel(SeriesRecording seriesRecording, HTSMessage msg) {
        if (msg.containsKey("id")) {
            seriesRecording.id = msg.getString("id");
        }
        if (msg.containsKey("enabled")) {
            seriesRecording.enabled = msg.getInt("enabled");
        }
        if (msg.containsKey("name")) {
            seriesRecording.name = msg.getString("name");
        }
        if (msg.containsKey("minDuration")) {
            seriesRecording.minDuration = msg.getInt("minDuration");
        }
        if (msg.containsKey("maxDuration")) {
            seriesRecording.maxDuration = msg.getInt("maxDuration");
        }
        if (msg.containsKey("retention")) {
            seriesRecording.retention = msg.getInt("retention");
        }
        if (msg.containsKey("daysOfWeek")) {
            seriesRecording.daysOfWeek = msg.getInt("daysOfWeek");
        }
        if (msg.containsKey("priority")) {
            seriesRecording.priority = msg.getInt("priority");
        }
        if (msg.containsKey("approxTime")) {
            seriesRecording.approxTime = msg.getInt("approxTime");
        }
        if (msg.containsKey("start")) {
            seriesRecording.start = msg.getInt("start");
        }
        if (msg.containsKey("startWindow")) {
            seriesRecording.startWindow = msg.getInt("startWindow");
        }
        if (msg.containsKey("startExtra")) {
            seriesRecording.startExtra = msg.getLong("startExtra");
        }
        if (msg.containsKey("stopExtra")) {
            seriesRecording.stopExtra = msg.getLong("stopExtra");
        }
        if (msg.containsKey("title")) {
            seriesRecording.title = msg.getString("title");
        }
        if (msg.containsKey("fulltext")) {
            seriesRecording.fulltext = msg.getInt("fulltext");
        }
        if (msg.containsKey("directory")) {
            seriesRecording.directory = msg.getString("directory");
        }
        if (msg.containsKey("channel")) {
            seriesRecording.channel = msg.getInt("channel");
        }
        if (msg.containsKey("owner")) {
            seriesRecording.owner = msg.getString("owner");
        }
        if (msg.containsKey("creator")) {
            seriesRecording.creator = msg.getString("creator");
        }
        if (msg.containsKey("dupDetect")) {
            seriesRecording.dupDetect = msg.getInt("dupDetect");
        }
        return seriesRecording;
    }


    public static TimerRecording convertMessageToTimerRecordingModel(TimerRecording timerRecording, HTSMessage msg) {
        if (msg.containsKey("id")) {
            timerRecording.id = msg.getString("id");
        }
        if (msg.containsKey("title")) {
            timerRecording.title = msg.getString("title");
        }
        if (msg.containsKey("directory")) {
            timerRecording.directory = msg.getString("directory", null);
        }
        if (msg.containsKey("enabled")) {
            timerRecording.enabled = msg.getInt("enabled");
        }
        if (msg.containsKey("name")) {
            timerRecording.name = msg.getString("name");
        }
        if (msg.containsKey("configName")) {
            timerRecording.configName = msg.getString("configName");
        }
        if (msg.containsKey("channel")) {
            timerRecording.channel = msg.getInt("channel");
        }
        if (msg.containsKey("daysOfWeek")) {
            timerRecording.daysOfWeek = msg.getInt("daysOfWeek");
        }
        if (msg.containsKey("priority")) {
            timerRecording.priority = msg.getInt("priority");
        }
        if (msg.containsKey("start")) {
            timerRecording.start = msg.getInt("start");
        }
        if (msg.containsKey("stop")) {
            timerRecording.stop = msg.getInt("stop");
        }
        if (msg.containsKey("retention")) {
            timerRecording.retention = msg.getInt("retention");
        }
        if (msg.containsKey("owner")) {
            timerRecording.owner = msg.getString("owner");
        }
        if (msg.containsKey("creator")) {
            timerRecording.creator = msg.getString("creator");
        }
        return timerRecording;
    }

}
