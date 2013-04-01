/*
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
package org.tvheadend.tvhguide;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.SparseArray;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.ChannelTag;
import org.tvheadend.tvhguide.model.HttpTicket;
import org.tvheadend.tvhguide.model.Packet;
import org.tvheadend.tvhguide.model.Programme;
import org.tvheadend.tvhguide.model.Recording;
import org.tvheadend.tvhguide.model.Subscription;

/**
 *
 * @author john-tornblom
 */
public class TVHGuideApplication extends Application {

    public static final String ACTION_CHANNEL_ADD = "org.me.tvhguide.CHANNEL_ADD";
    public static final String ACTION_CHANNEL_DELETE = "org.me.tvhguide.CHANNEL_DELETE";
    public static final String ACTION_CHANNEL_UPDATE = "org.me.tvhguide.CHANNEL_UPDATE";
    public static final String ACTION_TAG_ADD = "org.me.tvhguide.TAG_ADD";
    public static final String ACTION_TAG_DELETE = "org.me.tvhguide.TAG_DELETE";
    public static final String ACTION_TAG_UPDATE = "org.me.tvhguide.TAG_UPDATE";
    public static final String ACTION_DVR_ADD = "org.me.tvhguide.DVR_ADD";
    public static final String ACTION_DVR_DELETE = "org.me.tvhguide.DVR_DELETE";
    public static final String ACTION_DVR_UPDATE = "org.me.tvhguide.DVR_UPDATE";
    public static final String ACTION_PROGRAMME_ADD = "org.me.tvhguide.PROGRAMME_ADD";
    public static final String ACTION_PROGRAMME_DELETE = "org.me.tvhguide.PROGRAMME_DELETE";
    public static final String ACTION_PROGRAMME_UPDATE = "org.me.tvhguide.PROGRAMME_UPDATE";
    public static final String ACTION_SUBSCRIPTION_ADD = "org.me.tvhguide.SUBSCRIPTION_ADD";
    public static final String ACTION_SUBSCRIPTION_DELETE = "org.me.tvhguide.SUBSCRIPTION_DELETE";
    public static final String ACTION_SUBSCRIPTION_UPDATE = "org.me.tvhguide.SUBSCRIPTION_UPDATE";
    public static final String ACTION_SIGNAL_STATUS = "org.me.tvhguide.SIGNAL_STATUS";
    public static final String ACTION_PLAYBACK_PACKET = "org.me.tvhguide.PLAYBACK_PACKET";
    public static final String ACTION_LOADING = "org.me.tvhguide.LOADING";
    public static final String ACTION_TICKET_ADD = "org.me.tvhguide.TICKET";
    public static final String ACTION_ERROR = "org.me.tvhguide.ERROR";
    private final List<HTSListener> listeners = new ArrayList<HTSListener>();
    private final List<ChannelTag> tags = Collections.synchronizedList(new ArrayList<ChannelTag>());
    private final List<Channel> channels = Collections.synchronizedList(new ArrayList<Channel>());
    private final List<Recording> recordings = Collections.synchronizedList(new ArrayList<Recording>());
    private final List<Subscription> subscriptions = Collections.synchronizedList(new ArrayList<Subscription>());
    private volatile boolean loading = false;
    private Handler handler = new Handler();

    public void addListener(HTSListener l) {
        listeners.add(l);
    }

    public void removeListener(HTSListener l) {
        listeners.remove(l);
    }

    private void broadcastMessage(String action, Object obj) {
        synchronized (listeners) {
            for (HTSListener l : listeners) {
                l.onMessage(action, obj);
            }
        }
    }

    public void broadcastError(final String error) {
        //Don't show error if no views are open
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
        }
        handler.post(new Runnable() {

            public void run() {

                try {
                    Toast toast = Toast.makeText(TVHGuideApplication.this, error, Toast.LENGTH_LONG);
                    toast.show();
                } catch (Throwable ex) {
                }
            }
        });
        broadcastMessage(ACTION_ERROR, error);
    }

    public void broadcastPacket(Packet p) {
        broadcastMessage(ACTION_PLAYBACK_PACKET, p);
    }

    public List<ChannelTag> getChannelTags() {
        return tags;
    }

    public void addChannelTag(ChannelTag tag) {
        tags.add(tag);

        if (!loading) {
            broadcastMessage(ACTION_TAG_ADD, tag);
        }
    }

    public void removeChannelTag(ChannelTag tag) {
        tags.remove(tag);

        if (!loading) {
            broadcastMessage(ACTION_TAG_DELETE, tag);
        }
    }

    public void removeChannelTag(long id) {
        for (ChannelTag tag : getChannelTags()) {
            if (tag.id == id) {
                removeChannelTag(tag);
                return;
            }
        }
    }

    public ChannelTag getChannelTag(long id) {
        for (ChannelTag tag : getChannelTags()) {
            if (tag.id == id) {
                return tag;
            }
        }
        return null;
    }

    public void updateChannelTag(ChannelTag tag) {
        if (!loading) {
            broadcastMessage(ACTION_TAG_UPDATE, tag);
        }
    }

    public void addChannel(Channel channel) {
        channels.add(channel);

        if (!loading) {
            broadcastMessage(ACTION_CHANNEL_ADD, channel);
        }
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public void removeChannel(Channel channel) {
        channels.remove(channel);

        if (!loading) {
            broadcastMessage(ACTION_CHANNEL_DELETE, channel);
        }
    }

    public Channel getChannel(long id) {
        for (Channel ch : getChannels()) {
            if (ch.id == id) {
                return ch;
            }
        }
        return null;
    }

    public void removeChannel(long id) {
        for (Channel ch : getChannels()) {
            if (ch.id == id) {
                removeChannel(ch);
                return;
            }
        }
    }

    public void updateChannel(Channel ch) {
        if (!loading) {
            broadcastMessage(ACTION_CHANNEL_UPDATE, ch);
        }
    }

    public void addProgramme(Programme p) {
        if (!loading) {
            broadcastMessage(ACTION_PROGRAMME_ADD, p);
        }
    }

    public void removeProgramme(Programme p) {
        if (!loading) {
            broadcastMessage(ACTION_PROGRAMME_DELETE, p);
        }
    }

    public void updateProgramme(Programme p) {
        if (!loading) {
            broadcastMessage(ACTION_PROGRAMME_UPDATE, p);
        }
    }

    public void addRecording(Recording rec) {
        recordings.add(rec);

        if (!loading) {
            broadcastMessage(ACTION_DVR_ADD, rec);
        }
    }

    public List<Recording> getRecordings() {
        return recordings;
    }

    public void removeRecording(Recording rec) {
        recordings.remove(rec);

        if (!loading) {
            broadcastMessage(ACTION_DVR_DELETE, rec);
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
            broadcastMessage(ACTION_DVR_UPDATE, rec);
        }
    }

    public void setLoading(boolean b) {
        if (loading != b) {
            broadcastMessage(ACTION_LOADING, b);
        }
        loading = b;
    }

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
        tag.name = getString(R.string.pr_all_channels);
        tags.add(tag);
    }

    public void addSubscription(Subscription s) {
        subscriptions.add(s);

        if (!loading) {
            broadcastMessage(ACTION_SUBSCRIPTION_ADD, s);
        }
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void removeSubscription(Subscription s) {
        s.streams.clear();
        subscriptions.remove(s);

        if (!loading) {
            broadcastMessage(ACTION_SUBSCRIPTION_DELETE, s);
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
            broadcastMessage(ACTION_SUBSCRIPTION_UPDATE, s);
        }
    }
    
   
    public void addTicket(HttpTicket t) {
        broadcastMessage(ACTION_TICKET_ADD, t);
    }
    
    public boolean isLoading() {
        return loading;
    }
    

	public static SparseArray<String> getContentTypes(Context ctx) {
		SparseArray<String> ret = new SparseArray<String>();
		
		String[] s = ctx.getResources().getStringArray(R.array.pr_content_type0);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x00 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type1);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x10 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type2);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x20 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type3);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x30 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type4);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x40 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type5);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x50 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type6);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x60 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type7);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x70 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type8);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x80 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type9);
        for(int i=0; i<s.length; i++) {
        	ret.append(0x90 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type10);
        for(int i=0; i<s.length; i++) {
        	ret.append(0xa0 + i, s[i]);
        }
        
        s = ctx.getResources().getStringArray(R.array.pr_content_type11);
        for(int i=0; i<s.length; i++) {
        	ret.append(0xb0 + i, s[i]);
        }
        
		return ret;
	}
}
