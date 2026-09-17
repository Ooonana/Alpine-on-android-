LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:= libalpine
LOCAL_SRC_FILES:= alpine.c
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
include $(BUILD_SHARED_LIBRARY)
