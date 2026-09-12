package com.formsaathi.voice

import android.content.Context
import com.formsaathi.contracts.VoiceService
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RealVoiceService(context: Context) : VoiceService {

    private val whisper = WhisperManager()

    init {
        val modelPath = AssetUtils.copyModel(context)
        whisper.loadModel(modelPath)
    }

    override suspend fun transcribe(audioFile: File, language: SupportedLanguage): String {
        return withContext(Dispatchers.IO) {
            val bytes = audioFile.readBytes()
            
            // Assuming 16-bit PCM little-endian. If WAV, skipping header would be ideal, 
            // but for simplicity we'll decode directly.
            // A more robust implementation would parse the WAV header if present.
            val headerOffset = if (bytes.size > 44 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte()) 44 else 0
            
            val shortCount = (bytes.size - headerOffset) / 2
            val shortArray = ShortArray(shortCount)
            
            ByteBuffer.wrap(bytes, headerOffset, bytes.size - headerOffset)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .get(shortArray)
                
            val floatArray = FloatArray(shortCount)
            for (i in shortArray.indices) {
                floatArray[i] = shortArray[i].toFloat() / 32768.0f
            }
            
            whisper.transcribe(floatArray)
        }
    }
}
