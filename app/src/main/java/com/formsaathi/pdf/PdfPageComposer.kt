package com.formsaathi.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect
import com.formsaathi.model.TextFitWarning

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
 * 4. Collects and returns warnings for any answers that could not fit cleanly.
 */
class PdfPageComposer(
    private val textFitterProvider: () -> AnswerTextFitter = { AnswerTextFitter() },
    private val imageLoader: (String) -> Bitmap? = { null }
) {
    private val textFitter by lazy { textFitterProvider() }
    private val bitmapPaint by lazy { Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG) }

    /**
     * Composes one page of the PDF onto the provided PdfDocument Canvas.
     * @return List of warnings for fields where answer text was clipped at minimum font size.
     */
    fun composePage(
        canvas: Canvas,
        pageBitmap: Bitmap,
        pageWidthPoints: Float,
        pageHeightPoints: Float,
        fieldsOnPage: List<FormField>,
        answers: Map<String, FormAnswer>
    ): List<TextFitWarning> {
        val warnings = mutableListOf<TextFitWarning>()

        // 1. Draw source page bitmap scaled to PDF page dimensions
        val srcRect = Rect(0, 0, pageBitmap.width, pageBitmap.height)
        val dstRect = RectF(0f, 0f, pageWidthPoints, pageHeightPoints)
        canvas.drawBitmap(pageBitmap, srcRect, dstRect, bitmapPaint)

        // 2. Overlay answers for fields assigned to this page
        for (field in fieldsOnPage) {
            val answer = answers[field.id] ?: continue

            val answerBounds = toCanvasCoordinates(
                box = field.answerBox,
                pageWidth = pageWidthPoints,
                pageHeight = pageHeightPoints
            )

            if (answer.source == AnswerSource.PHOTO) {
                drawPhotoAnswer(canvas, field, answer, answerBounds, warnings)
                continue
            }

            val textToDraw = answer.normalizedValue.ifBlank { answer.rawValue }
            if (textToDraw.isBlank()) continue

            val isMultiLine = isMultiLineField(field)
            val fittedCleanly = textFitter.drawFittedText(
                canvas = canvas,
                text = textToDraw,
                bounds = answerBounds.toAndroidRectF(),
                isMultiLine = isMultiLine
            )

            if (!fittedCleanly) {
                warnings.add(
                    TextFitWarning(
                        fieldId = field.id,
                        fieldLabel = field.sourceLabel,
                        reason = "Answer text was clipped to fit within the answer box at minimum font size"
                    )
                )
            }
        }

        return warnings
    }

    private fun drawPhotoAnswer(
        canvas: Canvas,
        field: FormField,
        answer: FormAnswer,
        answerBounds: CanvasRect,
        warnings: MutableList<TextFitWarning>
    ) {
        val path = answer.normalizedValue.ifBlank { answer.rawValue }
        val photo = path.takeIf { it.isNotBlank() }?.let { imageLoader(it) }
        if (photo == null) {
            warnings.add(
                TextFitWarning(
                    fieldId = field.id,
                    fieldLabel = field.sourceLabel,
                    reason = "Attached photo could not be embedded in the PDF"
                )
            )
            return
        }
        // Fit inside the answer box preserving aspect ratio, aligned left.
        val scale = minOf(
            answerBounds.width / photo.width,
            answerBounds.height / photo.height
        ).coerceAtMost(1f)
        val drawWidth = photo.width * scale
        val drawHeight = photo.height * scale
        val dst = RectF(
            answerBounds.left,
            answerBounds.bottom - drawHeight,
            answerBounds.left + drawWidth,
            answerBounds.bottom
        )
        canvas.save()
        canvas.clipRect(answerBounds.toAndroidRectF())
        canvas.drawBitmap(photo, null, dst, bitmapPaint)
        canvas.restore()
        // imageLoader hands over a freshly decoded bitmap per call.
        photo.recycle()
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
        return when (field.type) {
            // Addresses wrap whatever the printed box looks like.
            FieldType.PERMANENT_ADDRESS, FieldType.CURRENT_ADDRESS -> true

            // Every other canonical field holds one short value. A printed box
            // that happens to be tall is still a single-line field, and wrapping
            // a phone number across two lines looks wrong on the form.
            FieldType.UNKNOWN -> {
                if (field.answerBox.height >= MULTI_LINE_HEIGHT) return true
                val aspectRatio = field.answerBox.height / field.answerBox.width.coerceAtLeast(0.01f)
                aspectRatio > 0.35f
            }

            else -> false
        }
    }

    private companion object {
        /** Roughly two lines of body text on an A4 page. */
        const val MULTI_LINE_HEIGHT = 0.035f
    }
}
