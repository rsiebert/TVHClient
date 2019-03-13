package org.tvheadend.tvhclient.data.service;

import android.content.Intent;
import android.text.TextUtils;

import org.tvheadend.tvhclient.data.service.htsp.HtspMessage;
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.domain.entity.ChannelTag;
import org.tvheadend.tvhclient.domain.entity.Program;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.domain.entity.SeriesRecording;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;
import org.tvheadend.tvhclient.domain.entity.TimerRecording;

import java.util.List;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class HtspUtils {

    static ChannelTag convertMessageToChannelTagModel(@NonNull ChannelTag tag, @NonNull HtspMessage msg, @NonNull List<Channel> channels) {
        if (msg.containsKey("tagId")) {
            tag.setTagId(msg.getInteger("tagId"));
        }
        if (msg.containsKey("tagName")) {
            tag.setTagName(msg.getString("tagName"));
        }
        if (msg.containsKey("tagIndex")) {
            if (msg.getInteger("tagIndex") > 0) {
                tag.setTagIndex(msg.getInteger("tagIndex"));
            }
        }
        if (msg.containsKey("tagIcon")) {
            if (!TextUtils.isEmpty(msg.getString("tagIcon"))) {
                tag.setTagIcon(msg.getString("tagIcon"));
            }
        }
        if (msg.containsKey("tagTitledIcon")) {
            if (msg.getInteger("tagTitledIcon") > 0) {
                tag.setTagTitledIcon(msg.getInteger("tagTitledIcon"));
            }
        }
        if (msg.containsKey("members")) {
            List<Integer> members = msg.getIntegerList("members");
            tag.setMembers(members);

            int channelCount = 0;
            for (Integer channelId : members) {
                for (Channel channel : channels) {
                    if (channel.getId() == channelId) {
                        channelCount++;
                        break;
                    }
                }
            }
            tag.setChannelCount(channelCount);
        }
        return tag;
    }

    static Channel convertMessageToChannelModel(@NonNull Channel channel, @NonNull HtspMessage msg) {
        if (msg.containsKey("channelId")) {
            channel.setId(msg.getInteger("channelId"));
        }
        if (msg.containsKey("channelNumber") && msg.containsKey("channelNumberMinor")) {
            int channelNumber = msg.getInteger("channelNumber");
            int channelNumberMinor = msg.getInteger("channelNumberMinor");
            channel.setNumber(channelNumber);
            channel.setNumberMinor(channelNumberMinor);
            channel.setDisplayNumber(channelNumber + "." + channelNumberMinor);

        } else if (msg.containsKey("channelNumber")) {
            int channelNumber = msg.getInteger("channelNumber");
            channel.setNumber(channelNumber);
            channel.setDisplayNumber(String.valueOf(channelNumber) + ".0");
        }
        if (msg.containsKey("channelName")) {
            channel.setName(msg.getString("channelName"));
        }
        if (msg.containsKey("channelIcon")) {
            if (!TextUtils.isEmpty(msg.getString("channelIcon"))) {
                channel.setIcon(msg.getString("channelIcon"));
            }
        }
        if (msg.containsKey("eventId")) {
            if (msg.getInteger("eventId") > 0) {
                channel.setEventId(msg.getInteger("eventId"));
            }
        }
        if (msg.containsKey("nextEventId")) {
            if (msg.getInteger("nextEventId") > 0) {
                channel.setNextEventId(msg.getInteger("nextEventId"));
            }
        }
        if (msg.containsKey("tags")) {
            List<Integer> tags = msg.getIntegerList("tags");
            channel.setTags(tags);
        }
        return channel;
    }

    static Recording convertMessageToRecordingModel(@NonNull Recording recording, @NonNull HtspMessage msg) {
        if (msg.containsKey("id")) {
            recording.setId(msg.getInteger("id"));
        }
        if (msg.containsKey("channel")) {
            if (msg.getInteger("channel") > 0) {
                recording.setChannelId(msg.getInteger("channel"));
            }
        }
        if (msg.containsKey("start")) {
            // The message value is in seconds, convert to milliseconds
            recording.setStart(msg.getLong("start") * 1000);
        }
        if (msg.containsKey("stop")) {
            // The message value is in seconds, convert to milliseconds
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
            if (msg.getInteger("eventId") > 0) {
                recording.setEventId(msg.getInteger("eventId"));
            }
        }
        if (msg.containsKey("autorecId")) {
            if (!TextUtils.isEmpty(msg.getString("autorecId"))) {
                recording.setAutorecId(msg.getString("autorecId"));
            }
        }
        if (msg.containsKey("timerecId")) {
            if (!TextUtils.isEmpty(msg.getString("timerecId"))) {
                recording.setTimerecId(msg.getString("timerecId"));
            }
        }
        if (msg.containsKey("contentType")) {
            if (msg.getInteger("contentType") > 0) {
                recording.setContentType(msg.getInteger("contentType"));
            }
        }
        if (msg.containsKey("title")) {
            if (!TextUtils.isEmpty(msg.getString("title"))) {
                recording.setTitle(msg.getString("title"));
            }
        }
        if (msg.containsKey("subtitle")) {
            if (!TextUtils.isEmpty(msg.getString("subtitle"))) {
                recording.setSubtitle(msg.getString("subtitle"));
            }
        }
        if (msg.containsKey("summary")) {
            if (!TextUtils.isEmpty(msg.getString("summary"))) {
                recording.setSummary(msg.getString("summary"));
            }
        }
        if (msg.containsKey("description")) {
            if (!TextUtils.isEmpty(msg.getString("description"))) {
                recording.setDescription(msg.getString("description"));
            }
        }
        if (msg.containsKey("state")) {
            recording.setState(msg.getString("state"));
        }
        if (msg.containsKey("error")) {
            if (!TextUtils.isEmpty(msg.getString("error"))) {
                recording.setError(msg.getString("error"));
            }
        }
        if (msg.containsKey("owner")) {
            if (!TextUtils.isEmpty(msg.getString("owner"))) {
                recording.setOwner(msg.getString("owner"));
            }
        }
        if (msg.containsKey("creator")) {
            if (!TextUtils.isEmpty(msg.getString("creator"))) {
                recording.setCreator(msg.getString("creator"));
            }
        }
        if (msg.containsKey("subscriptionError")) {
            if (!TextUtils.isEmpty(msg.getString("subscriptionError"))) {
                recording.setSubscriptionError(msg.getString("subscriptionError"));
            }
        }
        if (msg.containsKey("streamErrors")) {
            if (!TextUtils.isEmpty(msg.getString("streamErrors"))) {
                recording.setStreamErrors(msg.getString("streamErrors"));
            }
        }
        if (msg.containsKey("dataErrors")) {
            if (!TextUtils.isEmpty(msg.getString("dataErrors"))) {
                recording.setDataErrors(msg.getString("dataErrors"));
            }
        }
        if (msg.containsKey("path")) {
            if (!TextUtils.isEmpty(msg.getString("path"))) {
                recording.setPath(msg.getString("path"));
            }
        }
        if (msg.containsKey("dataSize")) {
            if (msg.getLong("dataSize") > 0) {
                recording.setDataSize(msg.getLong("dataSize"));
            }
        }
        if (msg.containsKey("enabled")) {
            recording.setEnabled(msg.getInteger("enabled") == 1);
        }
        if (msg.containsKey("duplicate")) {
            recording.setDuplicate(msg.getInteger("duplicate"));
        }

        if (msg.containsKey("image")) {
            if (!TextUtils.isEmpty(msg.getString("image"))) {
                recording.setImage(msg.getString("image"));
            }
        }
        if (msg.containsKey("fanart_image")) {
            if (!TextUtils.isEmpty(msg.getString("fanart_image"))) {
                recording.setFanartImage(msg.getString("fanart_image"));
            }
        }
        if (msg.containsKey("copyright_year")) {
            if (msg.getInteger("copyright_year") > 0) {
                recording.setCopyrightYear(msg.getInteger("copyright_year"));
            }
        }
        if (msg.containsKey("removal")) {
            if (msg.getInteger("removal") > 0) {
                recording.setRemoval(msg.getInteger("removal"));
            }
        }
        return recording;
    }

    static Program convertMessageToProgramModel(@NonNull Program program, @NonNull HtspMessage msg) {
        if (msg.containsKey("eventId")) {
            program.setEventId(msg.getInteger("eventId"));
        }
        if (msg.containsKey("channelId")) {
            program.setChannelId(msg.getInteger("channelId"));
        }
        if (msg.containsKey("start")) {
            // The message value is in seconds, convert to milliseconds
            program.setStart(msg.getLong("start") * 1000);
        }
        if (msg.containsKey("stop")) {
            // The message value is in seconds, convert to milliseconds
            program.setStop(msg.getLong("stop") * 1000);
        }
        if (msg.containsKey("title")) {
            if (!TextUtils.isEmpty(msg.getString("title"))) {
                program.setTitle(msg.getString("title"));
            }
        }
        if (msg.containsKey("subtitle")) {
            if (!TextUtils.isEmpty(msg.getString("subtitle"))) {
                program.setSubtitle(msg.getString("subtitle"));
            }
        }
        if (msg.containsKey("summary")) {
            if (!TextUtils.isEmpty(msg.getString("summary"))) {
                program.setSummary(msg.getString("summary"));
            }
        }
        if (msg.containsKey("description")) {
            if (!TextUtils.isEmpty(msg.getString("description"))) {
                program.setDescription(msg.getString("description"));
            }
        }
        if (msg.containsKey("serieslinkId")) {
            if (msg.getInteger("serieslinkId") > 0) {
                program.setSerieslinkId(msg.getInteger("serieslinkId"));
            }
        }
        if (msg.containsKey("episodeId")) {
            if (msg.getInteger("episodeId") > 0) {
                program.setEpisodeId(msg.getInteger("episodeId"));
            }
        }
        if (msg.containsKey("seasonId")) {
            if (msg.getInteger("seasonId") > 0) {
                program.setSeasonId(msg.getInteger("seasonId"));
            }
        }
        if (msg.containsKey("brandId")) {
            if (msg.getInteger("brandId") > 0) {
                program.setBrandId(msg.getInteger("brandId"));
            }
        }
        if (msg.containsKey("contentType")) {
            if (msg.getInteger("contentType") > 0) {
                program.setContentType(msg.getInteger("contentType"));
            }
        }
        if (msg.containsKey("ageRating")) {
            if (msg.getInteger("ageRating") > 0) {
                program.setAgeRating(msg.getInteger("ageRating"));
            }
        }
        if (msg.containsKey("starRating")) {
            if (msg.getInteger("starRating") > 0) {
                program.setStarRating(msg.getInteger("starRating"));
            }
        }
        if (msg.containsKey("firstAired")) {
            if (msg.getInteger("firstAired") > 0) {
                program.setFirstAired(msg.getLong("firstAired"));
            }
        }
        if (msg.containsKey("seasonNumber")) {
            if (msg.getInteger("seasonNumber") > 0) {
                program.setSeasonNumber(msg.getInteger("seasonNumber"));
            }
        }
        if (msg.containsKey("seasonCount")) {
            if (msg.getInteger("seasonCount") > 0) {
                program.setSeasonCount(msg.getInteger("seasonCount"));
            }
        }
        if (msg.containsKey("episodeNumber")) {
            if (msg.getInteger("episodeNumber") > 0) {
                program.setEpisodeNumber(msg.getInteger("episodeNumber"));
            }
        }
        if (msg.containsKey("episodeCount")) {
            if (msg.getInteger("episodeCount") > 0) {
                program.setEpisodeCount(msg.getInteger("episodeCount"));
            }
        }
        if (msg.containsKey("partNumber")) {
            if (msg.getInteger("partNumber") > 0) {
                program.setPartNumber(msg.getInteger("partNumber"));
            }
        }
        if (msg.containsKey("partCount")) {
            if (msg.getInteger("partCount") > 0) {
                program.setPartCount(msg.getInteger("partCount"));
            }
        }
        if (msg.containsKey("episodeOnscreen")) {
            if (!TextUtils.isEmpty(msg.getString("episodeOnscreen"))) {
                program.setEpisodeOnscreen(msg.getString("episodeOnscreen"));
            }
        }
        if (msg.containsKey("image")) {
            if (!TextUtils.isEmpty(msg.getString("image"))) {
                program.setImage(msg.getString("image"));
            }
        }
        if (msg.containsKey("dvrId")) {
            if (msg.getInteger("dvrId") > 0) {
                program.setDvrId(msg.getInteger("dvrId"));
            }
        }
        if (msg.containsKey("nextEventId")) {
            if (msg.getInteger("nextEventId") > 0) {
                program.setNextEventId(msg.getInteger("nextEventId"));
            }
        }
        if (msg.containsKey("episodeOnscreen")) {
            if (!TextUtils.isEmpty(msg.getString("episodeOnscreen"))) {
                program.setEpisodeOnscreen(msg.getString("episodeOnscreen"));
            }
        }
        if (msg.containsKey("serieslinkUri")) {
            if (!TextUtils.isEmpty(msg.getString("serieslinkUri"))) {
                program.setSerieslinkUri(msg.getString("serieslinkUri"));
            }
        }
        if (msg.containsKey("episodeUri")) {
            if (!TextUtils.isEmpty(msg.getString("episodeUri"))) {
                program.setEpisodeUri(msg.getString("episodeUri"));
            }
        }
        if (msg.containsKey("copyright_year")) {
            if (msg.getInteger("copyright_year") > 0) {
                program.setCopyrightYear(msg.getInteger("copyright_year"));
            }
        }
        /*
        if (msg.containsKey("credits")) {
            StringBuilder sb = new StringBuilder();
            for (String credit : msg.getStringArray("credits")) {
                sb.append(credit).append(",");
            }
            // Remove the last separator character
            program.setCredits(sb.substring(0, sb.lastIndexOf(",")));
        }
        if (msg.containsKey("category")) {
            StringBuilder sb = new StringBuilder();
            for (String s : msg.getStringArray("category")) {
                sb.append(s).append(",");
            }
            // Remove the last separator character
            program.setCredits(sb.substring(0, sb.lastIndexOf(",")));

        }
        if (msg.containsKey("keyword")) {
            StringBuilder sb = new StringBuilder();
            for (String s : msg.getStringArray("keyword")) {
                sb.append(s).append(",");
            }
            // Remove the last separator character
            program.setKeyword(sb.substring(0, sb.lastIndexOf(",")));
        }
        */
        return program;
    }

    static SeriesRecording convertMessageToSeriesRecordingModel(@NonNull SeriesRecording seriesRecording, @NonNull HtspMessage msg) {
        if (msg.containsKey("id")) {
            seriesRecording.setId(msg.getString("id"));
        }
        if (msg.containsKey("enabled")) {
            seriesRecording.setEnabled(msg.getInteger("enabled") == 1);
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
            // The message value is in minutes, convert to milliseconds
            seriesRecording.setStart(msg.getLong("start") * 1000 * 60);
        }
        if (msg.containsKey("startWindow")) {
            // The message value is in minutes, convert to milliseconds
            seriesRecording.setStartWindow(msg.getLong("startWindow") * 1000 * 60);
        }
        if (msg.containsKey("startExtra")) {
            seriesRecording.setStartExtra(msg.getLong("startExtra"));
        }
        if (msg.containsKey("stopExtra")) {
            seriesRecording.setStopExtra(msg.getLong("stopExtra"));
        }
        if (msg.containsKey("title")) {
            if (!TextUtils.isEmpty(msg.getString("title"))) {
                seriesRecording.setTitle(msg.getString("title"));
            }
        }
        if (msg.containsKey("fulltext")) {
            if (!TextUtils.isEmpty(msg.getString("fulltext"))) {
                seriesRecording.setFulltext(msg.getInteger("fulltext"));
            }
        }
        if (msg.containsKey("directory")) {
            if (!TextUtils.isEmpty(msg.getString("directory"))) {
                seriesRecording.setDirectory(msg.getString("directory"));
            }
        }
        if (msg.containsKey("channel")) {
            if (msg.getInteger("channel") > 0) {
                seriesRecording.setChannelId(msg.getInteger("channel"));
            }
        }
        if (msg.containsKey("owner")) {
            if (!TextUtils.isEmpty(msg.getString("owner"))) {
                seriesRecording.setOwner(msg.getString("owner"));
            }
        }
        if (msg.containsKey("creator")) {
            if (!TextUtils.isEmpty(msg.getString("creator"))) {
                seriesRecording.setCreator(msg.getString("creator"));
            }
        }
        if (msg.containsKey("dupDetect")) {
            if (msg.getInteger("dupDetect") > 0) {
                seriesRecording.setDupDetect(msg.getInteger("dupDetect"));
            }
        }
        if (msg.containsKey("maxCount")) {
            if (msg.getInteger("maxCount") > 0) {
                seriesRecording.setMaxCount(msg.getInteger("maxCount"));
            }
        }
        if (msg.containsKey("removal")) {
            if (msg.getInteger("removal") > 0) {
                seriesRecording.setRemoval(msg.getInteger("removal"));
            }
        }
        return seriesRecording;
    }

    static TimerRecording convertMessageToTimerRecordingModel(@NonNull TimerRecording timerRecording, @NonNull HtspMessage msg) {
        if (msg.containsKey("id")) {
            timerRecording.setId(msg.getString("id"));
        }
        if (msg.containsKey("title")) {
            timerRecording.setTitle(msg.getString("title"));
        }
        if (msg.containsKey("directory")) {
            if (!TextUtils.isEmpty(msg.getString("directory"))) {
                timerRecording.setDirectory(msg.getString("directory"));
            }
        }
        if (msg.containsKey("enabled")) {
            timerRecording.setEnabled(msg.getInteger("enabled") == 1);
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
            if (msg.getInteger("daysOfWeek") > 0) {
                timerRecording.setDaysOfWeek(msg.getInteger("daysOfWeek"));
            }
        }
        if (msg.containsKey("priority")) {
            if (msg.getInteger("priority") > 0) {
                timerRecording.setPriority(msg.getInteger("priority"));
            }
        }
        if (msg.containsKey("start")) {
            // The message value is in minutes
            timerRecording.setStart(msg.getLong("start"));
        }
        if (msg.containsKey("stop")) {
            // The message value is in minutes
            timerRecording.setStop(msg.getLong("stop"));
        }
        if (msg.containsKey("retention")) {
            if (msg.getInteger("retention") > 0) {
                timerRecording.setRetention(msg.getInteger("retention"));
            }
        }
        if (msg.containsKey("owner")) {
            if (!TextUtils.isEmpty(msg.getString("owner"))) {
                timerRecording.setOwner(msg.getString("owner"));
            }
        }
        if (msg.containsKey("creator")) {
            if (!TextUtils.isEmpty(msg.getString("creator"))) {
                timerRecording.setCreator(msg.getString("creator"));
            }
        }
        if (msg.containsKey("removal")) {
            if (msg.getInteger("removal") > 0) {
                timerRecording.setRemoval(msg.getInteger("removal"));
            }
        }
        return timerRecording;
    }

    public static ServerStatus convertMessageToServerStatusModel(@NonNull ServerStatus serverStatus, @NonNull HtspMessage msg) {
        if (msg.containsKey("htspversion")) {
            serverStatus.setHtspVersion(msg.getInteger("htspversion", 13));
        }
        if (msg.containsKey("servername")) {
            serverStatus.setServerName(msg.getString("servername"));
        }
        if (msg.containsKey("serverversion")) {
            serverStatus.setServerVersion(msg.getString("serverversion"));
        }
        if (msg.containsKey("webroot")) {
            String webroot = msg.getString("webroot");
            serverStatus.setWebroot(webroot == null ? "" : webroot);
        }
        if (msg.containsKey("servercapability")) {
            for (Object capabilitiy : msg.getArrayList("servercapability")) {
                Timber.d("Server supports " + capabilitiy);
            }
        }
        return serverStatus;
    }

    static HtspMessage convertIntentToAutorecMessage(@NonNull Intent intent, int htspVersion) {
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

    static HtspMessage convertIntentToDvrMessage(@NonNull Intent intent, int htspVersion) {
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

    static HtspMessage convertIntentToTimerecMessage(@NonNull Intent intent, int htspVersion) {
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

    static HtspMessage convertIntentToEventMessage(@NonNull Intent intent) {
        final int eventId = intent.getIntExtra("eventId", 0);
        final int channelId = intent.getIntExtra("channelId", 0);
        final int numFollowing = intent.getIntExtra("numFollowing", 0);
        final long maxTime = intent.getLongExtra("maxTime", 0);

        final HtspMessage request = new HtspMessage();
        request.put("method", "getEvents");
        if (eventId > 0) {
            request.put("eventId", eventId);
        }
        if (channelId > 0) {
            request.put("channelId", channelId);
        }
        if (numFollowing > 0) {
            request.put("numFollowing", numFollowing);
        }
        if (maxTime > 0) {
            request.put("maxTime", maxTime);
        }
        return request;
    }

    static HtspMessage convertIntentToEpgQueryMessage(@NonNull Intent intent) {
        final String query = intent.getStringExtra("query");
        final long channelId = intent.getIntExtra("channelId", 0);
        final long tagId = intent.getIntExtra("tagId", 0);
        final int contentType = intent.getIntExtra("contentType", 0);
        final int minDuration = intent.getIntExtra("minduration", 0);
        final int maxDuration = intent.getIntExtra("maxduration", 0);
        final String language = intent.getStringExtra("language");
        final boolean full = intent.getBooleanExtra("full", false);

        final HtspMessage request = new HtspMessage();
        request.put("method", "epgQuery");
        request.put("query", query);

        if (channelId > 0) {
            request.put("channelId", channelId);
        }
        if (tagId > 0) {
            request.put("tagId", tagId);
        }
        if (contentType > 0) {
            request.put("contentType", contentType);
        }
        if (minDuration > 0) {
            request.put("minDuration", minDuration);
        }
        if (maxDuration > 0) {
            request.put("maxDuration", maxDuration);
        }
        if (language != null) {
            request.put("language", language);
        }
        request.put("full", full);
        return request;
    }
}
