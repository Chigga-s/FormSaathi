package com.formsaathi.voice

import android.content.Context
import com.formsaathi.contracts.VoiceService
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class VoiceException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MissingModel(cause: Throwable? = null) : VoiceException("Offline speech model is unavailable. You can still type your answer.", cause)
    class InvalidAudio : VoiceException("Record mono 16 kHz PCM audio, up to 10 seconds.")
    class NoSpeech : VoiceException("No speech was detected. Please try again or type your answer.")
    class Inference(cause: Throwable? = null) : VoiceException("Offline transcription failed. Please try again or type your answer.", cause)
    class Recording(cause: Throwable? = null) : VoiceException("Unable to record audio. Check microphone permission.", cause)
}

class RealVoiceService internal constructor(
    private val inference: (FloatArray, SupportedLanguage) -> String
) : VoiceService, AutoCloseable {
    private var manager: WhisperManager? = null

    private constructor(context: Context, whisper: WhisperManager) : this({ audio, language ->
        whisper.loadModel(AssetUtils.copyModel(context))
        whisper.transcribe(audio, language)
    }) {
        manager = whisper
    }

    constructor(context: Context) : this(context.applicationContext, WhisperManager())

    override suspend fun transcribe(audioFile: File, language: SupportedLanguage): String = withContext(Dispatchers.IO) {
        val size = audioFile.length()
        if (size !in 2L..320000L || size % 2 != 0L || audioFile.extension != "pcm") {
            throw VoiceException.InvalidAudio()
        }
        val bytes = audioFile.readBytes()
        if (bytes.size.toLong() != size) throw VoiceException.InvalidAudio()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val samples = FloatArray(buffer.remaining()) { buffer.get().toFloat() / 32768f }
        inference(samples, language).trim().ifBlank { throw VoiceException.NoSpeech() }
    }

    override fun close() {
        manager?.release()
    }
}
