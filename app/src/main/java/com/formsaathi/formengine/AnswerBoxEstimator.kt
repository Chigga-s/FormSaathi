package com.formsaathi.formengine

import com.formsaathi.model.NormalizedRect

class AnswerBoxEstimator {

    private companion object {
        const val HORIZONTAL_PADDING = 0.015f
        const val RIGHT_PAGE_MARGIN = 0.95f

        const val VERTICAL_PADDING = 0.01f
        const val BELOW_ANSWER_HEIGHT = 0.06f

        // Height of the answer box drawn over a printed underline: one text
        // line whose baseline lands on the rule. Taller boxes push the
        // fitted text above the line and bleed into the next row.
        const val RULE_ANSWER_HEIGHT = 0.022f
        const val RULE_SEARCH_BELOW = 0.09f
        const val RULE_SEARCH_ABOVE = 0.01f

        const val MIN_ANSWER_WIDTH = 0.12f

        // A single-line answer area only needs to be as tall as the label line
        // itself (~0.009 of page height). The previous 0.01 minimum was a hair
        // taller than one OCR line, so right-of-label candidates were rejected
        // and the below-label fallback collided with the next stacked line.
        const val MIN_ANSWER_HEIGHT = 0.005f
    }

    private fun intersects(
        first: NormalizedRect,
        second: NormalizedRect
    ): Boolean {
        if (
            first.right <= second.left ||
            first.left >= second.right
        ) {
            return false
        }

        if (
            first.bottom <= second.top ||
            first.top >= second.bottom
        ) {
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
            top = top,
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

        return width >= MIN_ANSWER_WIDTH &&
            height >= MIN_ANSWER_HEIGHT
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

            if (
                block.blockIndex == label.blockIndex &&
                block.lineIndex == label.lineIndex
            ) {
                continue
            }

            if (intersects(candidate, block.box)) {
                return true
            }
        }

        return false
    }

    private fun estimateOnRule(
        label: OcrBlock,
        allBlocks: List<OcrBlock>,
        rules: List<RuleLine>
    ): NormalizedRect? {
        if (rules.isEmpty()) return null

        val nextTextTop = allBlocks
            .asSequence()
            .filter { it.pageIndex == label.pageIndex }
            .filterNot { it.blockIndex == label.blockIndex && it.lineIndex == label.lineIndex }
            .map { it.box.top }
            .filter { it >= label.box.bottom }
            .minOrNull()

        val rule = rules
            .asSequence()
            .filter { it.pageIndex == label.pageIndex }
            .filter { it.yBottom >= label.box.top - RULE_SEARCH_ABOVE }
            .filter { it.yTop <= label.box.bottom + RULE_SEARCH_BELOW }
            .filter { it.xEnd > label.box.right }
            .filter { nextTextTop == null || it.yTop < nextTextTop }
            .minByOrNull {
                kotlin.math.abs((it.yTop + it.yBottom) / 2 - label.box.bottom)
            } ?: return null

        val left = maxOf(label.box.right + HORIZONTAL_PADDING, rule.xStart)
        val right = minOf(rule.xEnd, RIGHT_PAGE_MARGIN + 0.03f)
        if (right - left < MIN_ANSWER_WIDTH) return null

        return NormalizedRect(
            left = left,
            top = rule.yTop - RULE_ANSWER_HEIGHT,
            right = right,
            bottom = rule.yBottom + 0.002f
        )
    }

    fun estimate(
        label: OcrBlock,
        allBlocks: List<OcrBlock>,
        rules: List<RuleLine> = emptyList()
    ): NormalizedRect? {

        val ruleCandidate = estimateOnRule(label, allBlocks, rules)?.clamped()

        if (
            ruleCandidate != null &&
            hasUsableSize(ruleCandidate) &&
            !collidesWithText(ruleCandidate, label, allBlocks)
        ) {
            return ruleCandidate
        }

        val rightCandidate =
            estimateRightOfLabel(label.box).clamped()

        if (
            hasUsableSize(rightCandidate) &&
            !collidesWithText(
                rightCandidate,
                label,
                allBlocks
            )
        ) {
            return rightCandidate
        }

        val belowCandidate =
            estimateBelowLabel(label.box).clamped()

        if (
            hasUsableSize(belowCandidate) &&
            !collidesWithText(
                belowCandidate,
                label,
                allBlocks
            )
        ) {
            return belowCandidate
        }

        return null
    }
}