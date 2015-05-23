package org.tvheadend.tvhclient.model;

import java.util.Comparator;
import java.util.Locale;

public class Profiles {
    public String uuid;
    public String name;
    public String comment;
    
    public static Comparator<Profiles> ProfilesNameSortor = new Comparator<Profiles>() {
	public int compare(Profiles x, Profiles y) {
	   return x.name.toLowerCase(Locale.US).compareTo(y.name.toLowerCase(Locale.US));
  }};
}
