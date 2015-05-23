package org.tvheadend.tvhclient.model;

import java.util.Comparator;
import java.util.Locale;

public class Profiles {
    public String uuid;
    public String name;
    public String comment;

    public static Comparator<Profiles> ProfilesNameSorter = new Comparator<Profiles>() {
        public int compare(Profiles x, Profiles y) {
            if (x != null && y != null && x.name != null && y.name != null) {
                return x.name.toLowerCase(Locale.getDefault()).compareTo(
                        y.name.toLowerCase(Locale.getDefault()));
            }
            return 0;
        }
    };
}
