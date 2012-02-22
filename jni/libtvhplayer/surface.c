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

#define SURFACE_DLSYM(dest, handle, name, err)		     \
  dest = (typeof(dest))dlsym(handle, name);		     \
  if(dest == NULL && err) {				     \
    ERROR("Failed to load symbol %s", name);		     \
    goto error;						     \
  }							     \

int surface_init(vout_sys_t *vo) {
  DEBUG("Initializing Surface library");

  vo->so_handle = dlopen("libgui.so", RTLD_NOW);
  if(vo->so_handle == NULL) {
    vo->so_handle = dlopen("libsurfaceflinger_client.so", RTLD_NOW);
  }
  if(vo->so_handle == NULL) {
    vo->so_handle = dlopen("libui.so", RTLD_NOW);
  }

  if(vo->so_handle == NULL) {
    ERROR("Failed to load surface library");
    goto error;
  }
  
  SURFACE_DLSYM(vo->lock, vo->so_handle, "_ZN7android7Surface4lockEPNS0_11SurfaceInfoEb", 0);
  SURFACE_DLSYM(vo->lockRegion, vo->so_handle, "_ZN7android7Surface4lockEPNS0_11SurfaceInfoEPNS_6RegionE", 0);
  SURFACE_DLSYM(vo->unlockAndPost, vo->so_handle, "_ZN7android7Surface13unlockAndPostEv", 1);

  pthread_mutex_init(&vo->mutex, NULL);
  pthread_cond_init(&vo->cond, NULL);
  TAILQ_INIT(&vo->render_queue);

  return 0;
  
 error:
  surface_destroy(vo);
  return -1;
}

void surface_open(vout_sys_t *vo, void* handle) {
  DEBUG("Opening surface for rendering");
  pthread_mutex_lock(&vo->mutex);

  vo->surface = handle;

  //Clear the buffer and get surface info
  if(vo->lock) {
    vo->lock(vo->surface, (void*)&vo->surface_info, 1);
  } else if(vo->lockRegion) {
    vo->lockRegion(vo->surface, (void*)&vo->surface_info, NULL);
  }

  memset(vo->surface_info.bits, 0, sizeof(uint32_t) * vo->surface_info.size);

  if(vo->unlockAndPost) {
    vo->unlockAndPost(vo->surface);
  }

  pthread_mutex_unlock(&vo->mutex);
}

//Not thread safe!
void surface_render(vout_sys_t *vo, vout_buffer_t *vb) {
  if(!vo->surface) {
    return;
  }

  if(vo->lock) {
    vo->lock(vo->surface, (void*)&vo->surface_info, 1);
  } else if(vo->lockRegion) {
    vo->lockRegion(vo->surface, (void*)&vo->surface_info, NULL);
  }

  if(vb->width != vo->surface_info.width || 
     vb->height != vo->surface_info.height) {
    memset(vo->surface_info.bits, 0, sizeof(uint32_t) * vo->surface_info.size);
  } else {
    memcpy(vo->surface_info.bits, vb->ptr, vb->len);
  }

  if(vo->unlockAndPost) {
    vo->unlockAndPost(vo->surface);
  }
}

void surface_close(vout_sys_t *vo) {
  DEBUG("Closing surface");
  pthread_mutex_lock(&vo->mutex);

  vo->surface = NULL;

  vout_buffer_t *vb;
  while(vb = TAILQ_FIRST(&vo->render_queue)) {
    TAILQ_REMOVE(&vo->render_queue, vb, entry);
    av_free(vb->ptr);
    free(vb);
  }
  pthread_mutex_unlock(&vo->mutex);
}

void surface_destroy(vout_sys_t *vo) {
  surface_close(vo);
  
  DEBUG("Destroying Surface");
  
  if(vo->so_handle != NULL) {
    dlclose(vo->so_handle);
    vo->so_handle = NULL;
    vo->lock = NULL;
    vo->lockRegion = NULL;
    vo->unlockAndPost = NULL;
  }

  pthread_mutex_destroy(&vo->mutex);
  pthread_cond_destroy(&vo->cond);
}
