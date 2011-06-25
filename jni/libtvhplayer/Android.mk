LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE     := tvhplayer
LOCAL_SRC_FILES  := tvhplayer.c surface.c opensles.c jni.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../libffmpeg
LOCAL_LDLIBS     := -llog

LOCAL_WHOLE_STATIC_LIBRARIES := libavcodec libavutil libswscale

include $(BUILD_SHARED_LIBRARY)
