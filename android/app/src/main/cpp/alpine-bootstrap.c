#include <jni.h>

extern jbyte blob[];
extern int blob_size;

JNIEXPORT jobject JNICALL Java_com_alpine_app_AlpineInstaller_getZipBuffer(JNIEnv *env, __attribute__((__unused__)) jclass clazz)
{
    return (*env)->NewDirectByteBuffer(env, blob, (jlong) blob_size);
}
