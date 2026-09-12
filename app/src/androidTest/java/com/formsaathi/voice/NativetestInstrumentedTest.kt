package com.formsaathi.voice


import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class NativeTestInstrumentedTest {


    @Test
    fun nativeLibraryReturnsExpectedString() {


        val result =
            WhisperBridge.version()


        println(
            "WHISPER VERSION = $result"
        )


        assert(
            result.isNotEmpty()
        )

    }

}