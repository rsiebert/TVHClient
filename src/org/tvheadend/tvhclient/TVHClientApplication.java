/*
 *  Copyright (C) 2013 Robert Siebert
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.HttpTicket;
import org.tvheadend.tvhclient.model.Packet;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.Subscription;

import android.app.Application;
import android.content.Context;
import android.util.SparseArray;

public class TVHClientApplication extends Application {

    private final List<HTSListener> listeners = new ArrayList<HTSListener>();
    private final List<ChannelTag> tags = Collections.synchronizedList(new ArrayList<ChannelTag>());
    private final List<Channel> channels = Collections.synchronizedList(new ArrayList<Channel>());
    private final List<Recording> recordings = Collections.synchronizedList(new ArrayList<Recording>());
    private final List<Subscription> subscriptions = Collections.synchronizedList(new ArrayList<Subscription>());
    private final Map<String, String> status = Collections.synchronizedMap(new HashMap<String, String>());

    private volatile boolean loading = false;

    // Holds a list of channels that are not allowed to load because the EPG
    // size did not change after the last loading call.
    private List<Channel> channelBlockingList = new ArrayList<Channel>();

    public void blockChannel(Channel channel) {
        channelBlockingList.add(channel);
    }
    
    public void unblockChannel(Channel channel) {
        channelBlockingList.remove(channel);
    }

    public void unblockAllChannels() {
        channelBlockingList.clear();
    }

    public Boolean isChannelBlocked(Channel channel) {
        return channelBlockingList.contains(channel);
    }

    /**
     * Adds a single listener to the list.
     * 
     * @param listener
     */
    public void addListener(HTSListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a single listener from the list.
     * 
     * @param listener
     */
    public void removeListener(HTSListener listener) {
        listeners.remove(listener);
    }

    /**
     * Removes all registered listeners from the list. This can be used prior
     * stopping the service before the application is closed.
     */
    public void removeListeners() {
        listeners.clear();
    }

    /**
     * Sends the given action and possible object with the data to all
     * registered listeners.
     * 
     * @param action
     * @param obj
     */
    private void broadcastMessage(final String action, final Object obj) {
        synchronized (listeners) {
            for (HTSListener l : listeners) {
                l.onMessage(action, obj);
            }
        }
    }

    /**
     * Informs all listeners about the current connection state.
     */
    public void setConnectionState(final String state) {
        broadcastMessage(state, null);
    }

    /**
     * Sends the given packet object for video playback to all registered
     * listeners
     * 
     * @param p
     */
    public void broadcastPacket(Packet p) {
        broadcastMessage(Constants.ACTION_PLAYBACK_PACKET, p);
    }

    /**
     * Returns the list of available channel tags
     * 
     * @return
     */
    public List<ChannelTag> getChannelTags() {
        return tags;
    }

    /**
     * Adds the new channel tag to the list. If loading is not in progress all
     * registered listeners will be informed.
     * 
     * @param tag
     */
    public void addChannelTag(ChannelTag tag) {
        synchronized (tags) {
            tags.add(tag);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_TAG_ADD, tag);
        }
    }

    /**
     * Removes the given channel tag from the list. If loading is not in progress
     * all registered listeners will be informed.
     * 
     * @param tag
     */
    public void removeChannelTag(ChannelTag tag) {
        synchronized (tags) {
            tags.remove(tag);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_TAG_DELETE, tag);
        }
    }
    
    /**
     * Removes the channel tag (given by the id) from the list. If loading is
     * not in progress all registered listeners will be informed.
     * 
     * @param tag
     */
    public void removeChannelTag(long id) {
        synchronized (tags) {
            for (ChannelTag tag : getChannelTags()) {
                if (tag.id == id) {
                    removeChannelTag(tag);
                    return;
                }
            }
        }
    }

    public ChannelTag getChannelTag(long id) {
        synchronized (tags) {
            for (ChannelTag tag : getChannelTags()) {
                if (tag.id == id) {
                    return tag;
                }
            }
        }
        return null;
    }

    public void updateChannelTag(ChannelTag tag) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_TAG_UPDATE, tag);
        }
    }

    public void addChannel(Channel channel) {
        synchronized (channels) {
            channels.add(channel);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_CHANNEL_ADD, channel);
        }
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public void removeChannel(Channel channel) {
        synchronized (channels) {
            channels.remove(channel);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_CHANNEL_DELETE, channel);
        }
    }

    public Channel getChannel(long id) {
        synchronized (channels) {
            for (Channel ch : getChannels()) {
                if (ch.id == id) {
                    return ch;
                }
            }
        }
        return null;
    }

    public void removeChannel(long id) {
        synchronized (channels) {
            for (Channel ch : getChannels()) {
                if (ch.id == id) {
                    removeChannel(ch);
                    return;
                }
            }
        }
    }

    public void updateChannel(Channel ch) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_CHANNEL_UPDATE, ch);
        }
    }

    public void addProgram(Program p) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_PROGRAM_ADD, p);
        }
    }

    public void removeProgram(Program p) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_PROGRAM_DELETE, p);
        }
    }

    public void updateProgram(Program p) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_PROGRAM_UPDATE, p);
        }
    }

    public void addRecording(Recording rec) {
        synchronized (recordings) {
            recordings.add(rec);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_DVR_ADD, rec);
        }
    }

    public List<Recording> getRecordings() {
        return recordings;
    }

    /**
     * 
     * @param type
     * @return
     */
    public List<Recording> getRecordings(int type) {
        List<Recording> recs = new ArrayList<Recording>();

        switch (type) {
        case Constants.RECORDING_TYPE_COMPLETED:
            synchronized (recordings) {
                for (Recording rec : recordings) {
                    if (rec.error == null && rec.state.equals("completed")) {
                        recs.add(rec);
                    }
                }
            }
            break;

        case Constants.RECORDING_TYPE_SCHEDULED:
            synchronized (recordings) {
                for (Recording rec : recordings) {
                    if (rec.error == null
                            && !rec.state.equals("autorec")
                            && (rec.state.equals("scheduled") || rec.state.equals("recording"))) {
                        recs.add(rec);
                    }
                }
            }
            break;

        case Constants.RECORDING_TYPE_SERIES:
            synchronized (recordings) {
                for (Recording rec : recordings) {
                    if (rec.error == null
                            && rec.state.equals("autorec")
                            && (rec.state.equals("scheduled") || rec.state.equals("recording"))) {
                        recs.add(rec);
                    }
                }
            }
            break;

        case Constants.RECORDING_TYPE_FAILED:
            synchronized (recordings) {
                for (Recording rec : recordings) {
                    if ((rec.error != null || (rec.state.equals("missed") || rec.state.equals("invalid")))) {
                        recs.add(rec);
                    }
                }
            }
            break;
        }
        return recs;
    }

    public void removeRecording(Recording rec) {
        synchronized (recordings) {
            recordings.remove(rec);
        }
        if (!loading) {
            broadcastMessage(Constants.ACTION_DVR_DELETE, rec);
        }
    }

    public Recording getRecording(long id) {
        for (Recording rec : getRecordings()) {
            if (rec.id == id) {
                return rec;
            }
        }
        return null;
    }

    public void removeRecording(long id) {
        for (Recording rec : getRecordings()) {
            if (rec.id == id) {
                removeRecording(rec);
                return;
            }
        }
    }

    public void updateRecording(Recording rec) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_DVR_UPDATE, rec);
        }
    }

    public void setLoading(boolean b) {
        if (loading != b) {
            broadcastMessage(Constants.ACTION_LOADING, b);
        }
        loading = b;
    }

    /**
     * Clears all channels, tags and recordings and sets the tag default value.
     */
    public void clearAll() {
        tags.clear();
        recordings.clear();

        for (Channel ch : channels) {
            ch.epg.clear();
            ch.recordings.clear();
        }
        channels.clear();

        for (Subscription s : subscriptions) {
            s.streams.clear();
        }
        subscriptions.clear();

        ChannelTag tag = new ChannelTag();
        tag.id = 0;
        tag.name = getString(R.string.all_channels);
        tags.add(tag);
    }

    public void addSubscription(Subscription s) {
        subscriptions.add(s);
        if (!loading) {
            broadcastMessage(Constants.ACTION_SUBSCRIPTION_ADD, s);
        }
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void removeSubscription(Subscription s) {
        s.streams.clear();
        subscriptions.remove(s);
        if (!loading) {
            broadcastMessage(Constants.ACTION_SUBSCRIPTION_DELETE, s);
        }
    }

    public Subscription getSubscription(long id) {
        for (Subscription s : getSubscriptions()) {
            if (s.id == id) {
                return s;
            }
        }
        return null;
    }

    public void removeSubscription(long id) {
        for (Subscription s : getSubscriptions()) {
            if (s.id == id) {
                removeSubscription(s);
                return;
            }
        }
    }

    public void updateSubscription(Subscription s) {
        if (!loading) {
            broadcastMessage(Constants.ACTION_SUBSCRIPTION_UPDATE, s);
        }
    }

    public void addTicket(HttpTicket t) {
        broadcastMessage(Constants.ACTION_TICKET_ADD, t);
    }

    public boolean isLoading() {
        return loading;
    }

    public void updateDiscSpace(Map<String, String> list) {
        status.putAll(list);
        if (!loading) {
            broadcastMessage(Constants.ACTION_DISC_SPACE, status);
        }
    }

    public static SparseArray<String> getContentTypes(Context ctx) {
        SparseArray<String> ret = new SparseArray<String>();

        String[] s = ctx.getResources().getStringArray(R.array.pr_content_type0);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x00 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type1);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x10 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type2);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x20 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type3);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x30 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type4);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x40 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type5);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x50 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type6);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x60 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type7);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x70 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type8);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x80 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type9);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x90 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type10);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xa0 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type11);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xb0 + i, s[i]);
        }

        return ret;
    }
}
