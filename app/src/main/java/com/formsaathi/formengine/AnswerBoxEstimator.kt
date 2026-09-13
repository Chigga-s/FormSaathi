package com.formsaathi.formengine

import com.formsaathi.model.FieldType
import com.formsaathi.model.NormalizedRect

/**
 * How an answer rectangle was established. The parser keeps this so the review
 * screen can flag fields whose answer position was guessed rather than read off
 * the printed form.
 */
enum class AnswerAreaSource {
    /** A printed rectangular field box on the page. */
    PRINTED_BOX,

    /** A printed underline immediately associated with the label. */
    UNDERLINE,

    /** Blank space beside or under the label; no printed cue was found. */
    ESTIMATED
}

data class AnswerArea(
    val rect: NormalizedRect,
    val source: AnswerAreaSource,
    /** The printed box this area came from, so callers can stop two labels claiming it. */
    val printedBox: PrintedBox? = null
) {
    val isPrinted: Boolean get() = source != AnswerAreaSource.ESTIMATED
}

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

        /** Printed borders are ink; keep drawn text clear of them. */
        const val BOX_INSET_X = 0.006f
        const val BOX_INSET_Y_FRACTION = 0.12f

        /** A box may start a hair left of the label's right edge from OCR jitter. */
        const val BOX_RIGHT_TOLERANCE = 0.01f

        /** Minimum clearance kept between a label and the text written beside it. */
        const val BOX_LABEL_GAP = 0.004f

        /** How far above a caption label ("Applicant's Signature") its rule may sit. */
        const val CAPTION_RULE_ABOVE = 0.05f
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
        val left = left.coerceIn(0f, 1f)
        val top = top.coerceIn(0f, 1f)
        return NormalizedRect(
            left = left,
            top = top,
            right = right.coerceIn(left, 1f),
            bottom = bottom.coerceIn(top, 1f)
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

    /** True when any recognized text sits inside [rect] on the same page. */
    private fun containsText(
        rect: NormalizedRect,
        pageIndex: Int,
        allBlocks: List<OcrBlock>
    ): Boolean {
        return allBlocks.any { it.pageIndex == pageIndex && intersects(rect, it.box) }
    }

    private fun verticalOverlap(
        first: NormalizedRect,
        second: NormalizedRect
    ): Float {
        return minOf(first.bottom, second.bottom) - maxOf(first.top, second.top)
    }

    /** Shrinks a printed box so drawn text clears its border. */
    private fun insetPrintedBox(rect: NormalizedRect): NormalizedRect {
        val insetX = minOf(BOX_INSET_X, rect.width * 0.2f)
        val insetY = rect.height * BOX_INSET_Y_FRACTION
        return NormalizedRect(
            left = rect.left + insetX,
            top = rect.top + insetY,
            right = rect.right - insetX,
            bottom = rect.bottom - insetY
        )
    }

    /**
     * Finds the printed field box this label belongs to: an empty box on the same
     * row, starting at or after the label's right edge. Boxes that already hold
     * printed text (section header strips, title bands) are rejected, as are boxes
     * another label has already claimed.
     */
    private fun estimateInPrintedBox(
        label: OcrBlock,
        allBlocks: List<OcrBlock>,
        boxes: List<PrintedBox>,
        claimedBoxes: Set<PrintedBox>
    ): AnswerArea? {
        if (boxes.isEmpty()) return null
        val labelHeight = (label.box.bottom - label.box.top).coerceAtLeast(0.0001f)

        val candidate = boxes
            .asSequence()
            .filter { it.pageIndex == label.pageIndex }
            .filterNot { it in claimedBoxes }
            .filter { it.rect.left >= label.box.right - BOX_RIGHT_TOLERANCE }
            .filter { it.rect.width >= MIN_ANSWER_WIDTH }
            .filter { verticalOverlap(it.rect, label.box) > labelHeight * 0.4f }
            .filterNot { containsText(it.rect, label.pageIndex, allBlocks) }
            .minByOrNull { it.rect.left }
            ?: return null

        val inset = insetPrintedBox(candidate.rect)
        // OCR label boxes can run a hair into the printed box; nudge the writable
        // area clear of the label rather than dropping an otherwise good field.
        val safe = NormalizedRect(
            left = maxOf(inset.left, label.box.right + BOX_LABEL_GAP),
            top = inset.top,
            right = inset.right,
            bottom = inset.bottom
        ).clamped()
        if (!hasUsableSize(safe)) return null
        return AnswerArea(safe, AnswerAreaSource.PRINTED_BOX, candidate)
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

    /**
     * Photo and signature areas are usually captioned underneath: the rule sits
     * above the label rather than beside it. Writing beside such a label drops the
     * answer into the middle of the footer, so the rule above is used instead.
     */
    private fun estimateAboveCaption(
        label: OcrBlock,
        allBlocks: List<OcrBlock>,
        rules: List<RuleLine>
    ): NormalizedRect? {
        val rule = rules
            .asSequence()
            .filter { it.pageIndex == label.pageIndex }
            .filter { it.yBottom <= label.box.top }
            .filter { label.box.top - it.yBottom <= CAPTION_RULE_ABOVE }
            .filter { it.xEnd > label.box.left && it.xStart < label.box.right }
            .maxByOrNull { it.yBottom }
            ?: return null

        val candidate = NormalizedRect(
            left = rule.xStart,
            top = (rule.yTop - RULE_ANSWER_HEIGHT * 1.6f).coerceAtLeast(0f),
            right = rule.xEnd,
            bottom = rule.yBottom
        ).clamped()

        if (!hasUsableSize(candidate)) return null
        if (containsText(candidate, label.pageIndex, allBlocks)) return null
        return candidate
    }

    /**
     * Resolves where an answer for [label] must be drawn, preferring printed
     * geometry over blank-space guesses.
     *
     * Order: printed field box, printed underline, caption underline above
     * (photo/signature only), blank space right of the label, blank space below.
     * Returns null when no candidate is safe, which the parser turns into a
     * manual-review warning rather than writing in the wrong place.
     */
    fun estimateArea(
        label: OcrBlock,
        allBlocks: List<OcrBlock>,
        rules: List<RuleLine> = emptyList(),
        boxes: List<PrintedBox> = emptyList(),
        claimedBoxes: Set<PrintedBox> = emptySet(),
        fieldType: FieldType = FieldType.UNKNOWN
    ): AnswerArea? {
        // The contract is that an answer is never written over its own label, so
        // every candidate passes through this gate before it is returned.
        return resolveArea(label, allBlocks, rules, boxes, claimedBoxes, fieldType)
            ?.takeUnless { intersects(it.rect, label.box) }
    }

    private fun resolveArea(
        label: OcrBlock,
        allBlocks: List<OcrBlock>,
        rules: List<RuleLine>,
        boxes: List<PrintedBox>,
        claimedBoxes: Set<PrintedBox>,
        fieldType: FieldType
    ): AnswerArea? {

        estimateInPrintedBox(label, allBlocks, boxes, claimedBoxes)?.let { return it }

        val isCaptionField = fieldType == FieldType.SIGNATURE || fieldType == FieldType.PHOTO
        if (isCaptionField) {
            estimateAboveCaption(label, allBlocks, rules)?.let {
                return AnswerArea(it, AnswerAreaSource.UNDERLINE)
            }
        }

        val ruleCandidate = estimateOnRule(label, allBlocks, rules)?.clamped()

        if (
            ruleCandidate != null &&
            hasUsableSize(ruleCandidate) &&
            !collidesWithText(ruleCandidate, label, allBlocks)
        ) {
            return AnswerArea(ruleCandidate, AnswerAreaSource.UNDERLINE)
        }

        // A printed box exists on this row but was rejected above (claimed, or it
        // holds text). Falling back to blank space would drop the answer into the
        // gap between the label and the box, which is the defect this ordering
        // exists to prevent.
        if (hasCompetingBoxOnRow(label, boxes)) return null

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
            return AnswerArea(rightCandidate, AnswerAreaSource.ESTIMATED)
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
            return AnswerArea(belowCandidate, AnswerAreaSource.ESTIMATED)
        }

        return null
    }

    private fun hasCompetingBoxOnRow(
        label: OcrBlock,
        boxes: List<PrintedBox>
    ): Boolean {
        val labelHeight = (label.box.bottom - label.box.top).coerceAtLeast(0.0001f)
        return boxes.any {
            it.pageIndex == label.pageIndex &&
                it.rect.left >= label.box.right - BOX_RIGHT_TOLERANCE &&
                verticalOverlap(it.rect, label.box) > labelHeight * 0.4f
        }
    }

    /** Rectangle-only form of [estimateArea], kept for callers that ignore provenance. */
    fun estimate(
        label: OcrBlock,
        allBlocks: List<OcrBlock>,
        rules: List<RuleLine> = emptyList(),
        boxes: List<PrintedBox> = emptyList()
    ): NormalizedRect? {
        return estimateArea(label, allBlocks, rules, boxes)?.rect
    }
}
