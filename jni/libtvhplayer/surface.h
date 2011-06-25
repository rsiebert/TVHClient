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

#ifndef __SURFACE_H__
#define __SURFACE_H__

#include <stdint.h>

// _ZN7android7Surface4lockEPNS0_11SurfaceInfoEb
typedef void (*Surface_lock)(void *, void *, int);
// _ZN7android7Surface13unlockAndPostEv
typedef void (*Surface_unlockAndPost)(void *);


typedef struct surface_info {
    uint32_t    width;
    uint32_t    height;
    uint32_t    size;
    uint32_t    usage;
    uint32_t    format;
    uint32_t*   bits;
    uint32_t    reserved[2];
} surface_info_t;

typedef struct vout_sys {
  Surface_lock                    lock;
  Surface_unlockAndPost           unlockAndPost;
  void                          * so_handle;
  void                          * surface;
  surface_info_t                  surface_info;
} vout_sys_t;

int surface_init(vout_sys_t *vo);
void surface_open(vout_sys_t *vo, void* handle);
void surface_close(vout_sys_t *vo);
void surface_destroy(vout_sys_t *vo);

#endif
