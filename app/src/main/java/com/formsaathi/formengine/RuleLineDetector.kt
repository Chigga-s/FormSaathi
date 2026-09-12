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
 * Pure-pixel scan over a luminance array so the core logic is JVM-testable;
 * only [luminanceOf] touches Android graphics classes.
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
        require(luminance.size == width * height) {
            "Luminance size ${luminance.size} != ${width}x$height"
        }
        val minRun = (width * MIN_RUN_FRACTION).toInt().coerceAtLeast(1)
        val rules = mutableListOf<RuleLine>()
        var y = 0
        while (y < height) {
            val run = longestDarkRun(luminance, width, y)
            if (run == null || run.second - run.first < minRun) {
                y++
                continue
            }
            var bandTop = y
            var bandBottom = y
            var bandStart = run.first
            var bandEnd = run.second
            while (bandBottom + 1 < height &&
                bandBottom - bandTop + 1 < MAX_THICKNESS_PX
            ) {
                val next = longestDarkRun(luminance, width, bandBottom + 1)
                if (next == null || next.second - next.first < minRun) break
                val overlapStart = maxOf(bandStart, next.first)
                val overlapEnd = minOf(bandEnd, next.second)
                if (overlapEnd - overlapStart < minRun / 2) break
                bandStart = minOf(bandStart, next.first)
                bandEnd = maxOf(bandEnd, next.second)
                bandBottom++
            }
            rules.add(
                RuleLine(
                    pageIndex = pageIndex,
                    yTop = bandTop.toFloat() / height,
                    yBottom = (bandBottom + 1).toFloat() / height,
                    xStart = bandStart.toFloat() / width,
                    xEnd = (bandEnd + 1).toFloat() / width
                )
            )
            y = bandBottom + 1
        }
        return rules
    }

    fun normalizedBox(rule: RuleLine): NormalizedRect {
        return NormalizedRect(
            left = rule.xStart,
            top = rule.yTop,
            right = rule.xEnd,
            bottom = rule.yBottom
        )
    }

    private fun longestDarkRun(
        luminance: IntArray,
        width: Int,
        y: Int
    ): Pair<Int, Int>? {
        val base = y * width
        var best: Pair<Int, Int>? = null
        var runStart = -1
        for (x in 0..width) {
            val dark = x < width && luminance[base + x] < DARK_THRESHOLD
            if (dark && runStart < 0) {
                runStart = x
            } else if (!dark && runStart >= 0) {
                if (best == null || x - runStart > best.second - best.first) {
                    best = runStart to x
                }
                runStart = -1
            }
        }
        return best
    }
}
