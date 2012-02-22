LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE     := tvhplayer
LOCAL_SRC_FILES  := tvhplayer.c surface.c opensles.c jni.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../libav/$(TARGET_ARCH_ABI)/include
LOCAL_LDLIBS     := -llog

LOCAL_LDFLAGS    += $(LOCAL_PATH)/../libav/$(TARGET_ARCH_ABI)/lib/libswscale.a
LOCAL_LDFLAGS    += $(LOCAL_PATH)/../libav/$(TARGET_ARCH_ABI)/lib/libavcodec.a
LOCAL_LDFLAGS    += $(LOCAL_PATH)/../libav/$(TARGET_ARCH_ABI)/lib/libavutil.a

include $(BUILD_SHARED_LIBRARY)
