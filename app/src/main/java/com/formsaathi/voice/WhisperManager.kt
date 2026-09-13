package com.formsaathi.voice

import com.formsaathi.model.SupportedLanguage

class WhisperManager {
    private var handle = 0L
    private var closed = false

    @Synchronized
    fun loadModel(path: String) {
        check(!closed) { "Voice service is closed" }
        if (handle != 0L) return
        if (!WhisperBridge.isAvailable) throw VoiceException.VoiceNotBuilt()
        try {
            handle = WhisperBridge.loadModel(path)
        } catch (error: LinkageError) {
            throw VoiceException.MissingModel(error)
        }
        if (handle == 0L) throw VoiceException.MissingModel()
    }

    @Synchronized
    fun transcribe(audio: FloatArray, language: SupportedLanguage = SupportedLanguage.ENGLISH): String {
        if (handle == 0L || closed) throw VoiceException.MissingModel()
        try {
            return WhisperBridge.transcribe(handle, audio, language.code)
        } catch (error: IllegalStateException) {
            throw VoiceException.Inference(error)
        }
    }

    @Synchronized
    fun release() {
        if (handle != 0L) WhisperBridge.freeModel(handle)
        handle = 0L
        closed = true
    }
}
