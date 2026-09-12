package com.formsaathi.formengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleLineDetectorTest {

    private val width = 100
    private val height = 60

    private fun blank(): IntArray = IntArray(width * height) { 255 }

    private fun drawRule(lum: IntArray, y: Int, x0: Int, x1: Int, thickness: Int = 2) {
        for (row in y until y + thickness) {
            for (x in x0..x1) {
                lum[row * width + x] = 0
            }
        }
    }

    @Test
    fun detectsSingleUnderline() {
        val lum = blank()
        drawRule(lum, 20, 10, 90)
        val rules = RuleLineDetector.detect(lum, width, height, 0)
        assertEquals(1, rules.size)
        val rule = rules.first()
        assertEquals(0.10f, rule.xStart, 0.02f)
        assertEquals(0.91f, rule.xEnd, 0.02f)
        assertEquals(20f / height, rule.yTop, 0.001f)
    }

    @Test
    fun groupsBandThicknessIntoOneRule() {
        val lum = blank()
        drawRule(lum, 30, 5, 95, thickness = 3)
        val rules = RuleLineDetector.detect(lum, width, height, 0)
        assertEquals(1, rules.size)
        assertEquals(30f / height, rules.first().yTop, 0.001f)
        assertEquals(33f / height, rules.first().yBottom, 0.001f)
    }

    @Test
    fun ignoresShortTextLikeRuns() {
        val lum = blank()
        drawRule(lum, 10, 10, 25)
        drawRule(lum, 40, 60, 80, thickness = 1)
        val rules = RuleLineDetector.detect(lum, width, height, 0)
        assertTrue("Short runs must not be rules: $rules", rules.isEmpty())
    }

    @Test
    fun detectsMultipleRules() {
        val lum = blank()
        drawRule(lum, 10, 0, 99)
        drawRule(lum, 40, 0, 99)
        val rules = RuleLineDetector.detect(lum, width, height, 0)
        assertEquals(2, rules.size)
    }

    @Test
    fun estimatorSnapsBoxOntoRule() {
        val label = OcrBlock(
            text = "Full Name:",
            pageIndex = 0,
            box = com.formsaathi.model.NormalizedRect(0.10f, 0.14f, 0.20f, 0.15f),
            confidence = 0.9f,
            blockIndex = 0,
            lineIndex = 0
        )
        val rule = RuleLine(
            pageIndex = 0, yTop = 0.152f, yBottom = 0.154f, xStart = 0.10f, xEnd = 0.90f
        )
        val box = requireNotNull(
            AnswerBoxEstimator().estimate(label, listOf(label), listOf(rule))
        )
        assertTrue("Box must sit on the rule: $box", box.top < rule.yTop && box.bottom > rule.yTop)
        assertEquals(0.215f, box.left, 0.001f)
        assertEquals(0.90f, box.right, 0.001f)
    }

    @Test
    fun ruleBelowNextTextLineIsIgnored() {
        val label = OcrBlock(
            text = "Full Name:",
            pageIndex = 0,
            box = com.formsaathi.model.NormalizedRect(0.10f, 0.14f, 0.20f, 0.15f),
            confidence = 0.9f,
            blockIndex = 0,
            lineIndex = 0
        )
        val next = OcrBlock(
            text = "Next:",
            pageIndex = 0,
            box = com.formsaathi.model.NormalizedRect(0.10f, 0.18f, 0.20f, 0.19f),
            confidence = 0.9f,
            blockIndex = 1,
            lineIndex = 0
        )
        // Rule belongs to the next row, not this label.
        val rule = RuleLine(
            pageIndex = 0, yTop = 0.192f, yBottom = 0.194f, xStart = 0.10f, xEnd = 0.90f
        )
        val box = requireNotNull(
            AnswerBoxEstimator().estimate(label, listOf(label, next), listOf(rule))
        )
        // Must fall back to the right-of-label box, not the other row's rule.
        assertEquals(0.14f, box.top, 0.0001f)
    }
}
