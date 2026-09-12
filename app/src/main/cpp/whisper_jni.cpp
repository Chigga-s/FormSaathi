#include <jni.h>
#include <android/log.h>
#include <string>

#include "whisper.h"


#define LOG_TAG "WHISPER"



extern "C"
JNIEXPORT jlong JNICALL
Java_com_formsaathi_voice_WhisperBridge_loadModel(
        JNIEnv *env,
        jobject,
        jstring modelPath
) {

    const char *path =
            env->GetStringUTFChars(
                    modelPath,
                    nullptr
            );


    __android_log_print(
            ANDROID_LOG_INFO,
            LOG_TAG,
            "Loading model: %s",
            path
    );


    whisper_context_params cparams = whisper_context_default_params();
    cparams.flash_attn = false; // Disable flash attention to avoid CPU crashes on Android emulator

    whisper_context *ctx =
            whisper_init_from_file_with_params(
                    path,
                    cparams
            );


    env->ReleaseStringUTFChars(
            modelPath,
            path
    );


    if(ctx == nullptr) {

        __android_log_print(
                ANDROID_LOG_ERROR,
                LOG_TAG,
                "Model loading failed"
        );

        return 0;
    }


    __android_log_print(
            ANDROID_LOG_INFO,
            LOG_TAG,
            "Model loaded successfully"
    );


    return reinterpret_cast<jlong>(ctx);
}





extern "C"
JNIEXPORT jstring JNICALL
Java_com_formsaathi_voice_WhisperBridge_transcribe(
        JNIEnv *env,
        jobject,
        jlong handle,
        jfloatArray audioArray
) {


    __android_log_print(
            ANDROID_LOG_INFO,
            LOG_TAG,
            "TRANSCRIBE START"
    );


    auto *ctx =
            reinterpret_cast<whisper_context *>(handle);



    if(ctx == nullptr) {

        return env->NewStringUTF(
                "CTX NULL"
        );
    }



    jsize length =
            env->GetArrayLength(
                    audioArray
            );


    __android_log_print(
            ANDROID_LOG_INFO,
            LOG_TAG,
            "Audio samples: %d",
            length
    );



    jfloat *audio =
            env->GetFloatArrayElements(
                    audioArray,
                    nullptr
            );



    whisper_full_params params =
            whisper_full_default_params(
                    WHISPER_SAMPLING_GREEDY
            );



    params.language = "auto";
    params.translate = false;

    params.print_progress = false;
    params.print_realtime = false;
    params.n_threads = 2; // Default limit for threads



    __android_log_print(
            ANDROID_LOG_INFO,
            LOG_TAG,
            "Calling whisper_full"
    );



    int ret =
            whisper_full(
                    ctx,
                    params,
                    audio,
                    length
            );



    __android_log_print(
            ANDROID_LOG_INFO,
            LOG_TAG,
            "whisper_full returned %d",
            ret
    );



    env->ReleaseFloatArrayElements(
            audioArray,
            audio,
            JNI_ABORT
    );



    if(ret != 0) {

        return env->NewStringUTF(
                "WHISPER FAILED"
        );
    }




    int segments =
            whisper_full_n_segments(
                    ctx
            );


    __android_log_print(
            ANDROID_LOG_INFO,
            LOG_TAG,
            "Segments: %d",
            segments
    );



    std::string result;



    for(int i = 0; i < segments; i++) {

        result +=
                whisper_full_get_segment_text(
                        ctx,
                        i
                );
    }



    return env->NewStringUTF(
            result.c_str()
    );
}







extern "C"
JNIEXPORT void JNICALL
Java_com_formsaathi_voice_WhisperBridge_freeModel(
        JNIEnv *,
        jobject,
        jlong handle
) {


    auto *ctx =
            reinterpret_cast<whisper_context *>(handle);



    if(ctx != nullptr) {

        whisper_free(ctx);

    }
}






extern "C"
JNIEXPORT jstring JNICALL
Java_com_formsaathi_voice_WhisperBridge_version(
        JNIEnv *env,
        jobject
) {


    return env->NewStringUTF(
            whisper_version()
    );
}

