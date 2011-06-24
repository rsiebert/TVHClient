#include <stdlib.h>

#include "tvhplayer.h"
#include "codec.h"

int vcodec_init(vcodec_sys_t *p_sys, int codec_id) {
  p_sys->p_codec = avcodec_find_decoder(codec_id);
  if(!p_sys->p_codec) {
    DEBUG("Unable to find video codec");
    return 0;
  }
  if(p_sys->p_codec->type != CODEC_TYPE_VIDEO) {
    DEBUG("Invalid codec type for video decoding");
    return 0;
  }

  p_sys->p_ctx = avcodec_alloc_context2(CODEC_TYPE_VIDEO);
  p_sys->p_frame = avcodec_alloc_frame();
  avcodec_get_frame_defaults(p_sys->p_frame);

  if(avcodec_open(p_sys->p_ctx, p_sys->p_codec) < 0) {
    DEBUG("Unable to open video codec");
    vcodec_close(p_sys);
    return 0;
  }
  
  return 1;
}

int vcodec_close(vcodec_sys_t *p_sys) {
  if(p_sys->p_ctx != NULL) {
    avcodec_close(p_sys->p_ctx);
  }
  
  if(p_sys->p_frame) {
    av_free(p_sys->p_frame);
  }
  
  p_sys->p_ctx = NULL;
  p_sys->p_codec = NULL;
  p_sys->p_frame = NULL;
}

int vcodec_decode(vcodec_sys_t *p_sys, AVPacket *packet) {
  int got_picture = 0;
  int len = 0;

  len = avcodec_decode_video2(p_sys->p_ctx, p_sys->p_frame, &got_picture, packet);

  
  return len;
}


int acodec_init(acodec_sys_t *p_sys, int codec_id) {
  p_sys->p_codec = avcodec_find_decoder(codec_id);
  if(!p_sys->p_codec) {
    DEBUG("Unable to find audio codec");
    return 0;
  }
  
  if(p_sys->p_codec->type != CODEC_TYPE_AUDIO) {
    DEBUG("Invalid codec type for audio decoding");
    return 0;
  }
  
  p_sys->p_ctx = avcodec_alloc_context2(CODEC_TYPE_AUDIO);
  p_sys->p_buf = av_malloc(AVCODEC_MAX_AUDIO_FRAME_SIZE*2);
  
  if(avcodec_open(p_sys->p_ctx, p_sys->p_codec) < 0) {
    DEBUG("Unable to open audio codec");
    acodec_close(p_sys);
    return 0;
  }

  return 1;
}

int acodec_decode(acodec_sys_t *p_sys, AVPacket *packet) {
  uint32_t frame_size = AVCODEC_MAX_AUDIO_FRAME_SIZE*2;
  int len = 0;

  len = avcodec_decode_audio3(p_sys->p_ctx, p_sys->p_buf, &frame_size, packet);
  p_sys->i_buf = frame_size;
  
  return len;
}

int acodec_close(acodec_sys_t *p_sys) {
  if(p_sys->p_ctx != NULL) {
    avcodec_close(p_sys->p_ctx);
  }

  if(p_sys->p_buf != NULL) {
    av_free(p_sys->p_buf);
  }
  
  p_sys->p_ctx = NULL;
  p_sys->p_codec = NULL;
  p_sys->p_buf = NULL;
}

