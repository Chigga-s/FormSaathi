package com.formsaathi.formengine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.OutputStream

@RunWith(AndroidJUnit4::class)
class FormParseDiagnosticsTest {

    @Test
    fun diagnoseTestForm1() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val report = StringBuilder()

        fun log(line: String) {
            Log.i("FORMSAATHI_DIAG", line)
            report.appendLine(line)
        }

        val pdfFile = File(appContext.cacheDir, "diag_form1.pdf")
        testContext.assets.open("debugforms/FormSaathi_Test_Form_1_Simple.pdf").use { input ->
            pdfFile.outputStream().use { input.copyTo(it) }
        }
        val uri = Uri.fromFile(pdfFile)

        val renderer = AndroidPdfPageRenderer(appContext)
        val ocr = MlKitOcrEngine()
        val detector = LabelDetector()
        val mapper = FieldMapper()
        val estimator = AnswerBoxEstimator()
        val extractor = RequirementExtractor()

        val pageCount = runBlocking { renderer.getPageCount(uri) }
        log("PAGE_COUNT=$pageCount")

        val allBlocks = mutableListOf<OcrBlock>()
        val allRules = mutableListOf<RuleLine>()
        for (pageIndex in 0 until pageCount) {
            val rendered = runBlocking { renderer.renderPage(uri, pageIndex) }
            log("PAGE $pageIndex info=${rendered.info}")
            publishBitmap(appContext, rendered.bitmap, "render_p$pageIndex.png")
            val rules = RuleLineDetector.detect(
                RuleLineDetector.luminanceOf(rendered.bitmap),
                rendered.bitmap.width, rendered.bitmap.height, pageIndex
            )
            allRules.addAll(rules)
            log("PAGE $pageIndex RULES=${rules.size}")
            rules.forEach { r -> log("RULE $r") }
            runBlocking { rawOcrStats(appContext, rendered.bitmap, ::log) }
            val blocks = runBlocking {
                ocr.recognize(rendered.bitmap, pageIndex, OcrScript.LATIN)
            }
            log("PAGE $pageIndex OCR_BLOCKS=${blocks.size}")
            blocks.forEach { b ->
                log("OCR p=${b.pageIndex} blk=${b.blockIndex} line=${b.lineIndex} box=${b.box} conf=${b.confidence} text='${b.text}'")
            }
            allBlocks.addAll(blocks)
            rendered.bitmap.recycle()
        }

        val candidates = detector.detect(allBlocks)
        log("CANDIDATES=${candidates.size}")
        candidates.forEach { c ->
            val match = mapper.map(c.text)
            log("LABEL '${c.text}' -> ${match.type} conf=${match.confidence} box=${c.box}")
            val answerBox = estimator.estimate(c, allBlocks, allRules)
            log("  ANSWER_BOX=$answerBox")
        }

        val dropped = allBlocks - candidates.toSet()
        log("DROPPED_BY_DETECTOR=${dropped.size}")
        dropped.forEach { b -> log("DROPPED '${b.text}' box=${b.box}") }

        val parser = RealFormParser(renderer, ocr, detector, mapper, estimator, extractor, OcrScript.LATIN)
        val parsed = runBlocking { parser.parse(uri) }
        log("PARSED fields=${parsed.fields.size} warnings=${parsed.warnings.size} docs=${parsed.documents.size}")
        parsed.fields.forEach { f ->
            log("FIELD id=${f.id} type=${f.type} label='${f.sourceLabel}' labelBox=${f.labelBox} answerBox=${f.answerBox} conf=${f.confidence}")
        }
        parsed.warnings.forEach { log("WARNING $it") }
        parsed.documents.forEach { log("DOC ${it.name} :: ${it.requirement}") }

        for (pageIndex in 0 until pageCount) {
            val rendered = runBlocking { renderer.renderPage(uri, pageIndex) }
            val overlay = DebugOverlayRenderer().draw(rendered.bitmap, parsed.fields, pageIndex)
            publishBitmap(appContext, overlay, "overlay_p$pageIndex.png")
            rendered.bitmap.recycle()
        }

        publishText(appContext, "report.txt", report.toString())
        Log.i("FORMSAATHI_DIAG", "PUBLISHED to Downloads/FormSaathiDiag")
    }

    private suspend fun rawOcrStats(context: Context, bitmap: Bitmap, log: (String) -> Unit) {
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result = client.process(InputImage.fromBitmap(bitmap, 0)).await()
            var lines = 0
            var nullBoxes = 0
            var elements = 0
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    lines++
                    if (line.boundingBox == null) nullBoxes++
                    elements += line.elements.size
                }
            }
            log("RAW_OCR blocks=${result.textBlocks.size} lines=$lines nullLineBoxes=$nullBoxes elements=$elements fullTextChars=${result.text.length}")
            log("RAW_OCR_TEXT<<${result.text.take(1500)}>>")
        } finally {
            client.close()
        }
    }

    private fun openDownload(context: Context, name: String, mime: String): OutputStream {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FormSaathiDiag")
        }
        val target = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        ) ?: error("MediaStore insert failed for $name")
        return context.contentResolver.openOutputStream(target)
            ?: error("MediaStore open failed for $name")
    }

    private fun publishBitmap(context: Context, bitmap: Bitmap, name: String) {
        openDownload(context, name, "image/png").use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Log.i("FORMSAATHI_DIAG", "PUBLISHED $name")
    }

    private fun publishText(context: Context, name: String, text: String) {
        openDownload(context, name, "text/plain").use { out ->
            out.write(text.toByteArray())
        }
        Log.i("FORMSAATHI_DIAG", "PUBLISHED $name ${text.length} chars")
    }
}
