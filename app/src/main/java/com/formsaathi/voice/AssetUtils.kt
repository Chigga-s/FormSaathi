package com.formsaathi.voice

import android.content.Context
import java.io.File


object AssetUtils {


    fun copyModel(
        context: Context
    ): String {


        val modelName =
            "models/whisper-tiny-multilingual-q5.bin"


        val outputFile =
            File(
                context.filesDir,
                modelName
            )


        if (!outputFile.exists()) {

            outputFile.parentFile?.mkdirs()


            context.assets
                .open(modelName)
                .use { input ->

                    outputFile
                        .outputStream()
                        .use { output ->

                            input.copyTo(output)

                        }
                }
        }


        return outputFile.absolutePath
    }
}