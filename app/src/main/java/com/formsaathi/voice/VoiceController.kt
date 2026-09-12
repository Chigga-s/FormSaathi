package com.formsaathi.voice

import android.content.Context


class VoiceController(
    context: Context
) {


    private val recorder =
        AudioRecorder()


    private val whisper =
        WhisperManager()



    init {

        val modelPath =
            AssetUtils.copyModel(
                context
            )


        whisper.loadModel(
            modelPath
        )
    }



    fun startListening() {

        recorder.startRecording()

    }



    fun stopListening(
        callback: (String) -> Unit
    ) {


        val audio =
            recorder.stopRecording()



        Thread {


            val text =
                whisper.transcribe(
                    audio
                )


            callback(
                text
            )

        }.start()

    }



    fun release() {

        whisper.release()

    }

}