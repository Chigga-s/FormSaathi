package com.formsaathi.formengine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect

class DebugOverlayRenderer {

    private companion object {
        const val STROKE_WIDTH = 4f
        const val TEXT_SIZE = 28f
    }

    fun draw(
        source: Bitmap,
        fields: List<FormField>,
        pageIndex: Int
    ): Bitmap {

        val output = source.copy(
            Bitmap.Config.ARGB_8888,
            true
        )

        val canvas = Canvas(output)

        val labelPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            style = Paint.Style.STROKE
            strokeWidth = STROKE_WIDTH
            color = Color.RED
        }

        val answerPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            style = Paint.Style.STROKE
            strokeWidth = STROKE_WIDTH
            color = Color.BLUE
        }

        val textPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            style = Paint.Style.FILL
            textSize = TEXT_SIZE
            color = Color.MAGENTA
        }

        fields
            .filter { it.pageIndex == pageIndex }
            .forEach { field ->

                val labelRect = field.labelBox.toPixelRect(
                    bitmapWidth = output.width,
                    bitmapHeight = output.height
                )

                val answerRect = field.answerBox.toPixelRect(
                    bitmapWidth = output.width,
                    bitmapHeight = output.height
                )

                canvas.drawRect(
                    labelRect,
                    labelPaint
                )

                canvas.drawRect(
                    answerRect,
                    answerPaint
                )

                val confidencePercent =
                    (field.confidence * 100f).toInt()

                val debugText =
                    "${field.type.name} ${confidencePercent}%"

                canvas.drawText(
                    debugText,
                    labelRect.left,
                    (labelRect.top - 8f).coerceAtLeast(TEXT_SIZE),
                    textPaint
                )
            }

        return output
    }

    private fun NormalizedRect.toPixelRect(
        bitmapWidth: Int,
        bitmapHeight: Int
    ): RectF {

        return RectF(
            left * bitmapWidth,
            top * bitmapHeight,
            right * bitmapWidth,
            bottom * bitmapHeight
        )
    }
}