package com.formsaathi.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.formsaathi.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Diagnostic sweep for the "records but transcribes blank" report.
 *
 * It records a short buffer from each audio source in turn and prints the peak
 * sample level, which separates a device or configuration that returns digital
 * silence from a speech model that returns nothing. Run it, read the printout,
 * and speak while it runs.
 *
 * This test reports rather than asserts, so it never fails the suite; the
 * assertions about capture live in AudioCapturePathTest.
 */
@RunWith(AndroidJUnit4::class)
class MicrophoneDiagnosticsTest {

    @Test
    fun reportLevelsForEachAudioSource() {
        assumeTrue("RECORD_AUDIO is not granted", MicrophonePermission.grant())

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(500)

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            println("MIC-DIAG microphoneMute=${audioManager.isMicrophoneMute}")
            println("MIC-DIAG mode=${audioManager.mode}")

            val sources = listOf(
                "MIC" to MediaRecorder.AudioSource.MIC,
                "VOICE_RECOGNITION" to MediaRecorder.AudioSource.VOICE_RECOGNITION,
                "DEFAULT" to MediaRecorder.AudioSource.DEFAULT,
                "CAMCORDER" to MediaRecorder.AudioSource.CAMCORDER,
                "VOICE_COMMUNICATION" to MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                "UNPROCESSED" to MediaRecorder.AudioSource.UNPROCESSED
            )

            for ((name, source) in sources) {
                println("MIC-DIAG $name -> ${probe(source)}")
            }
        } finally {
            scenario.close()
        }
    }

    private fun probe(source: Int): String {
        val minBuffer = AudioRecord.getMinBufferSize(
            16_000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return "minBufferSize=$minBuffer (unsupported)"

        var record: AudioRecord? = null
        return try {
            record = AudioRecord(
                source,
                16_000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) return "state=${record.state} (not initialized)"
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return "recordingState=${record.recordingState} (not recording)"
            }

            val buffer = ShortArray(minBuffer)
            var peak = 0
            var total = 0
            val deadline = System.currentTimeMillis() + 1_200
            while (System.currentTimeMillis() < deadline) {
                val read = record.read(buffer, 0, buffer.size)
                if (read <= 0) {
                    if (read < 0) return "read error $read"
                    continue
                }
                total += read
                for (i in 0 until read) {
                    val value = kotlin.math.abs(buffer[i].toInt())
                    if (value > peak) peak = value
                }
            }
            "samples=$total peak=$peak"
        } catch (error: Exception) {
            "exception: ${error.javaClass.simpleName}: ${error.message}"
        } finally {
            try {
                record?.stop()
            } catch (_: IllegalStateException) {
            }
            record?.release()
        }
    }
}
