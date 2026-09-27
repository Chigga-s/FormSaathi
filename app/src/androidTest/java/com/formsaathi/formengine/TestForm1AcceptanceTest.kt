package com.formsaathi.formengine

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.formsaathi.model.FieldType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Acceptance gate for the staged demo form (samples/forms/):
 * every useful field must be detected with a usable answer rectangle.
 * Guards the two regressions fixed in Sep 2026: transparent render
 * bitmaps starving ML Kit, and an over-strict minimum answer height
 * dropping tightly-stacked single-column labels.
 */
@RunWith(AndroidJUnit4::class)
class TestForm1AcceptanceTest {

    private val expectedTypesInOrder = listOf(
        FieldType.FULL_NAME,
        FieldType.FATHER_NAME,
        FieldType.MOTHER_NAME,
        FieldType.DATE_OF_BIRTH,
        FieldType.GENDER,
        FieldType.MOBILE,
        FieldType.EMAIL,
        FieldType.AADHAAR,
        FieldType.PERMANENT_ADDRESS,
        FieldType.CURRENT_ADDRESS,
        FieldType.STATE,
        FieldType.DISTRICT,
        FieldType.PINCODE,
        FieldType.CATEGORY,
        FieldType.ANNUAL_INCOME
    )

    @Test
    fun stagedFormYieldsAllUsefulFieldsWithUsableBoxes() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context

        val pdfFile = File(appContext.cacheDir, "acceptance_form1.pdf")
        testContext.assets.open("debugforms/FormSaathi_Test_Form_1_Simple.pdf").use { input ->
            pdfFile.outputStream().use { input.copyTo(it) }
        }

        val parser = RealFormParser(
            pageRenderer = AndroidPdfPageRenderer(appContext),
            ocrEngine = MlKitOcrEngine(),
            labelDetector = LabelDetector(),
            fieldMapper = FieldMapper(),
            answerBoxEstimator = AnswerBoxEstimator(),
            requirementExtractor = RequirementExtractor(),
            ocrScript = OcrScript.LATIN
        )
        val parsed = runBlocking { parser.parse(Uri.fromFile(pdfFile)) }

        assertEquals(1, parsed.pages.size)
        val actualTypes = parsed.fields.map { it.type }
        for (expected in expectedTypesInOrder) {
            assertTrue("Missing $expected in ${actualTypes}", expected in actualTypes)
        }
        // Titles, instructions, section headers, footers and document
        // bullets must not become questions: this form holds exactly the
        // 15 real fields, with no warnings and no bullet labels.
        assertEquals(actualTypes.toString(), expectedTypesInOrder.size, parsed.fields.size)
        assertTrue("Unexpected warnings: ${parsed.warnings}", parsed.warnings.isEmpty())
        for (field in parsed.fields) {
            assertTrue("Bullet label became a field: '${field.sourceLabel}'",
                !field.sourceLabel.trimStart().startsWith("•"))
        }
        for (field in parsed.fields) {
            val box = field.answerBox
            assertTrue("${field.id} box out of range: $box",
                box.left in 0f..1f && box.top in 0f..1f &&
                    box.right in 0f..1f && box.bottom in 0f..1f)
            assertTrue("${field.id} box degenerate: $box",
                box.right - box.left >= 0.05f && box.bottom - box.top >= 0.005f)
        }
        val docNames = parsed.documents.map { it.name }.toSet()
        assertTrue("Missing Aadhaar Card in $docNames", "Aadhaar Card" in docNames)
    }
}
