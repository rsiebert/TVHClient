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

/**
 *
 * @author john-tornblom
 */
public class Stream {

    public static final String STREAM_TYPE_AC3 = "AC3";
    public static final String STREAM_TYPE_MPEG2AUDIO = "MPEG2AUDIO";
    public static final String STREAM_TYPE_MPEG2VIDEO = "MPEG2VIDEO";
    public static final String STREAM_TYPE_MPEG4VIDEO = "MPEG4VIDEO";
    public static final String STREAM_TYPE_H264 = "H264";
    public static final String STREAM_TYPE_VP8 = "VP8";
    public static final String STREAM_TYPE_AAC = "AAC";
    public int index;
    public String type;
    public String language;
    public int height;
    public int width;
}
