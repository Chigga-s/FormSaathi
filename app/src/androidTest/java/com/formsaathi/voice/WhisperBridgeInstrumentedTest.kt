package com.formsaathi.voice

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhisperBridgeInstrumentedTest {

    @Test
    fun whisperLibraryIsLinked() {
        val version = WhisperBridge.version()
        assertTrue(version.isNotBlank())
    }
}