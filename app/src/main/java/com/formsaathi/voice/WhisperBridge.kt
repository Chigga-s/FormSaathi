package com.formsaathi.voice


object WhisperBridge {


    init {
        try {
            System.loadLibrary("formsaathi_native")
        } catch (_: UnsatisfiedLinkError) {
        }
    }


    external fun loadModel(
        path: String
    ): Long


    external fun transcribe(
        handle: Long,
        audio: FloatArray,
        language: String
    ): String


    external fun freeModel(
        handle: Long
    )


    external fun version(): String

}
