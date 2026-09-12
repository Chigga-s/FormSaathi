package com.formsaathi.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Generates a valid multi-page sample PDF matching the FakeFormParser field layout.
 * Used by the "Quick Mock Session" harness button to provide a real source PDF
 * that PdfRenderer can open, instead of an empty 0-byte file.
 *
 * The generated PDF has 2 pages at 595×842 points (A4), with labeled field rows
 * and separator lines at positions matching the normalized coordinates in FakeFormParser.
 */
object SamplePdfFactory {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

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
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        // Header
        canvas.drawText("Government Application Form — Page 1", 60f, 80f, headerPaint)
        canvas.drawLine(60f, 90f, 535f, 90f, linePaint)

        // Field rows matching FakeFormParser page 0 normalized coordinates
        drawFieldRow(canvas, "Full Name / आवेदक का पूरा नाम", 0.15f)
        drawFieldRow(canvas, "Father's Name / पिता का नाम", 0.20f)
        drawFieldRow(canvas, "Date of Birth (DD/MM/YYYY) / जन्म तिथि", 0.25f)
        drawFieldRow(canvas, "Gender / लिंग", 0.30f)
        drawFieldRow(canvas, "Mobile Number / मोबाइल नंबर", 0.35f)
        drawFieldRow(canvas, "Email Address / ईमेल", 0.40f)
        drawFieldRow(canvas, "Aadhaar Number / आधार संख्या", 0.45f)

        // Permanent address — taller box
        val addrY = (0.52f * PAGE_HEIGHT)
        canvas.drawText("Permanent Address / स्थायी पता", 60f, addrY, labelPaint)
        val boxTop = 0.56f * PAGE_HEIGHT
        val boxBottom = 0.64f * PAGE_HEIGHT
        canvas.drawRect(60f, boxTop, (0.90f * PAGE_WIDTH), boxBottom, boxPaint)

        // Same as permanent
        val sameY = (0.66f * PAGE_HEIGHT)
        canvas.drawText("Is Current Address same as Permanent Address? (Yes/No)", 60f, sameY, labelPaint)
        drawAnswerBox(canvas, 0.62f, 0.80f, 0.66f, 0.69f)

        // Current address — taller box
        val currY = (0.71f * PAGE_HEIGHT)
        canvas.drawText("Current Address / वर्तमान पता", 60f, currY, labelPaint)
        canvas.drawRect(60f, 0.75f * PAGE_HEIGHT, 0.90f * PAGE_WIDTH, 0.83f * PAGE_HEIGHT, boxPaint)

        // Footer
        canvas.drawText("Page 1 of 2", (PAGE_WIDTH / 2f) - 25f, PAGE_HEIGHT - 30f, labelPaint)

        document.finishPage(page)
    }

    private fun drawPage2(document: PdfDocument) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        canvas.drawText("Government Application Form — Page 2", 60f, 80f, headerPaint)
        canvas.drawLine(60f, 90f, 535f, 90f, linePaint)

        drawFieldRow(canvas, "Category (General/OBC/SC/ST) / वर्ग", 0.15f)
        drawFieldRow(canvas, "Annual Family Income / वार्षिक आय", 0.22f)

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
     * Draws a single-line field row: label on the left, answer box on the right.
     */
    private fun drawFieldRow(canvas: Canvas, label: String, normalizedY: Float) {
        val y = normalizedY * PAGE_HEIGHT
        canvas.drawText(label, 60f, y, labelPaint)
        // Answer box to the right
        val boxLeft = 0.42f * PAGE_WIDTH
        val boxRight = 0.90f * PAGE_WIDTH
        val boxTop = y - 10f
        val boxBottom = y + 4f
        canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, boxPaint)
        // Separator line below
        canvas.drawLine(60f, y + 8f, 535f, y + 8f, linePaint)
    }

    private fun drawAnswerBox(
        canvas: Canvas,
        normLeft: Float, normRight: Float,
        normTop: Float, normBottom: Float
    ) {
        canvas.drawRect(
            normLeft * PAGE_WIDTH,
            normTop * PAGE_HEIGHT,
            normRight * PAGE_WIDTH,
            normBottom * PAGE_HEIGHT,
            boxPaint
        )
    }
}
