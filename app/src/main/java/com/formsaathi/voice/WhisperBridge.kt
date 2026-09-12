package com.formsaathi.voice


object WhisperBridge {


    init {
        System.loadLibrary("formsaathi_native")
    }


    external fun loadModel(
        path: String
    ): Long


    external fun transcribe(
        handle: Long,
        audio: FloatArray
    ): String


    external fun freeModel(
        handle: Long
    )


    external fun version(): String

}