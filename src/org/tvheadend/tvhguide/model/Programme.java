/*
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhguide.model;

import java.util.Date;

/**
 *
 * @author john-tornblom
 */
public class Programme implements Comparable<Programme> {

    public long id;
    public long nextId;
    public int contentType;
    public Date start;
    public Date stop;
    public String title;
    public String description;
    public String summary;
    public SeriesInfo seriesInfo;
    public int starRating;
    public Channel channel;
    public Recording recording;

    public int compareTo(Programme that) {
        return this.start.compareTo(that.start);
    }

    public boolean isRecording() {
        return recording != null && "recording".equals(recording.state);
    }

    public boolean isScheduled() {
        return recording != null && "scheduled".equals(recording.state);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Programme) {
            return ((Programme) o).id == id;
        }

        return false;
    }
}
