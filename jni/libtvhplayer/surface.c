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

#include <stdlib.h>
#include <dlfcn.h>

#include "tvhplayer.h"
#include "surface.h"

#define SURFACE_DLSYM(dest, handle, name)		     \
  dest = (typeof(dest))dlsym(handle, name);		     \
  if(dest == NULL) {					     \
    ERROR("Failed to load symbol %s", name);		     \
    goto error;						     \
  }							     \

int surface_init(vout_sys_t *vo) {
  vo->so_handle = dlopen("libsurfaceflinger_client.so", RTLD_NOW);
  if(vo->so_handle == NULL) {
    vo->so_handle = dlopen("libui.so", RTLD_NOW);
  }

  if(vo->so_handle == NULL) {
    ERROR("Failed to load surface library");
    goto error;
  }
  
  SURFACE_DLSYM(vo->lock, vo->so_handle, "_ZN7android7Surface4lockEPNS0_11SurfaceInfoEb");
  SURFACE_DLSYM(vo->unlockAndPost, vo->so_handle, "_ZN7android7Surface13unlockAndPostEv");

  return 0;
  
 error:
  surface_destroy(vo);
  return -1;
}

void surface_open(vout_sys_t *vo, void* handle) {
  vo->surface = handle;
}

void surface_close(vout_sys_t *vo) {
  vo->surface = NULL;
}

void surface_destroy(vout_sys_t *vo) {
  surface_close(vo);

  if(vo->so_handle != NULL) {
    dlclose(vo->so_handle);
    vo->so_handle = NULL;
  }
}
