package com.formsaathi.formengine

import com.formsaathi.model.NormalizedRect

/**
 * A printed rectangular answer field detected on a rendered page, in normalized
 * 0..1 page coordinates. These are the tinted or outlined boxes that boxed-layout
 * forms print to the right of (or under) their labels.
 */
data class PrintedBox(
    val pageIndex: Int,
    val rect: NormalizedRect
)

/**
 * Finds printed rectangular answer boxes by scanning for tall horizontal bands of
 * non-paper pixels.
 *
 * A form's answer box differs from an underline only by height: an underline is a
 * few pixels thick, a box spans a whole text row or more. Detection therefore uses
 * the same row-run scan as [RuleLineDetector] but with a near-white threshold, so
 * faintly tinted or thin-outlined boxes are found, and keeps only bands that are
 * tall enough to be a box and short enough not to be a page panel.
 *
 * Section headers and title strips are also tinted bands; callers reject those by
 * discarding boxes that contain OCR text (see [AnswerBoxEstimator]).
 *
 * The scan is a pure function over a luminance array so it is testable on the JVM.
 */
object PrintedBoxDetector {

    /** Paper renders as 255; anything below this is printed fill, tint or ink. */
    private const val TINT_THRESHOLD = 252

    private const val MIN_WIDTH_FRACTION = 0.10f
    private const val MIN_HEIGHT_FRACTION = 0.012f

    /**
     * A band no thicker than a printed rule is a rule, whatever the page height.
     * Without this floor, a small rendering of a page turns underlines into boxes.
     */
    private const val MIN_HEIGHT_PX = 8
    private const val MAX_HEIGHT_FRACTION = 0.25f

    /** A row joins the band only if it covers most of the band's current width. */
    private const val ROW_OVERLAP_FRACTION = 0.70f

    fun detect(
        luminance: IntArray,
        width: Int,
        height: Int,
        pageIndex: Int
    ): List<PrintedBox> {
        if (width <= 0 || height <= 0) return emptyList()

        val minRun = (width * MIN_WIDTH_FRACTION).toInt().coerceAtLeast(1)
        val minHeight = maxOf((height * MIN_HEIGHT_FRACTION).toInt(), MIN_HEIGHT_PX)
        val maxHeight = (height * MAX_HEIGHT_FRACTION).toInt().coerceAtLeast(minHeight)

        return PageBandScanner.scan(
            luminance = luminance,
            width = width,
            height = height,
            threshold = TINT_THRESHOLD,
            minRun = minRun,
            overlapFraction = ROW_OVERLAP_FRACTION,
            // Stay inside the printed rectangle even when a row also catches
            // neighbouring ink.
            intersectColumns = true
        )
            .filter { it.height in minHeight..maxHeight && it.width >= minRun }
            .map { band ->
                PrintedBox(
                    pageIndex = pageIndex,
                    rect = NormalizedRect(
                        left = band.left.toFloat() / width,
                        top = band.top.toFloat() / height,
                        right = band.right.toFloat() / width,
                        bottom = band.bottom.toFloat() / height
                    )
                )
            }
    }
}
