package org.tvheadend.tvhclient.interfaces;

import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.model.TimerRecording2;

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
     * Informs the activity that user has selected a certain channel from the
     * list of channels. The item position within the list from 
     * which the programs shall be shown are also passed.
     * 
     * @param position Selected position in the list
     * @param channel Selected channel
     * @param tag Tag of the calling class
     */
    void onListItemSelected(final int position, final Channel channel, final String tag);
    
    /**
     * Informs the activity that user has selected a certain recording from the
     * list of recordings. The item position within the list is also passed.
     * 
     * @param position Selected position in the list
     * @param recording Selected recording
     * @param tag Tag of the calling class
     */
    void onListItemSelected(final int position, final Recording recording, final String tag);

    /**
     * Informs the activity that user has selected a certain series recording
     * from the list of series recordings. The item position within the list is
     * also passed.
     * 
     * @param position Selected position in the list
     * @param seriesRecording Selected series recording
     * @param tag Tag of the calling class
     */
    void onListItemSelected(final int position, final SeriesRecording seriesRecording, final String tag);

    /**
     * Informs the activity that user has selected a certain timer recording
     * from the list of timer recordings. The item position within the list is
     * also passed.
     * 
     * @param position Selected position in the list
     * @param timerRecording Selected timer recording
     * @param tag Tag of the calling class
     */
    void onListItemSelected(final int position, final TimerRecording2 timerRecording, final String tag);

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

    /**
     * Informs the activity that the data in the list view is not valid anymore.
     * The main activity can then force a refresh or something else.
     * 
     * @param tag Tag of the calling class
     */
    void listDataInvalid(final String tag);

    /**
     * Informs the activity that a selection was made from the channel time dialog.
     * This sets the start time for the programs that shall be shown in the channel 
     * list and the associated program list.
     * 
     * @param selection Selected position in the list
     * @param time Time in millis
     */
    void onChannelTimeSelected(int selection, long time);
}
