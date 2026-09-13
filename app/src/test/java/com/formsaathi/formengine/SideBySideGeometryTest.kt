package com.formsaathi.formengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Two fields printed on the same row must both be found. Keeping only the longest
 * run per scan line silently dropped the second one, which is how a signature line
 * printed next to a date line lost its underline and had its answer guessed into
 * the page margin instead.
 */
class SideBySideGeometryTest {

    private val width = 300
    private val height = 400

    private fun blank() = IntArray(width * height) { 255 }

    @Test
    fun twoUnderlinesOnTheSameRowAreBothDetected() {
        val lum = blank()
        for (y in 300 until 302) {
            for (x in 20 until 130) lum[y * width + x] = 0
            for (x in 170 until 280) lum[y * width + x] = 0
        }

        val rules = RuleLineDetector.detect(lum, width, height, 0)

        assertEquals("Both footer rules must be found: $rules", 2, rules.size)
        assertEquals(20f / width, rules[0].xStart, 0.01f)
        assertEquals(170f / width, rules[1].xStart, 0.01f)
    }

    @Test
    fun twoFieldBoxesOnTheSameRowAreBothDetected() {
        val lum = blank()
        for (y in 100 until 140) {
            for (x in 20 until 130) lum[y * width + x] = 249
            for (x in 170 until 280) lum[y * width + x] = 249
        }

        val boxes = PrintedBoxDetector.detect(lum, width, height, 0)

        assertEquals("Both boxes on the row must be found: $boxes", 2, boxes.size)
        assertTrue(boxes[0].rect.right <= boxes[1].rect.left)
    }

    @Test
    fun aSolidDarkBlockIsNotMistakenForAnUnderline() {
        val lum = blank()
        for (y in 100 until 160) {
            for (x in 20 until 280) lum[y * width + x] = 0
        }

        val rules = RuleLineDetector.detect(lum, width, height, 0)

        assertTrue("A filled block is not a rule: $rules", rules.isEmpty())
    }
}
