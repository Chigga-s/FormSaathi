package com.formsaathi.voice

import android.content.Context
import com.formsaathi.contracts.VoiceService
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VoiceController(
    context: Context,
    private val voiceService: VoiceService = RealVoiceService(context)
) : AutoCloseable {
    private val recorder = AudioRecorder(context.applicationContext)
    val isRecording = recorder.isRecording

    fun startListening() = recorder.startRecording()

    suspend fun stopListening(language: SupportedLanguage): String = withContext(Dispatchers.IO) {
        val file = recorder.stopRecording()
        try {
            voiceService.transcribe(file, language)
        } finally {
            file.delete()
        }
    }

    fun cancel() = recorder.cancel()

    override fun close() {
        recorder.cancel()
        (voiceService as? AutoCloseable)?.close()
    }
}
