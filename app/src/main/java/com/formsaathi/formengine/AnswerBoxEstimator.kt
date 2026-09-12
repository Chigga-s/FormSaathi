package com.formsaathi.formengine

import com.formsaathi.model.NormalizedRect

class AnswerBoxEstimator {
    private companion object {
        const val HORIZONTAL_PADDING = 0.015f
        const val RIGHT_PAGE_MARGIN = 0.95f
        
        const val VERTICAL_PADDING = 0.01f
        const val BELOW_ANSWER_HEIGHT = 0.06f
        
        const val MIN_ANSWER_WIDTH = 0.12f
        const val MIN_ANSWER_HEIGHT = 0.025f
    }
    private fun intersects(
        first: NormalizedRect,
        second: NormalizedRect
    ): Boolean {
        if (first.right <= second.left || first.left >= second.right) {
            return false
        }
        if (first.bottom <= second.top || first.top >= second.bottom) {
            return false
        }
        return true
    }  
    private fun estimateRightOfLabel(
        label: NormalizedRect
    ): NormalizedRect {
        return NormalizedRect(
        left = label.right + HORIZONTAL_PADDING,
        top = label.top,
        right = RIGHT_PAGE_MARGIN,
        bottom = label.bottom
        )
    }
    private fun estimateBelowLabel(
    label: NormalizedRect
    ): NormalizedRect {
        val top = label.bottom + VERTICAL_PADDING
        return NormalizedRect(
            left = label.left,
            top = label.bottom + VERTICAL_PADDING,
            right = RIGHT_PAGE_MARGIN,
            bottom = top + BELOW_ANSWER_HEIGHT
        )
    }
    private fun NormalizedRect.clamped(): NormalizedRect {
        return NormalizedRect(
            left = left.coerceIn(0f, 1f),
            top = top.coerceIn(0f, 1f),
            right = right.coerceIn(0f, 1f),
            bottom = bottom.coerceIn(0f, 1f)
        )
    }
    private fun hasUsableSize(
    rect: NormalizedRect
    ): Boolean {
        val width = rect.right - rect.left
        val height = rect.bottom - rect.top
        return width >= MIN_ANSWER_WIDTH && height >= MIN_ANSWER_HEIGHT
    }
    private fun collidesWithText(
    candidate: NormalizedRect,
    label: OcrBlock,
    allBlocks: List<OcrBlock>
    ): Boolean {
        for (block in allBlocks) {
            if (block.pageIndex != label.pageIndex) {
                continue
            }
            if (block.blockIndex == label.blockIndex && block.lineIndex == label.lineIndex) {
                continue
            }
            if (intersects(candidate, block.box)) {
                return true
            }
        }
        return false
    }
    fun estimate(
        label: OcrBlock,
        allBlocks: List<OcrBlock>
    ): NormalizedRect? {
        val rightCandidate = estimateRightOfLabel(label.box).clamped()
        if (hasUsableSize(rightCandidate) && !collidesWithText(rightCandidate, label, allBlocks)) {
            return rightCandidate
        }
        
        val belowCandidate = estimateBelowLabel(label.box).clamped()
        if (hasUsableSize(belowCandidate) && !collidesWithText(belowCandidate, label, allBlocks)) {
            return belowCandidate
        }
        return null
    }
}