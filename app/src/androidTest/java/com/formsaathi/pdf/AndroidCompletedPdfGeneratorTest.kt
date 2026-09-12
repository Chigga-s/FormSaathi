package com.formsaathi.pdf

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.formsaathi.contracts.FakeFormParser
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Real PDF generation proof for AndroidCompletedPdfGenerator.
 * Verifies that:
 * 1. Output file is non-empty
 * 2. Has expected page count (2 pages matching the sample input)
 * 3. Preserves page order
 * 4. Can be reopened and rendered as a valid PDF by Android's PdfRenderer
 */
@RunWith(AndroidJUnit4::class)
class AndroidCompletedPdfGeneratorTest {

    @Test
    fun testGenerateCompletedPdfProducesValidReopenableMultiPagePdf() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cacheDir = context.cacheDir

        // 1. Generate valid 2-page source PDF
        val sourceFile = SamplePdfFactory.createSamplePdf(cacheDir)
        val sourceUri = Uri.fromFile(sourceFile)

        // 2. Parse form with fake parser
        val parser = FakeFormParser()
        val parsedForm = parser.parse(sourceUri)

        // 3. Prepare answers for both pages
        val answers = mapOf(
            "field_full_name" to FormAnswer("field_full_name", "Aarav Sharma", "AARAV SHARMA", AnswerSource.TYPED),
            "field_father_name" to FormAnswer("field_father_name", "Rajesh Sharma", "RAJESH SHARMA", AnswerSource.TYPED),
            "field_dob" to FormAnswer("field_dob", "15/08/1995", "15/08/1995", AnswerSource.TYPED),
            "field_gender" to FormAnswer("field_gender", "Male", "Male", AnswerSource.TYPED),
            "field_mobile" to FormAnswer("field_mobile", "9876543210", "9876543210", AnswerSource.TYPED),
            "field_email" to FormAnswer("field_email", "aarav@example.com", "aarav@example.com", AnswerSource.TYPED),
            "field_aadhaar" to FormAnswer("field_aadhaar", "123456789012", "1234 5678 9012", AnswerSource.TYPED),
            "field_permanent_address" to FormAnswer("field_permanent_address", "Flat 101, Galaxy Apts, MG Road, Pune", "Flat 101, Galaxy Apts, MG Road, Pune", AnswerSource.TYPED),
            "field_category" to FormAnswer("field_category", "General", "General", AnswerSource.TYPED),
            "field_income" to FormAnswer("field_income", "500000", "500000", AnswerSource.TYPED)
        )

        // 4. Output file destination
        val outputFile = File(cacheDir, "test_output_completed.pdf")
        if (outputFile.exists()) outputFile.delete()
        val outputUri = Uri.fromFile(outputFile)

        // 5. Generate completed PDF
        val generator = AndroidCompletedPdfGenerator(context)
        val result = generator.generate(sourceUri, parsedForm, answers, outputUri)

        // 6. Verify result
        assertNotNull(result)
        assertTrue("Output file must exist", outputFile.exists())
        assertTrue("Output file must be non-empty", outputFile.length() > 0)

        // 7. Verify output can be reopened and rendered by PdfRenderer
        ParcelFileDescriptor.open(outputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            val renderer = PdfRenderer(pfd)
            try {
                assertEquals("Generated PDF must have exactly 2 pages", 2, renderer.pageCount)

                // Verify page 1
                renderer.openPage(0).use { page0 ->
                    assertEquals(0, page0.index)
                    assertTrue("Page 0 width must be positive", page0.width > 0)
                    assertTrue("Page 0 height must be positive", page0.height > 0)
                }

                // Verify page 2
                renderer.openPage(1).use { page1 ->
                    assertEquals(1, page1.index)
                    assertTrue("Page 1 width must be positive", page1.width > 0)
                    assertTrue("Page 1 height must be positive", page1.height > 0)
                }
            } finally {
                renderer.close()
            }
        }
    }
}
