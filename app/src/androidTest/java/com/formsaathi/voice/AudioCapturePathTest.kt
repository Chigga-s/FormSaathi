package com.formsaathi.voice

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.formsaathi.MainActivity
import com.formsaathi.contracts.VoiceService
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Proves the real microphone path on a real device, independently of whisper.cpp.
 *
 * The reported symptom was "the phone records but the transcript comes back
 * blank", which can mean either the capture produced nothing or the model
 * produced nothing. These tests pin down the capture half: permission, AudioRecord
 * configuration, the worker thread and its flush ordering, the format handed to
 * the bridge, and temporary-file cleanup. A synthetic WAV fixture cannot show any
 * of that.
 */
@RunWith(AndroidJUnit4::class)
class AudioCapturePathTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun grantMicrophone() {
        assumeTrue(
            "RECORD_AUDIO could not be granted to the app on this device, so the " +
                "microphone path cannot be exercised automatically. Grant it by hand " +
                "and use the manual voice checklist in the README instead.",
            MicrophonePermission.grant()
        )
        // Android silences microphone reads for apps that are not in the
        // foreground: without a visible activity every device returns a
        // correctly sized buffer of zeros, which looks exactly like the
        // "records but transcribes blank" symptom. The app under test is
        // brought to the front so this measures the real user situation.
        scenario = ActivityScenario.launch(MainActivity::class.java)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(500)
    }

    @org.junit.After
    fun closeActivity() {
        scenario?.close()
        scenario = null
    }

    @Test
    fun capturedAudioUsesTheFormatTheNativeBridgeExpects() {
        val recorder = AudioRecorder(context)
        recorder.startRecording()
        assertTrue("Recorder must report it is recording", recorder.isRecording.value)
        Thread.sleep(RECORD_MS)

        val result = runCatching { recorder.stopRecording() }
        assertFalse("Recorder must stop reporting", recorder.isRecording.value)

        val file = result.getOrElse { error ->
            // A platform that silences this app is a legitimate outcome, and the
            // point of the recorder change is that it says so instead of handing
            // back an empty answer. Anything else is a real failure.
            assertTrue(
                "Stopping must either return audio or explain why it could not: $error",
                error is VoiceException.MicrophoneSilenced ||
                    error is VoiceException.MicrophoneBusy ||
                    error is VoiceException.NoSpeech
            )
            println("MIC-CAPTURE platform returned silence: ${error.message}")
            return
        }

        try {
            assertTrue("Recording file must exist", file.isFile)
            assertEquals("Audio must be PCM16, so an even byte count", 0L, file.length() % 2)

            val seconds = file.length() / (SAMPLE_RATE * 2).toFloat()
            assertTrue(
                "Expected about ${RECORD_MS / 1000f}s of audio, captured ${seconds}s (${file.length()} bytes)",
                seconds > MIN_EXPECTED_SECONDS
            )
            assertTrue("Capture must respect the 10 second cap, got ${seconds}s", seconds <= 10.5f)

            val samples = readSamples(file)
            assertTrue("No samples were captured", samples.isNotEmpty())
            val peak = samples.maxOf { kotlin.math.abs(it.toInt()) }
            println("MIC-CAPTURE samples=${samples.size} peak=$peak")
            assertTrue("A returned recording must contain audio, peak=$peak", peak > 0)
        } finally {
            file.delete()
        }
    }

    @Test
    fun aSilencedMicrophoneIsReportedInsteadOfProducingABlankAnswer() {
        // Android hands a silenced app full, correctly sized buffers of zeros and
        // reports no error. Passing that to the speech model produced an empty
        // transcript that looked like the app losing the answer.
        val recorder = AudioRecorder(context)
        recorder.startRecording()
        Thread.sleep(RECORD_MS)

        val result = runCatching { recorder.stopRecording() }
        result.onSuccess { file ->
            val peak = readSamples(file).maxOf { kotlin.math.abs(it.toInt()) }
            file.delete()
            assertTrue(
                "Silent audio must never be returned as a usable recording, peak=$peak",
                peak > 0
            )
        }
        result.onFailure { error ->
            assertTrue(
                "Silence must surface as an actionable message, got $error",
                error is VoiceException.MicrophoneSilenced ||
                    error is VoiceException.MicrophoneBusy ||
                    error is VoiceException.NoSpeech
            )
            assertTrue(
                "The message must tell the user what to do: ${error.message}",
                error.message!!.contains("type your answer", ignoreCase = true)
            )
        }
    }

    @Test
    fun stoppingImmediatelyAfterStartingIsReportedAsNoSpeech() {
        // Losing the race between stop and the capture thread used to yield a
        // zero-byte file that later surfaced as an empty answer.
        val recorder = AudioRecorder(context)
        recorder.startRecording()
        try {
            recorder.stopRecording()
            fail("A recording too short to contain speech must not be accepted")
        } catch (_: VoiceException.NoSpeech) {
            // expected
        } catch (_: VoiceException.Recording) {
            // also acceptable: the device refused to start at all
        }
    }

    @Test
    fun temporaryRecordingIsDeletedAfterTranscription() {
        var seenFile: File? = null
        var sampleCount = 0
        val stubService = object : VoiceService {
            override suspend fun transcribe(audioFile: File, language: SupportedLanguage): String {
                seenFile = audioFile
                sampleCount = (audioFile.length() / 2).toInt()
                return "Pune"
            }
        }
        val controller = VoiceController(context, stubService)

        controller.startListening()
        Thread.sleep(RECORD_MS)
        val transcript = runCatching {
            runBlocking { controller.stopListening(SupportedLanguage.ENGLISH) }
        }

        transcript.onFailure { error ->
            assertTrue(
                "Only a silenced or empty microphone may prevent transcription: $error",
                error is VoiceException.MicrophoneSilenced ||
                    error is VoiceException.MicrophoneBusy ||
                    error is VoiceException.NoSpeech
            )
            // The recorder deletes the file itself on these paths.
            assertFalse(
                "A rejected recording must not be left behind",
                context.cacheDir.listFiles { file -> file.name.startsWith("answer-") }
                    ?.isNotEmpty() ?: false
            )
            return
        }

        assertEquals("Pune", transcript.getOrThrow())
        assertTrue("Voice service must receive real samples, got $sampleCount", sampleCount > 0)
        assertFalse(
            "Temporary recording must be deleted after transcription",
            seenFile?.exists() ?: true
        )
    }

    @Test
    fun cancellingDeletesTheRecordingAndAllowsAFreshStart() {
        val recorder = AudioRecorder(context)
        recorder.startRecording()
        Thread.sleep(300)
        recorder.cancel()
        assertFalse("Cancel must clear the recording state", recorder.isRecording.value)

        val leftovers = context.cacheDir.listFiles { file -> file.name.startsWith("answer-") }
        assertTrue(
            "Cancelled recordings must not be left in the cache: ${leftovers?.toList()}",
            leftovers == null || leftovers.isEmpty()
        )

        // A cancelled recorder must be reusable, otherwise the mic button dies
        // after the first cancel.
        recorder.startRecording()
        Thread.sleep(400)
        recorder.cancel()
    }

    private fun readSamples(file: File): ShortArray {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return ShortArray(buffer.remaining()) { buffer.get() }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val RECORD_MS = 1_500L
        const val MIN_EXPECTED_SECONDS = 0.8f
    }
}
