package com.formsaathi.voice

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Full offline voice round trip: microphone in, transcript out.
 *
 * This needs a person to speak and needs whisper.cpp plus the Tiny model compiled
 * into the APK, so it is skipped when the native library is absent rather than
 * reported as a pass. See "Offline voice" in the README for how to enable it and
 * for the manual checklist that covers what this cannot.
 */
@RunWith(AndroidJUnit4::class)
class VoiceControllerTest {

    @Before
    fun grantMicrophone() {
        assumeTrue("RECORD_AUDIO is not granted to the app", MicrophonePermission.grant())
    }

    @Test
    fun spokenAnswerIsTranscribedOffline() {
        assumeTrue(
            "whisper.cpp native library is not built into this APK; offline transcription cannot be exercised",
            WhisperBridge.isAvailable
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val controller = VoiceController(context)
        try {
            controller.startListening()
            println("Speak a short answer now...")
            Thread.sleep(5_000)
            val transcript = runBlocking { controller.stopListening(SupportedLanguage.ENGLISH) }
            println("Transcript: $transcript")
            org.junit.Assert.assertTrue("Transcript must not be blank", transcript.isNotBlank())
        } finally {
            controller.close()
        }
    }
}
