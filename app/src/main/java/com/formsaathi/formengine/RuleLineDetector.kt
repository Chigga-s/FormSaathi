package com.formsaathi.formengine

import android.graphics.Bitmap
import com.formsaathi.model.NormalizedRect

/**
 * A printed horizontal rule (underline) detected on a rendered page,
 * in normalized 0..1 page coordinates.
 */
data class RuleLine(
    val pageIndex: Int,
    val yTop: Float,
    val yBottom: Float,
    val xStart: Float,
    val xEnd: Float
) {
    val width: Float get() = xEnd - xStart
}

/**
 * Finds the printed underlines that mark where answers go on a form.
 *
 * Pure-pixel scan over a luminance array so the core logic is JVM-testable; only
 * [luminanceOf] touches Android graphics classes. Every qualifying run on a row is
 * kept, so two rules printed side by side (a date line next to a signature line)
 * are both found.
 */
object RuleLineDetector {

    private const val DARK_THRESHOLD = 128
    private const val MIN_RUN_FRACTION = 0.30f
    private const val MAX_THICKNESS_PX = 6

    fun luminanceOf(bitmap: Bitmap): IntArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return IntArray(width * height) { i ->
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }
    }

    fun detect(
        luminance: IntArray,
        width: Int,
        height: Int,
        pageIndex: Int
    ): List<RuleLine> {
        val minRun = (width * MIN_RUN_FRACTION).toInt().coerceAtLeast(1)
        return PageBandScanner.scan(
            luminance = luminance,
            width = width,
            height = height,
            threshold = DARK_THRESHOLD,
            minRun = minRun,
            overlapFraction = 0.5f,
            intersectColumns = false
        )
            // Anything thicker than a printed rule is a filled block or a box
            // border pair, not an underline an answer can sit on.
            .filter { it.height <= MAX_THICKNESS_PX }
            .map { band ->
                RuleLine(
                    pageIndex = pageIndex,
                    yTop = band.top.toFloat() / height,
                    yBottom = band.bottom.toFloat() / height,
                    xStart = band.left.toFloat() / width,
                    xEnd = band.right.toFloat() / width
                )
            }
    }

    fun normalizedBox(rule: RuleLine): NormalizedRect {
        return NormalizedRect(
            left = rule.xStart,
            top = rule.yTop,
            right = rule.xEnd,
            bottom = rule.yBottom
        )
    }

}
