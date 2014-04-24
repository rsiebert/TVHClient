package org.tvheadend.tvhclient.interfaces;

public interface ProgramGuideScrollingInterface {

    /**
     * Scrolls the list to the given position. The scrolling is not per pixel
     * but only per row. This is used when the program guide screen is visible.
     * The channel list is scrolled parallel with the program guide view.
     * 
     * @param index The index (starting at 0) of the list item to be selected
     */
    public void scrollListViewTo(int index);

    /**
     * Scrolls the list to the given pixel position. The scrolling is accurate
     * because the pixel value is used.
     * 
     * @param index The index (starting at 0) of the list item to be selected
     * @param pos The distance from the top edge of the list where the item will
     *            be positioned.
     */
    public void scrollListViewToPosition(int index, int pos);
}
