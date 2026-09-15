LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:= libalpine
LOCAL_SRC_FILES:= alpine.c
include $(BUILD_SHARED_LIBRARY)
