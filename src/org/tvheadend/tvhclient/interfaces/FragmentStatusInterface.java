package org.tvheadend.tvhclient.interfaces;

import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.model.TimerRecording;

public interface FragmentStatusInterface {

    /**
     * Informs the activity that the fragment requires more data. The activity
     * could then start the service to load more data. The fragment itself
     * listens to the service status and updates itself when new data has
     * arrived.
     * 
     * @param channel
     * @param tag
     */
    public void moreDataRequired(final Channel channel, final String tag);
    
    /**
     * Informs the activity that the fragment has no data. The activity
     * could then start the service to load more data. The fragment itself
     * listens to the service status and updates itself when new data has
     * arrived.
     *
     * @param channel
     * @param tag
     */
    public void noDataAvailable(final Channel channel, final String tag);
    
    /**
     * Informs the activity that user has selected a certain channel from the
     * list of channels. The item position within the list is also passed.
     * 
     * @param position
     * @param channel
     * @param tag
     */
    public void onListItemSelected(final int position, final Channel channel, final String tag);
    
    /**
     * Informs the activity that user has selected a certain recording from the
     * list of recordings. The item position within the list is also passed.
     * 
     * @param position
     * @param recording
     * @param tag
     */
    public void onListItemSelected(final int position, final Recording recording, final String tag);

    /**
     * Informs the activity that user has selected a certain series recording
     * from the list of series recordings. The item position within the list is
     * also passed.
     * 
     * @param position
     * @param seriesRecording
     * @param tag
     */
    public void onListItemSelected(final int position, final SeriesRecording seriesRecording, final String tag);

    /**
     * Informs the activity that user has selected a certain timer recording
     * from the list of timer recordings. The item position within the list is
     * also passed.
     * 
     * @param position
     * @param timerRecording
     * @param tag
     */
    public void onListItemSelected(final int position, final TimerRecording timerRecording, final String tag);

    /**
     * Informs the activity that user has selected a certain program from the
     * list of programs. The item position within the list is also passed.
     * 
     * @param position
     * @param program
     * @param tag
     */
    public void onListItemSelected(final int position, final Program program, final String tag);

    /**
     * Informs the activity that the list is showing all available data from the
     * list adapter.
     * 
     * @param tag
     */
    public void onListPopulated(final String tag);

    /**
     * Informs the activity that the user has changed the selected channel tag
     * 
     * @param tag
     */
    public void channelTagChanged(String tag);

    /**
     * Informs the activity that the data in the list view is not valid anymore.
     * The main activity can then force a refresh or something else.
     * 
     * @param tag
     */
    public void listDataInvalid(final String tag);
}
