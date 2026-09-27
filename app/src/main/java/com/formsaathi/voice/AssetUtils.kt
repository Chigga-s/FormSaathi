package com.formsaathi.voice

import android.content.Context
import java.io.File
import java.io.IOException

object AssetUtils {
    @Synchronized
    fun copyModel(context: Context): String {
        val name = "models/whisper-tiny-multilingual-q5.bin"
        val output = File(context.filesDir, name)
        if (output.isFile && output.length() > 0L) return output.absolutePath
        val temporary = File(output.path + ".partial")
        try {
            output.parentFile?.mkdirs()
            context.assets.open(name).use { input ->
                temporary.outputStream().use { input.copyTo(it) }
            }
            if (temporary.length() == 0L || !temporary.renameTo(output)) {
                throw IOException("Unable to install offline speech model")
            }
            return output.absolutePath
        } catch (error: IOException) {
            throw VoiceException.MissingModel(error)
        } finally {
            temporary.delete()
        }
    }
}
