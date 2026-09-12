package com.formsaathi.pdf

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.formsaathi.answer.RuleBasedAnswerProcessor
import com.formsaathi.formengine.AndroidPdfPageRenderer
import com.formsaathi.formengine.AnswerBoxEstimator
import com.formsaathi.formengine.FieldMapper
import com.formsaathi.formengine.LabelDetector
import com.formsaathi.formengine.MlKitOcrEngine
import com.formsaathi.formengine.OcrScript
import com.formsaathi.formengine.RealFormParser
import com.formsaathi.formengine.RequirementExtractor
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.SupportedLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end proof with real engines: parse the staged form, answer every
 * field with fictional values through the real normalizer, generate the
 * flattened PDF and publish it to Downloads for visual inspection.
 */
@RunWith(AndroidJUnit4::class)
class FormEndToEndPlacementTest {

    private val fictionalAnswers = mapOf(
        FieldType.FULL_NAME to "Aarav Sharma",
        FieldType.FATHER_NAME to "Ramesh Sharma",
        FieldType.MOTHER_NAME to "Sunita Sharma",
        FieldType.DATE_OF_BIRTH to "12/02/2006",
        FieldType.GENDER to "Male",
        FieldType.MOBILE to "9876543210",
        FieldType.EMAIL to "aarav@example.com",
        FieldType.AADHAAR to "123456789012",
        FieldType.PERMANENT_ADDRESS to "42 MG Road, Pune 411001",
        FieldType.CURRENT_ADDRESS to "42 MG Road, Pune 411001",
        FieldType.STATE to "Maharashtra",
        FieldType.DISTRICT to "Pune",
        FieldType.PINCODE to "411001",
        FieldType.CATEGORY to "General",
        FieldType.ANNUAL_INCOME to "250000"
    )

    @Test
    fun realEnginesProduceFilledPdf() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context

        val pdfFile = File(appContext.cacheDir, "e2e_form1.pdf")
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
        assertTrue("Expected all 15 fields, got ${parsed.fields.size}", parsed.fields.size == 15)

        val processor = RuleBasedAnswerProcessor()
        val answers = parsed.fields.associate { field ->
            val raw = fictionalAnswers[field.type] ?: "test"
            val normalized = processor.normalize(field.type, raw, SupportedLanguage.ENGLISH)
            field.id to FormAnswer(field.id, raw, normalized, AnswerSource.TYPED)
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "E2E_Completed_Form.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FormSaathiDiag")
        }
        val outputUri: Uri = appContext.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        ) ?: error("MediaStore insert failed")

        runBlocking {
            AndroidCompletedPdfGenerator(appContext).generate(
                sourceUri = Uri.fromFile(pdfFile),
                parsedForm = parsed,
                answers = answers,
                outputUri = outputUri
            )
        }

        appContext.contentResolver.openInputStream(outputUri)!!.use { input ->
            val bytes = input.readBytes()
            assertTrue("Generated PDF is empty", bytes.size > 20000)
        }
    }
}
