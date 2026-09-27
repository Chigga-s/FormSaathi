package com.formsaathi.formengine

import android.net.Uri
import com.formsaathi.contracts.FormParser
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormField
import com.formsaathi.model.PageInfo
import com.formsaathi.model.ParsedForm

class RealFormParser(
    private val pageRenderer: AndroidPdfPageRenderer,
    private val ocrEngine: MlKitOcrEngine,
    private val labelDetector: LabelDetector,
    private val fieldMapper: FieldMapper,
    private val answerBoxEstimator: AnswerBoxEstimator,
    private val requirementExtractor: RequirementExtractor,
    private val ocrScript: OcrScript = OcrScript.LATIN
) : FormParser {

    override suspend fun parse(
        uri: Uri
    ): ParsedForm {

        val pageCount = pageRenderer.getPageCount(uri)

        val pages = mutableListOf<PageInfo>()
        val allBlocks = mutableListOf<OcrBlock>()
        val allRules = mutableListOf<RuleLine>()

        for (pageIndex in 0 until pageCount) {
            val renderedPage = pageRenderer.renderPage(
                uri = uri,
                pageIndex = pageIndex
            )

            pages.add(renderedPage.info)

            try {
                allRules.addAll(
                    RuleLineDetector.detect(
                        luminance = RuleLineDetector.luminanceOf(renderedPage.bitmap),
                        width = renderedPage.bitmap.width,
                        height = renderedPage.bitmap.height,
                        pageIndex = pageIndex
                    )
                )

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

        val candidateLabels = labelDetector.detect(allBlocks)

        val mappedLabels = candidateLabels.map { label ->
            label to fieldMapper.map(label.text)
        }

        val fields = mutableListOf<FormField>()
        val warnings = mutableListOf<String>()

        for ((label, match) in mappedLabels) {

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

            val answerBox = answerBoxEstimator.estimate(
                label = label,
                allBlocks = allBlocks,
                rules = allRules
            )

            if (answerBox == null) {
                warnings.add(
                    "Could not estimate answer area for '${label.text}' on page ${label.pageIndex + 1}"
                )
                continue
            }

            val fieldId =
                "p${label.pageIndex}_b${label.blockIndex}_l${label.lineIndex}"

            val field = FormField(
                id = fieldId,
                sourceLabel = label.text,
                type = match.type,
                pageIndex = label.pageIndex,
                labelBox = label.box,
                answerBox = answerBox,
                required = false,
                confidence = match.confidence
            )

            fields.add(field)
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
}