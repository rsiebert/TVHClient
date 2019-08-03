package org.tvheadend.tvhclient.ui.features.epg

internal interface EpgScrollInterface {

    /**
     * To keep two list views synchronized both lists need to be scrolled.
     * Whenever the user has scrolled one list in one fragment this method will
     * inform the other fragment to scroll its list to the same position and
     * vice versa.
     *
     * @param position The first visible position of the list
     * @param offset The position in pixels from the top of the first item
     */
    fun onScroll(position: Int, offset: Int)

    /**
     * When the user has scrolled the visible program guide fragment, the other
     * available fragments in the view pager must be scrolled to the same
     * position so that their list elements are aligned when the user swipes the
     * view pager. To do this go through all program guide fragments that the
     * view pager contains and set the scrolling position.
     */
    fun onScrollStateChanged()
}
