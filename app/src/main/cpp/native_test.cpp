#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_formsaathi_voice_NativeTest_nativeHello(
        JNIEnv *env,
        jobject
) {
    return env->NewStringUTF("JNI works");
}