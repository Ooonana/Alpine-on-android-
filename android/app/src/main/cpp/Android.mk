LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libalpine-bootstrap
LOCAL_SRC_FILES := alpine-bootstrap-zip.S alpine-bootstrap.c
include $(BUILD_SHARED_LIBRARY)
