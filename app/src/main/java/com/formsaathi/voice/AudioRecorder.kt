package com.formsaathi.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.formsaathi.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Captures a short answer as mono 16 kHz PCM16 little-endian, the exact format the
 * native Whisper bridge expects.
 *
 * Ordering matters here: the capture loop owns the output stream and closes it
 * before the thread exits, and [stopRecording] joins that thread before it looks
 * at the file. Transcribing before the worker has flushed was a source of empty
 * recordings.
 */
class AudioRecorder(
    private val context: Context,
    private val maxSeconds: Int = MAX_SECONDS
) {
    private val recording = MutableStateFlow(false)
    val isRecording = recording.asStateFlow()

    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private var output: File? = null
    private var workerStarted: CountDownLatch? = null

    @Volatile
    private var capturing = false

    @Volatile
    private var failure: Exception? = null

    /** Loudest sample seen in the current recording, used to spot a dead mic. */
    @Volatile
    private var peakAmplitude = 0

    /**
     * Set when the platform reports it is feeding this app silence. Android does
     * this without any error: the read loop returns full, correctly sized buffers
     * of zeros, which is indistinguishable from a working mic until the transcript
     * comes back empty.
     */
    @Volatile
    private var silencedByPlatform = false

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val recordingCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
                val sessionId = recorder?.audioSessionId ?: return
                configs.firstOrNull { it.clientAudioSessionId == sessionId }
                    ?.let { if (it.isClientSilenced) silencedByPlatform = true }
            }
        }
    } else {
        null
    }

    private val maxBytes: Int get() = SAMPLE_RATE * BYTES_PER_SAMPLE * maxSeconds

    @Synchronized
    fun startRecording() {
        check(worker == null) { "A recording is already active" }
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw VoiceException.Recording()
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) throw VoiceException.Recording()
        // A larger ring buffer survives a scheduling hiccup without dropping audio.
        val bufferBytes = minBuffer * 2

        var device: AudioRecord? = null
        try {
            device = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes
            )
            if (device.state != AudioRecord.STATE_INITIALIZED) throw VoiceException.Recording()

            val file = File.createTempFile("answer-", ".pcm", context.cacheDir)
            val latch = CountDownLatch(1)

            recorder = device
            output = file
            workerStarted = latch
            failure = null
            peakAmplitude = 0
            silencedByPlatform = false
            capturing = true

            device.startRecording()
            if (device.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw VoiceException.Recording()
            }
            recording.value = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && recordingCallback != null) {
                audioManager?.registerAudioRecordingCallback(recordingCallback, null)
            }

            worker = thread(name = "formsaathi-recorder") {
                var written = 0
                try {
                    file.outputStream().use { stream ->
                        val buffer = ByteArray(bufferBytes)
                        latch.countDown()
                        while (capturing && written < maxBytes) {
                            val wanted = minOf(buffer.size, maxBytes - written)
                            val read = device.read(buffer, 0, wanted)
                            when {
                                read > 0 -> {
                                    stream.write(buffer, 0, read)
                                    written += read
                                    trackPeak(buffer, read)
                                }
                                // A zero-length read is a normal wake-up with no
                                // data ready; only a negative code is an error.
                                read == 0 -> Unit
                                else -> {
                                    if (capturing) throw VoiceException.Recording()
                                    return@use
                                }
                            }
                        }
                    }
                } catch (error: Exception) {
                    failure = error
                } finally {
                    latch.countDown()
                    capturing = false
                    recording.value = false
                    if (BuildConfig.DEBUG) {
                        Log.d(
                            TAG,
                            "capture finished: $written bytes " +
                                "(${written / (SAMPLE_RATE * BYTES_PER_SAMPLE).toFloat()}s) " +
                                "peak=$peakAmplitude silenced=$silencedByPlatform"
                        )
                    }
                }
            }
        } catch (error: Exception) {
            capturing = false
            recording.value = false
            try {
                device?.stop()
            } catch (_: IllegalStateException) {
            }
            device?.release()
            recorder = null
            worker = null
            workerStarted = null
            output?.delete()
            output = null
            throw if (error is VoiceException) error else VoiceException.Recording(error)
        }
    }

    /**
     * Stops capture and returns the finished PCM file. Blocks until the capture
     * thread has flushed and exited, so callers must not run this on the main
     * thread; [VoiceController] keeps it on an IO dispatcher.
     */
    @Synchronized
    fun stopRecording(): File {
        val file = output ?: throw VoiceException.Recording()
        finish()
        output = null

        failure?.let {
            file.delete()
            failure = null
            throw if (it is VoiceException) it else VoiceException.Recording(it)
        }
        if (file.length() < MIN_USABLE_BYTES) {
            file.delete()
            throw VoiceException.NoSpeech()
        }
        // A recording of exactly zero amplitude never comes from a working
        // microphone; the platform handed this app silence. Saying so is the
        // difference between a user who fixes the cause and a user who thinks
        // the app is broken. A call or VoIP session holding the mic is the most
        // common cause, so it gets its own message.
        if (silencedByPlatform || peakAmplitude == 0) {
            file.delete()
            throw if (isAnotherAppHoldingTheMicrophone()) {
                VoiceException.MicrophoneBusy()
            } else {
                VoiceException.MicrophoneSilenced()
            }
        }
        if (peakAmplitude < MIN_SPEECH_AMPLITUDE) {
            file.delete()
            throw VoiceException.NoSpeech()
        }
        return file
    }

    /** Aborts capture and deletes the partial recording. */
    @Synchronized
    fun cancel() {
        finish()
        failure = null
        output?.delete()
        output = null
    }

    /**
     * True while the device is in a call or a VoIP session. Android routes the
     * microphone to that session and silences everyone else without any error.
     */
    private fun isAnotherAppHoldingTheMicrophone(): Boolean {
        val mode = audioManager?.mode ?: return false
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
    }

    /** Peak of the little-endian PCM16 samples in the first [length] bytes. */
    private fun trackPeak(buffer: ByteArray, length: Int) {
        var peak = peakAmplitude
        var index = 0
        while (index + 1 < length) {
            val sample = ((buffer[index + 1].toInt() shl 8) or (buffer[index].toInt() and 0xFF)).toShort()
            val magnitude = kotlin.math.abs(sample.toInt())
            if (magnitude > peak) peak = magnitude
            index += 2
        }
        peakAmplitude = peak
    }

    private fun finish() {
        // Clear the loop flag first so the worker exits as soon as its blocking
        // read returns, then stop the device to make that read return now.
        capturing = false
        // If stop arrives before the worker reached its loop, wait briefly so the
        // stream is created and closed in order rather than left empty.
        workerStarted?.await(WORKER_START_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        workerStarted = null
        try {
            recorder?.stop()
        } catch (_: IllegalStateException) {
        }
        worker?.join(WORKER_JOIN_TIMEOUT_MS)
        worker = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && recordingCallback != null) {
            audioManager?.unregisterAudioRecordingCallback(recordingCallback)
        }
        recorder?.release()
        recorder = null
        recording.value = false
    }

    private companion object {
        const val TAG = "FormSaathiAudio"
        const val SAMPLE_RATE = 16_000
        const val BYTES_PER_SAMPLE = 2
        const val MAX_SECONDS = 10

        /** Below ~0.3 s there is nothing for the model to recognise. */
        const val MIN_USABLE_BYTES = SAMPLE_RATE * BYTES_PER_SAMPLE * 3 / 10

        /** Roughly -60 dBFS; below this the recording holds only room noise. */
        const val MIN_SPEECH_AMPLITUDE = 32

        const val WORKER_START_TIMEOUT_MS = 500L
        const val WORKER_JOIN_TIMEOUT_MS = 2_000L
    }
}
