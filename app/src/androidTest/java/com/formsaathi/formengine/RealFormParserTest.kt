package com.formsaathi.formengine

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formsaathi.model.FieldType
import com.formsaathi.model.ParsedForm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class RealFormParserTest {

    @Test
    fun parseSinglePage_returnsPageMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "single_page.pdf",
            pages = listOf(
                listOf("Full Name:")
            )
        )

        assertEquals(1, result.pages.size)

        val page = result.pages[0]

        assertEquals(0, page.pageIndex)
        assertTrue(page.pdfWidthPoints > 0f)
        assertTrue(page.pdfHeightPoints > 0f)
        assertTrue(page.renderedWidthPx > 0)
        assertTrue(page.renderedHeightPx > 0)
    }

    @Test
    fun parseKnownLabels_mapsCanonicalFieldTypes() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "known_fields.pdf",
            pages = listOf(
                listOf(
                    "Full Name:",
                    "Mobile Number:",
                    "Date of Birth:"
                )
            )
        )

        val types = result.fields.map { it.type }.toSet()

        assertTrue(FieldType.FULL_NAME in types)
        assertTrue(FieldType.MOBILE in types)
        assertTrue(FieldType.DATE_OF_BIRTH in types)
    }

    @Test
    fun detectedField_hasNormalizedLabelAndAnswerBoxes() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "field_geometry.pdf",
            pages = listOf(
                listOf("Full Name:")
            )
        )

        val field = requireNotNull(
            result.fields.firstOrNull {
                it.type == FieldType.FULL_NAME
            }
        )

        assertTrue(field.labelBox.left in 0f..1f)
        assertTrue(field.labelBox.top in 0f..1f)
        assertTrue(field.labelBox.right in 0f..1f)
        assertTrue(field.labelBox.bottom in 0f..1f)

        assertTrue(field.answerBox.left in 0f..1f)
        assertTrue(field.answerBox.top in 0f..1f)
        assertTrue(field.answerBox.right in 0f..1f)
        assertTrue(field.answerBox.bottom in 0f..1f)

        assertTrue(field.labelBox.width > 0f)
        assertTrue(field.labelBox.height > 0f)

        assertTrue(field.answerBox.width > 0f)
        assertTrue(field.answerBox.height > 0f)

        assertNotEquals(
            field.labelBox,
            field.answerBox
        )
    }

    @Test
    fun detectedKnownField_hasPositiveMappingConfidence() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "confidence.pdf",
            pages = listOf(
                listOf("Mobile Number:")
            )
        )

        val field = requireNotNull(
            result.fields.firstOrNull {
                it.type == FieldType.MOBILE
            }
        )

        assertTrue(field.confidence > 0f)
    }

    @Test
    fun extractedDocumentRequirement_isReturnedInParsedForm() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "requirement.pdf",
            pages = listOf(
                listOf(
                    "Self-attested Aadhaar Card"
                )
            )
        )

        val document = requireNotNull(
            result.documents.firstOrNull {
                it.name == "Aadhaar Card"
            }
        )

        assertEquals(
            "self-attested",
            document.requirement
        )
    }

    @Test
    fun multipleDocumentRequirements_areExtracted() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "multiple_requirements.pdf",
            pages = listOf(
                listOf(
                    "Self-attested Aadhaar Card",
                    "PAN Card",
                    "Original Income Certificate"
                )
            )
        )

        val documents = result.documents.associateBy {
            it.name
        }

        assertTrue(
            documents.containsKey("Aadhaar Card")
        )

        assertTrue(
            documents.containsKey("PAN Card")
        )

        assertTrue(
            documents.containsKey("Income Certificate")
        )

        assertEquals(
            "self-attested",
            documents["Aadhaar Card"]?.requirement
        )

        assertEquals(
            "original",
            documents["Income Certificate"]?.requirement
        )
    }

    @Test
    fun multiPagePdf_preservesPageOrder() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "multi_page.pdf",
            pages = listOf(
                listOf("Full Name:"),
                listOf("Mobile Number:")
            )
        )

        assertEquals(2, result.pages.size)

        assertEquals(
            listOf(0, 1),
            result.pages.map { it.pageIndex }
        )
    }

    @Test
    fun fieldsKeepCorrectPageIndex() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "field_pages.pdf",
            pages = listOf(
                listOf("Full Name:"),
                listOf("Mobile Number:")
            )
        )

        assertTrue(
            result.fields.any {
                it.type == FieldType.FULL_NAME &&
                    it.pageIndex == 0
            }
        )

        assertTrue(
            result.fields.any {
                it.type == FieldType.MOBILE &&
                    it.pageIndex == 1
            }
        )
    }

    @Test
    fun generatedFieldIds_areUnique() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "unique_ids.pdf",
            pages = listOf(
                listOf(
                    "Full Name:",
                    "Mobile Number:",
                    "Date of Birth:"
                )
            )
        )

        assertFalse(result.fields.isEmpty())

        val ids = result.fields.map {
            it.id
        }

        assertEquals(
            ids.size,
            ids.toSet().size
        )
    }

    @Test
    fun fieldIdsContainPageBlockAndLineInformation() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "field_id_format.pdf",
            pages = listOf(
                listOf("Full Name:")
            )
        )

        val field = requireNotNull(
            result.fields.firstOrNull {
                it.type == FieldType.FULL_NAME
            }
        )

        assertTrue(
            field.id.matches(
                Regex("p\\d+_b\\d+_l\\d+")
            )
        )
    }

    @Test
    fun blankPdf_returnsNoFieldsOrDocuments() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "blank.pdf",
            pages = listOf(
                emptyList()
            )
        )

        assertEquals(1, result.pages.size)
        assertTrue(result.fields.isEmpty())
        assertTrue(result.documents.isEmpty())
    }

    @Test
    fun sourceLabel_isPreservedFromOcr() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = parsePdf(
            context = context,
            fileName = "source_label.pdf",
            pages = listOf(
                listOf("Full Name:")
            )
        )

        val field = requireNotNull(
            result.fields.firstOrNull {
                it.type == FieldType.FULL_NAME
            }
        )

        assertTrue(
            field.sourceLabel.contains(
                "Full Name",
                ignoreCase = true
            )
        )
    }

    private fun parsePdf(
        context: Context,
        fileName: String,
        pages: List<List<String>>
    ): ParsedForm {

        val file = createTestPdf(
            context = context,
            fileName = fileName,
            pages = pages
        )

        val parser = createParser(context)

        return runBlocking {
            parser.parse(
                Uri.fromFile(file)
            )
        }
    }

    private fun createParser(
        context: Context
    ): RealFormParser {

        return RealFormParser(
            pageRenderer = AndroidPdfPageRenderer(context),
            ocrEngine = MlKitOcrEngine(),
            labelDetector = LabelDetector(),
            fieldMapper = FieldMapper(),
            answerBoxEstimator = AnswerBoxEstimator(),
            requirementExtractor = RequirementExtractor(),
            ocrScript = OcrScript.LATIN
        )
    }

    private fun createTestPdf(
        context: Context,
        fileName: String,
        pages: List<List<String>>
    ): File {

        val file = File(
            context.cacheDir,
            fileName
        )

        if (file.exists()) {
            file.delete()
        }

        val document = PdfDocument()

        try {
            pages.forEachIndexed { pageIndex, lines ->

                val pageInfo =
                    PdfDocument.PageInfo.Builder(
                        595,
                        842,
                        pageIndex + 1
                    ).create()

                val page = document.startPage(
                    pageInfo
                )

                page.canvas.drawColor(
                    Color.WHITE
                )

                val paint = Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color = Color.BLACK
                    textSize = 30f
                    typeface = Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                    )
                }

                var y = 120f

                for (line in lines) {
                    page.canvas.drawText(
                        line,
                        70f,
                        y,
                        paint
                    )

                    y += 120f
                }

                document.finishPage(page)
            }

            FileOutputStream(file).use {
                document.writeTo(it)
            }
        } finally {
            document.close()
        }

        return file
    }
}
