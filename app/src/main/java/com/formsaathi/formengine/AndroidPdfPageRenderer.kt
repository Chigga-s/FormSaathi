package com.formsaathi.formengine
import android.graphics.pdf.PdfRenderer
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import com.formsaathi.model.PageInfo

data class RenderedPage(
    val bitmap: Bitmap,
    val info: PageInfo
)

class AndroidPdfPageRenderer(
    private val context: Context
) {
    suspend fun renderPage(
        uri: Uri,
        pageIndex: Int
    ): RenderedPage {

        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?:throw IllegalArgumentException("Unable to open PDF file descriptor")
        
        return descriptor.use{ pfd->
            PdfRenderer(pfd).use{ renderer->
                    require(pageIndex >= 0 && pageIndex < renderer.pageCount) {
                        "Page index $pageIndex is out of bounds for PDF with ${renderer.pageCount} pages."
                    }
                    renderer.openPage(pageIndex).use{page->
                    val (bitmapWidth, bitmapHeight) = calculateBitmapSize(page.width, page.height)
                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    // PdfRenderer leaves unpainted regions transparent (alpha 0), which
                    // downstream consumers flatten to black: ML Kit then sees dark text
                    // on black and the PDF background prints black. An opaque white
                    // base guarantees dark-on-white for OCR and output alike.
                    Canvas(bitmap).drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val pageInfo = PageInfo(
                        pageIndex = pageIndex,
                        pdfWidthPoints = page.width.toFloat(),
                        pdfHeightPoints = page.height.toFloat(),
                        renderedWidthPx = bitmapWidth,
                        renderedHeightPx = bitmapHeight
                    )
                    RenderedPage(bitmap, pageInfo)
                }
             }
        }

        
        }
        suspend fun getPageCount(
            uri: Uri
        ): Int {

            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?:throw IllegalArgumentException("Unable to open PDF file descriptor")
            
            descriptor.use{ pfd->
                PdfRenderer(pfd).use{ renderer->
                    return renderer.pageCount
                }
            }
        }

        private fun calculateBitmapSize(
            pageWidth: Int,
            pageHeight: Int
        ): Pair<Int, Int> {
            val longestSide = maxOf(pageWidth, pageHeight)
            val scaleFactor = 1800f / longestSide.toFloat()
            val width = (pageWidth * scaleFactor).toInt()
            val height = (pageHeight * scaleFactor).toInt()
            return Pair(width, height)
    }
 }

    
