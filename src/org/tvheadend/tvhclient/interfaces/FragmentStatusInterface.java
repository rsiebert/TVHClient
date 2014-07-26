package org.tvheadend.tvhclient.interfaces;

import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;


public interface FragmentStatusInterface {

    /**
     * To keep two list views synchronized both lists need to be scrolled.
     * Whenever the user has scrolled one list in one fragment this method will
     * inform the other fragment to scroll its list to the same position and
     * vice versa.
     * 
     * @param position The first visible position of the list
     * @param offset The position in pixels from the top of the first item
     * @param tag Identifier string to differentiate the caller
     */
    public void onScrollingChanged(final int position, final int offset, final String tag);

    /**
     * When the user has scrolled the visible program guide fragment, the other
     * available fragments in the view pager must be scrolled to the same
     * position so that their list elements are aligned when the user swipes the
     * view pager. To do this go through all program guide fragments that the
     * view pager contains and set the scrolling position.
     * 
     * @param tag Identifier string to differentiate the caller
     */
    public void onScrollStateIdle(final String tag);

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
}
