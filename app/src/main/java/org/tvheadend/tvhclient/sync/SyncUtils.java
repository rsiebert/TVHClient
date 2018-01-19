package org.tvheadend.tvhclient.sync;

import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.ChannelTag;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.data.model.SeriesRecording;
import org.tvheadend.tvhclient.data.model.TimerRecording;
import org.tvheadend.tvhclient.htsp.HtspMessage;

public class SyncUtils {
    private SyncUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static ChannelTag convertMessageToChannelTagModel(ChannelTag tag, HtspMessage msg) {
        if (msg.containsKey("tagId")) {
            tag.setTagId(msg.getInteger("tagId"));
        }
        if (msg.containsKey("tagName")) {
            tag.setTagName(msg.getString("tagName"));
        }
        if (msg.containsKey("tagIndex")) {
            tag.setTagIndex(msg.getInteger("tagIndex"));
        }
        if (msg.containsKey("tagIcon")) {
            tag.setTagIcon(msg.getString("tagIcon"));
        }
        if (msg.containsKey("tagTitledIcon")) {
            tag.setTagTitledIcon(msg.getInteger("tagTitledIcon"));
        }
        if (msg.containsKey("members")) {
            tag.setMembers(msg.getIntegerList("members"));
        }
        return tag;
    }

    public static Channel convertMessageToChannelModel(Channel channel, HtspMessage msg) {
        if (msg.containsKey("channelId")) {
            channel.setChannelId(msg.getInteger("channelId"));
        }
        if (msg.containsKey("channelNumber")) {
            channel.setChannelNumber(msg.getInteger("channelNumber"));
        }
        if (msg.containsKey("channelNumberMinor")) {
            channel.setChannelNumberMinor(msg.getInteger("channelNumberMinor"));
        }
        if (msg.containsKey("channelName")) {
            channel.setChannelName(msg.getString("channelName"));
        }
        if (msg.containsKey("channelIcon")) {
            channel.setChannelIcon(msg.getString("channelIcon"));
        }
        if (msg.containsKey("eventId")) {
            channel.setEventId(msg.getInteger("eventId"));
        }
        if (msg.containsKey("nextEventId")) {
            channel.setNextEventId(msg.getInteger("nextEventId"));
        }
        if (msg.containsKey("tags")) {
            channel.setTags(msg.getIntegerList("tags"));
        }
        return channel;
    }

    public static Recording convertMessageToRecordingModel(Recording recording, HtspMessage msg) {
        if (msg.containsKey("id")) {
            recording.setId(msg.getInteger("id"));
        }
        if (msg.containsKey("channel")) {
            recording.setChannelId(msg.getInteger("channel"));
        }
        if (msg.containsKey("start")) {
            recording.setStart(msg.getLong("start") * 1000);
        }
        if (msg.containsKey("stop")) {
            recording.setStop(msg.getLong("stop") * 1000);
        }
        if (msg.containsKey("startExtra")) {
            recording.setStartExtra(msg.getLong("startExtra"));
        }
        if (msg.containsKey("stopExtra")) {
            recording.setStopExtra(msg.getLong("stopExtra"));
        }
        if (msg.containsKey("retention")) {
            recording.setRetention(msg.getLong("retention"));
        }
        if (msg.containsKey("priority")) {
            recording.setPriority(msg.getInteger("priority"));
        }
        if (msg.containsKey("eventId")) {
            recording.setEventId(msg.getInteger("eventId"));
        }
        if (msg.containsKey("autorecId")) {
            recording.setAutorecId(msg.getString("autorecId"));
        }
        if (msg.containsKey("timerecId")) {
            recording.setTimerecId(msg.getString("timerecId"));
        }
        if (msg.containsKey("contentType")) {
            recording.setContentType(msg.getInteger("contentType"));
        }
        if (msg.containsKey("title")) {
            recording.setTitle(msg.getString("title"));
        }
        if (msg.containsKey("subtitle")) {
            recording.setSubtitle(msg.getString("subtitle"));
        }
        if (msg.containsKey("summary")) {
            recording.setSummary(msg.getString("summary"));
        }
        if (msg.containsKey("description")) {
            recording.setDescription(msg.getString("description"));
        }
        if (msg.containsKey("state")) {
            recording.setState(msg.getString("state"));
        }
        if (msg.containsKey("error")) {
            recording.setError(msg.getString("error"));
        }
        if (msg.containsKey("owner")) {
            recording.setOwner(msg.getString("owner"));
        }
        if (msg.containsKey("creator")) {
            recording.setCreator(msg.getString("creator"));
        }
        if (msg.containsKey("subscriptionError")) {
            recording.setSubscriptionError(msg.getString("subscriptionError"));
        }
        if (msg.containsKey("streamErrors")) {
            recording.setStreamErrors(msg.getString("streamErrors"));
        }
        if (msg.containsKey("dataErrors")) {
            recording.setDataErrors(msg.getString("dataErrors"));
        }
        if (msg.containsKey("path")) {
            recording.setPath(msg.getString("path"));
        }
        if (msg.containsKey("dataSize")) {
            recording.setDataSize(msg.getLong("dataSize"));
        }
        if (msg.containsKey("enabled")) {
            recording.setEnabled(msg.getInteger("enabled"));
        }
        return recording;
    }

    public static Program convertMessageToProgramModel(Program program, HtspMessage msg) {

        if (msg.containsKey("eventId")) {
            program.setEventId(msg.getInteger("eventId"));
        }
        if (msg.containsKey("channelId")) {
            program.setChannelId(msg.getInteger("channelId"));
        }
        if (msg.containsKey("start")) {
            program.setStart(msg.getLong("start") * 1000);
        }
        if (msg.containsKey("stop")) {
            program.setStop(msg.getLong("stop") * 1000);
        }
        if (msg.containsKey("title")) {
            program.setTitle(msg.getString("title"));
        }
        if (msg.containsKey("subtitle")) {
            program.setSubtitle(msg.getString("subtitle"));
        }
        if (msg.containsKey("summary")) {
            program.setSummary(msg.getString("summary"));
        }
        if (msg.containsKey("description")) {
            program.setDescription(msg.getString("description"));
        }
        if (msg.containsKey("serieslinkId")) {
            program.setSerieslinkId(msg.getInteger("serieslinkId"));
        }
        if (msg.containsKey("episodeId")) {
            program.setEpisodeId(msg.getInteger("episodeId"));
        }
        if (msg.containsKey("seasonId")) {
            program.setSeasonId(msg.getInteger("seasonId"));
        }
        if (msg.containsKey("brandId")) {
            program.setBrandId(msg.getInteger("brandId"));
        }
        if (msg.containsKey("contentType")) {
            program.setContentType(msg.getInteger("contentType"));
        }
        if (msg.containsKey("ageRating")) {
            program.setAgeRating(msg.getInteger("ageRating"));
        }
        if (msg.containsKey("starRating")) {
            program.setStarRating(msg.getInteger("starRating"));
        }
        if (msg.containsKey("firstAired")) {
            program.setFirstAired(msg.getLong("firstAired"));
        }
        if (msg.containsKey("seasonNumber")) {
            program.setSeasonNumber(msg.getInteger("seasonNumber"));
        }
        if (msg.containsKey("seasonCount")) {
            program.setSeasonCount(msg.getInteger("seasonCount"));
        }
        if (msg.containsKey("episodeNumber")) {
            program.setEpisodeNumber(msg.getInteger("episodeNumber"));
        }
        if (msg.containsKey("episodeCount")) {
            program.setEpisodeCount(msg.getInteger("episodeCount"));
        }
        if (msg.containsKey("partNumber")) {
            program.setPartNumber(msg.getInteger("partNumber"));
        }
        if (msg.containsKey("partCount")) {
            program.setPartCount(msg.getInteger("partCount"));
        }
        if (msg.containsKey("episodeOnscreen")) {
            program.setEpisodeOnscreen(msg.getString("episodeOnscreen"));
        }
        if (msg.containsKey("image")) {
            program.setImage(msg.getString("image"));
        }
        if (msg.containsKey("dvrId")) {
            program.setDvrId(msg.getInteger("dvrId"));
        }
        if (msg.containsKey("nextEventId")) {
            program.setNextEventId(msg.getInteger("nextEventId"));
        }
        return program;
    }

    public static SeriesRecording convertMessageToSeriesRecordingModel(SeriesRecording seriesRecording, HtspMessage msg) {
        if (msg.containsKey("id")) {
            seriesRecording.setId(msg.getString("id"));
        }
        if (msg.containsKey("enabled")) {
            seriesRecording.setEnabled(msg.getInteger("enabled"));
        }
        if (msg.containsKey("name")) {
            seriesRecording.setName(msg.getString("name"));
        }
        if (msg.containsKey("minDuration")) {
            seriesRecording.setMinDuration(msg.getInteger("minDuration"));
        }
        if (msg.containsKey("maxDuration")) {
            seriesRecording.setMaxDuration(msg.getInteger("maxDuration"));
        }
        if (msg.containsKey("retention")) {
            seriesRecording.setRetention(msg.getInteger("retention"));
        }
        if (msg.containsKey("daysOfWeek")) {
            seriesRecording.setDaysOfWeek(msg.getInteger("daysOfWeek"));
        }
        if (msg.containsKey("priority")) {
            seriesRecording.setPriority(msg.getInteger("priority"));
        }
        if (msg.containsKey("approxTime")) {
            seriesRecording.setApproxTime(msg.getInteger("approxTime"));
        }
        if (msg.containsKey("start")) {
            seriesRecording.setStart(msg.getLong("start") * 1000 * 60);
        }
        if (msg.containsKey("startWindow")) {
            seriesRecording.setStartWindow(msg.getLong("startWindow") * 1000 * 60);
        }
        if (msg.containsKey("startExtra")) {
            seriesRecording.setStartExtra(msg.getLong("startExtra"));
        }
        if (msg.containsKey("stopExtra")) {
            seriesRecording.setStopExtra(msg.getLong("stopExtra"));
        }
        if (msg.containsKey("title")) {
            seriesRecording.setTitle(msg.getString("title"));
        }
        if (msg.containsKey("fulltext")) {
            seriesRecording.setFulltext(msg.getInteger("fulltext"));
        }
        if (msg.containsKey("directory")) {
            seriesRecording.setDirectory(msg.getString("directory"));
        }
        if (msg.containsKey("channel")) {
            seriesRecording.setChannelId(msg.getInteger("channel"));
        }
        if (msg.containsKey("owner")) {
            seriesRecording.setOwner(msg.getString("owner"));
        }
        if (msg.containsKey("creator")) {
            seriesRecording.setCreator(msg.getString("creator"));
        }
        if (msg.containsKey("dupDetect")) {
            seriesRecording.setDupDetect(msg.getInteger("dupDetect"));
        }
        return seriesRecording;
    }


    public static TimerRecording convertMessageToTimerRecordingModel(TimerRecording timerRecording, HtspMessage msg) {
        if (msg.containsKey("id")) {
            timerRecording.setId(msg.getString("id"));
        }
        if (msg.containsKey("title")) {
            timerRecording.setTitle(msg.getString("title"));
        }
        if (msg.containsKey("directory")) {
            timerRecording.setDirectory(msg.getString("directory", null));
        }
        if (msg.containsKey("enabled")) {
            timerRecording.setEnabled(msg.getInteger("enabled"));
        }
        if (msg.containsKey("name")) {
            timerRecording.setName(msg.getString("name"));
        }
        if (msg.containsKey("configName")) {
            timerRecording.setConfigName(msg.getString("configName"));
        }
        if (msg.containsKey("channel")) {
            timerRecording.setChannelId(msg.getInteger("channel"));
        }
        if (msg.containsKey("daysOfWeek")) {
            timerRecording.setDaysOfWeek(msg.getInteger("daysOfWeek"));
        }
        if (msg.containsKey("priority")) {
            timerRecording.setPriority(msg.getInteger("priority"));
        }
        if (msg.containsKey("start")) {
            timerRecording.setStart(msg.getLong("start") * 1000 * 60);
        }
        if (msg.containsKey("stop")) {
            timerRecording.setStop(msg.getLong("stop") * 1000 * 60);
        }
        if (msg.containsKey("retention")) {
            timerRecording.setRetention(msg.getInteger("retention"));
        }
        if (msg.containsKey("owner")) {
            timerRecording.setOwner(msg.getString("owner"));
        }
        if (msg.containsKey("creator")) {
            timerRecording.setCreator(msg.getString("creator"));
        }
        return timerRecording;
    }
}
