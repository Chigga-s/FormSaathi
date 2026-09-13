package com.formsaathi.formengine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.formsaathi.answer.RuleBasedAnswerProcessor
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect
import com.formsaathi.model.ParsedForm
import com.formsaathi.model.SupportedLanguage
import com.formsaathi.pdf.AndroidCompletedPdfGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Acceptance gate for both supported demo templates, run end to end on a device:
 * parse the real PDF with the real OCR pipeline, fill every field through the
 * real normalizer, generate the flattened PDF, and check where the answers landed.
 *
 * Each run also exports a PNG of every generated page under the app's external
 * files directory so the result can be inspected by eye, which is the only way to
 * confirm placement finally looks right.
 */
@RunWith(AndroidJUnit4::class)
class SupportedTemplatesAcceptanceTest {

    private val fictionalAnswers = mapOf(
        FieldType.FULL_NAME to "Aarav Sharma",
        FieldType.FATHER_NAME to "Ramesh Sharma",
        FieldType.MOTHER_NAME to "Sunita Sharma",
        FieldType.DATE_OF_BIRTH to "12/02/2006",
        FieldType.GENDER to "Male",
        FieldType.MOBILE to "9876543210",
        FieldType.EMAIL to "aarav@example.com",
        FieldType.AADHAAR to "123456789012",
        FieldType.PERMANENT_ADDRESS to "Flat 402, Lotus Heights, Sinhagad Road, Pune, Maharashtra 411041",
        FieldType.CURRENT_ADDRESS to "Flat 402, Lotus Heights, Sinhagad Road, Pune, Maharashtra 411041",
        FieldType.STATE to "Maharashtra",
        FieldType.DISTRICT to "Pune",
        FieldType.PINCODE to "411041",
        FieldType.CATEGORY to "General",
        FieldType.ANNUAL_INCOME to "250000"
    )

    @Test
    fun underlineTemplateIsParsedAndFilledCorrectly() {
        runTemplate(
            assetName = "FormSaathi_Test_Form_1_Simple.pdf",
            outputPrefix = "form1-underline",
            minimumFields = 8,
            requiredTypes = listOf(
                FieldType.FULL_NAME,
                FieldType.FATHER_NAME,
                FieldType.DATE_OF_BIRTH,
                FieldType.MOBILE,
                FieldType.AADHAAR,
                FieldType.DISTRICT,
                FieldType.PINCODE,
                FieldType.ANNUAL_INCOME
            )
        )
    }

    @Test
    fun boxedTemplateIsParsedAndFilledCorrectly() {
        val report = runTemplate(
            assetName = "FormSaathi_Test_Form_2_Boxed.pdf",
            outputPrefix = "form2-boxed",
            minimumFields = 7,
            requiredTypes = listOf(
                FieldType.FULL_NAME,
                FieldType.FATHER_NAME,
                FieldType.DATE_OF_BIRTH,
                FieldType.MOBILE,
                FieldType.CATEGORY,
                FieldType.ANNUAL_INCOME,
                FieldType.AADHAAR
            )
        )

        // The defining defect of this template was answers drawn in the gap
        // between the label and the printed box. Every field on this form has a
        // printed box starting at x ~0.37, so every answer must begin at or after
        // it, never immediately after the label.
        val misplaced = report.fields.filter { field ->
            field.type != FieldType.UNKNOWN &&
                field.type != FieldType.SIGNATURE &&
                field.type != FieldType.PHOTO &&
                field.answerBox.left < BOXED_FIELD_LEFT_EDGE - 0.02f
        }
        assertTrue(
            "Answers must be written inside the printed boxes, not beside the labels. " +
                "Misplaced: " + misplaced.joinToString { "${it.sourceLabel}@${it.answerBox.left}" },
            misplaced.isEmpty()
        )
    }

    private data class Report(val parsed: ParsedForm, val fields: List<FormField>)

    private fun runTemplate(
        assetName: String,
        outputPrefix: String,
        minimumFields: Int,
        requiredTypes: List<FieldType>
    ): Report {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context

        val sourceFile = File(appContext.cacheDir, "acceptance_$assetName")
        testContext.assets.open("debugforms/$assetName").use { input ->
            sourceFile.outputStream().use { input.copyTo(it) }
        }
        val sourceUri = Uri.fromFile(sourceFile)

        val ocr = MlKitOcrEngine()
        val parsed = try {
            val parser = RealFormParser(
                pageRenderer = AndroidPdfPageRenderer(appContext),
                ocrEngine = ocr,
                labelDetector = LabelDetector(),
                fieldMapper = FieldMapper(),
                answerBoxEstimator = AnswerBoxEstimator(),
                requirementExtractor = RequirementExtractor(),
                ocrScript = OcrScript.LATIN
            )
            runBlocking { parser.parse(sourceUri) }
        } finally {
            ocr.close()
        }

        val summary = StringBuilder()
        summary.appendLine("template: $assetName")
        summary.appendLine("pages: ${parsed.pages.size}")
        summary.appendLine("fields: ${parsed.fields.size}")
        parsed.fields.forEach {
            summary.appendLine(
                "  ${it.type}  '${it.sourceLabel}'  label=${it.labelBox.pretty()}  answer=${it.answerBox.pretty()}  conf=${it.confidence}"
            )
        }
        summary.appendLine("documents: ${parsed.documents.map { it.name }}")
        parsed.warnings.forEach { summary.appendLine("  warning: $it") }
        publish(appContext, "$outputPrefix-fields.txt", "text/plain") {
            it.write(summary.toString().toByteArray())
        }

        assertTrue(
            "Expected at least $minimumFields useful fields, got ${parsed.fields.size}\n$summary",
            parsed.fields.size >= minimumFields
        )
        val detectedTypes = parsed.fields.map { it.type }.toSet()
        val missing = requiredTypes.filterNot { it in detectedTypes }
        assertTrue("Missing expected field types $missing\n$summary", missing.isEmpty())

        // -- placement invariants on the real detected geometry ----------------
        for (field in parsed.fields) {
            val box = field.answerBox
            assertTrue("Answer box out of page for '${field.sourceLabel}': $box", box.left >= 0f && box.right <= 1f)
            assertTrue("Answer box out of page for '${field.sourceLabel}': $box", box.top >= 0f && box.bottom <= 1f)
            assertTrue("Empty answer box for '${field.sourceLabel}': $box", box.width > 0.01f && box.height > 0.002f)
            assertTrue(
                "Answer box equals the label box for '${field.sourceLabel}'",
                box != field.labelBox
            )
            assertTrue(
                "Answer box overlaps its own label for '${field.sourceLabel}': answer=$box label=${field.labelBox}",
                !overlaps(box, field.labelBox)
            )
        }
        for (i in parsed.fields.indices) {
            for (j in i + 1 until parsed.fields.size) {
                val a = parsed.fields[i]
                val b = parsed.fields[j]
                if (a.pageIndex != b.pageIndex) continue
                assertTrue(
                    "Answer boxes overlap: '${a.sourceLabel}' ${a.answerBox} and '${b.sourceLabel}' ${b.answerBox}",
                    !overlaps(a.answerBox, b.answerBox)
                )
                assertTrue(
                    "Answer for '${a.sourceLabel}' overlaps the label of '${b.sourceLabel}'",
                    !overlaps(a.answerBox, b.labelBox)
                )
            }
        }

        // -- generate the completed PDF and export page images -----------------
        val processor = RuleBasedAnswerProcessor()
        val answers = parsed.fields.mapNotNull { field ->
            val raw = fictionalAnswers[field.type] ?: return@mapNotNull null
            val normalized = processor.normalize(field.type, raw, SupportedLanguage.ENGLISH)
            field.id to FormAnswer(field.id, raw, normalized, AnswerSource.TYPED)
        }.toMap()

        assertTrue("No answers could be produced for $assetName", answers.isNotEmpty())

        val outputFile = File(appContext.cacheDir, "$outputPrefix-completed.pdf")
        val result = runBlocking {
            AndroidCompletedPdfGenerator(appContext).generate(
                sourceUri = sourceUri,
                parsedForm = parsed,
                answers = answers,
                outputUri = Uri.fromFile(outputFile)
            )
        }
        assertTrue("Generated PDF is empty", outputFile.length() > 0L)

        val sourcePages = parsed.pages.size
        val renderedPages = exportPdfPages(appContext, outputFile, outputPrefix)
        assertTrue(
            "Generated PDF has $renderedPages pages, source had $sourcePages",
            renderedPages == sourcePages
        )

        publish(appContext, "$outputPrefix-warnings.txt", "text/plain") { out ->
            out.write(result.warnings.joinToString("\n") { "${it.fieldLabel}: ${it.reason}" }.toByteArray())
        }
        publish(appContext, "$outputPrefix-completed.pdf", "application/pdf") { out ->
            outputFile.inputStream().use { it.copyTo(out) }
        }

        return Report(parsed, parsed.fields)
    }

    /**
     * Writes an artifact to Downloads/FormSaathiAcceptance. The app is uninstalled
     * when the instrumentation run finishes, so anything left in app storage would
     * be gone before it could be inspected.
     */
    private fun publish(
        context: Context,
        name: String,
        mimeType: String,
        write: (java.io.OutputStream) -> Unit
    ) {
        val resolver = context.contentResolver
        val folder = Environment.DIRECTORY_DOWNLOADS + "/FormSaathiAcceptance"
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH} LIKE ?",
            arrayOf(name, "%FormSaathiAcceptance%"),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                resolver.delete(
                    android.content.ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, id
                    ),
                    null,
                    null
                )
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, folder)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert failed for $name")
        resolver.openOutputStream(uri)!!.use(write)
    }

    /** Renders every page of [pdf] to PNG so placement can be checked by eye. */
    private fun exportPdfPages(context: Context, pdf: File, prefix: String): Int {
        android.os.ParcelFileDescriptor.open(
            pdf,
            android.os.ParcelFileDescriptor.MODE_READ_ONLY
        ).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                for (index in 0 until renderer.pageCount) {
                    renderer.openPage(index).use { page ->
                        val scale = 1400f / maxOf(page.width, page.height)
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt(),
                            (page.height * scale).toInt(),
                            Bitmap.Config.ARGB_8888
                        )
                        try {
                            Canvas(bitmap).drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            publish(context, "$prefix-page${index + 1}.png", "image/png") {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
                return renderer.pageCount
            }
        }
    }

    private fun overlaps(a: NormalizedRect, b: NormalizedRect): Boolean =
        a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

    private fun NormalizedRect.pretty(): String =
        "[%.4f,%.4f,%.4f,%.4f]".format(left, top, right, bottom)

    private companion object {
        /** Left edge of the printed answer boxes on the boxed template. */
        const val BOXED_FIELD_LEFT_EDGE = 0.3717f
    }
}
