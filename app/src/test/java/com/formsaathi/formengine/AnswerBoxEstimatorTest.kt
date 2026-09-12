package com.formsaathi.formengine

import org.junit.Assert.assertNull
import com.formsaathi.model.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerBoxEstimatorTest {

    @Test
    fun clearRightSide_returnsRightCandidate() {
        val label = OcrBlock(
            text = "Full Name",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.30f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val estimator = AnswerBoxEstimator()

        val result = estimator.estimate(
            label = label,
            allBlocks = listOf(label)
        )

        val box = requireNotNull(result)

        assertEquals(0.315f, box.left, 0.0001f)
        assertEquals(0.20f, box.top, 0.0001f)
        assertEquals(0.95f, box.right, 0.0001f)
        assertEquals(0.24f, box.bottom, 0.0001f)
    }
    
    @Test
    fun blockedRightSide_returnsBelowCandidate() {
        val label = OcrBlock(
            text = "Full Name",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.30f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val blocker = OcrBlock(
            text = "Some nearby text",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.40f,
                top = 0.20f,
                right = 0.60f,
                bottom = 0.24f
            ),
            confidence = 0.90f,
            blockIndex = 1,
            lineIndex = 0
        )

        val estimator = AnswerBoxEstimator()

        val result = estimator.estimate(
            label = label,
            allBlocks = listOf(label, blocker)
        )

        val box = requireNotNull(result)

        assertEquals(0.10f, box.left, 0.0001f)
        assertEquals(0.25f, box.top, 0.0001f)
        assertEquals(0.95f, box.right, 0.0001f)
        assertEquals(0.31f, box.bottom, 0.0001f)
    }
    
    @Test
    fun blockedRightAndBelow_returnsNull() {
        val label = OcrBlock(
            text = "Full Name",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.30f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val rightBlocker = OcrBlock(
            text = "Nearby text",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.40f,
                top = 0.20f,
                right = 0.60f,
                bottom = 0.24f
            ),
            confidence = 0.90f,
            blockIndex = 1,
            lineIndex = 0
        )

        val belowBlocker = OcrBlock(
            text = "More text",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.20f,
                top = 0.26f,
                right = 0.50f,
                bottom = 0.30f
            ),
            confidence = 0.90f,
            blockIndex = 2,
            lineIndex = 0
        )

        val estimator = AnswerBoxEstimator()

        val result = estimator.estimate(
            label = label,
            allBlocks = listOf(
                label,
                rightBlocker,
                belowBlocker
            )
        )

        assertEquals(null, result)
    }
    
    @Test
    fun blockerOnDifferentPage_isIgnored() {
        val label = OcrBlock(
            text = "Full Name",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.20f,
                right = 0.30f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val otherPageBlocker = OcrBlock(
            text = "Text on another page",
            pageIndex = 1,
            box = NormalizedRect(
                left = 0.40f,
                top = 0.20f,
                right = 0.60f,
                bottom = 0.24f
            ),
            confidence = 0.90f,
            blockIndex = 1,
            lineIndex = 0
        )

        val estimator = AnswerBoxEstimator()

        val result = estimator.estimate(
            label = label,
            allBlocks = listOf(label, otherPageBlocker)
        )

        val box = requireNotNull(result)

        assertEquals(0.315f, box.left, 0.0001f)
        assertEquals(0.20f, box.top, 0.0001f)
        assertEquals(0.95f, box.right, 0.0001f)
        assertEquals(0.24f, box.bottom, 0.0001f)
    }
    
    @Test
    fun rightCandidateTooNarrow_returnsBelowCandidate() {
        val label = OcrBlock(
            text = "Full Name",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.70f,
                top = 0.20f,
                right = 0.90f,
                bottom = 0.24f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val estimator = AnswerBoxEstimator()

        val result = estimator.estimate(
            label = label,
            allBlocks = listOf(label)
        )

        val box = requireNotNull(result)

        assertEquals(0.70f, box.left, 0.0001f)
        assertEquals(0.25f, box.top, 0.0001f)
        assertEquals(0.95f, box.right, 0.0001f)
        assertEquals(0.31f, box.bottom, 0.0001f)
    }
    
    @Test
    fun belowCandidatePastPageBottom_isClamped() {
        val label = OcrBlock(
            text = "Full Name",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.10f,
                top = 0.92f,
                right = 0.30f,
                bottom = 0.96f
            ),
            confidence = 0.95f,
            blockIndex = 0,
            lineIndex = 0
        )

        val rightBlocker = OcrBlock(
            text = "Nearby text",
            pageIndex = 0,
            box = NormalizedRect(
                left = 0.40f,
                top = 0.92f,
                right = 0.60f,
                bottom = 0.96f
            ),
            confidence = 0.90f,
            blockIndex = 1,
            lineIndex = 0
        )

        val estimator = AnswerBoxEstimator()

        val result = estimator.estimate(
            label = label,
            allBlocks = listOf(label, rightBlocker)
        )

        val box = requireNotNull(result)

        assertEquals(0.10f, box.left, 0.0001f)
        assertEquals(0.97f, box.top, 0.0001f)
        assertEquals(0.95f, box.right, 0.0001f)
        assertEquals(1.00f, box.bottom, 0.0001f)
    }
}
