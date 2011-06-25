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

typedef void(aout_callback_t)(aout_buffer_t *ab, void *args);

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
  void                          * so_handle;
  pthread_mutex_t                 mutex;
  int                             play_size;
  struct audio_queue              play_queue;
  int                             free_size;
  struct audio_queue              free_queue;
  aout_callback_t               * callback;
  void                          * callback_args;
} aout_sys_t;

int opensles_open(aout_sys_t *ao);
void opensles_close(aout_sys_t *ao);
void opensles_set_callback(aout_sys_t *ao, aout_callback_t *f, void *args);
void opensles_enqueue(aout_sys_t *ao, aout_buffer_t *ab);

#endif
