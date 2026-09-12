package com.formsaathi.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread


class AudioRecorder {


    private val sampleRate = 16000


    private var recorder: AudioRecord? = null


    private var recording = false


    private val audioData =
        mutableListOf<Short>()



    fun startRecording() {


        val bufferSize =
            AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )


        recorder =
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )


        audioData.clear()


        recording = true


        recorder?.startRecording()



        thread {


            val buffer =
                ShortArray(bufferSize)



            while(recording) {


                val read =
                    recorder?.read(
                        buffer,
                        0,
                        buffer.size
                    ) ?: 0



                for(i in 0 until read) {

                    audioData.add(
                        buffer[i]
                    )

                }
            }

        }
    }



    fun stopRecording(): FloatArray {


        recording = false


        recorder?.stop()


        recorder?.release()


        recorder = null



        return audioData.map {

            it.toFloat() / 32768f

        }.toFloatArray()

    }
}