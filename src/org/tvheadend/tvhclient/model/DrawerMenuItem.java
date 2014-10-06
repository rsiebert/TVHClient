package org.tvheadend.tvhclient.model;


public class DrawerMenuItem {

    // Default variables
    public int id = -1;
    public int icon = 0;
    public String header = "";
    public String title = "";
    public int count = 0;
    public boolean isMenu = false;
    public boolean isEnabled = true;
    public boolean isVisible = true;

    /**
     * Creates an header item with no text
     */
    public DrawerMenuItem() {
        // NOP
    }

    /**
     * Creates an header item with the given text
     * 
     * @param header
     */
    public DrawerMenuItem(String header) {
        this.header = header;
    }

    /**
     * Creates a menu item with the given title and id. The id is used to
     * identify which menu was selected
     * 
     * @param id
     * @param title
     */
    public DrawerMenuItem(int id, String title) {
        this.id = id;
        this.title = title;
        this.isMenu = true;
    }

    /**
     * Creates a menu item with the given title and id. The id is used to
     * identify which menu was selected
     * 
     * @param id
     * @param title
     * @param icon
     */
    public DrawerMenuItem(int id, String title, int icon) {
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.isMenu = true;
    }
}
