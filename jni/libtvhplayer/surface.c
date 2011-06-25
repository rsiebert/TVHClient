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
