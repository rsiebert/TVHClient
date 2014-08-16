package org.tvheadend.tvhclient.interfaces;

public interface FragmentControlInterface {

    /**
     * Reload all data and update the UI.
     */
    void reloadData();

    /**
     * Sets the initial selected item in the list view of the fragment. Useful
     * if the list view of a fragment has been loaded for the first time and
     * another one shall be shown then.
     * 
     * @param position
     */
    void setInitialSelection(final int position);

    /**
     * Sets the currently selected item in the list view of the fragment. Useful
     * if the list views of two fragments shall be synchronized.
     * 
     * @param position
     */
    void setSelection(final int position);

    /**
     * Sets the selected item and the exact positions y pixels from the top edge
     * of the ListView. This allows selecting the exact same position in the
     * list (e.g. half of a list item is only visible). Useful if the list views
     * of two fragments shall be synchronized.
     * 
     * @param position
     * @param index
     */
    void setSelectionFromTop(final int position, final int index);

    /**
     * Returns the currently selected item from the adapter
     * 
     * @return
     */
    Object getSelectedItem();

    /**
     * Returns the number of items in the adapter
     * 
     * @return
     */
    int getItemCount();
}
