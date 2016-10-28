LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS += -O3 -fno-strict-aliasing

LOCAL_MODULE    := salsa20
LOCAL_SRC_FILES := yuku_salsa20_cipher_Salsa20ImplNative.cpp

# for logging
LOCAL_LDLIBS    += -llog

include $(BUILD_SHARED_LIBRARY)
