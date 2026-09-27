package com.formsaathi.formengine

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

enum class OcrScript {
    LATIN,
    DEVANAGARI
}

class MlKitOcrEngine : AutoCloseable {
    private val latinRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )   
    private val devanagariRecognizer = TextRecognition.getClient(
        DevanagariTextRecognizerOptions.Builder().build()
    )

    override fun close() {
        latinRecognizer.close()
        devanagariRecognizer.close()
    }

    suspend fun recognize(
        bitmap: Bitmap,
        pageIndex: Int,
        script: OcrScript
    ): List<OcrBlock> {
        val image = InputImage.fromBitmap(bitmap,0)
        val recognizer = when (script) {
            OcrScript.LATIN -> latinRecognizer
            OcrScript.DEVANAGARI -> devanagariRecognizer
        }
        val result = recognizer.process(image).await()
        val output = mutableListOf<OcrBlock>()
        result.textBlocks.forEachIndexed {blockIndex, block->
            block.lines.forEachIndexed lineLoop@ { lineIndex, line->
                val rect = line.boundingBox ?: return@lineLoop
                val normalizedBox = rect.toNormalizedRect(bitmap.width, bitmap.height)
                val ocrBlock = OcrBlock(
                    text = line.text,
                    pageIndex = pageIndex,
                    box = normalizedBox,
                    confidence = line.confidence,
                    blockIndex = blockIndex,
                    lineIndex = lineIndex
                )
                output.add(ocrBlock)    
            }
        }
        return output
    } 
}
