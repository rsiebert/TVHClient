package org.tvheadend.tvhguide.model;

public class SeriesInfo {
	public int seasonNumber;
	public int seasonCount;

	public int episodeNumber;
	public int episodeCount;

	public int partNumber;
	public int partCount;

	public String onScreen;

	public String toString() {
		if (onScreen != null && onScreen.length() > 0)
			return onScreen;

		String s = "";

		if (seasonNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("season %02d", seasonNumber);
		}
		if (episodeNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("episode %02d", episodeNumber);
		}
		if (partNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("part %d", partNumber);
		}

		if(s.length() > 0) {
			s = s.substring(0,1).toUpperCase() + s.substring(1);
		}
		
		return s;
	}
}
