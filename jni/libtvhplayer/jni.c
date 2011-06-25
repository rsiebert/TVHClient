#include <jni.h>  
#include <string.h>  
#include <stdlib.h>

#include "tvhplayer.h"

static tvh_object_t *instance;

jboolean Java_org_me_tvhguide_TVHPlayer_setAudioCodec(JNIEnv* env, jobject thiz, jstring jCodec) {
  const char *cCodec = (*env)->GetStringUTFChars(env, jCodec, 0);

  if(tvh_audio_init(instance, cCodec) < 0) {
    return 0;
  }

  DEBUG("Setting audio codec to %s", cCodec);
  return 1;
}

jboolean Java_org_me_tvhguide_TVHPlayer_setVideoCodec(JNIEnv* env, jobject thiz, jstring jCodec) {
  const char *cCodec = (*env)->GetStringUTFChars(env, jCodec, 0);

  if(tvh_video_init(instance, cCodec) < 0) {
    return 0;
  }

  DEBUG("Setting video codec to %s", cCodec);
  return 1;
}

void Java_org_me_tvhguide_TVHPlayer_setSurface(JNIEnv* env, jobject thiz, jobject surface) {
  DEBUG("set surface handle");

  void *handle;
  jclass clz = (*env)->GetObjectClass(env, surface);
  jfieldID fid = fid = (*env)->GetFieldID(env, clz, "mSurface", "I");
  if(!fid) {
    jthrowable exp = (*env)->ExceptionOccurred(env);
    if(exp) {
      (*env)->DeleteLocalRef(env, exp);
      (*env)->ExceptionClear(env);
    }
    fid = (*env)->GetFieldID(env, clz, "mNativeSurface", "I");
  }

  if(!fid) {
    ERROR("Can't find Surface handle");
    return;
  }

  handle = (void*)(*env)->GetIntField(env, surface, fid);
  surface_open(instance->vo, handle);

  (*env)->DeleteLocalRef(env, clz);
}

void Java_org_me_tvhguide_TVHPlayer_clear(JNIEnv* env, jobject thiz) {

}

void Java_org_me_tvhguide_TVHPlayer_enqueueAudioFrame(JNIEnv* env, jobject thiz, jbyteArray byteArray) {
  jint len = (*env)->GetArrayLength(env, byteArray);
  jbyte *buf = (*env)->GetByteArrayElements(env, byteArray, NULL);
  
  tvh_audio_enqueue(instance, buf, len);

  (*env)->ReleaseByteArrayElements(env, byteArray, buf, JNI_ABORT);
}

void Java_org_me_tvhguide_TVHPlayer_enqueueVideoFrame(JNIEnv* env, jobject obj, jbyteArray byteArray, jlong pts, jlong dts, jlong duration) {
  jint len = (*env)->GetArrayLength(env, byteArray);
  jbyte *buf = (*env)->GetByteArrayElements(env, byteArray, NULL);

  tvh_video_enqueue(instance, buf, len, pts, dts, duration);

  (*env)->ReleaseByteArrayElements(env, byteArray, buf, JNI_ABORT);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  instance = (tvh_object_t*)malloc(sizeof(tvh_object_t));
  tvh_init(instance);

  return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
  tvh_destroy(instance);
}
