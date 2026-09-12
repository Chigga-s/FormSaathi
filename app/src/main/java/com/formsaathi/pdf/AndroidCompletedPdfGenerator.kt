package com.formsaathi.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.formsaathi.contracts.CompletedPdfGenerator
import com.formsaathi.model.FormAnswer
import com.formsaathi.model.GenerationResult
import com.formsaathi.model.ParsedForm
import com.formsaathi.model.TextFitWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.sqrt

/**
 * Production implementation of CompletedPdfGenerator.
 * Renders source PDF page-by-page via PdfRenderer, overlays user answers with PdfPageComposer,
 * and writes a flattened multi-page PDF via PdfDocument with strict memory recycling.
 *
 * Memory safety: bitmap pixel count is capped at [maxBitmapPixels] per page.
 * For pages that would exceed this budget, render scale is reduced dynamically.
 */
class AndroidCompletedPdfGenerator(
    private val context: Context,
    private val composer: PdfPageComposer? = null,
    private val maxBitmapPixels: Int = 4_000_000  // ~16MB ARGB_8888, safe for budget phones
) : CompletedPdfGenerator {

    private fun composer(): PdfPageComposer {
        return composer ?: PdfPageComposer(imageLoader = ::decodePhoto)
    }

    private fun decodePhoto(path: String): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) {
                sample *= 2
            }
            BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun generate(
        sourceUri: Uri,
        parsedForm: ParsedForm,
        answers: Map<String, FormAnswer>,
        outputUri: Uri
    ): GenerationResult = withContext(Dispatchers.IO) {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(sourceUri, "r")
            ?: throw IOException("Unable to open source PDF descriptor from URI: $sourceUri")

        var pdfRenderer: PdfRenderer? = null
        var pdfDocument: PdfDocument? = null
        val allWarnings = mutableListOf<TextFitWarning>()

        try {
            pdfRenderer = PdfRenderer(pfd)
            pdfDocument = PdfDocument()

            val totalPages = pdfRenderer.pageCount
            if (totalPages == 0) {
                throw IOException("Source PDF contains zero pages.")
            }

            for (pageIndex in 0 until totalPages) {
                val rendererPage = pdfRenderer.openPage(pageIndex)

                // Always use actual PDF page dimensions from PdfRenderer
                val pageWidthPoints = rendererPage.width.toFloat()
                val pageHeightPoints = rendererPage.height.toFloat()

                // Compute render scale capped by memory budget
                val desiredScale = 2.0f
                val rawPixels = pageWidthPoints * pageHeightPoints * (desiredScale * desiredScale)
                val effectiveScale = if (rawPixels > maxBitmapPixels) {
                    sqrt(maxBitmapPixels.toFloat() / (pageWidthPoints * pageHeightPoints))
                } else {
                    desiredScale
                }

                val bmpWidth = (pageWidthPoints * effectiveScale).toInt().coerceAtLeast(1)
                val bmpHeight = (pageHeightPoints * effectiveScale).toInt().coerceAtLeast(1)

                val pageBitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                // Paint white base so unpainted transparent regions do not render black,
                // and use RENDER_MODE_FOR_DISPLAY to avoid printer margin clipping.
                Canvas(pageBitmap).drawColor(Color.WHITE)
                rendererPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Start PdfDocument page
                val pageConfig = PdfDocument.PageInfo.Builder(
                    pageWidthPoints.toInt(),
                    pageHeightPoints.toInt(),
                    pageIndex + 1
                ).create()

                val documentPage = pdfDocument.startPage(pageConfig)

                try {
                    val fieldsOnPage = parsedForm.fields.filter { it.pageIndex == pageIndex }
                    val pageWarnings = composer().composePage(
                        canvas = documentPage.canvas,
                        pageBitmap = pageBitmap,
                        pageWidthPoints = pageWidthPoints,
                        pageHeightPoints = pageHeightPoints,
                        fieldsOnPage = fieldsOnPage,
                        answers = answers
                    )
                    allWarnings.addAll(pageWarnings)
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

        GenerationResult(warnings = allWarnings)
    }
}
