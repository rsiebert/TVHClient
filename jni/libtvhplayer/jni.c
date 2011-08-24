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
  void *handle;
  DEBUG("Setting surface");
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

void Java_org_me_tvhguide_TVHPlayer_start(JNIEnv* env, jobject thiz) {
  DEBUG("Starting playback");
  tvh_start(instance);
}

void Java_org_me_tvhguide_TVHPlayer_stop(JNIEnv* env, jobject thiz) {
  DEBUG("Stoping playback");
  tvh_stop(instance);
}

jboolean Java_org_me_tvhguide_TVHPlayer_enqueueAudioFrame(JNIEnv* env, jobject thiz, jbyteArray byteArray, jlong pts, jlong dts, jlong duration) {
  jint len = (*env)->GetArrayLength(env, byteArray);
  jbyte *buf = (*env)->GetByteArrayElements(env, byteArray, NULL);
  
  jboolean running = tvh_audio_enqueue(instance, buf, len, pts, dts, duration) > 0;

  (*env)->ReleaseByteArrayElements(env, byteArray, buf, JNI_ABORT);
  return running;
}

void Java_org_me_tvhguide_TVHPlayer_enqueueVideoFrame(JNIEnv* env, jobject obj, jbyteArray byteArray, jlong pts, jlong dts, jlong duration) {
  jint len = (*env)->GetArrayLength(env, byteArray);
  jbyte *buf = (*env)->GetByteArrayElements(env, byteArray, NULL);

  tvh_video_enqueue(instance, buf, len, pts, dts, duration);

  (*env)->ReleaseByteArrayElements(env, byteArray, buf, JNI_ABORT);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  instance = (tvh_object_t*)malloc(sizeof(tvh_object_t));
  memset(instance, 0, sizeof(tvh_object_t));

  tvh_init(instance);

  return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
  tvh_destroy(instance);
}
