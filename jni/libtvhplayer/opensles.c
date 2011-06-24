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
static int opensles_play(aout_sys_t *p_aout);

#define BUFF_QUEUE  128

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

static void opensles_clear(aout_sys_t *p_aout) {
  // Destroy buffer queue audio player object
  // and invalidate all associated interfaces
  if(p_aout->playerObject != NULL) {
    (*p_aout->playerObject)->Destroy(p_aout->playerObject);
    p_aout->playerObject = NULL;
  }
  
  // destroy output mix object, and invalidate all associated interfaces
  if(p_aout->outputMixObject != NULL) {
    (*p_aout->outputMixObject)->Destroy(p_aout->outputMixObject);
    p_aout->outputMixObject = NULL;
  }
  
  // destroy engine object, and invalidate all associated interfaces
  if(p_aout->engineObject != NULL) {
    (*p_aout->engineObject)->Destroy(p_aout->engineObject);
    p_aout->engineObject = NULL;
  }
  
  if(p_aout->p_so_handle != NULL) {
    dlclose(p_aout->p_so_handle);
    p_aout->p_so_handle = NULL;
  }

  // Clear queues.
  aout_buffer_t *ab;
  while (ab = TAILQ_FIRST(&p_aout->play_queue)) {
    TAILQ_REMOVE(&p_aout->play_queue, ab, entry);
    free(ab);
  }
  
  while (ab = TAILQ_FIRST(&p_aout->free_queue)) {
    TAILQ_REMOVE(&p_aout->free_queue, ab, entry);
    free(ab);
  }
  
  p_aout->play_size = 0;
  p_aout->free_size = 0;
}

int opensles_open(aout_sys_t *p_aout) {
  SLresult result;
  
  DEBUG("Opening OpenSLES");
  
  p_aout->playerObject     = NULL;
  p_aout->engineObject     = NULL;
  p_aout->outputMixObject  = NULL;
  p_aout->play_size        = -BUFF_QUEUE;
  
  TAILQ_INIT(&p_aout->play_queue);
  TAILQ_INIT(&p_aout->free_queue);
  
  // Acquiring LibOpenSLES symbols :
  p_aout->p_so_handle = dlopen("libOpenSLES.so", RTLD_NOW);
  if(p_aout->p_so_handle == NULL) {
    ERROR("Failed to load libOpenSLES");
    goto error;
  }

  slCreateEngine_t    slCreateEnginePtr = NULL;

  OPENSL_DLSYM(slCreateEnginePtr, p_aout->p_so_handle, "slCreateEngine");
  OPENSL_DLSYM(p_aout->SL_IID_ENGINE, p_aout->p_so_handle, "SL_IID_ENGINE");
  OPENSL_DLSYM(p_aout->SL_IID_PLAY, p_aout->p_so_handle, "SL_IID_PLAY");
  OPENSL_DLSYM(p_aout->SL_IID_VOLUME, p_aout->p_so_handle, "SL_IID_VOLUME");
  OPENSL_DLSYM(p_aout->SL_IID_ANDROIDSIMPLEBUFFERQUEUE, p_aout->p_so_handle, 
	       "SL_IID_ANDROIDSIMPLEBUFFERQUEUE");

  // create engine
  result = slCreateEnginePtr(&p_aout->engineObject, 0, NULL, 0, NULL, NULL);
  CHECK_OPENSL_ERROR(result, "Failed to create engine");

  // realize the engine in synchronous mode
  result = (*p_aout->engineObject)->Realize(p_aout->engineObject,
					    SL_BOOLEAN_FALSE);
  CHECK_OPENSL_ERROR(result, "Failed to realize engine");
  
  // get the engine interface, needed to create other objects
  result = (*p_aout->engineObject)->GetInterface(p_aout->engineObject,
						*p_aout->SL_IID_ENGINE, &p_aout->engineEngine);
  CHECK_OPENSL_ERROR(result, "Failed to get the engine interface");

  // create output mix, with environmental reverb specified as a non-required interface
  const SLInterfaceID ids1[] = {*p_aout->SL_IID_VOLUME};
  const SLboolean req1[] = {SL_BOOLEAN_FALSE};
  result = (*p_aout->engineEngine)->CreateOutputMix(p_aout->engineEngine,
						   &p_aout->outputMixObject, 1, ids1, req1);
  CHECK_OPENSL_ERROR(result, "Failed to create output mix");

  // realize the output mix in synchronous mode
  result = (*p_aout->outputMixObject)->Realize(p_aout->outputMixObject,
					      SL_BOOLEAN_FALSE);
  CHECK_OPENSL_ERROR(result, "Failed to realize output mix");
  
  // configure audio source - this defines the number of samples you can enqueue.
  SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
    SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
    BUFF_QUEUE
  };

  SLDataFormat_PCM format_pcm;
  format_pcm.formatType       = SL_DATAFORMAT_PCM;
  format_pcm.numChannels      = 2;
  format_pcm.samplesPerSec    = SL_SAMPLINGRATE_48;
  format_pcm.bitsPerSample    = SL_PCMSAMPLEFORMAT_FIXED_16;
  format_pcm.containerSize    = SL_PCMSAMPLEFORMAT_FIXED_16;
  format_pcm.channelMask      = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
  format_pcm.endianness       = SL_BYTEORDER_LITTLEENDIAN;

  SLDataSource audioSrc = {&loc_bufq, &format_pcm};

  // configure audio sink
  SLDataLocator_OutputMix loc_outmix = {
    SL_DATALOCATOR_OUTPUTMIX,
    p_aout->outputMixObject
  };
  SLDataSink audioSnk = {&loc_outmix, NULL};

  // create audio player
  const SLInterfaceID ids2[] = {*p_aout->SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
  const SLboolean     req2[] = {SL_BOOLEAN_TRUE};
  result = (*p_aout->engineEngine)->CreateAudioPlayer(p_aout->engineEngine,
						     &p_aout->playerObject, &audioSrc,
						     &audioSnk, 
						     sizeof(ids2)/sizeof(*ids2),
						     ids2, req2);
  CHECK_OPENSL_ERROR(result, "Failed to create audio player");

  // realize the player
  result = (*p_aout->playerObject)->Realize(p_aout->playerObject,
					   SL_BOOLEAN_FALSE);
  CHECK_OPENSL_ERROR(result, "Failed to realize player object.");

  // get the play interface
  result = (*p_aout->playerObject)->GetInterface(p_aout->playerObject,
						*p_aout->SL_IID_PLAY, &p_aout->playerPlay);
  CHECK_OPENSL_ERROR(result, "Failed to get player interface.");

  // get the buffer queue interface
  result = (*p_aout->playerObject)->GetInterface(p_aout->playerObject,
						*p_aout->SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
						&p_aout->playerBufferQueue);
  CHECK_OPENSL_ERROR(result, "Failed to get buff queue interface");

  result = (*p_aout->playerBufferQueue)->RegisterCallback(p_aout->playerBufferQueue,
							 opensles_callback,
							 (void*)p_aout);
  CHECK_OPENSL_ERROR(result, "Failed to register buff queue callback.");

  // set the player's state to playing
  result = (*p_aout->playerPlay)->SetPlayState(p_aout->playerPlay,
					      SL_PLAYSTATE_PLAYING);
  CHECK_OPENSL_ERROR(result, "Failed to switch to playing state");

  return 0;
  
 error:
  opensles_clear(p_aout);
  return -1;
}

void opensles_close(aout_sys_t *p_aout) {
  DEBUG("Closing OpenSLES");

  pthread_mutex_lock(&p_aout->mutex);

  (*p_aout->playerPlay)->SetPlayState(p_aout->playerPlay, SL_PLAYSTATE_STOPPED);
  // Flush remaining buffers if any.
  if(p_aout->playerBufferQueue != NULL) {
    (*p_aout->playerBufferQueue)->Clear(p_aout->playerBufferQueue);
    p_aout->playerBufferQueue = NULL;
  }

  pthread_mutex_unlock(&p_aout->mutex);

  opensles_clear(p_aout);
}

void opensles_enqueue(aout_sys_t *p_aout, uint8_t *p_buf, size_t i_buf) {
  aout_buffer_t *ab = (aout_buffer_t *) malloc(sizeof(aout_buffer_t));
  ab->ptr = malloc(i_buf);
  ab->len = i_buf;
  memcpy(ab->ptr, p_buf, i_buf);
  
  pthread_mutex_lock(&p_aout->mutex);

  TAILQ_INSERT_TAIL(&p_aout->play_queue, ab, entry);
  p_aout->play_size++;

  if(p_aout->play_size == BUFF_QUEUE) {
    p_aout->play_size -= opensles_play(p_aout);
  }

  pthread_mutex_unlock(&p_aout->mutex);
}


static void opensles_callback(SLAndroidSimpleBufferQueueItf caller, void *pContext) {
  aout_sys_t *p_aout = (aout_sys_t*)pContext;

  pthread_mutex_lock(&p_aout->mutex);

  // old callback, just return since we free memory in opensles_clear()
  if(caller != p_aout->playerBufferQueue) {
    return;
  }

  aout_buffer_t *ab = TAILQ_FIRST(&p_aout->free_queue);
  free(ab->ptr);
  TAILQ_REMOVE(&p_aout->free_queue, ab, entry);
  p_aout->free_size--;

  if(p_aout->free_size == 0) {
    p_aout->play_size -= opensles_play(p_aout);
  }

  // TODO: check for starvation and take actions against them somehow (back-off size of buffer??)

  pthread_mutex_unlock(&p_aout->mutex);
}


static int opensles_play(aout_sys_t *p_aout) {
  SLresult result;
  aout_buffer_t *ab;

  int i = 0;


  while (ab = TAILQ_FIRST(&p_aout->play_queue)) {
    if(p_aout->free_size >= BUFF_QUEUE) {
      break;
    }

    TAILQ_REMOVE(&p_aout->play_queue, ab, entry);
    TAILQ_INSERT_TAIL(&p_aout->free_queue, ab, entry);
    result = (*p_aout->playerBufferQueue)->Enqueue(p_aout->playerBufferQueue, 
						   ab->ptr,
						   ab->len);
    i++;
    p_aout->free_size++;
    if(result == SL_RESULT_SUCCESS) {
      continue;
    }
    
    ERROR("write error (%lu)", result);
  }

  DEBUG("Flushing %d frames", i);

  return i;
}
