package org.tvheadend.tvhclient.interfaces;

public interface ProgramGuideInterface {

    /**
     * To keep two list views synchronized both lists need to be scrolled.
     * Whenever the user has scrolled one list in one fragment this method will
     * inform the other fragment to scroll its list to the same position and
     * vice versa.
     * 
     * @param index The first visible position of the list
     * @param pos The position in pisels from the top of the first item
     * @param tag Identifier string to differentiate the caller
     */
    public void onScrollingChanged(final int index, final int pos, final String tag);

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
     * Returns the index of the first list item that is visible. This can be
     * used to scroll the list view to that position after an orientation
     * change.
     * 
     * @return index of the first visible item in the list
     */
    public int getScrollingSelectionIndex();

    /**
     * Returns the position offset of the first list item that is visible from
     * the top. This can be used to scroll to a certain position after an
     * orientation change. This value is in pixels so scrolling to the middle of
     * a list can be done.
     * 
     * @return position offset in pixels of the first visible item from the top
     */
    public int getScrollingSelectionPosition();

    boolean isChannelLoadingListEmpty();
}
