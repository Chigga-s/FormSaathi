package com.formsaathi.formengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintedBoxDetectorTest {

    private val width = 200
    private val height = 400

    private fun blank(): IntArray = IntArray(width * height) { 255 }

    /** Paints a filled field box: faint tint inside, darker border. */
    private fun drawBox(
        lum: IntArray,
        top: Int,
        bottom: Int,
        left: Int,
        right: Int,
        fill: Int = 250,
        border: Int = 172
    ) {
        for (y in top..bottom) {
            for (x in left..right) {
                val edge = y == top || y == bottom || x == left || x == right
                lum[y * width + x] = if (edge) border else fill
            }
        }
    }

    private fun drawRule(lum: IntArray, y: Int, x0: Int, x1: Int, thickness: Int = 2) {
        for (row in y until y + thickness) {
            for (x in x0..x1) {
                lum[row * width + x] = 0
            }
        }
    }

    @Test
    fun findsAFaintlyTintedFieldBox() {
        val lum = blank()
        drawBox(lum, top = 100, bottom = 140, left = 80, right = 190)

        val boxes = PrintedBoxDetector.detect(lum, width, height, 0)

        assertEquals(1, boxes.size)
        val rect = boxes.first().rect
        assertEquals(80f / width, rect.left, 0.01f)
        assertEquals(191f / width, rect.right, 0.01f)
        assertEquals(100f / height, rect.top, 0.01f)
        assertEquals(141f / height, rect.bottom, 0.01f)
    }

    @Test
    fun underlinesAreNotReportedAsBoxes() {
        val lum = blank()
        drawRule(lum, 50, 60, 190)
        drawRule(lum, 120, 60, 190, thickness = 4)

        val boxes = PrintedBoxDetector.detect(lum, width, height, 0)

        assertTrue("A thin rule is an underline, not a box: $boxes", boxes.isEmpty())
    }

    @Test
    fun separateStackedBoxesAreNotMergedIntoOne() {
        val lum = blank()
        drawBox(lum, top = 60, bottom = 100, left = 80, right = 190)
        drawBox(lum, top = 120, bottom = 160, left = 80, right = 190)

        val boxes = PrintedBoxDetector.detect(lum, width, height, 0)

        assertEquals(2, boxes.size)
        assertTrue("Boxes must not overlap: $boxes", boxes[0].rect.bottom <= boxes[1].rect.top)
    }

    @Test
    fun bodyTextDoesNotProduceBoxes() {
        val lum = blank()
        // Words separated by white space: each dark run is short.
        var x = 10
        while (x < 190) {
            for (y in 200 until 210) {
                for (dx in 0 until 12) lum[y * width + x + dx] = 20
            }
            x += 18
        }

        val boxes = PrintedBoxDetector.detect(lum, width, height, 0)

        assertTrue("Paragraph text must not look like a field box: $boxes", boxes.isEmpty())
    }

    @Test
    fun fullPagePanelsAreRejectedAsTooTall() {
        val lum = blank()
        drawBox(lum, top = 0, bottom = 300, left = 0, right = 199)

        val boxes = PrintedBoxDetector.detect(lum, width, height, 0)

        assertTrue("A page-height panel is not a field: $boxes", boxes.isEmpty())
    }

    @Test
    fun narrowBoxesAreRejected() {
        val lum = blank()
        drawBox(lum, top = 100, bottom = 140, left = 180, right = 195)

        val boxes = PrintedBoxDetector.detect(lum, width, height, 0)

        assertTrue("A box narrower than a usable answer area is ignored: $boxes", boxes.isEmpty())
    }

    @Test
    fun pageIndexIsCarriedThrough() {
        val lum = blank()
        drawBox(lum, top = 100, bottom = 140, left = 80, right = 190)

        val boxes = PrintedBoxDetector.detect(lum, width, height, 3)

        assertEquals(3, boxes.single().pageIndex)
    }
}
