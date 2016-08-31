package org.tvheadend.tvhclient.model;

public class DrawerMenuItem {

    // Default variables
    public int id = -1;
    public int icon = 0;
    public String title = "";
    public int count = 0;
    public boolean isSection = true;
    public boolean isVisible = true;

    /**
     * Creates a section item with no text
     */
    public DrawerMenuItem() {
        // NOP
    }

    /**
     * Creates a menu item with the given title and id. The id is used to
     * identify which menu was selected
     *
     * @param id    Id of the menu item
     * @param title Title text
     */
    public DrawerMenuItem(int id, String title) {
        this.id = id;
        this.title = title;
        this.isSection = false;
    }

    /**
     * Creates a menu item with the given title and id. The id is used to
     * identify which menu was selected
     *
     * @param id    Id of the menu item
     * @param title Title text
     * @param icon  Icon resource
     */
    public DrawerMenuItem(int id, String title, int icon) {
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.isSection = false;
    }
}
