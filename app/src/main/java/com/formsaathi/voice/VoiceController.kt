package com.formsaathi.voice

import android.content.Context
import com.formsaathi.contracts.VoiceService
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

class VoiceController(
    context: Context,
    private val voiceService: VoiceService = RealVoiceService(context)
) : AutoCloseable {
    private val recorder = AudioRecorder(context.applicationContext)
    val isRecording = recorder.isRecording

    fun startListening() = recorder.startRecording()

    /**
     * Stops capture and transcribes offline. The recording is deleted whether or
     * not transcription succeeded; temporary audio is never left on the device.
     */
    suspend fun stopListening(language: SupportedLanguage): String = withContext(Dispatchers.IO) {
        val file = recorder.stopRecording()
        try {
            voiceService.transcribe(file, language)
        } finally {
            file.delete()
        }
    }

    /**
     * Aborts a recording. Stopping joins the capture thread, so it is moved off
     * the caller's thread: this is reached from lifecycle callbacks on the main
     * thread and must not block the UI.
     */
    fun cancel() {
        thread(name = "formsaathi-recorder-cancel", isDaemon = true) {
            recorder.cancel()
        }
    }

    override fun close() {
        cancel()
        (voiceService as? AutoCloseable)?.close()
    }
}
