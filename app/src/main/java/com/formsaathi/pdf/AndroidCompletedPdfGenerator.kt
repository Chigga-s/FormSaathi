package com.formsaathi.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.formsaathi.contracts.CompletedPdfGenerator
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.ParsedForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Production implementation of CompletedPdfGenerator.
 * Renders source PDF page-by-page via PdfRenderer, overlays user answers with PdfPageComposer,
 * and writes a flattened multi-page PDF via PdfDocument with strict memory recycling.
 */
class AndroidCompletedPdfGenerator(
    private val context: Context,
    private val composer: PdfPageComposer = PdfPageComposer()
) : CompletedPdfGenerator {

    override suspend fun generate(
        sourceUri: Uri,
        parsedForm: ParsedForm,
        answers: Map<String, FormAnswer>,
        outputUri: Uri
    ): Unit = withContext(Dispatchers.IO) {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(sourceUri, "r")
            ?: throw IOException("Unable to open source PDF descriptor from URI: $sourceUri")

        var pdfRenderer: PdfRenderer? = null
        var pdfDocument: PdfDocument? = null

        try {
            pdfRenderer = PdfRenderer(pfd)
            pdfDocument = PdfDocument()

            val totalPages = pdfRenderer.pageCount
            if (totalPages == 0) {
                throw IOException("Source PDF contains zero pages.")
            }

            for (pageIndex in 0 until totalPages) {
                val rendererPage = pdfRenderer.openPage(pageIndex)

                // Match with PageInfo if parsed by Role 2, otherwise fallback to renderer dimensions
                val pageInfo = parsedForm.pages.firstOrNull { it.pageIndex == pageIndex }
                val pageWidthPoints = pageInfo?.pdfWidthPoints ?: rendererPage.width.toFloat()
                val pageHeightPoints = pageInfo?.pdfHeightPoints ?: rendererPage.height.toFloat()

                // Render high-clarity bitmap at 2x scale for sharp output while staying within safe memory bounds
                val renderScale = 2.0f
                val bmpWidth = (pageWidthPoints * renderScale).toInt().coerceAtLeast(1)
                val bmpHeight = (pageHeightPoints * renderScale).toInt().coerceAtLeast(1)

                val pageBitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                rendererPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                // Start PdfDocument page
                val pageConfig = PdfDocument.PageInfo.Builder(
                    pageWidthPoints.toInt(),
                    pageHeightPoints.toInt(),
                    pageIndex + 1
                ).create()

                val documentPage = pdfDocument.startPage(pageConfig)

                try {
                    val fieldsOnPage = parsedForm.fields.filter { it.pageIndex == pageIndex }
                    composer.composePage(
                        canvas = documentPage.canvas,
                        pageBitmap = pageBitmap,
                        pageWidthPoints = pageWidthPoints,
                        pageHeightPoints = pageHeightPoints,
                        fieldsOnPage = fieldsOnPage,
                        answers = answers
                    )
                } finally {
                    pdfDocument.finishPage(documentPage)
                    pageBitmap.recycle()
                    rendererPage.close()
                }
            }

            // Write completed document to output stream
            context.contentResolver.openOutputStream(outputUri, "wt")?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
            } ?: throw IOException("Unable to open output stream for URI: $outputUri")

        } finally {
            try {
                pdfDocument?.close()
            } catch (_: Exception) {}
            try {
                pdfRenderer?.close()
            } catch (_: Exception) {}
            try {
                pfd.close()
            } catch (_: Exception) {}
        }
    }
}
