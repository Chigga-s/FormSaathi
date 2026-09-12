package com.formsaathi.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.concurrent.thread

class AudioRecorder(private val context: Context) {
    private val recording = MutableStateFlow(false)
    val isRecording = recording.asStateFlow()
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private var output: File? = null
    @Volatile private var failure: Exception? = null

    @Synchronized
    fun startRecording() {
        check(worker == null) { "A recording is already active" }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw VoiceException.Recording()
        }
        val size = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (size <= 0) throw VoiceException.Recording()
        try {
            val device = AudioRecord(MediaRecorder.AudioSource.MIC, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
            recorder = device
            if (device.state != AudioRecord.STATE_INITIALIZED) throw VoiceException.Recording()
            val file = File.createTempFile("answer-", ".pcm", context.cacheDir)
            output = file
            failure = null
            device.startRecording()
            recording.value = true
            worker = thread(name = "formsaathi-recorder") {
                try {
                    file.outputStream().use { stream ->
                        val buffer = ByteArray(size)
                        var written = 0
                        while (recording.value && written < 320000) {
                            val read = device.read(buffer, 0, minOf(buffer.size, 320000 - written))
                            if (read < 0) {
                                if (recording.value) throw VoiceException.Recording()
                                break
                            }
                            if (read == 0) continue
                            stream.write(buffer, 0, read)
                            written += read
                        }
                    }
                } catch (error: Exception) {
                    failure = error
                } finally {
                    recording.value = false
                    try {
                        device.stop()
                    } catch (_: IllegalStateException) {
                    }
                }
            }
        } catch (error: Exception) {
            recorder?.release()
            recorder = null
            output?.delete()
            output = null
            recording.value = false
            throw VoiceException.Recording(error)
        }
    }

    @Synchronized
    fun stopRecording(): File {
        val file = output ?: throw VoiceException.Recording()
        finish()
        output = null
        failure?.let {
            file.delete()
            throw VoiceException.Recording(it)
        }
        if (file.length() == 0L) {
            file.delete()
            throw VoiceException.InvalidAudio()
        }
        return file
    }

    @Synchronized
    fun cancel() {
        finish()
        output?.delete()
        output = null
    }

    private fun finish() {
        recording.value = false
        try {
            recorder?.stop()
        } catch (_: IllegalStateException) {
        }
        worker?.join()
        worker = null
        recorder?.release()
        recorder = null
    }
}
