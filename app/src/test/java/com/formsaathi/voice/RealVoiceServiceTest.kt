package com.formsaathi.voice

import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RealVoiceServiceTest {
    @Test fun decodesPcmAndForwardsLanguage() = runBlocking {
        val file = File.createTempFile("voice", ".pcm")
        try {
            file.writeBytes(byteArrayOf(0, -128, 0, 0, -1, 127))
            val service = RealVoiceService { samples, language ->
                assertArrayEquals(floatArrayOf(-1f, 0f, 32767f / 32768f), samples, 0.00001f)
                assertEquals(SupportedLanguage.MARATHI, language)
                "होय"
            }
            assertEquals("होय", service.transcribe(file, SupportedLanguage.MARATHI))
        } finally {
            file.delete()
        }
    }

    @Test fun rejectsIncompleteSamplesBeforeInference() = runBlocking {
        val file = File.createTempFile("voice", ".pcm")
        try {
            file.writeBytes(byteArrayOf(1))
            val service = RealVoiceService { _, _ -> error("Inference must not run") }
            try {
                service.transcribe(file, SupportedLanguage.ENGLISH)
                fail("Expected invalid audio")
            } catch (_: VoiceException.InvalidAudio) {
            }
        } finally {
            file.delete()
        }
    }

    @Test fun rejectsEmptyTranscript() = runBlocking {
        val file = File.createTempFile("voice", ".pcm")
        try {
            file.writeBytes(byteArrayOf(0, 0))
            val service = RealVoiceService { _, _ -> "  " }
            try {
                service.transcribe(file, SupportedLanguage.HINDI)
                fail("Expected no speech")
            } catch (_: VoiceException.NoSpeech) {
            }
        } finally {
            file.delete()
        }
    }
}
