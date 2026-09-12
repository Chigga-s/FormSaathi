package com.formsaathi.voice


class WhisperManager {


    private var handle: Long = 0


    fun loadModel(path: String) {

        println("================================")
        println("Loading Whisper model")
        println("PATH = $path")

        val file =
            java.io.File(path)

        println("FILE EXISTS = ${file.exists()}")
        println("FILE SIZE = ${file.length()} bytes")


        handle =
            WhisperBridge.loadModel(
                path
            )


        println("NATIVE HANDLE = $handle")


        if (handle == 0L) {

            throw IllegalStateException(
                "Whisper model loading failed"
            )
        }

        println("MODEL LOADED SUCCESSFULLY")
        println("================================")
    }



    fun transcribe(
        audio: FloatArray
    ): String {


        if (handle == 0L) {

            throw IllegalStateException(
                "Model not loaded"
            )
        }


        return WhisperBridge.transcribe(
            handle,
            audio
        )
    }



    fun release() {

        if (handle != 0L) {

            WhisperBridge.freeModel(
                handle
            )

            handle = 0

        }
    }
}