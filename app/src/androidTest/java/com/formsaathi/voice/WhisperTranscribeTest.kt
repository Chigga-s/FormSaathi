package com.formsaathi.voice


import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class WhisperTranscribeTest {


    @Test
    fun transcribeTest() {


        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext


        val modelPath =
            AssetUtils.copyModel(
                context
            )
            
        val file = java.io.File(modelPath)
        println("Model copied to: $modelPath")
        println("Model size on disk: ${file.length()} bytes")

        println(
            "MODEL PATH = $modelPath"
        )


        val whisper =
            WhisperManager()


        whisper.loadModel(
            modelPath
        )


        println(
            "MODEL LOADED"
        )


        val audio =
            FloatArray(16000) {
                (Math.random() * 2.0 - 1.0).toFloat() * 0.1f // small random noise
            }


        val result =
            whisper.transcribe(
                audio
            )


        println(
            "RESULT = $result"
        )


        whisper.release()
    }
}