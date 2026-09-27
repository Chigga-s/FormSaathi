package com.formsaathi.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.formsaathi.model.NormalizedRect
import java.io.File
import java.io.FileOutputStream

/**
 * Generates a valid multi-page sample PDF matching the FakeFormParser field layout.
 * Used by the "Quick Mock Session" harness button to provide a real source PDF
 * that PdfRenderer can open, instead of an empty 0-byte file.
 *
 * The generated PDF has 2 pages at 595×842 points (A4). Every printed answer box
 * precisely matches the corresponding FormField.answerBox NormalizedRect in FakeFormParser.
 */
object SamplePdfFactory {

    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(33, 33, 33)
        textSize = 18f
        isFakeBoldText = true
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(50, 50, 50)
        textSize = 11f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 180, 180)
        strokeWidth = 0.8f
        style = Paint.Style.STROKE
    }

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(220, 220, 220)
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }

    /**
     * Creates a valid 2-page PDF in the app cache directory.
     * @return The created File, ready to be passed as a source URI.
     */
    fun createSamplePdf(cacheDir: File): File {
        val exportDir = File(cacheDir, "exports").apply { mkdirs() }
        val target = File(exportDir, "sample_input.pdf")
        if (target.exists()) target.delete()

        val document = PdfDocument()
        try {
            drawPage1(document)
            drawPage2(document)

            FileOutputStream(target).use { out ->
                document.writeTo(out)
                out.flush()
            }
        } finally {
            document.close()
        }

        return target
    }

    private fun drawPage1(document: PdfDocument) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        // Header
        canvas.drawText("Government Application Form — Page 1", 60f, 80f, headerPaint)
        canvas.drawLine(60f, 90f, 535f, 90f, linePaint)

        // Field rows matching FakeFormParser page 0 normalized coordinates exactly:
        // 1. Full Name: label (0.10, 0.15, 0.40, 0.18), answer (0.42, 0.15, 0.90, 0.18)
        drawFieldRow(
            canvas,
            "Full Name / आवेदक का पूरा नाम",
            NormalizedRect(0.10f, 0.15f, 0.40f, 0.18f),
            NormalizedRect(0.42f, 0.15f, 0.90f, 0.18f)
        )

        // 2. Father's Name: label (0.10, 0.20, 0.40, 0.23), answer (0.42, 0.20, 0.90, 0.23)
        drawFieldRow(
            canvas,
            "Father's Name / पिता का नाम",
            NormalizedRect(0.10f, 0.20f, 0.40f, 0.23f),
            NormalizedRect(0.42f, 0.20f, 0.90f, 0.23f)
        )

        // 3. Date of Birth: label (0.10, 0.25, 0.40, 0.28), answer (0.42, 0.25, 0.70, 0.28)
        drawFieldRow(
            canvas,
            "Date of Birth (DD/MM/YYYY) / जन्म तिथि",
            NormalizedRect(0.10f, 0.25f, 0.40f, 0.28f),
            NormalizedRect(0.42f, 0.25f, 0.70f, 0.28f)
        )

        // 4. Gender: label (0.10, 0.30, 0.30, 0.33), answer (0.32, 0.30, 0.60, 0.33)
        drawFieldRow(
            canvas,
            "Gender / लिंग",
            NormalizedRect(0.10f, 0.30f, 0.30f, 0.33f),
            NormalizedRect(0.32f, 0.30f, 0.60f, 0.33f)
        )

        // 5. Mobile Number: label (0.10, 0.35, 0.35, 0.38), answer (0.38, 0.35, 0.75, 0.38f)
        drawFieldRow(
            canvas,
            "Mobile Number / मोबाइल नंबर",
            NormalizedRect(0.10f, 0.35f, 0.35f, 0.38f),
            NormalizedRect(0.38f, 0.35f, 0.75f, 0.38f)
        )

        // 6. Email Address: label (0.10, 0.40, 0.35, 0.43), answer (0.38, 0.40, 0.85, 0.43)
        drawFieldRow(
            canvas,
            "Email Address / ईमेल",
            NormalizedRect(0.10f, 0.40f, 0.35f, 0.43f),
            NormalizedRect(0.38f, 0.40f, 0.85f, 0.43f)
        )

        // 7. Aadhaar Number: label (0.10, 0.45, 0.38, 0.48), answer (0.40, 0.45, 0.80, 0.48)
        drawFieldRow(
            canvas,
            "Aadhaar Number / आधार संख्या",
            NormalizedRect(0.10f, 0.45f, 0.38f, 0.48f),
            NormalizedRect(0.40f, 0.45f, 0.80f, 0.48f)
        )

        // 8. Permanent Address: label (0.10, 0.52, 0.40, 0.55), answer (0.10, 0.56, 0.90, 0.64)
        val permLabelBox = NormalizedRect(0.10f, 0.52f, 0.40f, 0.55f)
        val permAnswerBox = NormalizedRect(0.10f, 0.56f, 0.90f, 0.64f)
        canvas.drawText("Permanent Address / स्थायी पता", permLabelBox.left * PAGE_WIDTH, (permLabelBox.top * PAGE_HEIGHT) + 16f, labelPaint)
        drawAnswerBox(canvas, permAnswerBox)
        val permSepY = (permAnswerBox.bottom * PAGE_HEIGHT) + 4f
        canvas.drawLine(permLabelBox.left * PAGE_WIDTH, permSepY, 535f, permSepY, linePaint)

        // 9. Same as Permanent Address: label (0.10, 0.66, 0.60, 0.69), answer (0.62, 0.66, 0.80, 0.69)
        drawFieldRow(
            canvas,
            "Is Current Address same as Permanent Address? (Yes/No)",
            NormalizedRect(0.10f, 0.66f, 0.60f, 0.69f),
            NormalizedRect(0.62f, 0.66f, 0.80f, 0.69f)
        )

        // 10. Current Address: label (0.10, 0.71, 0.40, 0.74), answer (0.10, 0.75, 0.90, 0.83)
        val currLabelBox = NormalizedRect(0.10f, 0.71f, 0.40f, 0.74f)
        val currAnswerBox = NormalizedRect(0.10f, 0.75f, 0.90f, 0.83f)
        canvas.drawText("Current Address / वर्तमान पता", currLabelBox.left * PAGE_WIDTH, (currLabelBox.top * PAGE_HEIGHT) + 16f, labelPaint)
        drawAnswerBox(canvas, currAnswerBox)
        val currSepY = (currAnswerBox.bottom * PAGE_HEIGHT) + 4f
        canvas.drawLine(currLabelBox.left * PAGE_WIDTH, currSepY, 535f, currSepY, linePaint)

        // Footer
        canvas.drawText("Page 1 of 2", (PAGE_WIDTH / 2f) - 25f, PAGE_HEIGHT - 30f, labelPaint)

        document.finishPage(page)
    }

    private fun drawPage2(document: PdfDocument) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 2).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        canvas.drawText("Government Application Form — Page 2", 60f, 80f, headerPaint)
        canvas.drawLine(60f, 90f, 535f, 90f, linePaint)

        // 11. Category: label (0.10, 0.15, 0.35, 0.18), answer (0.38, 0.15, 0.70, 0.18)
        drawFieldRow(
            canvas,
            "Category (General/OBC/SC/ST) / वर्ग",
            NormalizedRect(0.10f, 0.15f, 0.35f, 0.18f),
            NormalizedRect(0.38f, 0.15f, 0.70f, 0.18f)
        )

        // 12. Annual Family Income: label (0.10, 0.22, 0.40, 0.25), answer (0.42, 0.22, 0.75, 0.25)
        drawFieldRow(
            canvas,
            "Annual Family Income / वार्षिक आय",
            NormalizedRect(0.10f, 0.22f, 0.40f, 0.25f),
            NormalizedRect(0.42f, 0.22f, 0.75f, 0.25f)
        )

        // Document requirements section
        val reqY = 0.40f * PAGE_HEIGHT
        canvas.drawText("Required Documents:", 60f, reqY, headerPaint)
        canvas.drawText("1. Aadhaar Card — Self-attested photocopy", 80f, reqY + 25f, labelPaint)
        canvas.drawText("2. Income Certificate — Issued within last 6 months", 80f, reqY + 45f, labelPaint)

        // Declaration
        val declY = 0.60f * PAGE_HEIGHT
        canvas.drawText("Declaration:", 60f, declY, headerPaint)
        canvas.drawText("I hereby declare that the information provided is true and correct.", 80f, declY + 25f, labelPaint)

        // Signature box
        canvas.drawText("Signature:", 60f, 0.75f * PAGE_HEIGHT, labelPaint)
        canvas.drawRect(60f, 0.76f * PAGE_HEIGHT, 250f, 0.84f * PAGE_HEIGHT, boxPaint)

        canvas.drawText("Page 2 of 2", (PAGE_WIDTH / 2f) - 25f, PAGE_HEIGHT - 30f, labelPaint)

        document.finishPage(page)
    }

    /**
     * Draws a single-line field row using exact NormalizedRects for label and answer box.
     */
    private fun drawFieldRow(
        canvas: Canvas,
        label: String,
        labelBox: NormalizedRect,
        answerBox: NormalizedRect
    ) {
        val labelY = (labelBox.top * PAGE_HEIGHT) + 16f
        canvas.drawText(label, labelBox.left * PAGE_WIDTH, labelY, labelPaint)
        drawAnswerBox(canvas, answerBox)
        val sepY = (maxOf(labelBox.bottom, answerBox.bottom) * PAGE_HEIGHT) + 4f
        canvas.drawLine(labelBox.left * PAGE_WIDTH, sepY, 535f, sepY, linePaint)
    }

    /**
     * Draws an answer box exactly matching its NormalizedRect coordinates.
     */
    private fun drawAnswerBox(canvas: Canvas, box: NormalizedRect) {
        canvas.drawRect(
            box.left * PAGE_WIDTH,
            box.top * PAGE_HEIGHT,
            box.right * PAGE_WIDTH,
            box.bottom * PAGE_HEIGHT,
            boxPaint
        )
    }
}
