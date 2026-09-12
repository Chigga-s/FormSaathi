package com.formsaathi.voice

object NativeTest {

    init {
        System.loadLibrary("formsaathi_native")
    }

    external fun nativeHello(): String
}