package org.tvheadend.tvhclient.interfaces;

public interface ProgramGuideScrollingInterface {

    /**
     * Scrolls the list to the given position. The scrolling is not per pixel
     * but only per row.
     * 
     * @param index The id (starting from 0) of the list item to be selected
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
