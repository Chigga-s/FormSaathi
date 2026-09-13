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
    class VoiceNotBuilt : VoiceException("Offline voice input is not included in this build. Please type your answer.")
    class InvalidAudio : VoiceException("Record mono 16 kHz PCM audio, up to 10 seconds.")
    class NoSpeech : VoiceException("No speech detected — try again or type your answer.")
    class MicrophoneSilenced : VoiceException(
        "Your phone is blocking the microphone for this app, so nothing was recorded. " +
            "Check the microphone permission and privacy settings, or type your answer."
    )
    class MicrophoneBusy : VoiceException(
        "A call or another app is using the microphone, so nothing was recorded. " +
            "End it and try again, or type your answer."
    )
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
        val text = inference(samples, language).trim()
        // Whisper emits the literal "[BLANK_AUDIO]" token when the recording
        // contains no speech. Accepting it as an answer fills forms with junk,
        // so it is treated exactly like an empty transcript.
        if (text.isBlank() || text.equals("[BLANK_AUDIO]", ignoreCase = true)) {
            throw VoiceException.NoSpeech()
        }
        text
    }

    override fun close() {
        manager?.release()
    }
}
