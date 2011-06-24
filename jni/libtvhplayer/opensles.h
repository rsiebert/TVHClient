#ifndef __OPENSLES_H__
#define __OPENSLES_H__

#include <sys/queue.h>
#include <pthread.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

TAILQ_HEAD(audio_queue, aout_buffer);

typedef struct aout_buffer {
  TAILQ_ENTRY(aout_buffer) entry;
  void *ptr;
  size_t len;
} aout_buffer_t;

typedef struct aout_sys {
  SLObjectItf                     engineObject;
  SLEngineItf                     engineEngine;
  SLObjectItf                     outputMixObject;
  SLAndroidSimpleBufferQueueItf   playerBufferQueue;
  SLObjectItf                     playerObject;
  SLPlayItf                       playerPlay;
  SLInterfaceID                 * SL_IID_ENGINE;
  SLInterfaceID                 * SL_IID_ANDROIDSIMPLEBUFFERQUEUE;
  SLInterfaceID                 * SL_IID_VOLUME;
  SLInterfaceID                 * SL_IID_PLAY;
  void                          * p_so_handle;
  pthread_mutex_t                 mutex;
  int                             play_size;
  struct audio_queue              play_queue;
  int                             free_size;
  struct audio_queue              free_queue;
} aout_sys_t;

int opensles_open(aout_sys_t *p_aout);
void opensles_close(aout_sys_t *p_aout);
void opensles_enqueue(aout_sys_t *p_aout, uint8_t *p_buf, size_t i_buf);

#endif
