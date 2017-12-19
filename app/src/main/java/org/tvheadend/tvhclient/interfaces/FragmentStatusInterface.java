package org.tvheadend.tvhclient.interfaces;

import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;

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
     * Informs the activity that user has selected a certain program from the
     * list of programs. The item position within the list is also passed.
     * 
     * @param position Selected position in the list
     * @param program Selected program
     * @param tag Tag of the calling class
     */
    void onListItemSelected(final int position, final Program program, final String tag);

    /**
     * Informs the activity that the list is showing all available data from the
     * list adapter.
     * 
     * @param tag Tag of the calling class
     */
    void onListPopulated(final String tag);

    /**
     * Informs the activity that the user has changed the selected channel tag
     * 
     * @param tag Tag of the calling class
     */
    void channelTagChanged(String tag);
}
