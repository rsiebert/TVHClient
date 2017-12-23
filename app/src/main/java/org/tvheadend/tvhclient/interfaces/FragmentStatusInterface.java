package org.tvheadend.tvhclient.interfaces;

import org.tvheadend.tvhclient.model.Channel;

public interface FragmentStatusInterface {

    /**
     * Informs the activity that the fragment requires more data. The activity
     * could then start the service to load more data. The fragment itself
     * listens to the service status and updates itself when new data has
     * arrived.
     * 
     * @param channel Channel
     * @param tag Tag of the calling class
     */
    void moreDataRequired(final Channel channel, final String tag);

    /**
     * Informs the activity that the list is showing all available data from the
     * list adapter.
     * 
     * @param tag Tag of the calling class
     */
    void onListPopulated(final String tag);
}
