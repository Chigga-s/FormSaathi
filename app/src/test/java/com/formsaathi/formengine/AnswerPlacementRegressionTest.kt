package com.formsaathi.formengine

import com.formsaathi.model.FieldType
import com.formsaathi.model.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Placement regressions taken from the two supported demo templates.
 *
 * Geometry is measured from the real rendered pages (Test Form 1 from its render,
 * Test Form 2 from the generator that produces it), so a failure here means the
 * generated PDF would be visibly wrong, not that an internal constant moved.
 */
class AnswerPlacementRegressionTest {

    private var nextBlock = 0

    private fun block(text: String, left: Float, top: Float, right: Float, bottom: Float, page: Int = 0) =
        OcrBlock(
            text = text,
            pageIndex = page,
            box = NormalizedRect(left, top, right, bottom),
            confidence = 0.9f,
            blockIndex = nextBlock++,
            lineIndex = 0
        )

    private fun printedBox(left: Float, top: Float, right: Float, bottom: Float, page: Int = 0) =
        PrintedBox(page, NormalizedRect(left, top, right, bottom))

    private fun contains(outer: NormalizedRect, inner: NormalizedRect): Boolean =
        inner.left >= outer.left - EPS && inner.right <= outer.right + EPS &&
            inner.top >= outer.top - EPS && inner.bottom <= outer.bottom + EPS

    private fun overlaps(a: NormalizedRect, b: NormalizedRect): Boolean =
        a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

    // ----------------------------------------------------------------------
    // Test Form 2 - "General Application Form": labels left, printed boxes right
    // ----------------------------------------------------------------------

    private data class BoxedRow(
        val label: OcrBlock,
        val box: PrintedBox,
        val type: FieldType
    )

    private fun boxedFormRows(): List<BoxedRow> {
        nextBlock = 0
        return listOf(
            Triple("Full Name", listOf(0.0717f, 0.2400f, 0.1600f, 0.2525f), listOf(0.3717f, 0.2286f, 0.9300f, 0.2610f)) to FieldType.FULL_NAME,
            Triple("Father's Name", listOf(0.0717f, 0.2819f, 0.1983f, 0.2944f), listOf(0.3717f, 0.2708f, 0.9300f, 0.3026f)) to FieldType.FATHER_NAME,
            Triple("Date of Birth (DD/MM/YYYY)", listOf(0.0717f, 0.3237f, 0.3171f, 0.3362f), listOf(0.3717f, 0.3123f, 0.9300f, 0.3447f)) to FieldType.DATE_OF_BIRTH,
            Triple("Mobile Number", listOf(0.0717f, 0.3656f, 0.2052f, 0.3781f), listOf(0.3717f, 0.3545f, 0.9300f, 0.3863f)) to FieldType.MOBILE,
            Triple("Category", listOf(0.0717f, 0.4075f, 0.1518f, 0.4200f), listOf(0.3717f, 0.3961f, 0.9300f, 0.4285f)) to FieldType.CATEGORY,
            Triple("Annual Income", listOf(0.0717f, 0.4494f, 0.2042f, 0.4619f), listOf(0.3717f, 0.4383f, 0.9300f, 0.4700f)) to FieldType.ANNUAL_INCOME,
            Triple("Residential Address", listOf(0.0717f, 0.5111f, 0.2494f, 0.5236f), listOf(0.3717f, 0.4798f, 0.9300f, 0.5520f)) to FieldType.PERMANENT_ADDRESS,
            Triple("Aadhaar Number", listOf(0.0717f, 0.6599f, 0.2206f, 0.6724f), listOf(0.3717f, 0.6491f, 0.9300f, 0.6803f)) to FieldType.AADHAAR
        ).map { (row, type) ->
            val (text, l, b) = row
            BoxedRow(
                label = block(text, l[0], l[1], l[2], l[3]),
                box = printedBox(b[0], b[1], b[2], b[3]),
                type = type
            )
        }
    }

    /** The two tinted section strips; both hold printed text and must be rejected. */
    private fun boxedFormSectionBands(): Pair<List<PrintedBox>, List<OcrBlock>> {
        val bands = listOf(
            printedBox(0.0717f, 0.1693f, 0.9300f, 0.2048f),
            printedBox(0.0717f, 0.5892f, 0.9300f, 0.6253f)
        )
        val text = listOf(
            block("Personal Details", 0.0880f, 0.1810f, 0.2400f, 0.1930f),
            block("Identity & Verification", 0.0880f, 0.6010f, 0.2900f, 0.6130f)
        )
        return bands to text
    }

    @Test
    fun boxedForm_answersLandInsideThePrintedBoxNotInTheGapBesideTheLabel() {
        val rows = boxedFormRows()
        val (bands, bandText) = boxedFormSectionBands()
        val blocks = rows.map { it.label } + bandText
        val boxes = rows.map { it.box } + bands
        val estimator = AnswerBoxEstimator()
        val claimed = mutableSetOf<PrintedBox>()

        for (row in rows) {
            val area = estimator.estimateArea(
                label = row.label,
                allBlocks = blocks,
                rules = emptyList(),
                boxes = boxes,
                claimedBoxes = claimed,
                fieldType = row.type
            )
            assertNotNull("No answer area found for '${row.label.text}'", area)
            area!!
            assertEquals(
                "'${row.label.text}' must be placed from the printed box, not guessed",
                AnswerAreaSource.PRINTED_BOX,
                area.source
            )
            assertTrue(
                "'${row.label.text}' answer must sit inside its printed box, " +
                    "was ${area.rect} for box ${row.box.rect}",
                contains(row.box.rect, area.rect)
            )
            // The reported defect: text drawn in the whitespace between the label
            // and the box instead of inside the box.
            assertTrue(
                "'${row.label.text}' answer starts left of the printed box at ${area.rect.left}",
                area.rect.left >= row.box.rect.left - EPS
            )
            claimed.add(area.printedBox!!)
        }
    }

    @Test
    fun boxedForm_noAnswerOverlapsAnyPrintedLabel() {
        val rows = boxedFormRows()
        val (bands, bandText) = boxedFormSectionBands()
        val blocks = rows.map { it.label } + bandText
        val boxes = rows.map { it.box } + bands
        val estimator = AnswerBoxEstimator()
        val claimed = mutableSetOf<PrintedBox>()

        val areas = rows.map { row ->
            val area = estimator.estimateArea(row.label, blocks, emptyList(), boxes, claimed, row.type)!!
            claimed.add(area.printedBox!!)
            row.label.text to area.rect
        }

        for ((text, rect) in areas) {
            for (label in blocks) {
                assertTrue(
                    "Answer for '$text' at $rect overlaps label '${label.text}' at ${label.box}",
                    !overlaps(rect, label.box)
                )
            }
        }
    }

    @Test
    fun boxedForm_sectionHeaderStripIsNeverUsedAsAnAnswerBox() {
        val rows = boxedFormRows()
        val (bands, bandText) = boxedFormSectionBands()
        val blocks = rows.map { it.label } + bandText
        val boxes = rows.map { it.box } + bands
        val estimator = AnswerBoxEstimator()

        val area = estimator.estimateArea(
            label = bandText.first(),
            allBlocks = blocks,
            boxes = boxes,
            fieldType = FieldType.UNKNOWN
        )
        // The strip holds text, so it cannot be claimed; nothing else is on that
        // row, so a section header yields no answer area at all.
        if (area != null) {
            assertTrue(
                "Section header must not be assigned a printed box",
                area.source != AnswerAreaSource.PRINTED_BOX
            )
        }
    }

    @Test
    fun boxedForm_twoLabelsCannotClaimTheSamePrintedBox() {
        val rows = boxedFormRows()
        val first = rows[0]
        // A second label on the same row, e.g. a bilingual caption under the first.
        val duplicate = block("Name of applicant", 0.0717f, 0.2440f, 0.2000f, 0.2560f)
        val blocks = listOf(first.label, duplicate)
        val boxes = listOf(first.box)
        val estimator = AnswerBoxEstimator()

        val firstArea = estimator.estimateArea(first.label, blocks, boxes = boxes, fieldType = FieldType.FULL_NAME)!!
        val secondArea = estimator.estimateArea(
            label = duplicate,
            allBlocks = blocks,
            boxes = boxes,
            claimedBoxes = setOf(firstArea.printedBox!!),
            fieldType = FieldType.FULL_NAME
        )

        assertNull(
            "A claimed printed box must not be handed to a second label; got $secondArea",
            secondArea
        )
    }

    @Test
    fun boxedForm_answerIsNotGuessedIntoTheGapWhenItsBoxIsAlreadyTaken() {
        // Regression guard for the fallback path: with a printed box present on
        // the row, falling back to blank space would reproduce the original bug.
        val rows = boxedFormRows()
        val first = rows[0]
        val estimator = AnswerBoxEstimator()

        val area = estimator.estimateArea(
            label = first.label,
            allBlocks = listOf(first.label),
            boxes = listOf(first.box),
            claimedBoxes = setOf(first.box),
            fieldType = FieldType.FULL_NAME
        )

        assertNull("Must not fall back to the whitespace beside the label", area)
    }

    // ----------------------------------------------------------------------
    // Test Form 1 - "Citizen Services Application Form": underlined fields
    // ----------------------------------------------------------------------

    private data class RuleRow(val label: OcrBlock, val rule: RuleLine, val type: FieldType)

    private fun underlineFormRows(): List<RuleRow> {
        nextBlock = 0
        val rows = listOf(
            Triple("Full Name:", listOf(0.1060f, 0.1428f, 0.1885f, 0.1517f), 0.1533f) to FieldType.FULL_NAME,
            Triple("Father's Name:", listOf(0.1060f, 0.1828f, 0.2231f, 0.1917f), 0.1928f) to FieldType.FATHER_NAME,
            Triple("Mother's Name:", listOf(0.1060f, 0.2222f, 0.2270f, 0.2311f), 0.2328f) to FieldType.MOTHER_NAME,
            Triple("Date of Birth:", listOf(0.1060f, 0.2622f, 0.2058f, 0.2711f), 0.2728f) to FieldType.DATE_OF_BIRTH,
            Triple("Gender:", listOf(0.1053f, 0.3011f, 0.1673f, 0.3111f), 0.3122f) to FieldType.GENDER,
            Triple("Mobile Number:", listOf(0.1060f, 0.3417f, 0.2278f, 0.3506f), 0.3522f) to FieldType.MOBILE,
            Triple("Email Address:", listOf(0.1060f, 0.3817f, 0.2215f, 0.3906f), 0.3917f) to FieldType.EMAIL,
            Triple("Aadhaar Number:", listOf(0.1053f, 0.4211f, 0.2427f, 0.4300f), 0.4317f) to FieldType.AADHAAR,
            Triple("Permanent Address:", listOf(0.1060f, 0.4611f, 0.3150f, 0.4700f), 0.4711f) to FieldType.PERMANENT_ADDRESS,
            Triple("Current Address:", listOf(0.1053f, 0.5000f, 0.2364f, 0.5100f), 0.5111f) to FieldType.CURRENT_ADDRESS,
            Triple("State:", listOf(0.1053f, 0.5400f, 0.1493f, 0.5500f), 0.5511f) to FieldType.STATE,
            Triple("District:", listOf(0.1060f, 0.5806f, 0.1618f, 0.5894f), 0.5906f) to FieldType.DISTRICT,
            Triple("PIN Code:", listOf(0.1060f, 0.6194f, 0.1846f, 0.6294f), 0.6306f) to FieldType.PINCODE,
            Triple("Category:", listOf(0.1053f, 0.6594f, 0.1799f, 0.6694f), 0.6700f) to FieldType.CATEGORY,
            Triple("Annual Income:", listOf(0.1053f, 0.6994f, 0.2255f, 0.7083f), 0.7100f) to FieldType.ANNUAL_INCOME
        )
        return rows.map { (row, type) ->
            val (text, l, ruleTop) = row
            RuleRow(
                label = block(text, l[0], l[1], l[2], l[3]),
                rule = RuleLine(0, ruleTop, ruleTop + 0.0017f, 0.3661f, 0.9057f),
                type = type
            )
        }
    }

    @Test
    fun underlineForm_everyAnswerSitsOnItsOwnRuleAndClearsItsLabel() {
        val rows = underlineFormRows()
        val blocks = rows.map { it.label }
        val rules = rows.map { it.rule }
        val estimator = AnswerBoxEstimator()

        for (row in rows) {
            val area = estimator.estimateArea(row.label, blocks, rules, fieldType = row.type)
            assertNotNull("No answer area for '${row.label.text}'", area)
            area!!
            assertEquals(
                "'${row.label.text}' must snap to its printed underline",
                AnswerAreaSource.UNDERLINE,
                area.source
            )
            assertTrue(
                "'${row.label.text}' answer at ${area.rect} must sit on rule ${row.rule.yTop}",
                area.rect.top < row.rule.yTop && area.rect.bottom > row.rule.yTop
            )
            assertTrue(
                "'${row.label.text}' answer must start right of its label",
                area.rect.left >= row.label.box.right
            )
        }
    }

    @Test
    fun underlineForm_districtAnswerDoesNotTouchTheDistrictLabelOrTheNextRow() {
        val rows = underlineFormRows()
        val blocks = rows.map { it.label }
        val rules = rows.map { it.rule }
        val district = rows.first { it.label.text == "District:" }
        val pinCode = rows.first { it.label.text == "PIN Code:" }

        val area = AnswerBoxEstimator()
            .estimateArea(district.label, blocks, rules, fieldType = FieldType.DISTRICT)!!

        assertTrue(
            "District answer ${area.rect} overlaps the 'District:' label ${district.label.box}",
            !overlaps(area.rect, district.label.box)
        )
        assertTrue(
            "District answer ${area.rect} runs into the PIN Code row",
            area.rect.bottom <= pinCode.label.box.top
        )
        assertTrue(
            "District answer ${area.rect} runs into the State row",
            area.rect.top >= rows.first { it.label.text == "State:" }.rule.yBottom
        )
    }

    @Test
    fun underlineForm_noAnswerOverlapsAnyOtherFieldsAnswer() {
        val rows = underlineFormRows()
        val blocks = rows.map { it.label }
        val rules = rows.map { it.rule }
        val estimator = AnswerBoxEstimator()

        val areas = rows.map { it.label.text to estimator.estimateArea(it.label, blocks, rules, fieldType = it.type)!!.rect }
        for (i in areas.indices) {
            for (j in i + 1 until areas.size) {
                assertTrue(
                    "Answer areas overlap: ${areas[i].first} ${areas[i].second} vs ${areas[j].first} ${areas[j].second}",
                    !overlaps(areas[i].second, areas[j].second)
                )
            }
        }
    }

    // ----------------------------------------------------------------------
    // Cross-cutting invariants
    // ----------------------------------------------------------------------

    @Test
    fun answerAreaIsNeverTheLabelBox() {
        nextBlock = 0
        // A label with text on every side: no safe area exists, and the estimator
        // must decline rather than write over the label itself.
        val label = block("Boxed In:", 0.40f, 0.40f, 0.55f, 0.42f)
        val around = listOf(
            block("left", 0.10f, 0.40f, 0.39f, 0.42f),
            block("right", 0.56f, 0.40f, 0.95f, 0.42f),
            block("below", 0.10f, 0.43f, 0.95f, 0.45f)
        )
        val area = AnswerBoxEstimator().estimateArea(label, listOf(label) + around)
        assertNull("Estimator must not fall back to the label's own box", area)
    }

    @Test
    fun printedBoxOnAnotherPageIsNotUsed() {
        nextBlock = 0
        val label = block("Full Name", 0.0717f, 0.2400f, 0.1600f, 0.2525f, page = 1)
        val boxOnPageZero = printedBox(0.3717f, 0.2286f, 0.9300f, 0.2610f, page = 0)

        val area = AnswerBoxEstimator().estimateArea(
            label = label,
            allBlocks = listOf(label),
            boxes = listOf(boxOnPageZero),
            fieldType = FieldType.FULL_NAME
        )

        assertTrue(
            "A box on page 1 must not be used for a label on page 2",
            area == null || area.source != AnswerAreaSource.PRINTED_BOX
        )
    }

    @Test
    fun everyAnswerAreaStaysWithinThePage() {
        val rows = boxedFormRows()
        val blocks = rows.map { it.label }
        val boxes = rows.map { it.box }
        val estimator = AnswerBoxEstimator()
        val claimed = mutableSetOf<PrintedBox>()

        for (row in rows) {
            val rect = estimator.estimateArea(row.label, blocks, boxes = boxes, claimedBoxes = claimed, fieldType = row.type)!!
            claimed.add(rect.printedBox!!)
            assertTrue("left out of page: $rect", rect.rect.left in 0f..1f)
            assertTrue("top out of page: $rect", rect.rect.top in 0f..1f)
            assertTrue("right out of page: $rect", rect.rect.right in 0f..1f)
            assertTrue("bottom out of page: $rect", rect.rect.bottom in 0f..1f)
            assertTrue("empty rect: $rect", rect.rect.width > 0f && rect.rect.height > 0f)
        }
    }

    @Test
    fun signatureCaptionUsesTheRuleAboveItNotTheSpaceBesideIt() {
        nextBlock = 0
        // Footer layout: a rule with its caption printed underneath.
        val caption = block("Applicant's Signature", 0.5800f, 0.9040f, 0.8400f, 0.9160f)
        val dateCaption = block("Date & Place", 0.1900f, 0.9040f, 0.3800f, 0.9160f)
        val rule = RuleLine(0, 0.8888f, 0.8906f, 0.5635f, 0.8643f)

        val area = AnswerBoxEstimator().estimateArea(
            label = caption,
            allBlocks = listOf(caption, dateCaption),
            rules = listOf(rule),
            fieldType = FieldType.SIGNATURE
        )

        assertNotNull("Signature must be placed on its printed line", area)
        assertTrue(
            "Signature must be drawn above its caption, was ${area!!.rect}",
            area.rect.bottom <= caption.box.top
        )
        assertTrue(
            "Signature must sit on the printed rule",
            area.rect.bottom > rule.yTop - EPS
        )
    }

    private companion object {
        const val EPS = 1e-4f
    }
}
