package com.formsaathi.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * High-precision text fitting engine for overlaying user answers onto government form PDFs.
 * Handles dynamic font scaling, multi-line wrapping for addresses, padding, and safe boundary clipping.
 */
class AnswerTextFitter(
    val maxTextSize: Float = 13f,
    val minTextSize: Float = 6.5f,
    val inkColor: Int = Color.rgb(13, 37, 70), // Deep pen-blue for visual distinction
    val typeface: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
) {
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        typeface = this@AnswerTextFitter.typeface
    }

    /**
     * Fits and draws answer text onto the canvas strictly bounded by the destination rectangle.
     * Returns true if fitted cleanly, or false if clipped due to minimum font size limits.
     */
    fun drawFittedText(
        canvas: Canvas,
        text: String,
        bounds: RectF,
        isMultiLine: Boolean = false
    ): Boolean {
        if (text.isBlank() || bounds.width() <= 0 || bounds.height() <= 0) return true

        // 4% safety margin padding
        val padX = bounds.width() * 0.04f
        val padY = bounds.height() * 0.04f
        val innerWidth = (bounds.width() - 2 * padX).coerceAtLeast(1f)
        val innerHeight = (bounds.height() - 2 * padY).coerceAtLeast(1f)

        canvas.save()
        // Strictly clip canvas to prevent overflowing onto adjacent printed form lines
        canvas.clipRect(bounds)

        var fitted = true
        if (isMultiLine) {
            fitted = drawMultiLineText(canvas, text, bounds, padX, padY, innerWidth, innerHeight)
        } else {
            fitted = drawSingleLineText(canvas, text, bounds, padX, padY, innerWidth, innerHeight)
        }

        canvas.restore()
        return fitted
    }

    private fun drawSingleLineText(
        canvas: Canvas,
        text: String,
        bounds: RectF,
        padX: Float,
        padY: Float,
        innerWidth: Float,
        innerHeight: Float
    ): Boolean {
        var currentSize = maxTextSize
        textPaint.textSize = currentSize

        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)

        // Decrement font size until single line fits width and height
        while ((textBounds.width() > innerWidth || textBounds.height() > innerHeight) && currentSize > minTextSize) {
            currentSize -= 0.5f
            textPaint.textSize = currentSize
            textPaint.getTextBounds(text, 0, text.length, textBounds)
        }

        val overflows = textBounds.width() > innerWidth || textBounds.height() > innerHeight

        // Vertical centering inside the answer box
        val textHeight = textBounds.height().toFloat()
        val drawX = bounds.left + padX
        val drawY = bounds.centerY() + (textHeight / 2f) - textBounds.bottom

        canvas.drawText(text, drawX, drawY, textPaint)
        return !overflows
    }

    private fun drawMultiLineText(
        canvas: Canvas,
        text: String,
        bounds: RectF,
        padX: Float,
        padY: Float,
        innerWidth: Float,
        innerHeight: Float
    ): Boolean {
        var currentSize = maxTextSize
        textPaint.textSize = currentSize

        var layout = createStaticLayout(text, textPaint, innerWidth.toInt())

        while (layout.height > innerHeight && currentSize > minTextSize) {
            currentSize -= 0.5f
            textPaint.textSize = currentSize
            layout = createStaticLayout(text, textPaint, innerWidth.toInt())
        }

        val overflows = layout.height > innerHeight

        canvas.save()
        canvas.translate(bounds.left + padX, bounds.top + padY)
        layout.draw(canvas)
        canvas.restore()

        return !overflows
    }

    private fun createStaticLayout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout {
        val safeWidth = width.coerceAtLeast(10)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.0f)
            .setIncludePad(false)
            .build()
    }
}
