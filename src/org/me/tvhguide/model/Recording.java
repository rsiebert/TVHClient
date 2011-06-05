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
package org.me.tvhguide.model;

/**
 *
 * @author john-tornblom
 */
public class Recording extends Programme {

    public String state;
    public String error;

    @Override
    public int compareTo(Programme that) {
        //Order by state
        if(that instanceof Recording) {
            int diff = state() - ((Recording)that).state();
            if(diff != 0) {
                return diff;
            } else if (state() != 1){
                return (int) (that.start.getTime() - this.start.getTime());
            }
        }
        return super.compareTo(that);
    }

    private int state() {
        if("recording".equals(state)) {
            return 0;
        } else if("scheduled".equals(state)) {
            return 1;
        }
        return 2;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Programme) {
            return ((Programme)o).id == id;
        }

        return false;
    }
}
