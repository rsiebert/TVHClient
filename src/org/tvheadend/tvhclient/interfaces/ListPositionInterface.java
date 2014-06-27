package org.tvheadend.tvhclient.interfaces;

public interface ListPositionInterface {

    /**
     * Returns the position of the previously selected item in the list view.
     * 
     * @return
     */
    int getPreviousListPosition();

    /**
     * Save the currently selected item from the list view.
     * 
     * @param position
     */
    void saveCurrentListPosition(int position);
}
