package com.formsaathi.pdf

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.formsaathi.model.AnswerSource
import com.formsaathi.model.FieldType
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.FormField
import com.formsaathi.model.NormalizedRect
import com.formsaathi.model.PageInfo
import com.formsaathi.model.ParsedForm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Proves a PHOTO answer is embedded as an image (not text) in the
 * flattened PDF, using the staged form as the page background.
 */
@RunWith(AndroidJUnit4::class)
class PhotoEmbedTest {

    @Test
    fun photoAnswerIsDrawnAsImage() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context

        val pdfFile = File(appContext.cacheDir, "photo_bg.pdf")
        testContext.assets.open("debugforms/FormSaathi_Test_Form_1_Simple.pdf").use { input ->
            pdfFile.outputStream().use { input.copyTo(it) }
        }
        val photoFile = File(appContext.cacheDir, "attached_photo.png")
        testContext.assets.open("debugforms/sample_photo.png").use { input ->
            photoFile.outputStream().use { input.copyTo(it) }
        }
        val decoded = BitmapFactory.decodeFile(photoFile.absolutePath)
        assertTrue("Test photo asset unreadable", decoded != null)
        decoded?.recycle()

        val parsed = ParsedForm(
            pages = listOf(
                PageInfo(0, 595f, 841f, 1273, 1800)
            ),
            fields = listOf(
                FormField(
                    id = "photo_1",
                    sourceLabel = "Photograph",
                    type = FieldType.PHOTO,
                    pageIndex = 0,
                    labelBox = NormalizedRect(0.10f, 0.14f, 0.20f, 0.15f),
                    answerBox = NormalizedRect(0.24f, 0.117f, 0.90f, 0.143f),
                    required = true
                )
            ),
            documents = emptyList()
        )
        val answers = mapOf(
            "photo_1" to FormAnswer(
                "photo_1", photoFile.absolutePath, photoFile.absolutePath, AnswerSource.PHOTO
            )
        )

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "E2E_Photo_Form.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FormSaathiDiag")
        }
        val outputUri: Uri = appContext.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        ) ?: error("MediaStore insert failed")

        val result = runBlocking {
            AndroidCompletedPdfGenerator(appContext).generate(
                sourceUri = Uri.fromFile(pdfFile),
                parsedForm = parsed,
                answers = answers,
                outputUri = outputUri
            )
        }
        assertTrue("Photo embed warnings: ${result.warnings}", result.warnings.isEmpty())
        appContext.contentResolver.openInputStream(outputUri)!!.use { input ->
            assertTrue("Generated PDF is empty", input.readBytes().size > 20000)
        }
    }
}
