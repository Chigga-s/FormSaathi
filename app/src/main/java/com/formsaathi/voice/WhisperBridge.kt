package com.formsaathi.voice

/**
 * JNI surface for whisper.cpp.
 *
 * The native library is only present when the app is built with the NDK and the
 * whisper.cpp sources checked out under `app/src/main/cpp/whisper.cpp` (see
 * README). [isAvailable] reports whether it loaded, so callers can tell "offline
 * voice was not built into this APK" apart from "the model file is missing" and
 * say so instead of failing with a generic error.
 */
object WhisperBridge {

    /** True when libformsaathi_native.so loaded; false in builds without the NDK. */
    val isAvailable: Boolean = try {
        System.loadLibrary("formsaathi_native")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    external fun loadModel(path: String): Long

    external fun transcribe(
        handle: Long,
        audio: FloatArray,
        language: String
    ): String

    external fun freeModel(handle: Long)

    external fun version(): String
}
