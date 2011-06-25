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
