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

#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <sys/time.h>
#include <time.h>
#include "tvhplayer.h"

static void tvh_video_close(tvh_object_t *tvh);
static void tvh_audio_close(tvh_object_t *tvh);
static void tvh_audio_callback(aout_buffer_t *ab, void *args);
static void tvh_sync_thread(void *args);

int tvh_init(tvh_object_t *tvh) {
  DEBUG("initilizing TVHPlayer");

  avcodec_init();
  avcodec_register_all();

  pthread_mutex_init(&tvh->mutex, NULL);

  tvh->ao = malloc(sizeof(aout_sys_t));
  memset(tvh->ao, 0, sizeof(aout_sys_t));

  tvh->acs = malloc(sizeof(acodec_sys_t));
  memset(tvh->acs, 0, sizeof(acodec_sys_t));

  tvh->vo = malloc(sizeof(vout_sys_t));
  memset(tvh->vo, 0, sizeof(vout_sys_t));

  tvh->vcs = malloc(sizeof(vcodec_sys_t));
  memset(tvh->vcs, 0, sizeof(vcodec_sys_t));

  if(opensles_init(tvh->ao) < 0) {
    ERROR("Unable to initilize OpenSL ES");
    return -1;
  }

  opensles_set_callback(tvh->ao, &tvh_audio_callback, tvh);

  if(surface_init(tvh->vo) < 0) {
    ERROR("Unable to initilize the surface library");
    return -1;
  }
  return 0;
}

void tvh_destroy(tvh_object_t *tvh) {
  DEBUG("Destroying TVHPlayer");

  opensles_destroy(tvh->ao);
  free(tvh->acs);
  free(tvh->ao);

  surface_destroy(tvh->vo);
  free(tvh->vcs);
  free(tvh->vo);

  pthread_mutex_destroy(&tvh->mutex);
  free(tvh);
}

void tvh_start(tvh_object_t *tvh) {
  DEBUG("Starting TVHPlayer");

  pthread_mutex_lock(&tvh->mutex);

  if(tvh->running) {
    pthread_mutex_unlock(&tvh->mutex);
    return;
  }

  tvh->running = 1;
  tvh->cur_pts = 0;

  pthread_create(&tvh->thread, NULL, (void*)&tvh_sync_thread, (void *)tvh);

  pthread_mutex_unlock(&tvh->mutex);
}

void tvh_stop(tvh_object_t *tvh) {
  DEBUG("Stopping TVHPlayer");

  pthread_mutex_lock(&tvh->mutex);

  if(!tvh->running) {
    pthread_mutex_unlock(&tvh->mutex);
    return;
  }

  tvh->running = 0;

  tvh_audio_close(tvh);
  tvh_video_close(tvh);

  pthread_mutex_unlock(&tvh->mutex);

  surface_close(tvh->vo);
  opensles_close(tvh->ao);

  pthread_join(tvh->thread, NULL);
}

int tvh_video_init(tvh_object_t *tvh, const char *codec) {
  int codec_id = 0;
  vcodec_sys_t *cs = tvh->vcs;

  DEBUG("Initializing video codec");
  pthread_mutex_lock(&tvh->mutex);
  
  if(!strcmp(codec, "H264")) {
    codec_id = CODEC_ID_H264;
  } else if(!strcmp(codec, "MPEG2VIDEO")) {
    codec_id = CODEC_ID_MPEG2VIDEO;
  }

  if(!codec_id) {
    DEBUG("Unknown video codec %s", codec);
    goto error;
  }

  cs->codec = avcodec_find_decoder(codec_id);
  if(!cs->codec) {
    DEBUG("Unable to find video codec %s", codec);
    goto error;
  }

  if(cs->codec->type != AVMEDIA_TYPE_VIDEO) {
    DEBUG("Invalid codec type for video decoding");
    goto error;
  }
  
  cs->ctx = avcodec_alloc_context2(AVMEDIA_TYPE_VIDEO);
  cs->frame = avcodec_alloc_frame();
  avcodec_get_frame_defaults(cs->frame);

  if(avcodec_open(cs->ctx, cs->codec) < 0) {
    ERROR("Unable to open video codec");
    tvh_video_close(tvh);
    goto error;
  }

  tvh->cur_pts = 0;
  pthread_mutex_unlock(&tvh->mutex);

  return 0;

 error:
  pthread_mutex_unlock(&tvh->mutex);
  return -1;
}

void tvh_video_enqueue(tvh_object_t *tvh, uint8_t *buf, size_t len, int64_t pts, int64_t dts, int64_t dur) {
  AVPacket packet;
  AVPicture pict;
  int length;
  int got_picture;
  int pix_fmt = PIX_FMT_BGR32;
  vcodec_sys_t *cs = tvh->vcs;
  vout_sys_t *vo = tvh->vo;
  
  pthread_mutex_lock(&tvh->mutex);

  if(!vo->surface || !tvh->running) {
    pthread_mutex_unlock(&tvh->mutex);
    return;
  }

  av_init_packet(&packet);
  packet.data = buf;
  packet.size = len;
  packet.pts  = pts;
  packet.dts  = dts;
  packet.duration = dur;

  length = avcodec_decode_video2(cs->ctx, cs->frame, &got_picture, &packet);
  if(length <= 0) {
    ERROR("Unable to decode video stream (%d)", length);
    pthread_mutex_unlock(&tvh->mutex);
    return;
  }

  if(!got_picture) {
    pthread_mutex_unlock(&tvh->mutex);
    return;
  }

  if(pts) {
    cs->frame->pts = pts;
  } else if(packet.dts != AV_NOPTS_VALUE && packet.dts) {
    cs->frame->pts = packet.dts;
  }

  memset(&pict, 0, sizeof(pict));
  vout_buffer_t *vb = (vout_buffer_t *) malloc(sizeof(vout_buffer_t));
  
  vb->len = avpicture_get_size(pix_fmt, vo->surface_info.size ? vo->surface_info.size : cs->ctx->width, cs->ctx->height);
  vb->ptr = av_malloc(vb->len);
  vb->pts = cs->frame->pts;
  vb->width = cs->ctx->width;
  vb->height = cs->ctx->height;

  avpicture_fill(&pict, 
		 vb->ptr, 
		 pix_fmt,
		 vo->surface_info.size ? vo->surface_info.size : cs->ctx->width, 
		 cs->ctx->height);


  cs->conv = sws_getCachedContext(cs->conv,
				  cs->ctx->width,
				  cs->ctx->height,
				  cs->ctx->pix_fmt,
				  cs->ctx->width,
				  cs->ctx->height,
				  pix_fmt,
				  SWS_FAST_BILINEAR,
				  NULL,
				  NULL,
				  NULL);

  sws_scale(cs->conv, 
	    (const uint8_t * const*)cs->frame->data, 
	    cs->frame->linesize,
	    0, 
	    cs->ctx->height, 
	    pict.data, 
	    pict.linesize);

  pthread_mutex_unlock(&tvh->mutex);

  pthread_mutex_lock(&vo->mutex);
  TAILQ_INSERT_TAIL(&vo->render_queue, vb, entry);
  pthread_mutex_unlock(&vo->mutex);
  pthread_cond_signal(&vo->cond);
}

static void tvh_video_close(tvh_object_t *tvh) {
  DEBUG("Closing video codec");

  vcodec_sys_t *cs = tvh->vcs;
  
  if(cs->ctx != NULL) {
    avcodec_close(cs->ctx);
  }
  
  if(cs->frame) {
    av_free(cs->frame);
  }
  
  cs->ctx = NULL;
  cs->codec = NULL;
  cs->frame = NULL;
  
}

int tvh_audio_init(tvh_object_t *tvh, const char *codec) {
  int codec_id = 0;
  acodec_sys_t *cs = tvh->acs;

  DEBUG("Initializing audio codec");
  pthread_mutex_lock(&tvh->mutex);

  if(!strcmp(codec, "AC3")) {
    codec_id = CODEC_ID_AC3;
  } else if(!strcmp(codec, "EAC3")) {
    codec_id = CODEC_ID_EAC3;
  } else if(!strcmp(codec, "AAC")) {
    codec_id = CODEC_ID_AAC;
  } else if(!strcmp(codec, "MPEG2AUDIO")) {
    codec_id = CODEC_ID_MP2;
  } else if(!strcmp(codec, "MP3")) {
    codec_id = CODEC_ID_MP3;
  }

  if(!codec_id) {
    ERROR("Unknown audio codec %s", codec);
    goto error;
  }

  cs->codec = avcodec_find_decoder(codec_id);
  if(!cs->codec) {
    ERROR("Unable to find audio codec %s", codec);
    goto error;
  }

  if(cs->codec->type != AVMEDIA_TYPE_AUDIO) {
    ERROR("Invalid codec type for audio decoding");
    goto error;
  }
  
  cs->ctx = avcodec_alloc_context2(AVMEDIA_TYPE_AUDIO);
  cs->buf = av_malloc(AVCODEC_MAX_AUDIO_FRAME_SIZE*2);

  if(avcodec_open(cs->ctx, cs->codec) < 0) {
    ERROR("Unable to open audio codec");
    tvh_audio_close(tvh);
    goto error;
  }

  tvh->cur_pts = 0;
  pthread_mutex_unlock(&tvh->mutex);

  return 0;

 error:
  pthread_mutex_unlock(&tvh->mutex);
  return -1;
}

int tvh_audio_enqueue(tvh_object_t *tvh, uint8_t *buf, size_t len, int64_t pts, int64_t dts, int64_t dur) {
  uint8_t *ptr;
  AVPacket packet;
  int length;
  acodec_sys_t *cs = tvh->acs;
  aout_sys_t *ao = tvh->ao;
  int running = 0;

  pthread_mutex_lock(&tvh->mutex);

  if(!tvh->running) {
    pthread_mutex_unlock(&tvh->mutex);
    return -1;
  }
  
  av_init_packet(&packet);
  packet.data = ptr = buf;
  packet.size = len;
  
  cs->len = AVCODEC_MAX_AUDIO_FRAME_SIZE*2;
  length = avcodec_decode_audio3(cs->ctx, cs->buf, &cs->len, &packet);
  if(length <= 0) {
    ERROR("Unable to decode audio stream (%d)", length);
    pthread_mutex_unlock(&tvh->mutex);
    return -1;
  }

  if(packet.pts != AV_NOPTS_VALUE) {
    pts = packet.pts;
  }

  aout_buffer_t *ab = (aout_buffer_t *) malloc(sizeof(aout_buffer_t));
  ab->ptr = av_malloc(cs->len);
  ab->len = cs->len;
  ab->pts = pts;
  memcpy(ab->ptr, cs->buf, cs->len);

  if(!opensles_is_open(tvh->ao)) {
    opensles_open(tvh->ao, cs->ctx->channels , cs->ctx->sample_rate*1000);
  }

  running = opensles_enqueue(ao, ab) > 0; 

  pthread_mutex_unlock(&tvh->mutex);

  return running;
}

static void tvh_audio_close(tvh_object_t *tvh) {
  DEBUG("Closing audio codec");
  acodec_sys_t *cs = tvh->acs;

  if(cs->ctx != NULL) {
    avcodec_close(cs->ctx);
  }

  if(cs->buf != NULL) {
    av_free(cs->buf);
  }
  
  cs->ctx = NULL;
  cs->codec = NULL;
  cs->buf = NULL;
}

static void tvh_audio_callback(aout_buffer_t *ab, void *args) {
  tvh_object_t *tvh = (tvh_object_t *)args;
  vout_sys_t *vo = tvh->vo;

  if(!tvh->running) {
    return;
  }

  tvh->cur_pts = ab->pts;

  pthread_cond_signal(&vo->cond);
}

static int tvh_cond_wait_timeout(pthread_cond_t *c, pthread_mutex_t *m, int delta) {
  struct timespec ts;
  
  clock_gettime(CLOCK_REALTIME, &ts);
  ts.tv_sec  +=  delta / 1000;
  ts.tv_nsec += (delta % 1000) * 1000000;
  
  if(ts.tv_nsec > 1000000000) {
    ts.tv_sec++;
    ts.tv_nsec -= 1000000000;
  }
  return pthread_cond_timedwait(c, m, &ts) == ETIMEDOUT;
}

static void tvh_sync_thread(void *args) {
  tvh_object_t *tvh = (tvh_object_t *)args;
  vout_sys_t *vo = tvh->vo;
  
  while(tvh->running) {
    tvh_cond_wait_timeout(&vo->cond, &vo->mutex, 100);
    vout_buffer_t *vb = TAILQ_FIRST(&vo->render_queue);

    if(!vb || (vb->pts != AV_NOPTS_VALUE && vb->pts > tvh->cur_pts)) {
      continue;
    }

    surface_render(vo, vb);
    
    TAILQ_REMOVE(&vo->render_queue, vb, entry);
    av_free(vb->ptr);
    free(vb);

    pthread_mutex_unlock(&vo->mutex);
  }
  pthread_exit(0);
}
