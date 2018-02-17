package org.tvheadend.tvhclient.service;

import android.content.Intent;
import android.util.Log;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.service.htsp.HtspMessage;

import java.util.List;

class EpgSyncUtils {
    private EpgSyncUtils() {
        throw new IllegalAccessError("Utility class");
    }

    static ChannelTag convertMessageToChannelTagModel(ChannelTag tag, HtspMessage msg) {
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
            List<Integer> members = msg.getIntegerList("members");
            tag.setChannelCount(members.size());
        }
        return tag;
    }

    static Channel convertMessageToChannelModel(Channel channel, HtspMessage msg) {
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
        return channel;
    }

    static Recording convertMessageToRecordingModel(Recording recording, HtspMessage msg) {
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

    static Program convertMessageToProgramModel(Program program, HtspMessage msg) {

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

    static SeriesRecording convertMessageToSeriesRecordingModel(SeriesRecording seriesRecording, HtspMessage msg) {
        Log.d("X", "convertMessageToSeriesRecordingModel() called with: seriesRecording = [" + seriesRecording + "], msg = [" + msg + "]");
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


    static TimerRecording convertMessageToTimerRecordingModel(TimerRecording timerRecording, HtspMessage msg) {
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
            Log.d("X", "convertMessageToTimerRecordingModel: start " + msg.getLong("start"));
            timerRecording.setStart(msg.getLong("start") * 1000 * 60);
        }
        if (msg.containsKey("stop")) {
            Log.d("X", "convertMessageToTimerRecordingModel: stop " + msg.getLong("stop"));
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

    static HtspMessage convertIntentToAutorecMessage(Intent intent, int htspVersion) {
        final long enabled = intent.getIntExtra("enabled", 1);
        final String title = intent.getStringExtra("title");
        final String fulltext = intent.getStringExtra("fulltext");
        final String directory = intent.getStringExtra("directory");
        final String name = intent.getStringExtra("name");
        final String configName = intent.getStringExtra("configName");
        final long channelId = intent.getIntExtra("channelId", 0);
        final long minDuration = intent.getIntExtra("minDuration", 0);
        final long maxDuration = intent.getIntExtra("maxDuration", 0);
        final long daysOfWeek = intent.getIntExtra("daysOfWeek", 127);
        final long priority = intent.getIntExtra("priority", 2);
        final long start = intent.getLongExtra("start", -1);
        final long startWindow = intent.getLongExtra("startWindow", -1);
        final long startExtra = intent.getLongExtra("startExtra", 0);
        final long stopExtra = intent.getLongExtra("stopExtra", 0);
        final long dupDetect = intent.getIntExtra("dupDetect", 0);
        final String comment = intent.getStringExtra("comment");

        final HtspMessage request = new HtspMessage();
        if (htspVersion >= 19) {
            request.put("enabled", enabled);
        }
        request.put("title", title);
        if (fulltext != null && htspVersion >= 20) {
            request.put("fulltext", fulltext);
        }
        if (directory != null) {
            request.put("directory", directory);
        }
        if (name != null) {
            request.put("name", name);
        }
        if (configName != null) {
            request.put("configName", configName);
        }
        // Don't add the channel id if none was given.
        // Assume the user wants to record on all channels
        if (channelId > 0) {
            request.put("channelId", channelId);
        }
        // Minimal duration in seconds (0 = Any)
        request.put("minDuration", minDuration);
        // Maximal duration in seconds (0 = Any)
        request.put("maxDuration", maxDuration);
        request.put("daysOfWeek", daysOfWeek);
        request.put("priority", priority);

        // Minutes from midnight (up to 24*60) (window +- 15 minutes) (Obsoleted from version 18)
        // Do not send the value if the default of -1 (no time specified) was set
        if (start >= 0 && htspVersion < 18) {
            request.put("approxTime", start);
        }
        // Minutes from midnight (up to 24*60) for the start of the time window.
        // Do not send the value if the default of -1 (no time specified) was set
        if (start >= 0 && htspVersion >= 18) {
            request.put("start", start);
        }
        // Minutes from midnight (up to 24*60) for the end of the time window (including, cross-noon allowed).
        // Do not send the value if the default of -1 (no time specified) was set
        if (startWindow >= 0 && htspVersion >= 18) {
            request.put("startWindow", startWindow);
        }
        request.put("startExtra", startExtra);
        request.put("stopExtra", stopExtra);

        if (htspVersion >= 20) {
            request.put("dupDetect", dupDetect);
        }
        if (comment != null) {
            request.put("comment", comment);
        }
        return request;
    }

    static HtspMessage convertIntentToDvrMessage(Intent intent, int htspVersion) {
        final long eventId = intent.getIntExtra("eventId", 0);
        final long channelId = intent.getIntExtra("channelId", 0);
        final long start = intent.getLongExtra("start", 0);
        final long stop = intent.getLongExtra("stop", 0);
        final long retention = intent.getLongExtra("retention", 0);
        final long priority = intent.getIntExtra("priority", 2);
        final long startExtra = intent.getLongExtra("startExtra", 0);
        final long stopExtra = intent.getLongExtra("stopExtra", 0);
        final String title = intent.getStringExtra("title");
        final String subtitle = intent.getStringExtra("subtitle");
        final String description = intent.getStringExtra("description");
        final String configName = intent.getStringExtra("configName");
        final long enabled = intent.getIntExtra("enabled", 1);
        // Controls that certain fields will only be added when the recording
        // is only scheduled and not being recorded
        final boolean isRecording = intent.getBooleanExtra("isRecording", false);

        final HtspMessage request = new HtspMessage();
        // If the eventId is set then an existing program from the program guide
        // shall be recorded. The server will then ignore the other fields
        // automatically.
        if (eventId > 0) {
            request.put("eventId", eventId);
        }
        if (channelId > 0 && htspVersion >= 22) {
            request.put("channelId", channelId);
        }
        if (!isRecording && start > 0) {
            request.put("start", start);
        }
        if (stop > 0) {
            request.put("stop", stop);
        }
        if (!isRecording && retention > 0) {
            request.put("retention", retention);
        }
        if (!isRecording && priority > 0) {
            request.put("priority", priority);
        }
        if (!isRecording && startExtra > 0) {
            request.put("startExtra", startExtra);
        }
        if (stopExtra > 0) {
            request.put("stopExtra", stopExtra);
        }
        // Only add the text fields if no event id was given
        if (eventId == 0) {
            if (title != null) {
                request.put("title", title);
            }
            if (subtitle != null && htspVersion >= 21) {
                request.put("subtitle", subtitle);
            }
            if (description != null) {
                request.put("description", description);
            }
        }
        if (configName != null) {
            request.put("configName", configName);
        }
        if (htspVersion >= 23) {
            request.put("enabled", enabled);
        }
        return request;
    }

    static HtspMessage convertIntentToTimerecMessage(Intent intent, int htspVersion) {
        final long enabled = intent.getIntExtra("enabled", 1);
        final String title = intent.getStringExtra("title");
        final String directory = intent.getStringExtra("directory");
        final String name = intent.getStringExtra("name");
        final String configName = intent.getStringExtra("configName");
        final long channelId = intent.getIntExtra("channelId", 0);
        final long daysOfWeek = intent.getIntExtra("daysOfWeek", 0);
        final long priority = intent.getIntExtra("priority", 2);
        final long start = intent.getLongExtra("start", -1);
        final long stop = intent.getLongExtra("stop", -1);
        final long retention = intent.getIntExtra("retention", -1);
        final String comment = intent.getStringExtra("comment");

        final HtspMessage request = new HtspMessage();
        if (htspVersion >= 19) {
            request.put("enabled", enabled);
        }
        request.put("title", title);
        if (directory != null) {
            request.put("directory", directory);
        }
        if (name != null) {
            request.put("name", name);
        }
        if (configName != null) {
            request.put("configName", configName);
        }
        if (channelId > 0) {
            request.put("channelId", channelId);
        }
        request.put("daysOfWeek", daysOfWeek);
        request.put("priority", priority);

        if (start >= 0) {
            request.put("start", start);
        }
        if (stop >= 0) {
            request.put("stop", stop);
        }
        if (retention > 0) {
            request.put("retention", retention);
        }
        if (comment != null) {
            request.put("comment", comment);
        }
        return request;
    }
}
