#ifndef __CODEC_H__
#define __CODEC_H__

#include <libavcodec/avcodec.h>

typedef struct acodec_sys {
  AVCodecContext * p_ctx;
  AVCodec        * p_codec;
  unsigned short * p_buf;
  size_t           i_buf;
} acodec_sys_t;

typedef struct vcodec_sys {
  AVCodecContext * p_ctx;
  AVCodec        * p_codec;
  AVFrame        * p_frame;
} vcodec_sys_t;

int vcodec_init(vcodec_sys_t *p_sys, int codec_id);
int vcodec_decode(vcodec_sys_t *p_sys, AVPacket *packet);
int vcodec_close(vcodec_sys_t *p_sys);

int acodec_init(acodec_sys_t *p_sys, int codec_id);
int acodec_decode(acodec_sys_t *p_sys, AVPacket *packet);
int acodec_close(acodec_sys_t *p_sys);

#endif
