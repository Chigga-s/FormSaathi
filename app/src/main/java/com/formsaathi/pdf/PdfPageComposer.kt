package com.formsaathi.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect

/**
 * Coordinate rectangle in PDF canvas points.
 * Pure Kotlin data structure enabling fast, zero-mock unit testing on the desktop JVM.
 */
data class CanvasRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun toAndroidRectF(): RectF = RectF(left, top, right, bottom)
}

/**
 * Orchestrates composition of a single PDF page:
 * 1. Draws the high-resolution source form page bitmap as the background.
 * 2. Maps normalized answer bounding boxes (0.0..1.0) into output PDF point coordinates.
 * 3. Overlays user answers with high-precision text fitting and clipping.
 */
class PdfPageComposer(
    private val textFitterProvider: () -> AnswerTextFitter = { AnswerTextFitter() }
) {
    private val textFitter by lazy { textFitterProvider() }
    private val bitmapPaint by lazy { Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG) }

    /**
     * Composes one page of the PDF onto the provided PdfDocument Canvas.
     */
    fun composePage(
        canvas: Canvas,
        pageBitmap: Bitmap,
        pageWidthPoints: Float,
        pageHeightPoints: Float,
        fieldsOnPage: List<FormField>,
        answers: Map<String, FormAnswer>
    ) {
        // 1. Draw source page bitmap scaled to PDF page dimensions
        val srcRect = Rect(0, 0, pageBitmap.width, pageBitmap.height)
        val dstRect = RectF(0f, 0f, pageWidthPoints, pageHeightPoints)
        canvas.drawBitmap(pageBitmap, srcRect, dstRect, bitmapPaint)

        // 2. Overlay answers for fields assigned to this page
        for (field in fieldsOnPage) {
            val answer = answers[field.id] ?: continue
            val textToDraw = answer.normalizedValue.ifBlank { answer.rawValue }
            if (textToDraw.isBlank()) continue

            val answerBounds = toCanvasCoordinates(
                box = field.answerBox,
                pageWidth = pageWidthPoints,
                pageHeight = pageHeightPoints
            )

            val isMultiLine = isMultiLineField(field)
            textFitter.drawFittedText(
                canvas = canvas,
                text = textToDraw,
                bounds = answerBounds.toAndroidRectF(),
                isMultiLine = isMultiLine
            )
        }
    }

    /**
     * Transforms normalized coordinates (0.0..1.0) to PDF output canvas points.
     */
    fun toCanvasCoordinates(
        box: NormalizedRect,
        pageWidth: Float,
        pageHeight: Float
    ): CanvasRect {
        val left = box.left * pageWidth
        val top = box.top * pageHeight
        val right = box.right * pageWidth
        val bottom = box.bottom * pageHeight
        return CanvasRect(left, top, right, bottom)
    }

    /**
     * Determines whether a field should allow multi-line text wrapping.
     */
    fun isMultiLineField(field: FormField): Boolean {
        if (field.type == FieldType.PERMANENT_ADDRESS || field.type == FieldType.CURRENT_ADDRESS) {
            return true
        }
        // If the box height is tall relative to its width, treat as multi-line
        val aspectRatio = field.answerBox.height / field.answerBox.width.coerceAtLeast(0.01f)
        return aspectRatio > 0.35f
    }
}
