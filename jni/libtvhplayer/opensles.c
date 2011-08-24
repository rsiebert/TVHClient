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
#include "opensles.h"

typedef SLresult (*slCreateEngine_t)(SLObjectItf*, 
				     SLuint32, 
				     const SLEngineOption*, 
				     SLuint32,  
				     const SLInterfaceID*, 
				     const SLboolean*);

static void opensles_callback(SLAndroidSimpleBufferQueueItf caller,  void *pContext);
static int opensles_play(aout_sys_t *ao);

#define BUFF_QUEUE  16

#define CHECK_OPENSL_ERROR(res, msg)		    \
  if(res != SL_RESULT_SUCCESS) {		    \
    ERROR(msg" (%lu)", res);			    \
    goto error;					    \
  }

#define OPENSL_DLSYM(dest, handle, name)		     \
  dest = (typeof(dest))dlsym(handle, name);		     \
  if(dest == NULL) {					     \
    ERROR("Failed to load symbol %s", name);		     \
    goto error;						     \
  }

void opensles_destroy(aout_sys_t *ao) {
  // Destroy buffer queue audio player object
  // and invalidate all associated interfaces
  if(ao->playerObject != NULL) {
    (*ao->playerObject)->Destroy(ao->playerObject);
    ao->playerObject = NULL;
  }
  
  // destroy output mix object, and invalidate all associated interfaces
  if(ao->outputMixObject != NULL) {
    (*ao->outputMixObject)->Destroy(ao->outputMixObject);
    ao->outputMixObject = NULL;
  }
  
  // destroy engine object, and invalidate all associated interfaces
  if(ao->engineObject != NULL) {
    (*ao->engineObject)->Destroy(ao->engineObject);
    ao->engineObject = NULL;
  }
  
  if(ao->so_handle != NULL) {
    dlclose(ao->so_handle);
    ao->so_handle = NULL;
  }

  pthread_mutex_destroy(&ao->mutex);
}

int opensles_init(aout_sys_t *ao) {
  SLresult result;

  DEBUG("Initializing OpenSL ES");

  ao->playerObject     = NULL;
  ao->engineObject     = NULL;
  ao->outputMixObject  = NULL;
  ao->play_size        = -BUFF_QUEUE;
  ao->is_open          = 0;

  pthread_mutex_init(&ao->mutex, NULL);
  TAILQ_INIT(&ao->play_queue);
  TAILQ_INIT(&ao->free_queue);
  
  // Acquiring LibOpenSLES symbols :
  ao->so_handle = dlopen("libOpenSLES.so", RTLD_NOW);
  if(ao->so_handle == NULL) {
    ERROR("Failed to load libOpenSLES");
    goto error;
  }
  
  slCreateEngine_t    slCreateEnginePtr = NULL;
  
  OPENSL_DLSYM(slCreateEnginePtr, ao->so_handle, "slCreateEngine");
  OPENSL_DLSYM(ao->SL_IID_ENGINE, ao->so_handle, "SL_IID_ENGINE");
  OPENSL_DLSYM(ao->SL_IID_PLAY, ao->so_handle, "SL_IID_PLAY");
  OPENSL_DLSYM(ao->SL_IID_VOLUME, ao->so_handle, "SL_IID_VOLUME");
  OPENSL_DLSYM(ao->SL_IID_ANDROIDSIMPLEBUFFERQUEUE, ao->so_handle, 
	       "SL_IID_ANDROIDSIMPLEBUFFERQUEUE");
  
  // create engine
  result = slCreateEnginePtr(&ao->engineObject, 0, NULL, 0, NULL, NULL);
  CHECK_OPENSL_ERROR(result, "Failed to create engine");
  
  // realize the engine in synchronous mode
  result = (*ao->engineObject)->Realize(ao->engineObject,
					SL_BOOLEAN_FALSE);
  CHECK_OPENSL_ERROR(result, "Failed to realize engine");
  
  // get the engine interface, needed to create other objects
  result = (*ao->engineObject)->GetInterface(ao->engineObject,
					     *ao->SL_IID_ENGINE, &ao->engineEngine);
  CHECK_OPENSL_ERROR(result, "Failed to get the engine interface");
  
  // create output mix, with environmental reverb specified as a non-required interface
  const SLInterfaceID ids1[] = {*ao->SL_IID_VOLUME};
  const SLboolean req1[] = {SL_BOOLEAN_FALSE};
  result = (*ao->engineEngine)->CreateOutputMix(ao->engineEngine,
						&ao->outputMixObject, 1, ids1, req1);
  CHECK_OPENSL_ERROR(result, "Failed to create output mix");
  
  // realize the output mix in synchronous mode
  result = (*ao->outputMixObject)->Realize(ao->outputMixObject,
					   SL_BOOLEAN_FALSE);
  CHECK_OPENSL_ERROR(result, "Failed to realize output mix");

  pthread_mutex_unlock(&ao->mutex);
  return 0;
  
 error:
  opensles_destroy(ao);
  return -1;
}

int opensles_open(aout_sys_t *ao, unsigned char channels, unsigned int sample_rate) {
  SLresult result;

  DEBUG("Opening OpenSL ES for playback, %d channel(s), %dHz", channels, sample_rate);
  // set the player's state to playing

  pthread_mutex_lock(&ao->mutex);
  
  if(ao->is_open) {
    ERROR("OpenSL ES interface allready opened");
    goto error;
  }
  
  // configure audio source - this defines the number of samples you can enqueue.
  SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
    SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
    BUFF_QUEUE
  };
  
  SLDataFormat_PCM format_pcm;
  format_pcm.formatType       = SL_DATAFORMAT_PCM;
  format_pcm.numChannels      = channels;
  format_pcm.samplesPerSec    = sample_rate;
  format_pcm.bitsPerSample    = SL_PCMSAMPLEFORMAT_FIXED_16;
  format_pcm.containerSize    = SL_PCMSAMPLEFORMAT_FIXED_16;
  format_pcm.channelMask      = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
  format_pcm.endianness       = SL_BYTEORDER_LITTLEENDIAN;
  
  SLDataSource audioSrc = {&loc_bufq, &format_pcm};
  
  // configure audio sink
  SLDataLocator_OutputMix loc_outmix = {
    SL_DATALOCATOR_OUTPUTMIX,
    ao->outputMixObject
  };
  SLDataSink audioSnk = {&loc_outmix, NULL};
  
  // create audio player
  const SLInterfaceID ids2[] = {*ao->SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
  const SLboolean     req2[] = {SL_BOOLEAN_TRUE};
  result = (*ao->engineEngine)->CreateAudioPlayer(ao->engineEngine,
						  &ao->playerObject, &audioSrc,
						  &audioSnk, 
						  sizeof(ids2)/sizeof(*ids2),
						  ids2, req2);
  CHECK_OPENSL_ERROR(result, "Failed to create audio player");
  
  // realize the player
  result = (*ao->playerObject)->Realize(ao->playerObject,
					SL_BOOLEAN_FALSE);
  CHECK_OPENSL_ERROR(result, "Failed to realize player object.");
  
  // get the play interface
  result = (*ao->playerObject)->GetInterface(ao->playerObject,
					     *ao->SL_IID_PLAY, &ao->playerPlay);
  CHECK_OPENSL_ERROR(result, "Failed to get player interface.");
  
  // get the buffer queue interface
  result = (*ao->playerObject)->GetInterface(ao->playerObject,
					     *ao->SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
					     &ao->playerBufferQueue);
  CHECK_OPENSL_ERROR(result, "Failed to get buff queue interface");
  
  result = (*ao->playerBufferQueue)->RegisterCallback(ao->playerBufferQueue,
						      opensles_callback,
						      (void*)ao);
  CHECK_OPENSL_ERROR(result, "Failed to register buff queue callback.");
  
  result = (*ao->playerPlay)->SetPlayState(ao->playerPlay,
					   SL_PLAYSTATE_PLAYING);
  CHECK_OPENSL_ERROR(result, "Failed to switch to playing state");

  ao->is_open = 1;

  pthread_mutex_unlock(&ao->mutex);
  return 0;
  
 error:
  pthread_mutex_unlock(&ao->mutex);
  return -1;
}

int opensles_is_open(aout_sys_t *ao) {
  int ret = 0;
  
  pthread_mutex_lock(&ao->mutex);
  ret = ao->is_open;
  pthread_mutex_unlock(&ao->mutex);
  
  return ret;
}

void opensles_close(aout_sys_t *ao) {
  DEBUG("Closing OpenSL ES");

  pthread_mutex_lock(&ao->mutex);
  
  (*ao->playerPlay)->SetPlayState(ao->playerPlay, SL_PLAYSTATE_STOPPED);
  // Flush remaining buffers if any.
  if(ao->playerBufferQueue != NULL) {
    (*ao->playerBufferQueue)->Clear(ao->playerBufferQueue);
  }
  
  // Clear queues.
  aout_buffer_t *ab;
  while(ab = TAILQ_FIRST(&ao->play_queue)) {
    TAILQ_REMOVE(&ao->play_queue, ab, entry);
    av_free(ab->ptr);
    free(ab);
  }
  
  while(ab = TAILQ_FIRST(&ao->free_queue)) {
    TAILQ_REMOVE(&ao->free_queue, ab, entry);
    av_free(ab->ptr);
    free(ab);
  }
  
  ao->play_size = 0;
  ao->free_size = 0;
  ao->is_open = 0;

  pthread_mutex_unlock(&ao->mutex);
}

int opensles_enqueue(aout_sys_t *ao, aout_buffer_t *ab) {
  int buf_count = 0;

  pthread_mutex_lock(&ao->mutex);
  
  TAILQ_INSERT_TAIL(&ao->play_queue, ab, entry);
  ao->play_size++;
  
  if(ao->play_size >= BUFF_QUEUE) {
    ao->play_size -= opensles_play(ao);
  }
  
  buf_count = ao->free_size;

  pthread_mutex_unlock(&ao->mutex);

  if(!buf_count) {
	  ERROR("Buffer underrun");
  }

  return buf_count;
}

void opensles_set_callback(aout_sys_t *ao, aout_callback_t *f, void *args) {
  pthread_mutex_lock(&ao->mutex);
  ao->callback = f;
  ao->callback_args = args;
  pthread_mutex_unlock(&ao->mutex);
}

static void opensles_callback(SLAndroidSimpleBufferQueueItf caller, void *pContext) {
  aout_sys_t *ao = (aout_sys_t*)pContext;
  
  pthread_mutex_lock(&ao->mutex);
  
  // old callback, just return since we free memory in opensles_clear()
  if(caller != ao->playerBufferQueue) {
    pthread_mutex_unlock(&ao->mutex);
    return;
  }
  
  aout_buffer_t *ab = TAILQ_FIRST(&ao->free_queue);
  TAILQ_REMOVE(&ao->free_queue, ab, entry);
  ao->free_size--;
  
  if(ao->free_size == 0) {
    ao->play_size -= opensles_play(ao);
  }
  
  ao->callback(ab, ao->callback_args);
  av_free(ab->ptr);
  free(ab);

  // TODO: check for starvation and take actions against them somehow (back-off size of buffer??)
  

  pthread_mutex_unlock(&ao->mutex);
}

static int opensles_play(aout_sys_t *ao) {
  SLresult result;
  aout_buffer_t *ab;
  int i = 0;
  
  while (ab = TAILQ_FIRST(&ao->play_queue)) {
    if(ao->free_size >= BUFF_QUEUE) {
      break;
    }

    TAILQ_REMOVE(&ao->play_queue, ab, entry);
    TAILQ_INSERT_TAIL(&ao->free_queue, ab, entry);

    result = (*ao->playerBufferQueue)->Enqueue(ao->playerBufferQueue, 
					       ab->ptr,
					       ab->len);
    i++;
    ao->free_size++;
    if(result == SL_RESULT_SUCCESS) {
      continue;
    }
    
    ERROR("write error (%lu)", result);
  }
  
  return i;
}
