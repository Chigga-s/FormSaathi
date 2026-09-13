package com.formsaathi.formengine

import android.net.Uri
import com.formsaathi.contracts.ParseStage
import com.formsaathi.contracts.ProgressReportingFormParser
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormField
import com.formsaathi.model.PageInfo
import com.formsaathi.model.ParsedForm
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class RealFormParser(
    private val pageRenderer: AndroidPdfPageRenderer,
    private val ocrEngine: MlKitOcrEngine,
    private val labelDetector: LabelDetector,
    private val fieldMapper: FieldMapper,
    private val answerBoxEstimator: AnswerBoxEstimator,
    private val requirementExtractor: RequirementExtractor,
    private val ocrScript: OcrScript = OcrScript.LATIN
) : ProgressReportingFormParser {

    @Volatile
    private var progressListener: ((ParseStage, Int, Int) -> Unit)? = null

    override fun setProgressListener(listener: ((ParseStage, Int, Int) -> Unit)?) {
        progressListener = listener
    }

    private fun report(stage: ParseStage, pageIndex: Int, pageCount: Int) {
        progressListener?.invoke(stage, pageIndex, pageCount)
    }

    override suspend fun parse(
        uri: Uri
    ): ParsedForm {

        val pageCount = pageRenderer.getPageCount(uri)

        val pages = mutableListOf<PageInfo>()
        val allBlocks = mutableListOf<OcrBlock>()
        val allRules = mutableListOf<RuleLine>()
        val allBoxes = mutableListOf<PrintedBox>()

        for (pageIndex in 0 until pageCount) {
            currentCoroutineContext().ensureActive()
            report(ParseStage.RENDERING, pageIndex, pageCount)

            // One page is rendered, scanned and recycled before the next is
            // opened, so peak memory stays at a single page bitmap.
            val renderedPage = pageRenderer.renderPage(
                uri = uri,
                pageIndex = pageIndex
            )

            pages.add(renderedPage.info)

            try {
                val width = renderedPage.bitmap.width
                val height = renderedPage.bitmap.height
                // Both geometry scans read the same luminance array; computing it
                // twice would double the largest allocation in the pipeline.
                val luminance = RuleLineDetector.luminanceOf(renderedPage.bitmap)

                allRules.addAll(
                    RuleLineDetector.detect(luminance, width, height, pageIndex)
                )
                allBoxes.addAll(
                    PrintedBoxDetector.detect(luminance, width, height, pageIndex)
                )

                currentCoroutineContext().ensureActive()
                report(ParseStage.READING_TEXT, pageIndex, pageCount)

                val blocks = ocrEngine.recognize(
                    bitmap = renderedPage.bitmap,
                    pageIndex = pageIndex,
                    script = ocrScript
                )

                allBlocks.addAll(blocks)
            } finally {
                renderedPage.bitmap.recycle()
            }
        }

        currentCoroutineContext().ensureActive()
        report(ParseStage.PREPARING_QUESTIONS, pageCount - 1, pageCount)

        val requirementRegionTops = requirementHeadingTops(allBlocks)

        val candidateLabels = labelDetector
            .detect(allBlocks)
            .filterNot { isInRequirementSection(it, requirementRegionTops) }

        val fields = mutableListOf<FormField>()
        val warnings = mutableListOf<String>()
        val claimedBoxes = mutableSetOf<PrintedBox>()

        for (label in candidateLabels) {
            val match = fieldMapper.map(label.text)

            // Titles, section headers, instructions and footers map to UNKNOWN
            // and carry no fill-in cue. Asking them as generic questions
            // confuses users, so they are left out (document requirements are
            // still extracted separately). A genuine-but-unknown field label
            // on a form almost always ends with ':' or '?', or maps to a
            // known type, so it is kept.
            if (match.type == FieldType.UNKNOWN &&
                !isFillableLabelCue(label.text)
            ) {
                continue
            }

            val area = answerBoxEstimator.estimateArea(
                label = label,
                allBlocks = allBlocks,
                rules = allRules,
                boxes = allBoxes,
                claimedBoxes = claimedBoxes,
                fieldType = match.type
            )

            // An unknown label with no printed box or underline is almost always a
            // heading or a sentence, not a field. Guessing a rectangle beside it
            // produces a question the user cannot interpret and ink in the wrong
            // place, so it is dropped quietly rather than warned about.
            if (match.type == FieldType.UNKNOWN && area?.isPrinted != true) {
                continue
            }

            if (area == null) {
                warnings.add(
                    "Could not find a safe answer area for '${label.text}' on page ${label.pageIndex + 1}. Please fill this field by hand."
                )
                continue
            }

            if (overlapsLabel(area, label)) {
                warnings.add(
                    "Answer area for '${label.text}' on page ${label.pageIndex + 1} overlapped its own label and was skipped."
                )
                continue
            }

            if (overlapsExistingField(area, label.pageIndex, fields)) {
                warnings.add(
                    "Answer area for '${label.text}' on page ${label.pageIndex + 1} collided with another field and was skipped."
                )
                continue
            }

            area.printedBox?.let { claimedBoxes.add(it) }

            val fieldId =
                "p${label.pageIndex}_b${label.blockIndex}_l${label.lineIndex}"

            fields.add(
                FormField(
                    id = fieldId,
                    sourceLabel = label.text,
                    type = match.type,
                    pageIndex = label.pageIndex,
                    labelBox = label.box,
                    answerBox = area.rect,
                    required = false,
                    // A guessed rectangle is reported as low confidence so the
                    // review screen marks it for manual checking.
                    confidence = if (area.isPrinted) match.confidence else match.confidence * 0.5f
                )
            )

            if (!area.isPrinted) {
                warnings.add(
                    "No printed answer box was found for '${label.text}' on page ${label.pageIndex + 1}. Please check this answer's position in the completed PDF."
                )
            }
        }

        val documents = requirementExtractor.extract(allBlocks)

        return ParsedForm(
            pages = pages,
            fields = fields,
            documents = documents,
            warnings = warnings
        )
    }

    private fun isFillableLabelCue(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.endsWith(":") || trimmed.endsWith("?")
    }

    private fun overlapsLabel(area: AnswerArea, label: OcrBlock): Boolean {
        return area.rect.left < label.box.right &&
            area.rect.right > label.box.left &&
            area.rect.top < label.box.bottom &&
            area.rect.bottom > label.box.top
    }

    private fun overlapsExistingField(
        area: AnswerArea,
        pageIndex: Int,
        fields: List<FormField>
    ): Boolean {
        return fields.any { existing ->
            existing.pageIndex == pageIndex &&
                area.rect.left < existing.answerBox.right &&
                area.rect.right > existing.answerBox.left &&
                area.rect.top < existing.answerBox.bottom &&
                area.rect.bottom > existing.answerBox.top
        }
    }

    /**
     * Finds the top of any "documents to attach" style heading, per page. Lines
     * below it list enclosures, not fields, and asking about them produced
     * questions like a 12-digit Aadhaar prompt for a bullet that merely mentions
     * an Aadhaar card.
     */
    private fun requirementHeadingTops(blocks: List<OcrBlock>): Map<Int, Float> {
        val headings = mutableMapOf<Int, Float>()
        for (block in blocks) {
            val text = block.text.lowercase()
            val isHeading = ("document" in text || "enclosure" in text) &&
                ("attach" in text || "enclose" in text || "required" in text || "submit" in text) ||
                text.startsWith("documents to attach")
            if (!isHeading) continue
            val existing = headings[block.pageIndex]
            if (existing == null || block.box.top < existing) {
                headings[block.pageIndex] = block.box.top
            }
        }
        return headings
    }

    private fun isInRequirementSection(
        block: OcrBlock,
        headingTops: Map<Int, Float>
    ): Boolean {
        val top = headingTops[block.pageIndex] ?: return false
        return block.box.top >= top
    }
}
