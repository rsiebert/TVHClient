package org.tvheadend.tvhclient.ui.epg;

public interface ProgramGuideControlInterface {

    /**
     * Reload all data and update the UI.
     */
    void reloadData();

    /**
     * Sets the selected item and the exact positions y pixels from the top edge
     * of the ListView. This allows selecting the exact same position in the
     * list (e.g. half of a list item is only visible). Useful if the list views
     * of two fragments shall be synchronized.
     * 
     * @param position Position in the list
     * @param offset Offset in pixels from the top of edge
     */
    void setSelection(final int position, final int offset);
}
