#ifndef __TVHPLAYER_H__
#define __TVHPLAYER_H__

#include <android/log.h>  

#define TAG "TVHPlayer"

#define DEBUG(args...)					\
  __android_log_print(ANDROID_LOG_DEBUG, TAG, args)
#define INFO(args...)					\
  __android_log_print(ANDROID_LOG_INFO, TAG, args)
#define ERROR(args...)					\
  __android_log_print(ANDROID_LOG_ERROR, TAG, args)

#endif
